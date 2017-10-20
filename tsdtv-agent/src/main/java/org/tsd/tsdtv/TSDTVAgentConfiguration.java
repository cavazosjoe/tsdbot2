package org.tsd.tsdtv;

import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class TSDTVAgentConfiguration extends Configuration {

    @NotNull
    @NotEmpty
    private String agentId;

    @NotNull
    @NotEmpty
    private String tsdbotUrl;

    @NotNull
    @NotEmpty
    private String tsdtvUrl;

    @NotNull
    @NotEmpty
    private String inventoryPath;

    @NotNull
    @NotEmpty
    private String ffmpeg;

    @NotNull
    @NotEmpty
    private String ffprobe;

    public String getTsdtvUrl() {
        return tsdtvUrl;
    }

    public void setTsdtvUrl(String tsdtvUrl) {
        this.tsdtvUrl = tsdtvUrl;
    }

    public String getTsdbotUrl() {
        return tsdbotUrl;
    }

    public void setTsdbotUrl(String tsdbotUrl) {
        this.tsdbotUrl = tsdbotUrl;
    }

    public String getInventoryPath() {
        return inventoryPath;
    }

    public void setInventoryPath(String inventoryPath) {
        this.inventoryPath = inventoryPath;
    }

    public String getFfmpeg() {
        return ffmpeg;
    }

    public void setFfmpeg(String ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    public String getFfprobe() {
        return ffprobe;
    }

    public void setFfprobe(String ffprobe) {
        this.ffprobe = ffprobe;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
}
