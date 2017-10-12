package org.tsd.tsdbot.app.config;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class BitlyConfig {

    @NotEmpty
    @NotNull
    private String user;

    @NotEmpty
    @NotNull
    private String apiKey;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
