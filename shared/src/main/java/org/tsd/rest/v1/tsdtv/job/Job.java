package org.tsd.rest.v1.tsdtv.job;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Job {

    private String id = UUID.randomUUID().toString();
    private JobType type;
    private Map<String, String> parameters = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
