package org.tsd.app.config;

import javax.validation.constraints.NotNull;

public class TSDTVConfig {

    @NotNull
    private String streamUrl;

    @NotNull
    private String ffmpegExec;

    @NotNull
    private String ffprobeExec;

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getFfmpegExec() {
        return ffmpegExec;
    }

    public void setFfmpegExec(String ffmpegExec) {
        this.ffmpegExec = ffmpegExec;
    }

    public String getFfprobeExec() {
        return ffprobeExec;
    }

    public void setFfprobeExec(String ffprobeExec) {
        this.ffprobeExec = ffprobeExec;
    }
}
