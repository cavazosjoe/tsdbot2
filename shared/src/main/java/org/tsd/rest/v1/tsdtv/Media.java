package org.tsd.rest.v1.tsdtv;

public abstract class Media {

    protected MediaInfo mediaInfo;

    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    public void setMediaInfo(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }
}
