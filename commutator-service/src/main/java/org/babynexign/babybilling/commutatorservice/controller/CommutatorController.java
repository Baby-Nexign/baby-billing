package org.babynexign.babybilling.commutatorservice.controller;

import org.babynexign.babybilling.commutatorservice.service.CallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commutator")
public class CommutatorController {

    private final CallService callService;

    @Autowired
    public CommutatorController(CallService callService) {
        this.callService = callService;
    }

    /**
     * Endpoint to trigger generation of call detail records.
     * Each time this endpoint is called, the year range shifts by one.
     *
     * @return Response with status information
     */
    @PostMapping("/generate-cdr")
    public ResponseEntity<String> generateCallRecords() {
        callService.generateCDRecords();
        return ResponseEntity.ok("CDR generation completed successfully.");
    }
}
