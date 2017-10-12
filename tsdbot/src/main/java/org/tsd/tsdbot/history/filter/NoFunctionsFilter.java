package org.tsd.tsdbot.history.filter;

import org.tsd.tsdbot.discord.DiscordMessage;
import org.tsd.tsdbot.discord.MessageType;

public class NoFunctionsFilter implements MessageHistoryFilter {
    @Override
    public boolean test(DiscordMessage discordMessage) {
        return !discordMessage.getType().equals(MessageType.FUNCTION);
    }
}
