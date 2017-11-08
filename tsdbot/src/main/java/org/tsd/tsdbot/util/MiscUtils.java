package org.tsd.tsdbot.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.Constants;
import org.tsd.tsdbot.discord.DiscordMessage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

public class MiscUtils {

    private static final Logger log = LoggerFactory.getLogger(MiscUtils.class);

    public static <T> T getRandomItemInList(List<T> list) {
        return CollectionUtils.isEmpty(list) ? null : list.get(RandomUtils.nextInt(0, list.size()));
    }

    public static String formatRandom(String input, List<String> formattingChoices) {
        return formatRandom(new String[]{input}, formattingChoices);
    }

    public static String formatRandom(String[] input, List<String> formattingChoices) {
        String format = getRandomItemInList(formattingChoices);
        if (format == null) {
            throw new RuntimeException("No format");
        }
        return String.format(format, (Object[]) input);
    }

    public static List<String> getEmojisInMessage(DiscordMessage<?> message) {
        String text = message.getContent();
        List<String> result = new LinkedList<>();
        if (text != null) {
            Matcher matcher = Constants.Emoji.EMOJI_MENTION_PATTERN.matcher(text);
            while (matcher.find()) {
                log.debug("Parsed emoji from text: \"{}\" -> {},{}",
                        text, matcher.group(1), matcher.group(2));
                result.add(matcher.group(2));
            }
        }
        return result;
    }

    public static String bold(String input) {
        return "**"+input+"**";
    }

    public static BufferedImage overlayImages(BufferedImage bgImage,
                                              BufferedImage fgImage) {
        Graphics2D g = bgImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(bgImage, 0, 0, null);
        g.drawImage(fgImage, 0, 0, null);
        g.dispose();
        return bgImage;
    }

}
