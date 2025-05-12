package org.babynexign.babybilling.hrsservice.service;

import org.babynexign.babybilling.hrsservice.dto.ServiceDTO;
import org.babynexign.babybilling.hrsservice.exception.ServiceNotFoundException;
import org.babynexign.babybilling.hrsservice.exception.ServiceValidationException;
import org.babynexign.babybilling.hrsservice.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing telecom operator services.
 * Provides operations for retrieving service information.
 */
@Service
public class OperatorServiceService {
    private final ServiceRepository serviceRepository;

    @Autowired
    public OperatorServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<ServiceDTO> getAllServices(){
        return serviceRepository.findAll().stream().map(ServiceDTO::fromEntity).toList();
    }

    public ServiceDTO getOneService(Long id){
        if (id == null) {
            throw new ServiceValidationException("Service ID cannot be null");
        }
        return ServiceDTO.fromEntity(serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException("Service with ID " + id + " not found")));
    }
}
