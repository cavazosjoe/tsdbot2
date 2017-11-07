package org.tsd.tsdbot;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.app.module.TSDTVModule;
import org.tsd.app.module.UtilityModule;
import org.tsd.tsdbot.app.config.TSDBotConfiguration;
import org.tsd.tsdbot.app.module.DiscordModule;
import org.tsd.tsdbot.app.module.HibernateModule;
import org.tsd.tsdbot.app.module.S3Module;
import org.tsd.tsdbot.app.module.TSDBotModule;
import org.tsd.tsdbot.async.ChannelThreadFactory;
import org.tsd.tsdbot.discord.DiscordChannel;
import org.tsd.tsdbot.filename.FilenameLibrary;
import org.tsd.tsdbot.filename.S3FilenameLibrary;
import org.tsd.tsdbot.history.HistoryCache;
import org.tsd.tsdbot.history.filter.FilterFactory;
import org.tsd.tsdbot.listener.CreateMessageListener;
import org.tsd.tsdbot.listener.MessageFilter;
import org.tsd.tsdbot.listener.MessageHandler;
import org.tsd.tsdbot.listener.channel.*;
import org.tsd.tsdbot.odb.OdbItem;
import org.tsd.tsdbot.printout.PrintoutLibrary;
import org.tsd.tsdbot.resources.*;
import org.tsd.tsdbot.tsdtv.AgentRegistry;
import org.tsd.tsdbot.tsdtv.TSDTVAgent;
import org.tsd.tsdbot.tsdtv.TSDTVEpisodicItem;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.util.Arrays;
import java.util.List;

public class TSDBotApplication extends Application<TSDBotConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(TSDBotApplication.class);

    private final HibernateBundle<TSDBotConfiguration> hibernate = new HibernateBundle<TSDBotConfiguration>(
            OdbItem.class,
            TSDTVAgent.class,
            TSDTVEpisodicItem.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(TSDBotConfiguration configuration) {
            return configuration.getDatabase();
        }
    };

    public static void main(final String[] args) throws Exception {
        new TSDBotApplication().run(args);
    }

    @Override
    public String getName() {
        return "TSDBot";
    }

    @Override
    public void initialize(final Bootstrap<TSDBotConfiguration> bootstrap) {
        bootstrap.addBundle(new ViewBundle<>());
        bootstrap.addBundle(new AssetsBundle());
        bootstrap.addBundle(hibernate);
    }

    @Override
    public void run(final TSDBotConfiguration configuration,
                    final Environment environment) {

        DiscordAPI api = Javacord.getApi(configuration.getBotToken(), true);
        api.connectBlocking();
        log.info("Connected!");

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {

                install(new UtilityModule());
                install(new DiscordModule(api, configuration));
                install(new HibernateModule(hibernate));
                install(new TSDBotModule(configuration));
                install(new TSDTVModule(configuration.getTsdtv()));

                bind(Twitter.class)
                        .toInstance(TwitterFactory.getSingleton());

                install(new S3Module(configuration));

                bind(FilenameLibrary.class)
                        .to(S3FilenameLibrary.class);

                bind(PrintoutLibrary.class);
                bind(AgentRegistry.class);
                bind(TSDTVLibrary.class);

                install(new FactoryModuleBuilder().build(ChannelThreadFactory.class));
                install(new FactoryModuleBuilder().build(FilterFactory.class));
            }
        });

        HistoryCache historyCache = injector.getInstance(HistoryCache.class);

        CreateMessageListener messageListener = injector.getInstance(CreateMessageListener.class);
        messageListener.addFilter(historyCache);

        List<MessageHandler<DiscordChannel>> channelMessageHandlers = Arrays.asList(
                injector.getInstance(ReplaceHandler.class),
                injector.getInstance(ChooseHandler.class),
                injector.getInstance(DeejHandler.class),
                injector.getInstance(DorjHandler.class),
                injector.getInstance(FilenameHandler.class),
                injector.getInstance(GvHandler.class),
                injector.getInstance(HustleHandler.class),
                injector.getInstance(PrintoutHandler.class),
                injector.getInstance(OmniDatabaseHandler.class),
                injector.getInstance(TSDTVHandler.class));

        List<MessageFilter> messageFilters = Arrays.asList(
                injector.getInstance(HustleFilter.class));

        for (MessageHandler<DiscordChannel> channelMessageHandler : channelMessageHandlers) {
            messageListener.addChannelHandler(channelMessageHandler);
            historyCache.addChannelMessageHandler(channelMessageHandler);
        }

        for (MessageFilter messageFilter : messageFilters) {
            messageListener.addFilter(messageFilter);
            historyCache.addMessageFilter(messageFilter);
        }

        api.registerListener(messageListener);
        historyCache.initialize();

        environment.jersey().register(injector.getInstance(FilenameResource.class));
        environment.jersey().register(injector.getInstance(RandomFilenameResource.class));
        environment.jersey().register(injector.getInstance(HustleResource.class));
        environment.jersey().register(injector.getInstance(PrintoutResource.class));
        environment.jersey().register(injector.getInstance(TSDTVResource.class));
        environment.jersey().register(injector.getInstance(JobResource.class));
    }
}
