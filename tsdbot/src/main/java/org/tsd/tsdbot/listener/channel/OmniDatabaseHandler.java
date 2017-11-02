package org.tsd.tsdbot.listener.channel;

import de.btobastian.javacord.DiscordAPI;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Constants;
import org.tsd.tsdbot.discord.DiscordChannel;
import org.tsd.tsdbot.discord.DiscordMessage;
import org.tsd.tsdbot.listener.MessageHandler;
import org.tsd.tsdbot.odb.OdbItem;
import org.tsd.tsdbot.odb.OdbItemDao;
import org.tsd.tsdbot.odb.OmniDbException;
import org.tsd.tsdbot.util.OdbUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;

public class OmniDatabaseHandler extends MessageHandler<DiscordChannel> {

    private static final Logger log = LoggerFactory.getLogger(OmniDatabaseHandler.class);

    private final OdbItemDao odbItemDao;

    @Inject
    public OmniDatabaseHandler(DiscordAPI api, OdbItemDao odbItemDao) {
        super(api);
        this.odbItemDao = odbItemDao;
    }

    @Override
    public boolean isValid(DiscordMessage<DiscordChannel> message) {
        return startsWith(message.getContent(), Constants.OmniDatabase.COMMAND_PREFIX);
    }

    @Override
    public void doHandle(DiscordMessage<DiscordChannel> message, DiscordChannel channel) throws Exception {
        log.info("Handling odb: channel={}, message={}",
                channel.getName(), message.getContent());

        String input = substringAfter(message.getContent(), Constants.OmniDatabase.COMMAND_PREFIX).trim();
        String[] parts = input.split("\\s+");
        log.info("Parsed input: {} -> {}", input, Arrays.toString(parts));

        Mode mode = parseModeFromInput(input);
        log.info("ODB mode: {}", mode);

        try {
            switch (mode) {
                case add: {
                    handleAdd(message, parts);
                    break;
                }
                case mod: {
                    break;
                }
                case del: {
                    handleDelete(message, parts);
                    break;
                }
                case get_random: {
                    handleGetRandom(message);
                    break;
                }
                case get_search: {
                    handleGetSearch(message, parts);
                    break;
                }
                default: {
                    channel.sendMessage("USAGE: .odb (add <#tag1> <#tag2> <item>) | ");
                }
            }
        } catch (OmniDbException e) {
            channel.sendMessage("Error: " + e.getMessage());
        }
    }

    private void handleAdd(DiscordMessage<DiscordChannel> message, String[] parts) throws OmniDbException {
        log.info("Handling .odb add: parts = {}", Arrays.toString(parts));
        Set<String> tags = new HashSet<>();
        StringBuilder itemData = new StringBuilder();

        for (int i=1 ; i < parts.length ; i++) {
            String word = parts[i];
            if (word.startsWith("#") && word.length() > 1) {
                log.info("Detected tag word: {}", word);
                tags.add(StringUtils.substring(word, 1));
            } else if (!word.startsWith("#")) {
                log.info("Detected non-tag word: {}", word);
                while (i < parts.length) {
                    itemData.append(word).append(" ");
                    i++;
                    if (i < parts.length) {
                        word = parts[i];
                        log.info("Next word: {}", word);
                    }
                }
            }
        }

        log.info("Result: tags = {}, item = \"{}\"", tags, itemData);

        if (CollectionUtils.isEmpty(tags) || StringUtils.isBlank(itemData.toString())) {
            message.getRecipient().sendMessage("USAGE: .odb add #tag1 #tag2 <item>");
        } else {
            String result = odbItemDao.addItem(itemData.toString(), tags.toArray(new String[tags.size()]));
            message.getRecipient().sendMessage("Item added to Omni Database, id = " + result);
        }
    }

    private void handleDelete(DiscordMessage<DiscordChannel> message, String[] parts) throws OmniDbException {
        log.info("Handling .odb del: parts = {}", Arrays.toString(parts));
        if (parts.length <= 1) {
            message.getRecipient().sendMessage("USAGE: .odb del <ItemID>");
        } else {
            if (!message.authorHasRole(Constants.Role.TSD)) {
                message.getRecipient().sendMessage(Constants.Role.NOT_AUTHORIZED_MESSAGE);
            } else {
                String itemId = parts[1];
                odbItemDao.deleteItem(itemId);
                message.getRecipient().sendMessage("Successfully deleted item "+itemId);
            }
        }
    }

    private void handleGetRandom(DiscordMessage<DiscordChannel> message) throws OmniDbException {
        log.info("Handling .odb get_random");
        OdbItem randomItem = odbItemDao.getRandomItem();
        message.getRecipient().sendMessage("ODB: "+buildFullItem(randomItem));
    }

    private void handleGetSearch(DiscordMessage<DiscordChannel> message, String[] parts) throws OmniDbException {
        log.info("Handling .odb get_search: parts = {}", Arrays.toString(parts));

        Set<String> tagsToSearch = Arrays.stream(parts)
                .filter(part -> !StringUtils.equalsIgnoreCase("get", part))
                .map(OdbUtils::sanitizeTag)
                .collect(Collectors.toSet());
        log.info("Searching for tags: {}", tagsToSearch);

        if (CollectionUtils.isEmpty(tagsToSearch)) {
            handleGetRandom(message);
        } else {
            OdbItem item = odbItemDao.searchForItem(tagsToSearch);
            if (item == null) {
                message.getRecipient().sendMessage("Found no items in the Omni Database matching those tags");
            } else {
                message.getRecipient().sendMessage("ODB: " + item.getItem());
            }
        }
    }

    private static String buildFullItem(OdbItem item) {
        StringBuilder builder = new StringBuilder(item.getItem());
        for (String tag : item.getTags()) {
            builder.append(" #").append(tag);
        }
        return builder.toString().trim();
    }

    private static Mode parseModeFromInput(String input) {
        if (StringUtils.isBlank(input)) {
            return Mode.get_random;
        }
        String[] words = input.split("\\s+");
        switch (words[0]) {
            case "add": return Mode.add;
            case "mod": return Mode.mod;
            case "del": return Mode.del;
            case "get":
            default: return Mode.get_search;
        }
    }

    enum Mode {
        add,
        get_search,
        get_random,
        mod,
        del
    }

}
