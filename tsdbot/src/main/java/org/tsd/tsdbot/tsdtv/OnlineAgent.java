package org.tsd.tsdbot.tsdtv;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.rest.v1.tsdtv.Inventory;

import java.time.LocalDateTime;

public class OnlineAgent {

    private TSDTVAgent agent;
    private LocalDateTime lastHeartbeat;
    private Double bitrate;
    private Inventory inventory;

    public OnlineAgent(TSDTVAgent agent, Heartbeat heartbeat) {
        this.agent = agent;
        this.lastHeartbeat = LocalDateTime.now();
        this.inventory = heartbeat.getInventory();
        this.bitrate = heartbeat.getUploadBitrate();
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public TSDTVAgent getAgent() {
        return agent;
    }

    public void setAgent(TSDTVAgent agent) {
        this.agent = agent;
    }

    public Double getBitrate() {
        return bitrate;
    }

    public void setBitrate(Double bitrate) {
        this.bitrate = bitrate;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        OnlineAgent that = (OnlineAgent) o;

        return new EqualsBuilder()
                .append(agent, that.agent)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(agent)
                .toHashCode();
    }
}
