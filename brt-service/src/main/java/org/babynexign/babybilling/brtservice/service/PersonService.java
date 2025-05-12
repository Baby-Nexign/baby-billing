package org.babynexign.babybilling.brtservice.service;

import org.babynexign.babybilling.brtservice.dto.PersonDTO;
import org.babynexign.babybilling.brtservice.dto.billing.BillingResponse;
import org.babynexign.babybilling.brtservice.dto.commutator.CallRestrictionRequest;
import org.babynexign.babybilling.brtservice.dto.commutator.NewSubscriberRequest;
import org.babynexign.babybilling.brtservice.dto.request.*;
import org.babynexign.babybilling.brtservice.dto.response.CountTariffPaymentResponse;
import org.babynexign.babybilling.brtservice.dto.response.TariffInformationResponse;
import org.babynexign.babybilling.brtservice.entity.ExtraService;
import org.babynexign.babybilling.brtservice.entity.Person;
import org.babynexign.babybilling.brtservice.entity.QuantService;
import org.babynexign.babybilling.brtservice.entity.Tariff;
import org.babynexign.babybilling.brtservice.entity.enums.QuantServiceType;
import org.babynexign.babybilling.brtservice.exception.MsisdnAlreadyExistsException;
import org.babynexign.babybilling.brtservice.exception.SubscriberNotFoundException;
import org.babynexign.babybilling.brtservice.repository.PersonRepository;
import org.babynexign.babybilling.brtservice.senders.CommutatorSender;
import org.babynexign.babybilling.brtservice.senders.HrsSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing subscriber operations.
 * Handles billing, tariff changes, balance operations and subscriber management.
 */
@Service
public class PersonService {
    private final PersonRepository personRepository;
    private final CommutatorSender commutatorSender;
    private final HrsSender hrsSender;

    @Autowired
    public PersonService(PersonRepository personRepository, CommutatorSender commutatorSender,
                         HrsSender hrsSender) {
        this.personRepository = personRepository;
        this.commutatorSender = commutatorSender;
        this.hrsSender = hrsSender;
    }

    /**
     * Processes a billing response by updating the subscriber's balance.
     * If the new balance is negative, it restricts the subscriber's calls.
     *
     * @param billingResponse The billing response containing the person ID and payment amount
     * @throws SubscriberNotFoundException if the person with the specified ID is not found
     */
    public void processBillingResponse(BillingResponse billingResponse) {
        Person subscriber = personRepository.findById(billingResponse.personId())
                .orElseThrow(() -> new SubscriberNotFoundException("Person with id " + billingResponse.personId() + " not found"));
        long newBalance = subscriber.getBalance() - billingResponse.payment();

        if (newBalance < 0) {
            commutatorSender.sendCallRestrictionRequest(new CallRestrictionRequest(subscriber.getMsisdn(), true));
            subscriber.setIsRestricted(true);
        }
        subscriber.setBalance(newBalance);

        personRepository.save(subscriber);
    }

    /**
     * Creates a new subscriber with default tariff and services.
     *
     * @param createPersonRequest Request containing new person information
     * @return PersonDTO with created person information
     * @throws MsisdnAlreadyExistsException if the MSISDN is already in use
     */
    public PersonDTO createPerson(CreatePersonRequest createPersonRequest) {
        if (personRepository.findByMsisdn(createPersonRequest.msisdn()).isPresent()) {
            throw new MsisdnAlreadyExistsException("Subscriber with MSISDN " + createPersonRequest.msisdn() + " already exists");
        }

        Tariff baseTariff = Tariff.builder()
                .tariffId(11L)
                .startDate(LocalDate.now())
                .build();

        List<QuantService> baseQuantServices = List.of(QuantService
                .builder()
                .serviceType(QuantServiceType.MINUTES)
                .amountLeft(0L)
                .build());

        Person newSubscriber = Person
                .builder()
                .name(createPersonRequest.name())
                .balance(100L)
                .description(createPersonRequest.description())
                .msisdn(createPersonRequest.msisdn())
                .tariff(baseTariff)
                .isRestricted(false)
                .quantServices(baseQuantServices)
                .registrationDate(LocalDateTime.now())
                .build();

        PersonDTO newSubscriberDto = PersonDTO.fromEntity(personRepository.save(newSubscriber));
        commutatorSender.sendNewSubscriberRequest(new NewSubscriberRequest(createPersonRequest.msisdn()));
        return newSubscriberDto;
    }

    /**
     * Initiates a tariff change for a subscriber by sending a tariff information request.
     *
     * @param msisdn The MSISDN of the subscriber whose tariff is being changed
     * @param changePersonTariffRequest The request containing the new tariff ID
     * @throws SubscriberNotFoundException if no subscriber with the given MSISDN is found
     */
    public void changePersonTariff(String msisdn, ChangePersonTariffRequest changePersonTariffRequest) {
        Person subscriber = personRepository.findByMsisdn(msisdn)
                .orElseThrow(() -> new SubscriberNotFoundException("Person with MSISDN " + msisdn + " not found"));
        hrsSender.sendTariffInformationRequest(
                new TariffInformationRequest(subscriber.getId(), changePersonTariffRequest.newTariff()));
    }

