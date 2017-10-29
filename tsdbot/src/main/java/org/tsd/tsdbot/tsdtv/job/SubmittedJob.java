package org.tsd.tsdbot.tsdtv.job;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.job.Job;
import org.tsd.rest.v1.tsdtv.job.JobResult;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SubmittedJob<JOB extends Job, RESULT extends JobResult> {

    private static final Logger log = LoggerFactory.getLogger(SubmittedJob.class);

    private final String agentId;
    private final LocalDateTime created;
    private final JOB job;
    private final long timeout;

    private final Lock lock = new ReentrantLock();
    private final Condition pendingResult = lock.newCondition();

    private LocalDateTime taken = null;
    private RESULT result = null;

    public SubmittedJob(String agentId, JOB job, long timeout) {
        this.agentId = agentId;
        this.job = job;
        this.timeout = timeout;
        this.created = LocalDateTime.now();
    }

    public RESULT waitForResult() throws JobTimeoutException {
        lock.lock();
        try {
            long expirationTime = System.currentTimeMillis() + timeout;
            while (result == null && System.currentTimeMillis() < expirationTime) {
                try {
                    pendingResult.await(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.info("Interrupted, jobId={}", job.getId());
                }
            }

            if (result == null) {
                throw new JobTimeoutException();
            }

            return result;
        } finally {
            lock.unlock();
        }
    }

    public void updateResult(RESULT result) {
        lock.lock();
        try {
            this.result = result;
            pendingResult.signal();
        } finally {
            lock.unlock();
        }
    }

    public JOB getJob() {
        return job;
    }

    public void take() {
        this.taken = LocalDateTime.now();
    }

    public LocalDateTime getTaken() {
        return taken;
    }

    public String getAgentId() {
        return agentId;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("agentId", agentId)
                .append("created", created)
                .append("job", job.getClass())
                .append("timeout", timeout)
                .append("taken", taken)
                .append("result", result)
                .toString();
    }
}
