package org.babynexign.babybilling.hrsservice.controller;

import org.babynexign.babybilling.hrsservice.dto.CreateServiceRequest;
import org.babynexign.babybilling.hrsservice.dto.ServiceDTO;
import org.babynexign.babybilling.hrsservice.service.OperatorServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final OperatorServiceService operatorServiceService;

    @Autowired
    public ServiceController(OperatorServiceService operatorServiceService) {
        this.operatorServiceService = operatorServiceService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceDTO>> getAllServices(){
       List<ServiceDTO> services = operatorServiceService.getAllServices();
       return ResponseEntity.ok(services);
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceDTO> getService(@PathVariable Long serviceId){
        ServiceDTO service = operatorServiceService.getOneService(serviceId);
        return ResponseEntity.ok(service);
    }

    @PostMapping
    public ResponseEntity<ServiceDTO> createService(@RequestBody CreateServiceRequest createServiceRequest){
        ServiceDTO service = operatorServiceService.createService(createServiceRequest);
        return ResponseEntity.ok(service);
    }
}
