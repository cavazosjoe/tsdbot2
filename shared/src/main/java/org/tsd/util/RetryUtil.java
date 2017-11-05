package org.tsd.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    public static void executeWithRetry(int times, long periodMillis, RetryInstruction instructions) {
        while (times > 0) {
            try {
                instructions.run();
                times = 0;
            } catch (Exception e) {
                log.error("Error", e);
                try {
                    Thread.sleep(periodMillis);
                } catch (Exception interrupt) {
                    log.error("Interrupted");
                }
                times--;
            }
        }
    }

    public interface RetryInstruction {
        void run() throws Exception;
    }
}
