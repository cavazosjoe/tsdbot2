package org.tsd.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Heartbeat;

import javax.ws.rs.core.MediaType;
import java.net.URL;

public class TSDBotClient {

    private static final Logger log = LoggerFactory.getLogger(TSDBotClient.class);

    private final HttpClient httpClient;
    private final URL tsdbotUrl;
    private final ObjectMapper objectMapper;

    public TSDBotClient(HttpClient httpClient, URL tsdbotUrl, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.tsdbotUrl = tsdbotUrl;
        this.objectMapper = objectMapper;
    }

    public long sendTsdtvAgentHeartbeat(Heartbeat heartbeat) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(tsdbotUrl.toURI())
                .setPath("/tsdtv/agent/"+heartbeat.getAgentId());
        HttpPut put = new HttpPut(uriBuilder.build());
        put.setEntity(new StringEntity(objectMapper.writeValueAsString(heartbeat)));
        put.setHeader("Content-Type", MediaType.APPLICATION_JSON);
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(put)) {
            if (response.getStatusLine().getStatusCode()/100 != 2) {
                String msg = String.format("HTTP error %d: %s",
                        response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                throw new Exception(msg);
            }
            String sleepString = EntityUtils.toString(response.getEntity());
            log.debug("Heartbeat successful, received sleep period: {}", sleepString);
            return Long.parseLong(sleepString);
        }
    }
}
