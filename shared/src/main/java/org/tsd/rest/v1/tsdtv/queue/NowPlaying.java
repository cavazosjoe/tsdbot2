package org.tsd.rest.v1.tsdtv.queue;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.tsd.rest.v1.tsdtv.Media;

public class NowPlaying {

    private Media media;
    private NowPlayingStatus nowPlayingStatus;

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public NowPlayingStatus getNowPlayingStatus() {
        return nowPlayingStatus;
    }

    public void setNowPlayingStatus(NowPlayingStatus nowPlayingStatus) {
        this.nowPlayingStatus = nowPlayingStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("media", media)
                .append("nowPlayingStatus", nowPlayingStatus)
                .toString();
    }
}
