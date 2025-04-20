package org.babynexign.babybilling.brtservice.listeners;

import org.babynexign.babybilling.brtservice.dto.CallDTO;
import org.babynexign.babybilling.brtservice.service.CDRRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
public class CDRMessageListener {

    private final CDRRecordService cdrRecordService;

    @Autowired
    public CDRMessageListener(CDRRecordService cdrRecordService) {
        this.cdrRecordService = cdrRecordService;
    }

    @Bean
    public Consumer<List<CallDTO>> cdrConsumer() {
        return cdrRecordService::processCDRs;
    }
}
