package org.tsd.tsdbot;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Server;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.app.TSDTVModule;
import org.tsd.tsdbot.app.BotUrl;
import org.tsd.tsdbot.app.DiscordServer;
import org.tsd.tsdbot.app.Stage;
import org.tsd.tsdbot.app.config.TSDBotConfiguration;
import org.tsd.tsdbot.app.module.S3Module;
import org.tsd.tsdbot.async.ChannelThreadFactory;
import org.tsd.tsdbot.discord.DiscordChannel;
import org.tsd.tsdbot.discord.DiscordUser;
import org.tsd.tsdbot.filename.FilenameLibrary;
import org.tsd.tsdbot.filename.S3FilenameLibrary;
import org.tsd.tsdbot.history.HistoryCache;
import org.tsd.tsdbot.history.filter.FilterFactory;
import org.tsd.tsdbot.listener.CreateMessageListener;
import org.tsd.tsdbot.listener.MessageFilter;
import org.tsd.tsdbot.listener.MessageHandler;
import org.tsd.tsdbot.listener.channel.*;
import org.tsd.tsdbot.odb.OdbItem;
import org.tsd.tsdbot.odb.OdbItemDao;
import org.tsd.tsdbot.printout.PrintoutLibrary;
import org.tsd.tsdbot.resources.*;
import org.tsd.tsdbot.tsdtv.*;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                bind(DiscordAPI.class).toInstance(api);

                bind(SessionFactory.class)
                        .toInstance(hibernate.getSessionFactory());

                String stageString = configuration.getStage();
                Stage stage = Stage.valueOf(stageString);
                bind(Stage.class)
                        .toInstance(stage);

                bind(String.class)
                        .annotatedWith(Names.named(Constants.Annotations.GOOGLE_GIS_CX))
                        .toInstance(configuration.getGoogle().getGisCx());

                bind(String.class)
                        .annotatedWith(Names.named(Constants.Annotations.GOOGLE_API_KEY))
                        .toInstance(configuration.getGoogle().getApiKey());

                install(new TSDTVModule(configuration.getTsdtv()));

                try {
                    URL botUrl = new URL(configuration.getBotUrl());
                    bind(URL.class)
                            .annotatedWith(BotUrl.class)
                            .toInstance(botUrl);
                } catch (Exception e) {
                    log.error("Error reading botUrl from config: " + configuration.getBotUrl(), e);
                    throw new RuntimeException(e);
                }

                bind(String.class)
                        .annotatedWith(Names.named(Constants.Annotations.MASHAPE_API_KEY))
                        .toInstance(configuration.getMashapeApiKey());

                HttpClient httpClient = HttpClients.createDefault();
                bind(HttpClient.class)
                        .toInstance(httpClient);

                Server borgu = api.getServerById(configuration.getServerId());
                bind(Server.class)
                        .annotatedWith(DiscordServer.class)
                        .toInstance(borgu);

                Optional<DiscordUser> owner = api.getUsers()
                        .stream()
                        .filter(user -> StringUtils.equals(configuration.getOwner(), user.getName()))
                        .map(DiscordUser::new)
                        .findAny();

                if (!owner.isPresent()) {
                    throw new RuntimeException("Could not find owner in chat: " + configuration.getOwner());
                }

                bind(DiscordUser.class)
                        .annotatedWith(Names.named(Constants.Annotations.OWNER))
                        .toInstance(owner.get());

                bind(String.class)
                        .annotatedWith(Names.named(Constants.Annotations.OWNER_KEY))
                        .toInstance(configuration.getOwnerKey());

                bind(DiscordUser.class)
                        .annotatedWith(Names.named(Constants.Annotations.SELF))
                        .toInstance(new DiscordUser(api.getYourself()));

                bind(TSDBotConfiguration.class)
                        .toInstance(configuration);

                bind(Twitter.class)
                        .toInstance(TwitterFactory.getSingleton());

                ExecutorService executorService
                        = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                bind(ExecutorService.class)
                        .toInstance(executorService);

                install(new S3Module(configuration));

                bind(FilenameLibrary.class)
                        .to(S3FilenameLibrary.class);

                bind(PrintoutLibrary.class);

                UnitOfWorkAwareProxyFactory proxyFactory = new UnitOfWorkAwareProxyFactory(hibernate);

                OdbItemDao odbItemDao = proxyFactory
                        .create(OdbItemDao.class, SessionFactory.class, hibernate.getSessionFactory());
                bind(OdbItemDao.class)
                        .toInstance(odbItemDao);

                TSDTVAgentDao tsdtvAgentDao = proxyFactory
                        .create(TSDTVAgentDao.class, SessionFactory.class, hibernate.getSessionFactory());
                bind(TSDTVAgentDao.class)
                        .toInstance(tsdtvAgentDao);

                TSDTVEpisodicItemDao tsdtvEpisodicItemDao = proxyFactory
                        .create(TSDTVEpisodicItemDao.class, SessionFactory.class, hibernate.getSessionFactory());
                bind(TSDTVEpisodicItemDao.class)
                        .toInstance(tsdtvEpisodicItemDao);

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
