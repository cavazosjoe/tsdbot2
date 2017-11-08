package org.tsd.tsdbot.view;

import io.dropwizard.views.View;
import org.tsd.Constants;
import org.tsd.tsdbot.tsdtv.library.TSDTVLibrary;
import org.tsd.tsdbot.tsdtv.library.TSDTVListing;

public class TSDTVView extends View {

    private final TSDTVLibrary library;
    private final PlayerType playerType;
    private final String streamUrl;

    public TSDTVView(TSDTVLibrary library, PlayerType playerType) {
        super(Constants.View.TSDTV_VIEW, Constants.UTF_8);
        this.library = library;
        this.streamUrl = library.getStreamUrl();
        this.playerType = playerType;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public TSDTVListing getListings() {
        return library.getListings();
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public enum PlayerType {
        vlc,
        videojs;
    }
}
