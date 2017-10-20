package org.tsd.tsdtv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.client.TSDBotClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TSDTVAgentApplication extends Application<TSDTVAgentConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(TSDTVAgentApplication.class);

    public static void main(final String[] args) throws Exception {
        new TSDTVAgentApplication().run(args);
    }

    @Override
    public String getName() {
        return "TSDTV Agent";
    }

    public void run(final TSDTVAgentConfiguration tsdtvAgentConfiguration, Environment environment) throws Exception {

        final ExecutorService executorService
                = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {

                bind(String.class)
                        .annotatedWith(Names.named("agentId"))
                        .toInstance(tsdtvAgentConfiguration.getAgentId());

                URL tsdbotUrl;
                try {
                    tsdbotUrl = new URL(tsdtvAgentConfiguration.getTsdbotUrl());
                    bind(URL.class)
                            .annotatedWith(Names.named("tsdbotUrl"))
                            .toInstance(tsdbotUrl);
                } catch (Exception e) {
                    throw new RuntimeException("Invalid TSDBot URL: " + tsdtvAgentConfiguration.getTsdbotUrl(), e);
                }

                bind(String.class)
                        .annotatedWith(Names.named("tsdtvUrl"))
                        .toInstance(tsdtvAgentConfiguration.getTsdtvUrl());

                File inventoryDirectory;
                try {
                    inventoryDirectory = new File(tsdtvAgentConfiguration.getInventoryPath());
                    if (!inventoryDirectory.exists()) {
                        throw new IOException("TSDTV inventory folder does not exist: " + tsdtvAgentConfiguration.getInventoryPath());
                    }
                    bind(File.class)
                            .annotatedWith(Names.named("inventory"))
                            .toInstance(inventoryDirectory);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize TSDTV inventory", e);
                }

                FFprobe ffProbe;
                try {
                    ffProbe = new FFprobe(tsdtvAgentConfiguration.getFfprobe());
                    bind(FFprobe.class)
                            .toInstance(ffProbe);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize FFprobe: " + tsdtvAgentConfiguration.getFfprobe(), e);
                }

                FFmpeg ffMpeg;
                try {
                    ffMpeg = new FFmpeg(tsdtvAgentConfiguration.getFfmpeg());
                    bind(FFmpeg.class)
                            .toInstance(ffMpeg);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize FFmpeg: " + tsdtvAgentConfiguration.getFfmpeg(), e);
                }

                bind(ExecutorService.class)
                        .toInstance(executorService);

                HttpClient httpClient = HttpClients.createDefault();
                TSDBotClient tsdBotClient = new TSDBotClient(httpClient, tsdbotUrl, new ObjectMapper());
                bind(TSDBotClient.class)
                        .toInstance(tsdBotClient);
            }
        });

        executorService.submit(injector.getInstance(NetworkMonitor.class));

        HeartbeatThread heartbeatThread = injector.getInstance(HeartbeatThread.class);
        executorService.submit(heartbeatThread);
    }
}
