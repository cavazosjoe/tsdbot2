package org.tsd.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Media;

import java.util.concurrent.ExecutorService;

@Singleton
public class TSDTVPlayer {

    private static final Logger log = LoggerFactory.getLogger(TSDTVPlayer.class);

    private FFmpegJob runningStream = null;

    private final FFmpeg fFmpeg;
    private final FFprobe fFprobe;
    private final String tsdtvUrl;
    private final AgentInventory agentInventory;
    private final ExecutorService executorService;

    @Inject
    public TSDTVPlayer(ExecutorService executorService,
                       AgentInventory agentInventory,
                       FFmpeg fFmpeg,
                       FFprobe fFprobe,
                       @Named("tsdtvUrl") String tsdtvUrl) {
        this.fFmpeg = fFmpeg;
        this.fFprobe = fFprobe;
        this.agentInventory = agentInventory;
        this.tsdtvUrl = tsdtvUrl;
        this.executorService = executorService;
    }

    public void play(int mediaId) throws Exception {
        log.info("Playing mediaId: {}", mediaId);
        Media media = agentInventory.getFileByMediaId(mediaId);
        if (media == null) {
            throw new Exception("Could not find media in inventory with id "+mediaId);
        }

        log.info("Found media: {}", media);
        FFmpegOutputBuilder outputBuilder = new FFmpegBuilder()
                .setInput(media.getMediaInfo().getFilePath())
                .addOutput(tsdtvUrl)
                .setFormat("flv")

                .setAudioCodec("aac")
                .setAudioSampleRate(44_100)
                .setAudioBitRate(128_000)

                .setVideoCodec("libx264")
                .setVideoFrameRate(24, 1)
                .setVideoBitRate(1200_000)
                .setVideoPixelFormat("yuv420p")

                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL);

        if (CollectionUtils.isNotEmpty(media.getMediaInfo().getSubtitleStreams())) {
            outputBuilder.setVideoFilter("subtitles='"+media.getMediaInfo().getFilePath()+"'");
        }

        FFmpegBuilder builder = outputBuilder.done();
        FFmpegExecutor executor = new FFmpegExecutor(fFmpeg, fFprobe);
        runningStream = executor.createJob(builder);
        executorService.submit(runningStream);
        log.info("Media playing...");
    }

    public void stop() {
        if (runningStream != null) {
            log.warn("Stopping stream");
            runningStream.notify();
            log.warn("Stopped stream");
        }
    }
}
