package org.tsd.tsdbot.emoji;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.btobastian.javacord.entities.CustomEmoji;
import de.btobastian.javacord.entities.Server;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.app.DiscordServer;
import org.tsd.tsdbot.discord.DiscordEmoji;

import java.util.concurrent.TimeUnit;

@Singleton
public class EmojiCache {

    private static final Logger log = LoggerFactory.getLogger(EmojiCache.class);

    private final Server server;
    private final CloseableHttpClient httpClient;

    private final LoadingCache<String, DiscordEmoji> emojiCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, DiscordEmoji>() {
                @Override
                public DiscordEmoji load(String id) throws Exception {
                    log.info("Loading emoji with id: {}", id);
                    CustomEmoji apiEmoji = server.getCustomEmojiById(id);
                    DiscordEmoji emoji = new DiscordEmoji(apiEmoji);

                    HttpGet get = new HttpGet(apiEmoji.getImageUrl().toURI());
                    try (CloseableHttpResponse response = httpClient.execute(get)) {
                        byte[] bytes = IOUtils.toByteArray(response.getEntity().getContent());
                        emoji.setData(bytes);
                    }

                    return emoji;
                }
            });

    @Inject
    public EmojiCache(@DiscordServer Server server, CloseableHttpClient httpClient) {
        this.server = server;
        this.httpClient = httpClient;
    }

    public DiscordEmoji getEmoji(String id) {
        return emojiCache.getUnchecked(id);
    }
}
