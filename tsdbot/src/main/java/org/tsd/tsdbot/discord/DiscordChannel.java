package org.tsd.tsdbot.discord;

import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.message.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.concurrent.TimeUnit;

public class DiscordChannel implements MessageRecipient {

    private final String id;
    private final String name;

    private final de.btobastian.javacord.entities.Server server;
    private final Channel channel;

    public DiscordChannel(Channel channel) {
        this.channel = channel;
        this.server = channel.getServer();
        this.id = channel.getId();
        this.name = channel.getName();
    }

    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public Channel getChannel() {
        return channel;
    }

    public de.btobastian.javacord.entities.Server getServer() {
        return server;
    }

    @Override
    public DiscordMessage<DiscordUser> sendMessage(String content) {
        try {
            Message message = channel.sendMessage(content).get(10, TimeUnit.SECONDS);
            return new DiscordMessage<>(message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending message to channel: " + this, e);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("channel", channel)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DiscordChannel that = (DiscordChannel) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }
}
