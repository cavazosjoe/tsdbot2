package org.tsd.rest.v1.tsdtv;

public class HeartbeatResponse {
    private int sleepSeconds;
    private boolean sendInventory;

    public boolean isSendInventory() {
        return sendInventory;
    }

    public void setSendInventory(boolean sendInventory) {
        this.sendInventory = sendInventory;
    }

    public int getSleepSeconds() {
        return sleepSeconds;
    }

    public void setSleepSeconds(int sleepSeconds) {
        this.sleepSeconds = sleepSeconds;
    }
}
