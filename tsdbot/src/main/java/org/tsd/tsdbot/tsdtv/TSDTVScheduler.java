package org.tsd.tsdbot.tsdtv;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.tsd.rest.v1.tsdtv.schedule.Schedule;
import org.tsd.rest.v1.tsdtv.schedule.ScheduledBlock;
import org.tsd.tsdbot.Constants;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Singleton
public class TSDTVScheduler {

    private final TSDTVQueue tsdtvQueue;
    private final Scheduler scheduler;
    private final AmazonS3 s3Client;
    private final String tsdtvBucket;
    private final ObjectMapper objectMapper;

    @Inject
    public TSDTVScheduler(TSDTVQueue tsdtvQueue,
                          AmazonS3 s3Client,
                          ObjectMapper objectMapper,
                          @Named(Constants.Annotations.S3_TSDTV_BUCKET) String tsdtvBucket) {
        this.tsdtvQueue = tsdtvQueue;
        this.s3Client = s3Client;
        this.tsdtvBucket = tsdtvBucket;
        this.objectMapper = objectMapper;

        try {
            this.scheduler = StdSchedulerFactory.getDefaultScheduler();
            this.scheduler.start();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing TSDTV scheduler", e);
        }
    }

    public void loadSchedule() throws SchedulerException, IOException {
        scheduler.pauseAll();
        Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.groupEquals(Constants.Scheduler.TSDTV_GROUP_ID));
        scheduler.deleteJobs(new LinkedList<>(keys));

        S3Object scheduleFile = s3Client.getObject(tsdtvBucket, "tsdtvSchedule.json");
        Schedule schedule = objectMapper.readValue(scheduleFile.getObjectContent(), Schedule.class);

        for (ScheduledBlock block : schedule.getScheduledBlocks()) {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("blockInfo", block);

            JobDetail job = newJob(ScheduledBlockJob.class)
                    .withIdentity(block.getId(), Constants.Scheduler.TSDTV_GROUP_ID)
                    .setJobData(jobDataMap)
                    .build();

            Trigger cronTrigger = newTrigger()
                    .withSchedule(cronSchedule(block.getCronString()))
                    .build();

            scheduler.scheduleJob(job, cronTrigger);
        }
    }

    class ScheduledBlockJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            ScheduledBlock block = (ScheduledBlock) jobExecutionContext.getJobDetail().getJobDataMap().get("blockInfo");
            tsdtvQueue.startScheduledBlock(block);
        }
    }
}
