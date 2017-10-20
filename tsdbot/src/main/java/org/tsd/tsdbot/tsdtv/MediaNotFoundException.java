package org.tsd.tsdbot.tsdtv;

public class MediaNotFoundException extends Exception {
    public MediaNotFoundException(String agentId, int mediaId) {
        super("Media not found, agentId = "+agentId+", mediaId="+mediaId);
    }
}
