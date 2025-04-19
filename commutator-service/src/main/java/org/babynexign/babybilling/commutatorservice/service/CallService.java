package org.babynexign.babybilling.commutatorservice.service;

import org.babynexign.babybilling.commutatorservice.entity.Call;
import org.babynexign.babybilling.commutatorservice.entity.Subscriber;
import org.babynexign.babybilling.commutatorservice.entity.enums.CallType;
import org.babynexign.babybilling.commutatorservice.repository.CallRepository;
import org.babynexign.babybilling.commutatorservice.repository.SubscriberRepository;
import org.babynexign.babybilling.commutatorservice.sender.CDRSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CallService {
    private static final int MIN_CALLS_TO_GENERATE = 500;
    private static final int MAX_CALLS_TO_GENERATE = 1000;
    private static final int MAX_THREAD_COUNT = 10;
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 60;
    private static final int MIN_CALL_DURATION_SECONDS = 10;
    private static final int MAX_CALL_DURATION_SECONDS = 86400;
    private static final int CDR_BATCH_SIZE = 10;
    private static final int HISTORY_YEARS = 1;

    private final SubscriberRepository subscriberRepository;
    private final CallRepository callRepository;
    private final CDRSender sender;
    private final ConcurrentMap<Long, Object> lockObjects = new ConcurrentHashMap<>();

    @Autowired
    public CallService(SubscriberRepository subscriberRepository, CallRepository callRepository, CDRSender sender) {
        this.subscriberRepository = subscriberRepository;
        this.callRepository = callRepository;
        this.sender = sender;
    }

    public void generateCDRecords() {
        List<Subscriber> subscribers = subscriberRepository.findAll();
        if (subscribers.isEmpty()) {
            throw new IllegalStateException("No subscribers found. Cannot generate CDR records");
        }

        LocalDateTime startDate = LocalDateTime.now().minusYears(HISTORY_YEARS);
        LocalDateTime endDate = LocalDateTime.now();
        int numberOfCalls = ThreadLocalRandom.current().nextInt(
                MIN_CALLS_TO_GENERATE,
                MAX_CALLS_TO_GENERATE + 1
        );

        int processors = Runtime.getRuntime().availableProcessors();
        int numThreads = Math.min(MAX_THREAD_COUNT, processors);
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        ConcurrentMap<Long, Set<TimeRange>> subscriberBusyPeriods = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfCalls; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    generateCallPair(subscribers, startDate, endDate, subscriberBusyPeriods);
                } catch (Exception e) {
                    throw new RuntimeException("Error generating call records", e);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error generating calls", e);
            }
        }

        sendGeneratedCDRecords();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void sendGeneratedCDRecords() {
        int page = 0;
        Pageable pageable = PageRequest.of(page, CDR_BATCH_SIZE);
        Page<Call> recordPage;

        do {
            recordPage = callRepository.findAllByOrderByCallStartAsc(pageable);

            if (recordPage.hasContent()) {
                sender.sendCDR(new ArrayList<>(recordPage.getContent()));
            }

            pageable = recordPage.nextPageable();
        } while (recordPage.hasNext());
    }

    private Object getLockObject(Long subscriberId) {
        return lockObjects.computeIfAbsent(subscriberId, k -> new Object());
    }

    private void generateCallPair(
            List<Subscriber> subscribers,
            LocalDateTime startDate,
            LocalDateTime endDate,
            ConcurrentMap<Long, Set<TimeRange>> subscriberBusyPeriods) {

        Random random = new Random();

        Subscriber caller = null;
        Subscriber receiver = null;
        LocalDateTime callStart = null;
        LocalDateTime callEnd = null;
        boolean validCallFound = false;

        while (!validCallFound) {
            caller = subscribers.get(random.nextInt(subscribers.size()));

            do {
                receiver = subscribers.get(random.nextInt(subscribers.size()));
            } while (receiver.getMsisdn().equals(caller.getMsisdn()));

            callStart = getRandomDateTimeInRange(startDate, endDate);
            callEnd = addRandomDuration(
                    callStart,
                    MIN_CALL_DURATION_SECONDS,
                    MAX_CALL_DURATION_SECONDS
            );

            TimeRange newCallRange = new TimeRange(callStart, callEnd);

            Long lockId1 = caller.getMsisdn();
            Long lockId2 = receiver.getMsisdn();

            if (lockId1 > lockId2) {
                Long temp = lockId1;
                lockId1 = lockId2;
                lockId2 = temp;
            }

            synchronized (getLockObject(lockId1)) {
                synchronized (getLockObject(lockId2)) {
                    if (isSubscriberFree(caller.getMsisdn(), newCallRange, subscriberBusyPeriods) &&
                            isSubscriberFree(receiver.getMsisdn(), newCallRange, subscriberBusyPeriods)) {
                        markSubscriberBusy(caller.getMsisdn(), newCallRange, subscriberBusyPeriods);
                        markSubscriberBusy(receiver.getMsisdn(), newCallRange, subscriberBusyPeriods);
                        validCallFound = true;
                    }
                }
            }
        }

        List<TimeRange> splitRanges = splitCallAcrossDays(callStart, callEnd);

        for (TimeRange range : splitRanges) {
            Call outgoingCall = Call.builder()
                    .callType(CallType.OUTCOMING)
                    .callingSubscriber(caller)
                    .receivingSubscriber(receiver)
                    .callStart(range.start())
                    .callEnd(range.end())
                    .build();

            Call incomingCall = Call.builder()
                    .callType(CallType.INCOMING)
                    .callingSubscriber(receiver)
                    .receivingSubscriber(caller)
                    .callStart(range.start())
                    .callEnd(range.end())
                    .build();

            callRepository.save(outgoingCall);
            callRepository.save(incomingCall);
        }
    }

    private boolean isSubscriberFree(
            Long msisdn,
            TimeRange newCallRange,
            ConcurrentMap<Long, Set<TimeRange>> subscriberBusyPeriods) {

        Set<TimeRange> busyPeriods = subscriberBusyPeriods.getOrDefault(msisdn, Collections.emptySet());

        for (TimeRange busyRange : busyPeriods) {
            if (busyRange.overlaps(newCallRange)) {
                return false;
            }
        }

        return true;
    }

    private void markSubscriberBusy(
            Long msisdn,
            TimeRange newCallRange,
            ConcurrentMap<Long, Set<TimeRange>> subscriberBusyPeriods) {

        subscriberBusyPeriods.computeIfAbsent(msisdn, k -> ConcurrentHashMap.newKeySet())
                .add(newCallRange);
    }

    private List<TimeRange> splitCallAcrossDays(LocalDateTime callStart, LocalDateTime callEnd) {
        List<TimeRange> result = new ArrayList<>();

        if (callStart.toLocalDate().equals(callEnd.toLocalDate())) {
            result.add(new TimeRange(callStart, callEnd));
            return result;
        }

        LocalDateTime currentStart = callStart;

        while (!currentStart.toLocalDate().equals(callEnd.toLocalDate())) {
            LocalDateTime dayEnd = LocalDateTime.of(
                    currentStart.toLocalDate(),
                    LocalTime.of(23, 59, 59)
            );

            result.add(new TimeRange(currentStart, dayEnd));

            currentStart = LocalDateTime.of(
                    currentStart.toLocalDate().plusDays(1),
                    LocalTime.of(0, 0, 0)
            );
        }

        if (!currentStart.isAfter(callEnd)) {
            result.add(new TimeRange(currentStart, callEnd));
        }

        return result;
    }

    private LocalDateTime getRandomDateTimeInRange(LocalDateTime start, LocalDateTime end) {
        long startEpochSecond = start.toEpochSecond(ZoneOffset.UTC);
        long endEpochSecond = end.toEpochSecond(ZoneOffset.UTC);
        long randomEpochSecond = ThreadLocalRandom.current().nextLong(startEpochSecond, endEpochSecond);
        return LocalDateTime.ofEpochSecond(randomEpochSecond, 0, ZoneOffset.UTC);
    }

    private LocalDateTime addRandomDuration(LocalDateTime dateTime, int minSeconds, int maxSeconds) {
        int randomSeconds = ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds + 1);
        return dateTime.plusSeconds(randomSeconds);
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {
        public boolean overlaps(TimeRange other) {
            return !this.start.isAfter(other.end) && !this.end.isBefore(other.start);
        }
    }
}