    /**
     * Processes a tariff change response by updating the subscriber's tariff and services.
     * This method is called after receiving a tariff information response from the HRS service.
     *
     * @param tariffInformationResponse The response containing tariff and service information
     * @throws SubscriberNotFoundException if the person with the specified ID is not found
     */
    public void processChangePersonTariff(TariffInformationResponse tariffInformationResponse) {
        Person subscriber = personRepository.findById(tariffInformationResponse.personId())
                .orElseThrow(() -> new SubscriberNotFoundException("Person with id " + tariffInformationResponse.personId() + " not found"));

        List<QuantService> newQuantServices = tariffInformationResponse.quantServices().stream()
                .map(service -> QuantService.builder()
                        .serviceType(QuantServiceType.valueOf(service.serviceType()))
                        .amountLeft(service.amountLeft())
                        .build())
                .toList();

        List<ExtraService> newExtraServices = tariffInformationResponse.extraServices().stream()
                .map(service -> ExtraService.builder().extraServiceId(service.extraServiceId()).startDate(LocalDate.now()).build())
                .toList();

        subscriber.setQuantServices(newQuantServices);
        subscriber.setExtraServices(newExtraServices);

        Tariff newTariff = subscriber.getTariff();
        newTariff.setTariffId(tariffInformationResponse.tariffId());
        newTariff.setStartDate(LocalDate.now());
        subscriber.setTariff(newTariff);

        personRepository.save(subscriber);
    }

    /**
     * Adds money to a subscriber's balance and removes call restrictions if the balance becomes positive.
     *
     * @param personMsisdn The MSISDN of the subscriber whose balance is being replenished
     * @param money The amount of money to add to the balance
     * @throws SubscriberNotFoundException if no subscriber with the given MSISDN is found
     */
    public void replenishBalance(String personMsisdn, Long money) {
        Person subscriber = personRepository.findByMsisdn(personMsisdn)
                .orElseThrow(() -> new SubscriberNotFoundException("Person with MSISDN " + personMsisdn + " not found"));

        if (subscriber.getBalance() < 0 && subscriber.getBalance() + money >= 0) {
            commutatorSender.sendCallRestrictionRequest(new CallRestrictionRequest(subscriber.getMsisdn(), false));
            subscriber.setIsRestricted(false);
        }

        subscriber.setBalance(subscriber.getBalance() + money);
        personRepository.save(subscriber);
    }

    /**
     * Retrieves a subscriber by their MSISDN number.
     *
     * @param personMsisdn The MSISDN number to search for
     * @return PersonDTO with the person's information
     * @throws SubscriberNotFoundException if no person with the given MSISDN exists
     */
    public PersonDTO getPersonByMsisdn(String personMsisdn) {
        Person person = personRepository.findByMsisdn(personMsisdn)
                .orElseThrow(() -> new SubscriberNotFoundException("Person with MSISDN " + personMsisdn + " not found"));

        return PersonDTO.fromEntity(person);
    }

    /**
     * Initiates tariff payment withdrawal for all non-restricted subscribers.
     * Sends payment calculation requests to the HRS service for each eligible subscriber.
     *
     * @param tariffPaymentRequest The request containing the current date for payment calculations
     */
    public void withdrawTariffPayment(TariffPaymentRequest tariffPaymentRequest) {
        List<Person> allPersons = personRepository.findAllByIsRestrictedFalse();
        for (Person person : allPersons) {
            hrsSender.sendCountTariffPaymentRequest(new CountTariffPaymentRequest(person.getId(), person.getTariff().getTariffId(), person.getTariff().getStartDate(), tariffPaymentRequest.currentDate()));
        }
    }

    /**
     * Processes a tariff payment calculation response.
     * Updates the subscriber's balance based on the calculated cost and refreshes their tariff.
     *
     * @param countTariffPaymentResponse The response containing the person ID and calculated cost
     * @throws SubscriberNotFoundException if the person with the specified ID is not found
     */
    public void processCountTariffPaymentResponse(CountTariffPaymentResponse countTariffPaymentResponse) {
        processBillingResponse(new BillingResponse(countTariffPaymentResponse.personId(), countTariffPaymentResponse.cost()));
        Person person = personRepository.findById(countTariffPaymentResponse.personId())
                .orElseThrow(() -> new SubscriberNotFoundException("Person with ID " + countTariffPaymentResponse.personId() + " not found"));
        changePersonTariff(person.getMsisdn(), new ChangePersonTariffRequest(person.getTariff().getTariffId()));
    }
}
