package org.babynexign.babybilling.authservice.client;

import org.babynexign.babybilling.authservice.dto.PersonDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "brt-service")
public interface BrtClient {

    @GetMapping("/api/person/{msisdn}")
    ResponseEntity<PersonDTO> getPersonByMsisdn(@PathVariable("msisdn") String msisdn);
}
