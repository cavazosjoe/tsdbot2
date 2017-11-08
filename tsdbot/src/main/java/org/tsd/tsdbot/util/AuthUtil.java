package org.tsd.tsdbot.util;

import de.btobastian.javacord.entities.Server;
import org.tsd.Constants;
import org.tsd.tsdbot.app.DiscordServer;
import org.tsd.tsdbot.discord.DiscordUser;

import javax.inject.Inject;

public class AuthUtil {

    private final Server server;

    @Inject
    public AuthUtil(@DiscordServer Server server) {
        this.server = server;
    }

    public boolean userIsAdmin(DiscordUser user) {
        return userHasRole(user, Constants.Role.TSD);
    }

    public boolean userHasRole(DiscordUser user, String role) {
        return user.hasRole(server, role);
    }
}
