package org.babynexign.babybilling.brtservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.babynexign.babybilling.brtservice.dto.PersonDTO;
import org.babynexign.babybilling.brtservice.dto.request.ChangePersonTariffRequest;
import org.babynexign.babybilling.brtservice.dto.request.CreatePersonRequest;
import org.babynexign.babybilling.brtservice.dto.request.TariffPaymentRequest;
import org.babynexign.babybilling.brtservice.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/person")
public class PersonController {
    private final PersonService personService;

    @Autowired
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping
    public ResponseEntity<PersonDTO> createPerson(@Valid @RequestBody CreatePersonRequest createPersonRequest) {
        PersonDTO personDTO = personService.createPerson(createPersonRequest);
        return new ResponseEntity<>(personDTO, HttpStatus.CREATED);
    }

    @PutMapping("/{msisdn}/tariff")
    public ResponseEntity<Void> changePersonTariff(@PathVariable("msisdn") @Pattern(regexp = "^[0-9]{11}$") String personMsisdn, @Valid @RequestBody ChangePersonTariffRequest changePersonTariffRequest) {
        personService.changePersonTariff(personMsisdn, changePersonTariffRequest);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{msisdn}/balance")
    public ResponseEntity<Void> replenishBalance(
            @PathVariable("msisdn") @Pattern(regexp = "^[0-9]{11}$") String msisdn,
            @RequestParam("money") @NotNull Long money) {
        personService.replenishBalance(msisdn, money);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{msisdn}")
    public ResponseEntity<PersonDTO> getPersonByMsisdn(@PathVariable("msisdn") @Pattern(regexp = "^[0-9]{11}$") String msisdn) {
        PersonDTO personDTO = personService.getPersonByMsisdn(msisdn);
        return ResponseEntity.ok(personDTO);
    }

    @PostMapping("/tariff-payment")
    public ResponseEntity<Void> tariffPayment(@Valid @RequestBody TariffPaymentRequest tariffPaymentRequest){
        personService.withdrawTariffPayment(tariffPaymentRequest);
        return ResponseEntity.ok().build();
    }
}
