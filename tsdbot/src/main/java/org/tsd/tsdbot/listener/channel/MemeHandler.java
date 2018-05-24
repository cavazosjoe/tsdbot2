package org.tsd.tsdbot.listener.channel;

import com.google.inject.Inject;
import de.btobastian.javacord.DiscordAPI;
import org.apache.commons.lang3.StringUtils;
import org.tsd.tsdbot.discord.DiscordChannel;
import org.tsd.tsdbot.discord.DiscordMessage;
import org.tsd.tsdbot.history.HistoryCache;
import org.tsd.tsdbot.history.HistoryRequest;
import org.tsd.tsdbot.history.filter.FilterFactory;
import org.tsd.tsdbot.history.filter.StandardMessageFilters;
import org.tsd.tsdbot.listener.MessageHandler;
import org.tsd.tsdbot.meme.MemegenClient;
import org.tsd.tsdbot.util.BitlyUtil;
import org.tsd.tsdbot.util.MiscUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MemeHandler extends MessageHandler<DiscordChannel> {

    private final HistoryCache historyCache;
    private final FilterFactory filterFactory;
    private final StandardMessageFilters standardMessageFilters;
    private final MemegenClient memegenClient;
    private final BitlyUtil bitlyUtil;

    @Inject
    public MemeHandler(DiscordAPI api,
                       MemegenClient memegenClient,
                       HistoryCache historyCache,
                       FilterFactory filterFactory,
                       StandardMessageFilters standardMessageFilters,
                       BitlyUtil bitlyUtil) {
        super(api);
        this.memegenClient = memegenClient;
        this.historyCache = historyCache;
        this.filterFactory = filterFactory;
        this.bitlyUtil = bitlyUtil;
        this.standardMessageFilters = standardMessageFilters;
    }

    @Override
    public boolean isValid(DiscordMessage<DiscordChannel> message) {
        return StringUtils.startsWithIgnoreCase(message.getContent(), ".maymay");
    }

    @Override
    public void doHandle(DiscordMessage<DiscordChannel> message, DiscordChannel recipient) throws Exception {
        List<String> templates = new LinkedList<>(memegenClient.getTemplates());
        Random random = new Random();
        String template = templates.get(random.nextInt(templates.size()));

        HistoryRequest<DiscordChannel> request = HistoryRequest.create(recipient, message)
                .withFilters(standardMessageFilters.getStandardFilters())
                .withFilter(filterFactory.createLengthFilter(2, 150));

        String message1 = null;
        while (StringUtils.isBlank(message1)) {
            message1 = MiscUtils.getSanitizedContent(historyCache.getRandomChannelMessage(request));
        }

        String message2 = null;
        while (StringUtils.isBlank(message2) || message2.equals(message1)) {
            message2 = MiscUtils.getSanitizedContent(historyCache.getRandomChannelMessage(request));
        }

        String flavorText = FLAVOR_TEXT.get(random.nextInt(FLAVOR_TEXT.size()));

        recipient.sendMessage(flavorText+": "+bitlyUtil.shortenUrl(memegenClient.generateMemeUrl(template, message1, message2)));
    }

    private static final List<String> FLAVOR_TEXT = Arrays.asList(
            "LEL",
            "lel",
            "lol",
            "rofl",
            "kek",
            "*KFLEKL*",
            "Hit em with the good stuff",
            "A meme for the ages",
            "They said it couldn't be done"
    );
}
