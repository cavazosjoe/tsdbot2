package org.tsd.tsdbot.history.filter;

import com.google.inject.Inject;
import org.tsd.tsdbot.discord.DiscordMessage;
import org.tsd.tsdbot.history.RemoteConfiguration;

public class IgnorableFilter implements MessageHistoryFilter {

    private final RemoteConfiguration remoteConfiguration;

    @Inject
    public IgnorableFilter(RemoteConfiguration remoteConfiguration) {
        this.remoteConfiguration = remoteConfiguration;
    }

    @Override
    public boolean test(DiscordMessage message) {
        return !remoteConfiguration.isMessageFromIgnorableUser(message)
                && !remoteConfiguration.isMessageInIgnorablePattern(message);
    }
}
