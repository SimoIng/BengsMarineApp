package tests;

import base.BaseAppiumTest;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class VideoRecordingTests extends BaseAppiumTest {

    @Test
    void testSoloVideo() throws Exception {
        AndroidDriver driver = BaseAppiumTest.getDriver();

        System.out.println("DEBUG VIDEO - startRecordingScreen()");
        driver.startRecordingScreen();

        Thread.sleep(5000);

        System.out.println("DEBUG VIDEO - stopRecordingScreen()");
        String base64Video = driver.stopRecordingScreen();

        if (base64Video == null || base64Video.isEmpty()) {
            System.out.println("DEBUG VIDEO - stringa base64 vuota, NESSUN video generato da Appium.");
            return;
        }

        byte[] videoBytes = Base64.getDecoder().decode(base64Video);

        Path dir = Paths.get("build/videos");
        Files.createDirectories(dir);
        Path file = dir.resolve("testSoloVideo.mp4");
        Files.write(file, videoBytes);

        System.out.println("DEBUG VIDEO - Video salvato in: " + file.toAbsolutePath());
    }
}
