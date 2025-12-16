package base;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseAppiumTest {

    protected AndroidDriver driver;

    // === Driver static per JUnit extensions (screenshot/video) ===
    private static WebDriver driverStatic;

    public static WebDriver getDriverStatic() { return driverStatic; }
    protected static void setDriverStatic(WebDriver d) { driverStatic = d; }

    // ====== CONFIG (override se vuoi) ======
    protected String udid() { return System.getProperty("udid", "C50000000020322"); }
    protected String appPackage() { return System.getProperty("appPackage", "com.bithiatec.bengsMarine"); }
    protected String appActivity() { return System.getProperty("appActivity", "com.bithiatec.bengs_marine.MainActivity"); }
    protected String appiumUrl() { return System.getProperty("appiumUrl", "http://127.0.0.1:4723"); }

    @BeforeAll
    void setUpClass() throws Exception {
        debug("@BeforeAll setUpClass");
        debug("udid=" + udid());
        debug("appPackage=" + appPackage());
        debug("appActivity=" + appActivity());
        debug("appiumUrl=" + appiumUrl());

        UiAutomator2Options options = new UiAutomator2Options()
                .setUdid(udid())
                .setAppPackage(appPackage())
                .setAppActivity(appActivity())
                .setNewCommandTimeout(Duration.ofSeconds(120))
                .autoGrantPermissions();

        debug("Creo AndroidDriver verso Appium...");
        driver = new AndroidDriver(new URL(appiumUrl()), options);
        debug("AndroidDriver creato correttamente");

        setDriverStatic(driver);
    }

    @AfterAll
    void tearDownClass() {
        debug("@AfterAll tearDownClass");
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
        setDriverStatic(null);
    }

    // ====== LOG ======
    protected void debug(String msg) {
        System.out.println("DEBUG - " + msg);
    }

    // ====== CLICK SAFE ======
    protected boolean clickIfPresent(By by, String name) {
        try {
            List<WebElement> els = driver.findElements(by);
            if (!els.isEmpty() && els.get(0).isDisplayed()) {
                debug("CLICK popup: " + name + " | by=" + by);
                els.get(0).click();
                return true;
            }
        } catch (Exception e) {
            debug("clickIfPresent EX (" + name + "): " + e.getMessage());
        }
        return false;
    }

    /** Dialog errore login: bottone Ok */
    protected void dismissBlockingDialogs() {
        for (int i = 0; i < 5; i++) {
            boolean closed = false;
            closed |= clickIfPresent(AppiumBy.accessibilityId("Ok"), "Dialog Errore (Ok)");
            closed |= clickIfPresent(AppiumBy.accessibilityId("OK"), "Dialog Errore (OK)");
            if (!closed) return;
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
    }

    /** Termini & Condizioni: clicca "Accetta" se presente */
    protected void acceptTermsIfPresent() {
        By accetta = AppiumBy.accessibilityId("Accetta");
        try {
            List<WebElement> els = driver.findElements(accetta);
            if (!els.isEmpty() && els.get(0).isDisplayed()) {
                debug("Termini & Condizioni trovati -> clicco 'Accetta'");
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(accetta))
                        .click();
                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            }
        } catch (Exception e) {
            debug("acceptTermsIfPresent EX: " + e.getMessage());
        }
    }

    /** Da chiamare ad inizio test */
    protected void handleStartupFlow() {
        debug("handleStartupFlow - start | activity=" + safeActivity());

        // chiude eventuali dialog
        dismissBlockingDialogs();

        // prova a gestire termini (possono apparire “in ritardo”)
        for (int i = 0; i < 3; i++) {
            acceptTermsIfPresent();
            dismissBlockingDialogs();
        }

        debug("handleStartupFlow - end");
    }

    /** Attende login pronta: bottone Accedi + 2 EditText */
    protected void waitForLoginPageReady() {
        dismissBlockingDialogs();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        debug("Aspetto bottone 'Accedi' visibile...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.accessibilityId("Accedi")));

        debug("Aspetto 2 campi EditText...");
        wait.until(d -> d.findElements(By.className("android.widget.EditText")).size() >= 2);

        dismissBlockingDialogs();
    }

    protected String safeActivity() {
        try { return driver.currentActivity(); }
        catch (Exception e) { debug("safeActivity EX: " + e.getMessage()); return "<unknown>"; }
    }
}
