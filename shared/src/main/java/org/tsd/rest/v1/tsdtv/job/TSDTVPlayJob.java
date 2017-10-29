package org.tsd.rest.v1.tsdtv.job;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class TSDTVPlayJob extends Job {
    private int mediaId;

    public int getMediaId() {
        return mediaId;
    }

    public void setMediaId(int mediaId) {
        this.mediaId = mediaId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("mediaId", mediaId)
                .append("id", id)
                .toString();
    }
}
