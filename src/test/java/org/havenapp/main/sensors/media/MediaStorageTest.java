package org.havenapp.main.sensors.media;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MediaStorageTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createsEmptyMarkerAndMissingDirectories() throws IOException {
        File mediaDirectory = new File(temporaryFolder.getRoot(), "nested/media");

        MediaStorage.ensureNoMediaFile(mediaDirectory);

        File marker = new File(mediaDirectory, ".nomedia");
        assertTrue(marker.isFile());
        assertEquals(0L, marker.length());
    }

    @Test
    public void preservesExistingMarker() throws IOException {
        File mediaDirectory = temporaryFolder.newFolder("media");
        File marker = new File(mediaDirectory, ".nomedia");
        byte[] contents = {1, 2, 3};
        java.nio.file.Files.write(marker.toPath(), contents);

        MediaStorage.ensureNoMediaFile(mediaDirectory);

        assertArrayEquals(contents, java.nio.file.Files.readAllBytes(marker.toPath()));
    }

    @Test
    public void failsWhenMediaDirectoryCannotBeCreated() throws IOException {
        File pathThatIsAFile = temporaryFolder.newFile("not-a-directory");
        File mediaDirectory = new File(pathThatIsAFile, "media");

        try {
            MediaStorage.ensureNoMediaFile(mediaDirectory);
            fail("Expected an IOException when the media path is not a directory");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unable to create media directory"));
        }
    }
}
