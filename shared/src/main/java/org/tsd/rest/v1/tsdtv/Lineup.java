package org.tsd.rest.v1.tsdtv;

import org.tsd.rest.v1.tsdtv.queue.QueuedItem;
import org.tsd.rest.v1.tsdtv.schedule.ScheduledBlockSummary;

import java.util.LinkedList;
import java.util.List;

public class Lineup {
    private QueuedItem nowPlaying;
    private List<QueuedItem> queue = new LinkedList<>();
    private List<ScheduledBlockSummary> remainingBlocks = new LinkedList<>();

    public QueuedItem getNowPlaying() {
        return nowPlaying;
    }

    public void setNowPlaying(QueuedItem nowPlaying) {
        this.nowPlaying = nowPlaying;
    }

    public List<QueuedItem> getQueue() {
        return queue;
    }

    public void setQueue(List<QueuedItem> queue) {
        this.queue = queue;
    }

    public List<ScheduledBlockSummary> getRemainingBlocks() {
        return remainingBlocks;
    }

    public void setRemainingBlocks(List<ScheduledBlockSummary> remainingBlocks) {
        this.remainingBlocks = remainingBlocks;
    }
}
