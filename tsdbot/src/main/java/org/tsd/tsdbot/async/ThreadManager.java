package org.tsd.tsdbot.async;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.discord.DiscordChannel;

import java.util.concurrent.ExecutorService;

@Singleton
public class ThreadManager {

    private static final Logger log = LoggerFactory.getLogger(ThreadManager.class);

    private final ExecutorService executorService;
    private final Table<Class<? extends ChannelThread>, DiscordChannel, ChannelThread> channelThreadTable;

    @Inject
    public ThreadManager(ExecutorService executorService) {
        this.executorService = executorService;
        this.channelThreadTable = HashBasedTable.create();
    }

    public synchronized <T extends ChannelThread> void addThread(T channelThread) throws DuplicateThreadException {
        log.info("Adding thread: {}", channelThread);

        if (channelThreadTable.contains(channelThread.getClass(), channelThread.getChannel())) {
            throw new DuplicateThreadException(channelThread.getClass(), channelThread.getChannel());
        }

        channelThreadTable.put(channelThread.getClass(), channelThread.getChannel(), channelThread);
        executorService.submit(channelThread);
    }

    public synchronized <T extends ChannelThread> void removeThread(T channelThread) {
        log.info("Removing thread: {}", channelThread);
        channelThreadTable.remove(channelThread.getClass(), channelThread.getChannel());
    }

    @SuppressWarnings("unchecked")
    public <T extends ChannelThread> T getChannelThread(Class<T> clazz, DiscordChannel channel) {
        return (T) channelThreadTable.get(clazz, channel);
    }
}
