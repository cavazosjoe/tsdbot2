package org.tsd.app;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.tsd.app.config.TSDTVConfig;

public class TSDTVModule extends AbstractModule {

    private final TSDTVConfig tsdtvConfig;

    public TSDTVModule(TSDTVConfig tsdtvConfig) {
        this.tsdtvConfig = tsdtvConfig;
    }

    @Override
    protected void configure() {
        FFprobe ffProbe;
        try {
            ffProbe = new FFprobe(tsdtvConfig.getFfprobeExec());
            bind(FFprobe.class)
                    .toInstance(ffProbe);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize FFprobe: " + tsdtvConfig.getFfprobeExec(), e);
        }

        FFmpeg ffMpeg;
        try {
            ffMpeg = new FFmpeg(tsdtvConfig.getFfmpegExec());
            bind(FFmpeg.class)
                    .toInstance(ffMpeg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize FFmpeg: " + tsdtvConfig.getFfprobeExec(), e);
        }

        bind(String.class)
                .annotatedWith(Names.named("tsdtvStreamUrl"))
                .toInstance(tsdtvConfig.getStreamUrl());
    }
}
