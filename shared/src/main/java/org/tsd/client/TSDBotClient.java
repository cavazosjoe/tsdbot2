package org.tsd.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.rest.v1.tsdtv.HeartbeatResponse;
import org.tsd.rest.v1.tsdtv.job.JobUpdate;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class TSDBotClient {

    private static final Logger log = LoggerFactory.getLogger(TSDBotClient.class);

    private static final int MAX_ATTEMPTS = 10;
    private static final long ATTEMPT_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(2);

    private final HttpClient httpClient;
    private final URL tsdbotUrl;
    private final ObjectMapper objectMapper;

    public TSDBotClient(HttpClient httpClient, URL tsdbotUrl, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.tsdbotUrl = tsdbotUrl;
        this.objectMapper = objectMapper;
    }

    public HeartbeatResponse sendTsdtvAgentHeartbeat(Heartbeat heartbeat) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(tsdbotUrl.toURI())
                .setPath("/tsdtv/agent/"+heartbeat.getAgentId());
        HttpPut put = new HttpPut(uriBuilder.build());
        put.setEntity(new StringEntity(objectMapper.writeValueAsString(heartbeat)));
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

    public void sendJobUpdate(JobUpdate jobUpdate) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(tsdbotUrl.toURI())
                .setPath("/job/"+jobUpdate.getJobId());
        HttpPut put = new HttpPut(uriBuilder.build());
        put.setEntity(new StringEntity(objectMapper.writeValueAsString(jobUpdate)));
        put.setHeader("Content-Type", MediaType.APPLICATION_JSON);
        try (CloseableHttpResponse response = getResponseWithRedundancy(httpClient, put)) {
            if (response.getStatusLine().getStatusCode()/100 != 2) {
                String msg = String.format("HTTP error %d: %s",
                        response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                throw new Exception(msg);
            }
            String responseString = EntityUtils.toString(response.getEntity());
            log.debug("Job update successful, received response: {}", responseString);
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
}
