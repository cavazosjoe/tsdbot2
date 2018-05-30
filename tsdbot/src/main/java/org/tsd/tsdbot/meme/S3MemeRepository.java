package org.tsd.tsdbot.meme;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.Constants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class S3MemeRepository implements MemeRepository {

    private static final Logger log = LoggerFactory.getLogger(S3MemeRepository.class);

    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(30);

    private final AmazonS3 s3Client;
    private final HttpClient httpClient;
    private final String memesBucket;
    private final Clock clock;

    @Inject
    public S3MemeRepository(AmazonS3 s3Client,
                            HttpClient httpClient,
                            Clock clock,
                            @Named(Constants.Annotations.S3_MEMES_BUCKET) String memesBucket) {
        this.s3Client = s3Client;
        this.memesBucket = memesBucket;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    @Override
    public byte[] getMeme(String id) throws IOException {
        log.info("Getting meme, id={}", id);
        S3Object object = s3Client.getObject(memesBucket, id);
        log.info("Retrieved meme from S3, {} -> {}", id, object.toString());
        return IOUtils.toByteArray(object.getObjectContent());
    }

    @Override
    public String storeMeme(String memeUrl) throws IOException {
        log.info("Storing meme, url={}", memeUrl);

        HttpGet get = new HttpGet(memeUrl);
        byte[] responseBytes;
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(get)) {
            log.info("Meme response, {} -> {} \"{}\"",
                    memeUrl,
                    response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase());

            if (response.getStatusLine().getStatusCode()/100 != 2) {
                throw new RuntimeException("Failed to download meme at URL "+memeUrl+": "+response.getStatusLine());
            }

            responseBytes = EntityUtils.toByteArray(response.getEntity());
        }

        String randomFilename = RandomStringUtils.randomAlphabetic(10)+".jpg";
        s3Client.putObject(memesBucket, randomFilename, new ByteArrayInputStream(responseBytes), new ObjectMetadata());
        log.info("Stored meme in S3, {} -> {}", memeUrl, randomFilename);

        new Thread(this::pruneOldMemes).start();

        return randomFilename;
    }

    private void pruneOldMemes() {
        log.info("Pruning old memes...");
        ListObjectsV2Result list = s3Client.listObjectsV2(memesBucket);

        long now = clock.millis();

        List<S3ObjectSummary> memesToDelete = list.getObjectSummaries()
                .stream()
                .filter(s3ObjectSummary -> now-(s3ObjectSummary.getLastModified().getTime()) > MAX_AGE)
                .collect(Collectors.toList());

        for (S3ObjectSummary toDelete : memesToDelete) {
            log.info("Deleting old meme ({}): {}", toDelete.getLastModified(), toDelete.getKey());
            s3Client.deleteObject(memesBucket, toDelete.getKey());
        }
    }
}
