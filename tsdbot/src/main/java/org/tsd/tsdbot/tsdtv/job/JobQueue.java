package org.tsd.tsdbot.tsdtv.job;

import com.google.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.job.Job;
import org.tsd.rest.v1.tsdtv.job.JobResult;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class JobQueue {

    private static final Logger log = LoggerFactory.getLogger(JobQueue.class);

    private final Map<String, SubmittedJob> submittedJobs = new ConcurrentHashMap<>();

    public <JOB extends Job, RESULT extends JobResult> RESULT submitJob(String agentId, JOB job, long timeout)
            throws JobTimeoutException {
        SubmittedJob<JOB, RESULT> submittedJob = new SubmittedJob<>(agentId, job, timeout);
        submittedJobs.put(job.getId(), submittedJob);
        return submittedJob.waitForResult();
    }

    @SuppressWarnings("unchecked")
    public <T extends JobResult> void updateJobResult(T result) {
        log.info("Updating job result: {}", result);
        SubmittedJob submittedJob = submittedJobs.remove(result.getJobId());
        if (submittedJob == null) {
            log.error("Job ID {} does not exist in submitted jobs map", result.getJobId());
        } else {
            submittedJob.updateResult(result);
        }
    }

    public Job pollForJob(String agentId) {
        synchronized (submittedJobs) {
            log.debug("Polling jobs for agent: {}", agentId);
            List<Map.Entry<String, SubmittedJob>> jobsForAgent = submittedJobs.entrySet()
                    .stream()
                    .filter(entry -> StringUtils.equals(entry.getValue().getAgentId(), agentId))
                    .filter(entry -> entry.getValue().getTaken() == null)
                    .sorted(Comparator.comparing(entry -> entry.getValue().getCreated().toEpochSecond(ZoneOffset.UTC)))
                    .collect(Collectors.toList());
            log.debug("Found jobs for agent {}: {}", agentId, jobsForAgent);
            if (CollectionUtils.isNotEmpty(jobsForAgent)) {
                SubmittedJob submittedJob = submittedJobs.get(jobsForAgent.get(0).getKey());
                submittedJob.take();
                return submittedJob.getJob();
            }
            return null;
        }
    }

    public void handleOfflineAgent(String agentId) {
        synchronized (submittedJobs) {
            log.warn("Removing jobs for offline agent: {}", agentId);
            submittedJobs.entrySet()
                    .removeIf(entry -> StringUtils.equals(entry.getValue().getAgentId(), agentId));
        }
    }
}
