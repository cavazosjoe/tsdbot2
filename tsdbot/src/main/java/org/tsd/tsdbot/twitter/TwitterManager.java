package org.tsd.tsdbot.twitter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.app.config.TSDBotConfiguration;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Singleton
public class TwitterManager {

    private static final Logger log = LoggerFactory.getLogger(TwitterManager.class);

    private final Twitter twitter;

    @Inject
    public TwitterManager(Twitter twitter, TSDBotConfiguration configuration) {

        String CONSUMER_KEY =           configuration.getTwitter().getConsumerKey();
        String CONSUMER_KEY_SECRET =    configuration.getTwitter().getConsumerKeySecret();
        String ACCESS_TOKEN =           configuration.getTwitter().getAccessToken();
        String ACCESS_TOKEN_SECRET =    configuration.getTwitter().getAccessTokenSecret();

        this.twitter = twitter;
        this.twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_KEY_SECRET);
        this.twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET));

        log.info("Twitter API initialized successfully");
    }

    public Status postTweet(String text) throws TwitterException {
        return postTweet(text, null);
    }

    public Status postTweet(String text, byte[] media) throws TwitterException {
        if(text.length() > 140) {
            throw new TwitterException("Must be 140 characters or less");
        }
        StatusUpdate statusUpdate = new StatusUpdate(text);
        if (ArrayUtils.isNotEmpty(media)) {
            statusUpdate.setMedia(UUID.randomUUID().toString(), new ByteArrayInputStream(media));
        }
        return twitter.updateStatus(statusUpdate);
    }
}
