package org.tsd.tsdbot.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.discord.DiscordChannel;

import java.util.concurrent.Callable;

public abstract class ChannelThread implements Callable<Void> {

    private static final Logger log = LoggerFactory.getLogger(ChannelThread.class);

    private final ThreadManager threadManager;
    private final long duration;

    protected final DiscordChannel channel;
    protected final Object mutex = new Object();

    public ChannelThread(ThreadManager threadManager, DiscordChannel channel, long duration) {
        this.channel = channel;
        this.duration = duration;
        this.threadManager = threadManager;
    }

    public DiscordChannel getChannel() {
        return channel;
    }

    @Override
    public Void call() throws Exception {
        handleStart();
        synchronized (mutex) {
            try {
                mutex.wait(duration);
            } catch (InterruptedException e) {
                log.info("Interrupted: " + channel.getName() + " / " + getClass());
            }
        }
        handleEnd();
        threadManager.removeThread(this);
        return null;
    }

    public abstract void handleStart();
    public abstract void handleEnd();
}
