package org.tsd.tsdbot.tsdtv;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.Constants;
import org.tsd.rest.v1.tsdtv.schedule.Schedule;
import org.tsd.rest.v1.tsdtv.schedule.ScheduledBlock;
import org.tsd.tsdbot.tsdtv.quartz.ScheduledBlockJob;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Singleton
public class TSDTVScheduler {

    private static final Logger log = LoggerFactory.getLogger(TSDTVScheduler.class);

    private static final String SCHEDULE_FILE = "tsdtvSchedule.json";

    private final TSDTV tsdtv;
    private final Scheduler scheduler;
    private final AmazonS3 s3Client;
    private final String tsdtvBucket;
    private final ObjectMapper objectMapper;

    @Inject
    public TSDTVScheduler(TSDTV tsdtv,
                          AmazonS3 s3Client,
                          ObjectMapper objectMapper,
                          Scheduler scheduler,
                          @Named(Constants.Annotations.S3_TSDTV_BUCKET) String tsdtvBucket) {
        this.tsdtv = tsdtv;
        this.s3Client = s3Client;
        this.tsdtvBucket = tsdtvBucket;
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;

        try {
            log.warn("Starting TSDTVScheduler...");
            scheduler.start();
            log.warn("Started quartz scheduler");
            loadSchedule();
            log.warn("Successfully started TSDTVScheduler");
        } catch (Exception e) {
            throw new RuntimeException("Error initializing TSDTV scheduler", e);
        }
    }

    public void loadSchedule() throws SchedulerException, IOException {
        log.warn("Loading TSDTV schedule...");
        scheduler.pauseAll();
        log.warn("Paused quartz scheduler");
        Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.groupEquals(Constants.Scheduler.TSDTV_GROUP_ID));
        log.warn("Deleting job keys for group: {} -> {}",
                Constants.Scheduler.TSDTV_GROUP_ID, keys.stream().map(Key::getName).collect(Collectors.joining(",")));
        scheduler.deleteJobs(new LinkedList<>(keys));

        log.info("Fetching schedule file from S3: {}/{}", tsdtvBucket, SCHEDULE_FILE);
        S3Object scheduleFile = s3Client.getObject(tsdtvBucket, SCHEDULE_FILE);
        if (scheduleFile == null) {
            throw new IOException("Could not find TSDTV schedule file in S3: " + tsdtvBucket + "/" + SCHEDULE_FILE);
        }
        
        Schedule schedule = objectMapper.readValue(scheduleFile.getObjectContent(), Schedule.class);
        log.info("Read schedule from S3: {}", schedule);

        for (ScheduledBlock block : schedule.getScheduledBlocks()) {
            log.info("Evaluating scheduled block: {}", block);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(Constants.Scheduler.TSDTV_BLOCK_DATA_KEY, block);

            JobDetail job = newJob(ScheduledBlockJob.class)
                    .withIdentity(block.getId(), Constants.Scheduler.TSDTV_GROUP_ID)
                    .setJobData(jobDataMap)
                    .build();

            Trigger cronTrigger = newTrigger()
                    .withSchedule(cronSchedule(block.getCronString()).inTimeZone(Constants.Scheduler.TSDTV_TIME_ZONE))
                    .build();

            scheduler.scheduleJob(job, cronTrigger);
            log.warn("Successfully scheduled block {}/{}", block.getId(), block.getName());
        }
        scheduler.resumeAll();
        log.warn("Resumed quartz scheduler");
    }

}
