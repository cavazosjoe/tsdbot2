package org.tsd.tsdbot.tsdtv.job;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.tsd.rest.v1.tsdtv.job.Job;
import org.tsd.rest.v1.tsdtv.job.JobResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class UT_SubmittedJob {

    private ExecutorService executorService;

    @Before
    public void setup() {
        executorService
                = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @After
    public void cleanup() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void testLockUnlock() throws JobTimeoutException, InterruptedException {
        FakeJob job = new FakeJob();
        SubmittedJob<FakeJob, FakeJobResult> submittedJob
                = new SubmittedJob<>("agentId", job, TimeUnit.DAYS.toMillis(1));
        executorService.submit(submittedJob::waitForResult);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        submittedJob.updateResult(new FakeJobResult());
    }

    class FakeJob extends Job {

    }

    class FakeJobResult extends JobResult {

    }

}
