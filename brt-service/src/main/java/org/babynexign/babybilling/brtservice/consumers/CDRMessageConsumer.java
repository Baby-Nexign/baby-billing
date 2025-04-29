package org.babynexign.babybilling.brtservice.consumers;

import org.babynexign.babybilling.brtservice.dto.CallDTO;
import org.babynexign.babybilling.brtservice.service.CDRRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Consumer;

@Configuration
public class CDRMessageConsumer {

    private final CDRRecordService cdrRecordService;

    @Autowired
    public CDRMessageConsumer(CDRRecordService cdrRecordService) {
        this.cdrRecordService = cdrRecordService;
    }

    @Bean
    public Consumer<List<CallDTO>> cdrConsumer() {
        return cdrRecordService::processCDRs;
    }
}
