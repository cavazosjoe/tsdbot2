package org.tsd.tsdbot.filename;

public class Filename {
    private final byte[] data;
    private final String name;

    public Filename(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return name;
    }
}
