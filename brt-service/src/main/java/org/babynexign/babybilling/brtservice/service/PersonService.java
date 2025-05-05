package org.babynexign.babybilling.brtservice.service;

import jakarta.persistence.EntityNotFoundException;
import org.babynexign.babybilling.brtservice.dto.*;
import org.babynexign.babybilling.brtservice.dto.billing.BillingResponse;
import org.babynexign.babybilling.brtservice.dto.commutator.CallRestrictionRequest;
import org.babynexign.babybilling.brtservice.dto.request.ChangePersonTariffRequest;
import org.babynexign.babybilling.brtservice.dto.request.CreatePersonRequest;
import org.babynexign.babybilling.brtservice.dto.commutator.NewSubscriberRequest;
import org.babynexign.babybilling.brtservice.dto.request.TariffInformationRequest;
import org.babynexign.babybilling.brtservice.dto.response.TariffInformationResponse;
import org.babynexign.babybilling.brtservice.entity.ExtraService;
import org.babynexign.babybilling.brtservice.entity.Person;
import org.babynexign.babybilling.brtservice.entity.QuantService;
import org.babynexign.babybilling.brtservice.entity.Tariff;
import org.babynexign.babybilling.brtservice.entity.enums.QuantServiceType;
import org.babynexign.babybilling.brtservice.repository.PersonRepository;
import org.babynexign.babybilling.brtservice.senders.CommutatorSender;
import org.babynexign.babybilling.brtservice.senders.HrsSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    public void processBillingResponse(BillingResponse billingResponse) {
        Person subscriber = personRepository.findById(billingResponse.personId())
                .orElseThrow(() -> new EntityNotFoundException("Person with id " + billingResponse.personId() + " not found"));
        long newBalance = subscriber.getBalance() - billingResponse.payment();

        if (newBalance < 0) {
            commutatorSender.sendCallRestrictionRequest(new CallRestrictionRequest(subscriber.getMsisdn(), true));
            subscriber.setIsRestricted(true);
        }
        subscriber.setBalance(newBalance);

        personRepository.save(subscriber);
    }

    public PersonDTO createPerson(CreatePersonRequest createPersonRequest) {
        Tariff baseTariff = Tariff.builder()
                .tariffId(11L)
                .startDate(LocalDateTime.now())
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
                .registrationDate(LocalDateTime.now())
                .msisdn(createPersonRequest.msisdn())
                .tariff(baseTariff)
                .isRestricted(false)
                .quantServices(baseQuantServices)
                .build();

        PersonDTO newSubscriberDto = PersonDTO.fromEntity(personRepository.save(newSubscriber));
        commutatorSender.sendNewSubscriberRequest(new NewSubscriberRequest(createPersonRequest.msisdn()));
        return newSubscriberDto;
    }

    public void changePersonTariff(ChangePersonTariffRequest changePersonTariffRequest) {
        Person subscriber = personRepository.findByMsisdn(changePersonTariffRequest.personMsisdn())
                .orElseThrow(() -> new EntityNotFoundException("Person with MSISDN " + changePersonTariffRequest.personMsisdn() + " not found"));
        hrsSender.sendTariffInformationRequest(
                new TariffInformationRequest(subscriber.getId(), changePersonTariffRequest.newTariffId()));
    }

    public void processChangePersonTariff(TariffInformationResponse tariffInformationResponse) {
        Person subscriber = personRepository.findById(tariffInformationResponse.personId())
                .orElseThrow(() -> new EntityNotFoundException("Person with id " + tariffInformationResponse.personId() + " not found"));

        List<QuantService> newQuantServices = tariffInformationResponse.quantServices().stream()
                .map(service -> QuantService.builder()
                        .serviceType(QuantServiceType.valueOf(service.serviceType()))
                        .amountLeft(service.amountLeft())
                        .build())
                .toList();

        List<ExtraService> newExtraServices = tariffInformationResponse.extraServices().stream()
                .map(service -> ExtraService.builder().extraServiceId(service.extraServiceId()).startDate(LocalDateTime.now()).build())
                .toList();

        subscriber.setQuantServices(newQuantServices);
        subscriber.setExtraServices(newExtraServices);

        Tariff newTariff = subscriber.getTariff();
        newTariff.setTariffId(tariffInformationResponse.tariffId());
        newTariff.setStartDate(LocalDateTime.now());
        subscriber.setTariff(newTariff);

        personRepository.save(subscriber);
    }

    public void replenishBalance(String personMsisdn, Long money) {
        Person subscriber = personRepository.findByMsisdn(personMsisdn)
                .orElseThrow(() -> new EntityNotFoundException("Person with id " + personMsisdn + " not found"));

        if (subscriber.getBalance() < 0 && subscriber.getBalance() + money >= 0){
            commutatorSender.sendCallRestrictionRequest(new CallRestrictionRequest(subscriber.getMsisdn(), false));
        }

        subscriber.setBalance(subscriber.getBalance() + money);
        personRepository.save(subscriber);
    }

    public PersonDTO getPersonByMsisdn(String personMsisdn) {
        Person person = personRepository.findByMsisdn(personMsisdn)
                .orElseThrow(() -> new EntityNotFoundException("Person with MSISDN " + personMsisdn + " not found"));

        return PersonDTO.fromEntity(person);
    }
}
