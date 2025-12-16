package utils;

import base.BaseAppiumTest;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotOnFailureExtension implements TestWatcher {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        try {
            WebDriver driver = BaseAppiumTest.getDriverStatic();
            if (driver == null) return;
            if (!(driver instanceof TakesScreenshot)) return;

            String testName = context.getDisplayName().replaceAll("[^a-zA-Z0-9._-]", "_");
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            File dir = new File("build/screenshots");
            dir.mkdirs();

            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dst = new File(dir, testName + "___FAIL_" + ts + ".png");
            Files.copy(src.toPath(), dst.toPath());

            System.out.println("📸 Screenshot (FAIL) salvato in: " + dst.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("ScreenshotOnFailureExtension EX: " + e.getMessage());
        }
    }
}
