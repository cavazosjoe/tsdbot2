package org.tsd.tsdbot.app.config;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class GoogleConfig {

    @NotNull
    @NotEmpty
    private String gisCx;

    @NotNull
    @NotEmpty
    private String apiKey;

    public String getGisCx() {
        return gisCx;
    }

    public void setGisCx(String gisCx) {
        this.gisCx = gisCx;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
