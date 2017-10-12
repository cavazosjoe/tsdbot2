package org.tsd.tsdbot.history.filter;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import org.tsd.tsdbot.discord.DiscordMessage;

public class LengthFilter implements MessageHistoryFilter {

    private final Integer min;
    private final Integer max;

    @Inject
    public LengthFilter(@Assisted(value = "min") Integer min, @Assisted(value = "max") Integer max) {
        this.min = min == null ? Integer.MIN_VALUE : min;
        this.max = max == null ? Integer.MAX_VALUE : max;
    }

    @Override
    public boolean test(DiscordMessage discordMessage) {
        int length = StringUtils.length(discordMessage.getContent());
        return length >= min && length <= max;
    }
}
