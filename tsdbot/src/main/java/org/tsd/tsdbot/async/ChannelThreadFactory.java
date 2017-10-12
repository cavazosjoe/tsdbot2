package org.tsd.tsdbot.async;

import org.tsd.tsdbot.async.dorj.DorjThread;
import org.tsd.tsdbot.discord.DiscordChannel;
import org.tsd.tsdbot.discord.DiscordUser;

public interface ChannelThreadFactory {
    public DorjThread createDorjThread(DiscordChannel channel, DiscordUser author);
}
