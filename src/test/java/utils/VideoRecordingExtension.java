package utils;

import base.BaseAppiumTest;
import io.appium.java_client.screenrecording.CanRecordScreen;
import org.junit.jupiter.api.extension.*;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class VideoRecordingExtension implements BeforeEachCallback, AfterEachCallback {

    private static final String OUT_DIR = "build/videos";

    @Override
    public void beforeEach(ExtensionContext context) {
        try {
            var driver = BaseAppiumTest.getDriverStatic();
            if (driver == null) {
                System.out.println("🎥 Video: driverStatic NULL -> skip startRecording");
                return;
            }
            if (!(driver instanceof CanRecordScreen)) {
                System.out.println("🎥 Video: driver non supporta CanRecordScreen -> skip startRecording");
                return;
            }

            String testName = sanitize(context.getDisplayName());
            System.out.println("🎥 Avvio registrazione schermo per: " + testName);

            ((CanRecordScreen) driver).startRecordingScreen();
        } catch (Exception e) {
            System.out.println("🎥 VideoRecordingExtension beforeEach EX: " + e.getMessage());
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            var driver = BaseAppiumTest.getDriverStatic();
            if (driver == null) {
                System.out.println("🎥 Video: driverStatic NULL -> skip stopRecording");
                return;
            }
            if (!(driver instanceof CanRecordScreen)) {
                System.out.println("🎥 Video: driver non supporta CanRecordScreen -> skip stopRecording");
                return;
            }

            String base64 = ((CanRecordScreen) driver).stopRecordingScreen();
            if (base64 == null || base64.isBlank()) {
                System.out.println("🎥 Video: stopRecordingScreen ha restituito vuoto -> niente da salvare");
                return;
            }

            String testName = sanitize(context.getDisplayName());
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            File dir = new File(OUT_DIR);
            dir.mkdirs();

            File out = new File(dir, testName + "___" + ts + ".mp4");
            byte[] bytes = Base64.getDecoder().decode(base64);
            Files.write(out.toPath(), bytes);

            System.out.println("🎥 Video salvato in: " + out.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("🎥 VideoRecordingExtension afterEach EX: " + e.getMessage());
        }
    }

    private String sanitize(String name) {
        if (name == null) return "test";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
