package org.tsd.tsdbot.listener;

import de.btobastian.javacord.DiscordAPI;
import org.tsd.tsdbot.discord.DiscordMessage;

public abstract class MessageFilter {

    protected final DiscordAPI api;

    public MessageFilter(DiscordAPI api) {
        this.api = api;
    }

    public abstract boolean isHistorical();
    public abstract void filter(DiscordMessage<?> message) throws MessageFilterException;
}
