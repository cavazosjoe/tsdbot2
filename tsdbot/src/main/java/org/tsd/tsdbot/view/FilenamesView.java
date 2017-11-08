package org.tsd.tsdbot.view;

import io.dropwizard.views.View;
import org.tsd.Constants;
import org.tsd.tsdbot.filename.FilenameLibrary;

import java.io.IOException;
import java.util.List;

public class FilenamesView extends View {

    private final FilenameLibrary filenameLibrary;

    public FilenamesView(FilenameLibrary library) {
        super(Constants.View.FILENAMES_VIEW, Constants.UTF_8);
        this.filenameLibrary = library;
    }

    public List<String> getAllFilenames() throws IOException {
        return filenameLibrary.listAllFilenames();
    }
}
