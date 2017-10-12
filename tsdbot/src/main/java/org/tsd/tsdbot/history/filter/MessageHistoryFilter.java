package org.tsd.tsdbot.history.filter;

import org.tsd.tsdbot.discord.DiscordMessage;

import java.util.function.Predicate;

public interface MessageHistoryFilter extends Predicate<DiscordMessage> {
}
