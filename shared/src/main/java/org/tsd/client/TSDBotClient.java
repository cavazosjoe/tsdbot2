package org.tsd.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.rest.v1.tsdtv.HeartbeatResponse;
import org.tsd.rest.v1.tsdtv.StoppedPlayingNotification;
import org.tsd.rest.v1.tsdtv.job.Job;
import org.tsd.rest.v1.tsdtv.job.JobResult;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class TSDBotClient {

    private static final Logger log = LoggerFactory.getLogger(TSDBotClient.class);

    private static final int MAX_ATTEMPTS = 10;
    private static final long ATTEMPT_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(2);

    private final HttpClient httpClient;
    private final URL tsdbotUrl;
    private final ObjectMapper objectMapper;
    private final String agentId;
    private final String password;

    public TSDBotClient(HttpClient httpClient,
                        URL tsdbotUrl,
                        String agentId,
                        String password,
                        ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.tsdbotUrl = tsdbotUrl;
        this.agentId = agentId;
        this.password = password;
        this.objectMapper = objectMapper;
        log.info("Initialized TSDBotClient with URL {}", tsdbotUrl);
    }

    public HeartbeatResponse sendTsdtvAgentHeartbeat(Heartbeat heartbeat) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(tsdbotUrl.toURI())
                .setPath("/tsdtv/agent/"+heartbeat.getAgentId());
        URI uri = uriBuilder.build();
        log.info("Sending TSDTV agent heartbeat, URI={}", uri);

        HttpPut put = new HttpPut(uri);
        String entity = objectMapper.writeValueAsString(heartbeat);
        log.info("Heartbeat size: {} KB", entity.getBytes().length/1024);
        put.setEntity(new StringEntity(entity, ContentType.APPLICATION_JSON));
        put.setHeader("Content-Type", MediaType.APPLICATION_JSON);
        try (CloseableHttpResponse response = getResponseWithRedundancy(httpClient, put)) {
            if (response.getStatusLine().getStatusCode()/100 != 2) {
                String msg = String.format("HTTP error %d: %s",
                        response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                throw new Exception(msg);
            }
            String responseString = EntityUtils.toString(response.getEntity());
            log.debug("Heartbeat successful, received response: {}", responseString);
            return objectMapper.readValue(responseString, HeartbeatResponse.class);
        }
    }

    public void sendMediaStoppedNotification(int mediaId, boolean error) throws Exception {
        StoppedPlayingNotification notification = new StoppedPlayingNotification();
        notification.setAgentId(agentId);
        notification.setMediaId(mediaId);
        notification.setError(error);

        URIBuilder uriBuilder = new URIBuilder(tsdbotUrl.toURI())
                .setPath("/tsdtv/stopped")
                .addParameter("password", password);
        URI uri = uriBuilder.build();
        log.info("Sending TSDTV agent stopped notification, URI={}, entity={}", uri, notification);

        HttpPost post = new HttpPost(uri);
        String entity = objectMapper.writeValueAsString(notification);
        post.setEntity(new StringEntity(entity, ContentType.APPLICATION_JSON));
        post.setHeader("Content-Type", MediaType.APPLICATION_JSON);
        try (CloseableHttpResponse response = getResponseWithRedundancy(httpClient, post)) {
            if (response.getStatusLine().getStatusCode()/100 != 2) {
                String msg = String.format("HTTP error %d: %s",
                        response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                throw new Exception(msg);
            }
        }
    }

    public Job pollForJob() throws Exception {
        URIBuilder uriBuilder = new URIBuilder(tsdbotUrl.toURI())
                .setPath("/job/"+agentId);
        URI uri = uriBuilder.build();
        log.debug("Sending job poll request, URI={}", uri);

        HttpGet get = new HttpGet(uri);
        try (CloseableHttpResponse response = getResponseWithRedundancy(httpClient, get)) {
            if (response.getStatusLine().getStatusCode()/100 != 2) {
                String msg = String.format("HTTP error %d: %s",
                        response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                throw new Exception(msg);
            }
            if (response.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode()) {
                String responseString = EntityUtils.toString(response.getEntity());
                Job job = objectMapper.readValue(responseString, Job.class);
                log.debug("Received job from server: {}", job);
                return job;
            }
            return null;
        }
    }

    public void sendJobResult(JobResult result) {
        try {
            URIBuilder uriBuilder = new URIBuilder(tsdbotUrl.toURI())
                    .setPath("/job/" + result.getJobId());
            URI uri = uriBuilder.build();
            log.debug("Sending job poll request, URI={}, result={}", uri, result);

            HttpPut put = new HttpPut(uri);
            String entity = objectMapper.writeValueAsString(result);
            put.setEntity(new StringEntity(entity, ContentType.APPLICATION_JSON));
            put.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            try (CloseableHttpResponse response = getResponseWithRedundancy(httpClient, put)) {
                if (response.getStatusLine().getStatusCode() / 100 != 2) {
                    String msg = String.format("HTTP error %d: %s",
                            response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                    throw new Exception(msg);
                }
                String responseString = EntityUtils.toString(response.getEntity());
                log.debug("Job update successful, received response: {}", responseString);
            }
        } catch (Exception e) {
            log.error("Error sending job result: " + result, e);
        }
    }

    private CloseableHttpResponse getResponseWithRedundancy(HttpClient client, HttpUriRequest request) throws Exception {
        int attempts = 0;
        Exception error = null;
        while (attempts < MAX_ATTEMPTS) {
            try {
                return (CloseableHttpResponse) client.execute(request);
            } catch (Exception e) {
                log.error("Error during execution, retrying after " + ATTEMPT_INTERVAL_MILLIS + " ms");
                log.error("Error", e);
                try {
                    error = e;
                    attempts++;
                    Thread.sleep(ATTEMPT_INTERVAL_MILLIS);
                } catch (InterruptedException ie) {
                    log.error("Interrupted");
                }
            }
        }

        throw error;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("tsdbotUrl", tsdbotUrl)
                .append("agentId", agentId)
                .append("password", password)
                .toString();
    }
}