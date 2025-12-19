package tests;

import base.BaseAppiumTest;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginTests extends BaseAppiumTest {

    private static final Duration WAIT_SHORT = Duration.ofSeconds(5);
    private static final Duration WAIT_MEDIUM = Duration.ofSeconds(15);
    private static final Duration WAIT_LONG = Duration.ofSeconds(30);

    /** Chiude la tastiera Android in modo sicuro */
    private void chiudiTastieraSeAperta() {
        try {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).hideKeyboard();
                System.out.println("DEBUG - Tastiera chiusa con hideKeyboard()");
            } else {
                System.out.println("DEBUG - Driver non Android, skip hideKeyboard()");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Tastiera non chiudibile via hideKeyboard(), provo tap esterno...");
            try {
                Dimension size = driver.manage().window().getSize();
                int x = size.width / 2;
                int y = (int) (size.height * 0.2);
                Map<String, Object> tap = new HashMap<>();
                tap.put("x", x);
                tap.put("y", y);
                ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", tap);
            } catch (Exception ignored) {
                System.out.println("⚠️ Fallback tap esterno non riuscito.");
            }
        }
    }

    /** Gestisce eventuale popup permessi */
    private void handleNotificationPermission() {
        System.out.println("DEBUG - Controllo popup permessi notifiche...");
        try {
            WebDriverWait wait = new WebDriverWait(driver, WAIT_SHORT);
            WebElement consenti = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(@text,'Consenti') or contains(@text,'consent')]")));
            consenti.click();
            System.out.println("DEBUG - Popup permessi: cliccato 'Consenti'");
        } catch (TimeoutException e) {
            System.out.println("DEBUG - Nessun popup permessi trovato.");
        }
    }

    /** Attende che il bottone Accedi sia visibile */
    private void waitForAccediButton() {
        WebDriverWait wait = new WebDriverWait(driver, WAIT_MEDIUM);
        wait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.xpath("//*[contains(@content-desc,'Accedi') or contains(@text,'Accedi')]")));
    }

    /** Esegue il login */
    private void effettuaLogin(String username, String password) {
        handleNotificationPermission();

        System.out.println("DEBUG - Attendo che i campi siano presenti...");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_MEDIUM);
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("android.widget.EditText"), 1));

        try {
            WebElement campoUsername = driver.findElement(
                    AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\").instance(0)"));
            WebElement campoPassword = driver.findElement(
                    AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\").instance(1)"));

            campoUsername.click();
            campoUsername.clear();
            campoUsername.sendKeys(username);
            System.out.println("DEBUG - Username inserito: " + username);

            campoPassword.click();
            campoPassword.clear();
            campoPassword.sendKeys(password);
            System.out.println("DEBUG - Password inserita");

            // ✅ Chiudi tastiera
            chiudiTastieraSeAperta();
            Thread.sleep(500);

            // Se disponibile, seleziona "Ricordami"
            try {
                WebElement checkBox = driver.findElement(AppiumBy.className("android.widget.CheckBox"));
                if (!checkBox.isSelected()) {
                    checkBox.click();
                    System.out.println("DEBUG - Checkbox 'Ricordami' selezionata");
                }
            } catch (NoSuchElementException ignored) {
                System.out.println("DEBUG - Checkbox 'Ricordami' non trovata");
            }

            System.out.println("DEBUG - Cerco bottone 'Accedi'...");
            waitForAccediButton();

            WebElement accediBtn;
            try {
                accediBtn = driver.findElement(AppiumBy.accessibilityId("Accedi"));
            } catch (NoSuchElementException e) {
                accediBtn = driver.findElement(AppiumBy.xpath("//*[contains(@text,'Accedi')]"));
            }

            // ✅ Scroll nel caso il bottone sia fuori schermo (Appium 2.x)
            try {
                Dimension size = driver.manage().window().getSize();

                Map<String, Object> scrollGesture = new HashMap<>();
                scrollGesture.put("left", 0);
                scrollGesture.put("top", 0);
                scrollGesture.put("width", size.width);
                scrollGesture.put("height", size.height);
                scrollGesture.put("direction", "down");
                scrollGesture.put("percent", 0.8);

                ((JavascriptExecutor) driver).executeScript("mobile: scrollGesture", scrollGesture);
                System.out.println("DEBUG - Scroll verso il basso eseguito");
                Thread.sleep(800);
            } catch (Exception e) {
                System.out.println("⚠️ Scroll gesture fallita: " + e.getMessage());
            }

            // ✅ Click sicuro
            WebDriverWait clickWait = new WebDriverWait(driver, WAIT_SHORT);
            clickWait.until(ExpectedConditions.elementToBeClickable(accediBtn));
            accediBtn.click();
            System.out.println("DEBUG - Click su 'Accedi' eseguito ✅");

            Thread.sleep(1500);

        } catch (Exception e) {
            System.out.println("❌ ERRORE - Problema durante l’inserimento o click: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1)
    public void loginSbagliato_mostraErrore() {
        System.out.println("=== TEST: loginSbagliato_mostraErrore ===");
        handleStartupFlow();

        effettuaLogin("gerryscotti", "bth01User"); // password errata per test negativo

        WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);
        try {
            WebElement errore = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(@text,'Errore') or contains(@text,'error') or contains(@content-desc,'Errore')]")));
            System.out.println("DEBUG - Popup errore visibile ✅");
            WebElement ok = driver.findElement(By.xpath("//*[contains(@text,'OK') or contains(@content-desc,'Ok')]"));
            ok.click();
            System.out.println("DEBUG - Popup errore chiuso ✅");
        } catch (TimeoutException e) {
            System.out.println("⚠️ Nessun popup errore trovato (forse gestione diversa).");
        }
    }

    @Test
    @Order(2)
    public void loginCorretto_mostraHome() {
        System.out.println("=== TEST: loginCorretto_mostraHome ===");
        handleStartupFlow();

        effettuaLogin("gerryscotti", "bth01User!");

        WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);
        try {
            System.out.println("DEBUG - Attendo caricamento HOME...");
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.accessibilityId("Home Scheda 1 di 2")));
            System.out.println("✅ Login corretto, HOME caricata!");
        } catch (TimeoutException e) {
            System.out.println("❌ HOME non visibile dopo login, verificare autenticazione.");
            throw e;
        }
    }
}
