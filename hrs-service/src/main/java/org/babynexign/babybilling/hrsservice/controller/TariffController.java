package org.babynexign.babybilling.hrsservice.controller;

import jakarta.validation.Valid;
import org.babynexign.babybilling.hrsservice.dto.CreateTariffRequest;
import org.babynexign.babybilling.hrsservice.dto.TariffDTO;
import org.babynexign.babybilling.hrsservice.service.TariffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
public class TariffController {
    private final TariffService tariffService;

    @Autowired
    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @GetMapping
    public ResponseEntity<List<TariffDTO>> getAllTariffs(){
       List<TariffDTO> tariffs = tariffService.getAllTariffs();
       return ResponseEntity.ok(tariffs);
    }

    @GetMapping("/{tariffId}")
    public ResponseEntity<TariffDTO> getTariff(@PathVariable Long tariffId){
        TariffDTO tariff = tariffService.getOneTariff(tariffId);
        return ResponseEntity.ok(tariff);
    }

    @PostMapping
    public ResponseEntity<TariffDTO> createTariff(@Valid @RequestBody CreateTariffRequest createTariffRequest){
        TariffDTO tariff = tariffService.createTariff(createTariffRequest);
        return ResponseEntity.ok(tariff);
    }
}
