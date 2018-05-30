package org.tsd.tsdbot.meme;

import java.io.IOException;

public interface MemeRepository {
    byte[] getMeme(String id) throws IOException;
    String storeMeme(String memeUrl) throws IOException;
}
