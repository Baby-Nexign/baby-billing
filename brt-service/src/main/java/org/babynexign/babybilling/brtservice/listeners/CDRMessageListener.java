package org.babynexign.babybilling.brtservice.listeners;

import org.babynexign.babybilling.brtservice.dto.CallDTO;
import org.babynexign.babybilling.brtservice.service.CDRRecordService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CDRMessageListener {

    private final CDRRecordService cdrRecordService;

    @Autowired
    public CDRMessageListener(CDRRecordService cdrRecordService) {
        this.cdrRecordService = cdrRecordService;
    }

    @RabbitListener(queues = "cdr.processing.brt")
    public void receiveCDRs(List<CallDTO> callDTOs) {
        cdrRecordService.processCDRs(callDTOs);
    }
}
