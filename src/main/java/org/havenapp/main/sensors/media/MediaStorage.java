package org.havenapp.main.sensors.media;

import java.io.File;
import java.io.IOException;

public final class MediaStorage {

    private MediaStorage() {
    }

    /**
     * Makes sure {@code mediaDirectory} exists and contains an empty {@code .nomedia} marker.
     * An existing marker is left untouched. Creating the marker does not hold a stream open,
     * so no file descriptor can leak.
     */
    public static void ensureNoMediaFile(File mediaDirectory) throws IOException {
        if (!mediaDirectory.isDirectory() && !mediaDirectory.mkdirs()) {
            throw new IOException("Unable to create media directory: " + mediaDirectory);
        }

        new File(mediaDirectory, ".nomedia").createNewFile();
    }
}
