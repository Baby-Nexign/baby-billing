package org.babynexign.babybilling.hrsservice.service;

import org.babynexign.babybilling.hrsservice.dto.CreateServiceRequest;
import org.babynexign.babybilling.hrsservice.dto.ServiceDTO;
import org.babynexign.babybilling.hrsservice.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return ServiceDTO.fromEntity(serviceRepository.findById(id).orElseThrow());
    }

    public ServiceDTO createService(CreateServiceRequest createServiceRequest){
        // TODO: implement
        return new ServiceDTO(null,null, null, null, null, null, null, null, null, null);
    }
}
