package org.tsd.tsdbot.discord;

import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.permissions.Role;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiscordUser implements MessageRecipient {

    private final String id;
    private final String name;

    private final User user;

    public DiscordUser(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getUser() {
        return user;
    }

    public List<Role> getRoles(Server server) {
        return new LinkedList<>(user.getRoles(server));
    }

    public boolean hasRole(Server server, String name) {
        return getRoles(server).stream()
                .anyMatch(role -> StringUtils.equals(role.getName(), name));
    }

    @Override
    public DiscordMessage<DiscordUser> sendMessage(String content) {
        try {
            Message message = user.sendMessage(content).get(10, TimeUnit.SECONDS);
            return new DiscordMessage<>(message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending message to user: " + this, e);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("user", user)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DiscordUser that = (DiscordUser) o;

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
