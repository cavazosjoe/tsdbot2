package org.tsd.tsdbot.hustle;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum HustleSentiment {
    Positive,
    Negative,
    Neutral;

    public static HustleSentiment fromString(String s) {
        return Arrays.stream(values())
                .filter(e -> StringUtils.equalsIgnoreCase(e.name(), s))
                .findAny().orElse(null);
    }
}
