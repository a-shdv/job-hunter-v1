package com.company.parser.rabbitmq.service;

import com.company.parser.rabbitmq.dto.ReceiveMessageDto;
import com.company.parser.service.HabrParserService;
import com.company.parser.service.HhRuParserService;
import com.company.parser.service.RabotaRuParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@EnableRabbit
@Slf4j
public class RabbitMqReceiverService {
    private final HabrParserService habrParserService;
    private final HhRuParserService hhRuParserService;
    private final RabotaRuParserService rabotaRuParserService;

    @RabbitListener(queues = "${rabbitmq.queue-to-receive}")
    public void receive(ReceiveMessageDto receiveMessageDto) {
        log.info("RECEIVED: {}", receiveMessageDto.toString());
        CompletableFuture.allOf(
                habrParserService
                        .findAllVacancies(
                                receiveMessageDto.getTitle(),
                                receiveMessageDto.getAmount(),
                                receiveMessageDto.getSalary(),
                                receiveMessageDto.isOnlyWithSalary(),
                                receiveMessageDto.getExperience(),
                                receiveMessageDto.getCityId(),
                                receiveMessageDto.isRemoteAvailable()
                        ),
                hhRuParserService
                        .findAllVacancies(
                                receiveMessageDto.getTitle(),
                                receiveMessageDto.getAmount(),
                                receiveMessageDto.getSalary(),
                                receiveMessageDto.isOnlyWithSalary(),
                                receiveMessageDto.getExperience(),
                                receiveMessageDto.getCityId(),
                                receiveMessageDto.isRemoteAvailable()
                        ),
                rabotaRuParserService
                        .findAllVacancies(
                                receiveMessageDto.getTitle(),
                                receiveMessageDto.getAmount(),
                                receiveMessageDto.getSalary(),
                                receiveMessageDto.isOnlyWithSalary(),
                                receiveMessageDto.getExperience(),
                                receiveMessageDto.getCityId(),
                                receiveMessageDto.isRemoteAvailable()
                        )
        );
    }
}
