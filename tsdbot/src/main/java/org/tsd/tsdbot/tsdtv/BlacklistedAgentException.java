package org.tsd.tsdbot.tsdtv;

public class BlacklistedAgentException extends Exception {
    public BlacklistedAgentException(String agentId) {
        super(agentId+" is blacklisted");
    }
}
