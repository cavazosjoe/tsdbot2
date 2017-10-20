package org.tsd.rest.v1.tsdtv.job;

import java.util.HashMap;
import java.util.Map;

public class JobUpdate {
    private String jobId;
    private JobUpdateType jobUpdateType;
    private Map<String, String> data = new HashMap<>();

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobUpdateType getJobUpdateType() {
        return jobUpdateType;
    }

    public void setJobUpdateType(JobUpdateType jobUpdateType) {
        this.jobUpdateType = jobUpdateType;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
