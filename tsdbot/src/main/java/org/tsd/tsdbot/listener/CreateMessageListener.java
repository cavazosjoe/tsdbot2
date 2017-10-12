package org.tsd.tsdbot.listener;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.app.Stage;
import org.tsd.tsdbot.discord.DiscordChannel;
import org.tsd.tsdbot.discord.DiscordMessage;
import org.tsd.tsdbot.discord.DiscordUser;
import org.tsd.tsdbot.discord.MessageType;
import org.tsd.tsdbot.history.HistoryCache;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CreateMessageListener implements MessageCreateListener {

    private static final Logger log = LoggerFactory.getLogger(CreateMessageListener.class);

    private final Stage stage;
    private final HistoryCache historyCache;

    private final List<MessageFilter> messageFilters
            = Collections.synchronizedList(new LinkedList<>());

    private final List<MessageHandler<DiscordChannel>> channelMessageHandlers
            = Collections.synchronizedList(new LinkedList<>());

    private final List<MessageHandler<DiscordUser>> userMessageHandlers
            = Collections.synchronizedList(new LinkedList<>());

    @Inject
    public CreateMessageListener(HistoryCache historyCache, Stage stage) {
        this.historyCache = historyCache;
        this.stage = stage;
    }

    public void addFilter(MessageFilter filter) {
        this.messageFilters.add(filter);
    }

    public void addChannelHandler(MessageHandler<DiscordChannel> handler) {
        this.channelMessageHandlers.add(handler);
    }

    public void addUserHandler(MessageHandler<DiscordUser> handler) {
        this.userMessageHandlers.add(handler);
    }

    @Override
    public void onMessageCreate(DiscordAPI discordAPI, Message apiMessage) {

        boolean isChannelMessage = apiMessage.getChannelReceiver() != null;
        DiscordMessage<?> discordMessage = new DiscordMessage<>(apiMessage);

        for (MessageFilter filter : messageFilters) {
            try {
                filter.filter(discordMessage);
            } catch (MessageFilterException e) {
                // doHandle
                return;
            }
        }

        boolean handled = false;

        if (isChannelMessage) {
            DiscordMessage<DiscordChannel> channelMessage = (DiscordMessage<DiscordChannel>) discordMessage;
            if (isValidForStage(channelMessage)) {
                for (MessageHandler<DiscordChannel> handler : channelMessageHandlers) {
                    try {
                        handled |= handler.handle(channelMessage);
                    } catch (Exception e) {
                        log.error("Error handling channel message: " + discordMessage, e);
                    }
                }
            }
        } else {
            DiscordMessage<DiscordUser> userMessage = (DiscordMessage<DiscordUser>) discordMessage;
            for (MessageHandler<DiscordUser> handler : userMessageHandlers) {
                try {
                    handled |= handler.handle(userMessage);
                } catch (Exception e) {
                    log.error("Error handling user message: " + discordMessage, e);
                }
            }
        }

        if (handled) {
            historyCache.markMessage(discordMessage, MessageType.FUNCTION);
        }
    }

    private boolean isValidForStage(DiscordMessage<DiscordChannel> channelDiscordMessage) {
        return stage.equals(Stage.prod)
                || channelDiscordMessage.getRecipient().getName().equalsIgnoreCase("tsdbot");
    }
}
