package org.tsd.tsdbot.async;

import org.tsd.tsdbot.discord.DiscordChannel;

public class DuplicateThreadException extends Exception {
    public DuplicateThreadException(Class<? extends ChannelThread> clazz, DiscordChannel channel) {
        super("A thread already exists of type " + clazz + " for channel " + channel.getName());
    }
}
