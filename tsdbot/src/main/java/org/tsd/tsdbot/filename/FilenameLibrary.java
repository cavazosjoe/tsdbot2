package org.tsd.tsdbot.filename;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.tsd.tsdbot.util.MiscUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public interface FilenameLibrary {
    Filename getRandomRealFilename() throws IOException;
    Filename getFilename(String name) throws IOException;
    List<String> listAllFilenames() throws IOException;

    Filename createRandomFilename() throws IOException;
    void addFileToRandomFilenameBucket(String url) throws FilenameValidationException;
    Filename getRandomFilename(String name);

    default String pickRandomFilenameString() throws IOException {
        if (RandomUtils.nextBoolean()) {
            String random = getRandomRealFilename().getName();
            return StringUtils.substringBeforeLast(random, ".");
        } else {
            return MiscUtils.getRandomItemInList(Arrays.asList(RandomFilenames.FILENAMES));
        }
    }
}
