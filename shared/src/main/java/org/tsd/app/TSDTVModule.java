package org.tsd.app;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.app.config.TSDTVConfig;

import java.io.File;

public class TSDTVModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(TSDTVModule.class);

    private final TSDTVConfig tsdtvConfig;

    public TSDTVModule(TSDTVConfig tsdtvConfig) {
        this.tsdtvConfig = tsdtvConfig;
    }

    @Override
    protected void configure() {
        FFprobe ffProbe;
        try {
            log.info("Binding FFprobe to: {}", tsdtvConfig.getFfprobeExec());
            ffProbe = new FFprobe(tsdtvConfig.getFfprobeExec());
            bind(FFprobe.class)
                    .toInstance(ffProbe);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize FFprobe: " + tsdtvConfig.getFfprobeExec(), e);
        }

        FFmpeg ffMpeg;
        try {
            log.info("Binding FFmpeg to: {}", tsdtvConfig.getFfmpegExec());
            File ffmpegFile = new File(tsdtvConfig.getFfmpegExec());
            System.err.println("ffmpeg exists="+ffmpegFile.exists()+", "+ffmpegFile);
            ffMpeg = new FFmpeg(tsdtvConfig.getFfmpegExec());
            bind(FFmpeg.class)
                    .toInstance(ffMpeg);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize FFmpeg: " + tsdtvConfig.getFfmpegExec(), e);
        }

        log.info("Binding tsdtvStreamUrl to: {}", tsdtvConfig.getStreamUrl());
        bind(String.class)
                .annotatedWith(Names.named("tsdtvStreamUrl"))
                .toInstance(tsdtvConfig.getStreamUrl());
    }
}
