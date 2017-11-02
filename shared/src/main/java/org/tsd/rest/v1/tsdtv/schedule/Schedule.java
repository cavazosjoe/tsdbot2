package org.tsd.rest.v1.tsdtv.schedule;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedList;
import java.util.List;

public class Schedule {
    private List<ScheduledBlock> scheduledBlocks = new LinkedList<>();

    public List<ScheduledBlock> getScheduledBlocks() {
        return scheduledBlocks;
    }

    public void setScheduledBlocks(List<ScheduledBlock> scheduledBlocks) {
        this.scheduledBlocks = scheduledBlocks;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("scheduledBlocks", scheduledBlocks)
                .toString();
    }
}
