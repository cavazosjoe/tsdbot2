package org.tsd.tsdtv;

import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;
import org.tsd.app.config.FfmpegConfig;

import javax.validation.Valid;
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
    private String inventoryPath;

    @NotNull
    @NotEmpty
    private String password;

    @NotNull
    @Valid
    private FfmpegConfig ffmpeg;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public FfmpegConfig getFfmpeg() {
        return ffmpeg;
    }

    public void setFfmpeg(FfmpegConfig ffmpeg) {
        this.ffmpeg = ffmpeg;
    }
}
