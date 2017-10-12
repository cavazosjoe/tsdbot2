package org.tsd.tsdbot.app.config;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class AwsConfig {

    @NotEmpty
    @NotNull
    private String accessKey;

    @NotEmpty
    @NotNull
    private String secretKey;

    @NotEmpty
    @NotNull
    private String filenamesBucket;

    @NotEmpty
    @NotNull
    private String randomFilenamesBucket;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getFilenamesBucket() {
        return filenamesBucket;
    }

    public void setFilenamesBucket(String filenamesBucket) {
        this.filenamesBucket = filenamesBucket;
    }

    public String getRandomFilenamesBucket() {
        return randomFilenamesBucket;
    }

    public void setRandomFilenamesBucket(String randomFilenamesBucket) {
        this.randomFilenamesBucket = randomFilenamesBucket;
    }
}
