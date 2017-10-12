package org.tsd.tsdbot.app.config;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class TwitterConfig {

    @NotEmpty
    @NotNull
    private String consumerKey;

    @NotEmpty
    @NotNull
    private String consumerKeySecret;

    @NotEmpty
    @NotNull
    private String accessToken;

    @NotEmpty
    @NotNull
    private String accessTokenSecret;

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerKeySecret() {
        return consumerKeySecret;
    }

    public void setConsumerKeySecret(String consumerKeySecret) {
        this.consumerKeySecret = consumerKeySecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }
}
