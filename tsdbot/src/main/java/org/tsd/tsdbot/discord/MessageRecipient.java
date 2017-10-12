package org.tsd.tsdbot.discord;

public interface MessageRecipient {
    public String getId();
    public String getName();
    public DiscordMessage<? extends MessageRecipient> sendMessage(String content);
}
