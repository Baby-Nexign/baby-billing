package org.babynexign.babybilling.commutatorservice.sender.impl;

import org.babynexign.babybilling.commutatorservice.config.RabbitMQConfig;
import org.babynexign.babybilling.commutatorservice.dto.RecordDTO;
import org.babynexign.babybilling.commutatorservice.entity.Record;
import org.babynexign.babybilling.commutatorservice.sender.CDRSender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RabbitCDRSender implements CDRSender {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public RabbitCDRSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendCDR(List<Record> calls) {
        if (calls != null && !calls.isEmpty()) {
            List<RecordDTO> recordDTOs = calls.stream()
                    .map(RecordDTO::fromEntity)
                    .collect(Collectors.toList());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CDR_EXCHANGE_NAME,
                    RabbitMQConfig.CDR_ROUTING_KEY,
                    recordDTOs
            );
        }
    }
}
