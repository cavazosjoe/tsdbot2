package org.tsd.tsdbot.history;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.Constants;
import org.tsd.tsdbot.discord.DiscordMessage;

import java.io.IOException;
import java.io.Serializable;

@Singleton
public class RemoteConfiguration implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(RemoteConfiguration.class);

    private static final String IGNORABLE_INFO_FILE = "ignorableInfo.json";

    private IgnorableMessageInfo ignorableMessageInfo;

    private final AmazonS3 s3Client;
    private final String tsdbotConfigBucket;
    private final ObjectMapper objectMapper;

    @Inject
    public RemoteConfiguration(AmazonS3 s3Client,
                               @Named(Constants.Annotations.S3_TSDBOT_CONFIG_BUCKET) String tsdbotConfigBucket) {
        this.s3Client = s3Client;
        this.tsdbotConfigBucket = tsdbotConfigBucket;
        this.objectMapper = new ObjectMapper();

        try {
            load();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load TSDBot config from AWS, bucket="+tsdbotConfigBucket, e);
        }
    }

    public void load() throws IOException {
        log.warn("Loading remote config info, tsdbotConfigBucket={}, ignorableMessageInfoFile={}",
                tsdbotConfigBucket, IGNORABLE_INFO_FILE);

        synchronized (this) {
            S3Object object = s3Client.getObject(tsdbotConfigBucket, IGNORABLE_INFO_FILE);
            this.ignorableMessageInfo = objectMapper.readValue(object.getObjectContent(), IgnorableMessageInfo.class);
            log.info("Retrieved ignorable message info: {}", ignorableMessageInfo);
        }
    }

    public boolean isMessageInIgnorablePattern(DiscordMessage message) {
        String text = message.getContent();
        return ignorableMessageInfo.getPatterns().stream().anyMatch(text::matches);
    }

    public boolean isMessageFromIgnorableUser(DiscordMessage message) {
        String author = message.getAuthor().getName();
        return ignorableMessageInfo.getUsers()
                .stream()
                .anyMatch(ignorableUser -> StringUtils.equalsIgnoreCase(ignorableUser, author));
    }
}
