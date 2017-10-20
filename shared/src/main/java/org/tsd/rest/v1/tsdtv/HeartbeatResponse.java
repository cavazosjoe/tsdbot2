package org.tsd.rest.v1.tsdtv;

import org.tsd.rest.v1.tsdtv.job.Job;

import java.util.LinkedList;
import java.util.List;

public class HeartbeatResponse {
    private int sleepSeconds;
    private List<Job> jobsToExecute = new LinkedList<>();

    public int getSleepSeconds() {
        return sleepSeconds;
    }

    public void setSleepSeconds(int sleepSeconds) {
        this.sleepSeconds = sleepSeconds;
    }

    public List<Job> getJobsToExecute() {
        return jobsToExecute;
    }

    public void setJobsToExecute(List<Job> jobsToExecute) {
        this.jobsToExecute = jobsToExecute;
    }
}
