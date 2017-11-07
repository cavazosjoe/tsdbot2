package org.tsd.tsdbot.app.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.tsd.tsdbot.Constants;
import org.tsd.tsdbot.app.BotUrl;
import org.tsd.tsdbot.app.Stage;
import org.tsd.tsdbot.app.config.TSDBotConfiguration;

import java.net.URL;

public class TSDBotModule extends AbstractModule {

    private final TSDBotConfiguration configuration;

    public TSDBotModule(TSDBotConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bind(TSDBotConfiguration.class)
                .toInstance(configuration);

        String stageString = configuration.getStage();
        Stage stage = Stage.valueOf(stageString);
        bind(Stage.class)
                .toInstance(stage);

        try {
            URL botUrl = new URL(configuration.getBotUrl());
            bind(URL.class)
                    .annotatedWith(BotUrl.class)
                    .toInstance(botUrl);
        } catch (Exception e) {
            System.err.println("Error reading botUrl from config: " + configuration.getBotUrl());
            throw new RuntimeException(e);
        }

        bind(String.class)
                .annotatedWith(Names.named(Constants.Annotations.GOOGLE_GIS_CX))
                .toInstance(configuration.getGoogle().getGisCx());

        bind(String.class)
                .annotatedWith(Names.named(Constants.Annotations.GOOGLE_API_KEY))
                .toInstance(configuration.getGoogle().getApiKey());

        bind(String.class)
                .annotatedWith(Names.named(Constants.Annotations.MASHAPE_API_KEY))
                .toInstance(configuration.getMashapeApiKey());
    }
}
