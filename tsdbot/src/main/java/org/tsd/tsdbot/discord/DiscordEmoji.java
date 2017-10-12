package org.tsd.tsdbot.discord;

import de.btobastian.javacord.entities.CustomEmoji;

import java.net.URL;

public class DiscordEmoji {

    private final String id;
    private final String name;
    private final String mentionTag;
    private final URL imageUrl;

    private byte[] data;

    public DiscordEmoji(CustomEmoji apiEmoji) {
        this.id = apiEmoji.getId();
        this.name = apiEmoji.getName();
        this.mentionTag = apiEmoji.getMentionTag();
        this.imageUrl = apiEmoji.getImageUrl();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMentionTag() {
        return mentionTag;
    }

    public URL getImageUrl() {
        return imageUrl;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
