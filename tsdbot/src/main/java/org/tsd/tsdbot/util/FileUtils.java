package org.tsd.tsdbot.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

    public String detectMimeType(byte[] data) throws IOException {
        return detectMimeType(data, null);
    }

    public String detectMimeType(byte[] data, String filenameWithExtension) throws IOException {
        Detector detector = tikaConfig.getDetector();
        TikaInputStream stream = TikaInputStream.get(data);

        Metadata metadata = new Metadata();
        if (StringUtils.isNotBlank(filenameWithExtension)) {
            metadata.add(Metadata.RESOURCE_NAME_KEY, filenameWithExtension);
        }
        MediaType mediaType = detector.detect(stream, metadata);
        return mediaType.toString();
    }
}
