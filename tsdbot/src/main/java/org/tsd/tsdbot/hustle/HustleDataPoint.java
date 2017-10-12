package org.tsd.tsdbot.hustle;

import java.time.LocalDateTime;

public class HustleDataPoint {
    private final String text;
    private final HustleSentiment sentiment;
    private final double confidence;
    private final LocalDateTime date;

    private double newHhr;
//        public double delta;

    public HustleDataPoint(String text, HustleSentiment sentiment, double confidence) {
        this.text = text;
        this.sentiment = sentiment;
        this.confidence = confidence;
        this.date = LocalDateTime.now();
    }

    public String getText() {
        return text;
    }

    public HustleSentiment getSentiment() {
        return sentiment;
    }

    public double getConfidence() {
        return confidence;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public double getNewHhr() {
        return newHhr;
    }

    public void setNewHhr(double newHhr) {
        this.newHhr = newHhr;
    }

    public double getScore() {
        return (confidence/100) * Math.log(text.length() + 1);
    }
}
