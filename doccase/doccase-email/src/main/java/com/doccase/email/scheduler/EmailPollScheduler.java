package com.doccase.email.scheduler;

import com.doccase.email.service.EmailPollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailPollScheduler {

    private final EmailPollingService emailPollingService;

    @Scheduled(fixedDelayString = "${email.poll.default-interval-minutes:15}000", initialDelay = 30000)
    public void scheduledPoll() {
        log.info("Starting scheduled email poll");
        emailPollingService.pollAllAccounts();
    }
}
