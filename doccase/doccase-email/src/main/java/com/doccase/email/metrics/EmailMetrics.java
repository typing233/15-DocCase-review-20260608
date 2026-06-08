package com.doccase.email.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class EmailMetrics {

    private final Counter archivedCounter;
    private final Counter duplicateCounter;
    private final Counter encryptedCounter;
    private final Counter errorCounter;
    private final Counter pollCounter;
    private final Counter pollErrorCounter;
    private final Timer pollTimer;

    public EmailMetrics(MeterRegistry meterRegistry) {
        this.archivedCounter = Counter.builder("email.attachments.archived").register(meterRegistry);
        this.duplicateCounter = Counter.builder("email.attachments.duplicate").register(meterRegistry);
        this.encryptedCounter = Counter.builder("email.attachments.encrypted").register(meterRegistry);
        this.errorCounter = Counter.builder("email.attachments.errors").register(meterRegistry);
        this.pollCounter = Counter.builder("email.polls.total").register(meterRegistry);
        this.pollErrorCounter = Counter.builder("email.polls.errors").register(meterRegistry);
        this.pollTimer = Timer.builder("email.poll.duration").register(meterRegistry);
    }

    public void recordArchived() { archivedCounter.increment(); }
    public void recordDuplicate() { duplicateCounter.increment(); }
    public void recordEncrypted() { encryptedCounter.increment(); }
    public void recordError() { errorCounter.increment(); }

    public void recordPoll(Long accountId, int messagesProcessed) {
        pollCounter.increment();
    }

    public void recordPollError(Long accountId) {
        pollErrorCounter.increment();
    }
}
