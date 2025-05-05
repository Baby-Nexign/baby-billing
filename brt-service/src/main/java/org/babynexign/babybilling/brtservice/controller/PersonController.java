package org.babynexign.babybilling.brtservice.controller;

import org.babynexign.babybilling.brtservice.dto.PersonDTO;
import org.babynexign.babybilling.brtservice.dto.request.ChangePersonTariffRequest;
import org.babynexign.babybilling.brtservice.dto.request.CreatePersonRequest;
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
    public ResponseEntity<PersonDTO> createPerson(@RequestBody CreatePersonRequest createPersonRequest) {
        PersonDTO personDTO = personService.createPerson(createPersonRequest);
        return new ResponseEntity<>(personDTO, HttpStatus.CREATED);
    }

    @PutMapping("/tariff")
    public ResponseEntity<Void> changePersonTariff(@RequestBody ChangePersonTariffRequest changePersonTariffRequest) {
        personService.changePersonTariff(changePersonTariffRequest);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{msisdn}/balance")
    public ResponseEntity<Void> replenishBalance(
            @PathVariable("msisdn") String msisdn,
            @RequestParam("money") Long money) {
        personService.replenishBalance(msisdn, money);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{msisdn}")
    public ResponseEntity<PersonDTO> getPersonByMsisdn(@PathVariable("msisdn") String msisdn) {
        PersonDTO personDTO = personService.getPersonByMsisdn(msisdn);
        return ResponseEntity.ok(personDTO);
    }
}
