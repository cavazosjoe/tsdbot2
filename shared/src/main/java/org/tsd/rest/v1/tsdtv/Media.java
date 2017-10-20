package org.tsd.rest.v1.tsdtv;

public abstract class Media {

    protected int id;
    protected String agentId;
    protected MediaInfo mediaInfo;

    public Media() {
    }

    public Media(String agentId, MediaInfo mediaInfo) {
        this.agentId = agentId;
        this.mediaInfo = mediaInfo;
        this.id = (agentId+mediaInfo.getFilePath()).hashCode();
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    public void setMediaInfo(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }
}
