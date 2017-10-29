package org.tsd.rest.v1.tsdtv.queue;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.tsd.rest.v1.tsdtv.Media;

public class QueuedItem {

    private Media media;
    private QueuedItemType type;
    private long startTime;
    private long endTime;

    public QueuedItemType getType() {
        return type;
    }

    public void setType(QueuedItemType type) {
        this.type = type;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void updateEndTime() {
        endTime = startTime + (media.getMediaInfo().getDurationSeconds()*1000);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("media", media)
                .append("type", type)
                .append("startTime", startTime)
                .append("endTime", endTime)
                .toString();
    }
}
