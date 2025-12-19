package base;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class BaseAppiumTest {

    // Driver condiviso per tutti i test
    protected static AndroidDriver driver;

    // Configurazione dispositivo e app
    private static final String UDID = "C50000000020322";
    private static final String APP_PACKAGE = "com.bithiatec.bengsMarine";
    private static final String APP_ACTIVITY = "com.bithiatec.bengs_marine.MainActivity";
    private static final String APPIUM_URL = "http://127.0.0.1:4723";

    // ======= SETUP ==================================================
    @BeforeAll
    public static void setUpClass() throws MalformedURLException {
        System.out.println("DEBUG - @BeforeAll setUpClass");
        System.out.println("DEBUG - udid=" + UDID);
        System.out.println("DEBUG - appPackage=" + APP_PACKAGE);
        System.out.println("DEBUG - appActivity=" + APP_ACTIVITY);
        System.out.println("DEBUG - appiumUrl=" + APPIUM_URL);

        DesiredCapabilities caps = new DesiredCapabilities();

        // ✅ Tutte le capability devono avere prefisso appium: per Appium 9+
        caps.setCapability("platformName", "Android");
        caps.setCapability("appium:automationName", "UIAutomator2");
        caps.setCapability("appium:deviceName", "Android Device");
        caps.setCapability("appium:udid", UDID);
        caps.setCapability("appium:appPackage", APP_PACKAGE);
        caps.setCapability("appium:appActivity", APP_ACTIVITY);
        caps.setCapability("appium:autoGrantPermissions", true);
        caps.setCapability("appium:newCommandTimeout", 120);
        caps.setCapability("appium:noReset", false);

        System.out.println("DEBUG - Creo AndroidDriver verso Appium...");
        System.out.println("DEBUG - Capabilities: " + caps.asMap());

        try {
            driver = new AndroidDriver(new URL(APPIUM_URL), caps);
            System.out.println("DEBUG - AndroidDriver creato correttamente");
        } catch (Exception e) {
            System.err.println("❌ ERRORE - Impossibile creare AndroidDriver: " + e.getMessage());
            throw e;
        }
    }

    // ======= TEARDOWN ==================================================
    @AfterAll
    public static void tearDownClass() {
        System.out.println("DEBUG - @AfterAll tearDownClass");
        if (driver != null) {
            driver.quit();
        }
    }

    // ======= UTILITY: Gestione flussi ==================================
    protected void handleStartupFlow() {
        System.out.println("DEBUG - handleStartupFlow - start | activity=" + safeActivity());
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement accettaButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.accessibilityId("Accetta")));
            if (accettaButton != null) {
                System.out.println("DEBUG - Termini & Condizioni trovati -> clicco 'Accetta'");
                accettaButton.click();
            }
        } catch (TimeoutException e) {
            System.out.println("DEBUG - Nessun Termini & Condizioni trovato.");
        }
        System.out.println("DEBUG - handleStartupFlow - end");
    }

    protected void handlePostLoginFlow() {
        System.out.println("DEBUG - handlePostLoginFlow - start | activity=" + safeActivity());
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement dialogErrore = wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.accessibilityId("Dialog Errore")));

            if (dialogErrore != null) {
                System.out.println("DEBUG - CLICK popup: Dialog Errore (Ok)");
                driver.findElement(AppiumBy.accessibilityId("Ok")).click();
            }
        } catch (TimeoutException e) {
            System.out.println("DEBUG - Nessun dialogo Errore trovato, login probabilmente OK.");
        }
        System.out.println("DEBUG - handlePostLoginFlow - end");
    }

    // ======= UTILITY: Wait e helper =====================================
    protected void waitForHomeReady() {
        System.out.println("DEBUG - Attendo HOME pronta (tab Home/Colonnina)...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("tabHome")));
            System.out.println("DEBUG - Home trovata!");
        } catch (TimeoutException e) {
            System.out.println("DEBUG - Home NON trovata. Activity corrente: " + safeActivity());
            throw e;
        }
    }

    protected boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // ======= UTILITY: Activity e driver access ==========================
    protected String safeActivity() {
        try {
            String activity = driver.currentActivity();
            if (activity == null || activity.isEmpty()) {
                return "unknown";
            }
            return activity;
        } catch (Exception e) {
            System.out.println("DEBUG - Impossibile leggere activity corrente: " + e.getMessage());
            return "unknown";
        }
    }

    // ✅ Metodo statico per screenshot e video extension
    public static AndroidDriver getDriverStatic() {
        return driver;
    }
}
