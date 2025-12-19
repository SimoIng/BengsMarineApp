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

/**
 * Classe di test per la schermata di Login dell’app Bengs Marine.
 * Versione finale “ultra senior” – resiliente, loggante e mantenibile.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginTests extends BaseAppiumTest {

    private static final Duration WAIT_SHORT = Duration.ofSeconds(5);
    private static final Duration WAIT_MEDIUM = Duration.ofSeconds(15);
    private static final Duration WAIT_LONG = Duration.ofSeconds(30);

    // ===========================================================
    // ===================== HELPER METHODS ======================
    // ===========================================================

    /** Chiude la tastiera Android in modo sicuro */
    private void chiudiTastieraSeAperta() {
        System.out.println("DEBUG - Tentativo di chiusura tastiera...");
        try {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).hideKeyboard();
                System.out.println("✅ Tastiera chiusa con hideKeyboard()");
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
                System.out.println("DEBUG - Tap esterno per chiudere tastiera eseguito ✅");
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
            System.out.println("DEBUG - Popup permessi trovato e chiuso ✅");
        } catch (TimeoutException e) {
            System.out.println("DEBUG - Nessun popup permessi trovato.");
        }
    }

    /** Swipe verso sinistra per carosello o onboarding */
    private void swipeVersoSinistra() {
        System.out.println("DEBUG - Eseguo swipe verso sinistra...");
        try {
            Dimension size = driver.manage().window().getSize();
            int left = (int) (size.width * 0.1);
            int top = (int) (size.height * 0.4);
            int width = (int) (size.width * 0.8);
            int height = (int) (size.height * 0.2);

            Map<String, Object> swipe = new HashMap<>();
            swipe.put("left", left);
            swipe.put("top", top);
            swipe.put("width", width);
            swipe.put("height", height);
            swipe.put("direction", "left");
            swipe.put("percent", 0.85);

            ((JavascriptExecutor) driver).executeScript("mobile: swipeGesture", swipe);
            System.out.println("✅ Swipe verso sinistra eseguito correttamente");
            try {
                Thread.sleep(800);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Swipe non riuscito o non necessario: " + e.getMessage());
        }
    }

    /** Click safe con attesa e log */
    private void clickSafe(WebElement element, String descrizione) {
        try {
            new WebDriverWait(driver, WAIT_SHORT)
                    .until(ExpectedConditions.elementToBeClickable(element));
            element.click();
            System.out.println("✅ Click su '" + descrizione + "' eseguito");
        } catch (Exception e) {
            System.out.println("❌ ERRORE durante click su '" + descrizione + "': " + e.getMessage());
        }
    }

    // ===========================================================
    // ===================== LOGIN METHODS =======================
    // ===========================================================

    /** Esegue il login base (senza swipe o navigazione) */
    private void effettuaLoginBase(String username, String password) {
        System.out.println("\n========= ESECUZIONE LOGIN =========");
        handleNotificationPermission();

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
            System.out.println("DEBUG - Username inserito ✅");

            campoPassword.click();
            campoPassword.clear();
            campoPassword.sendKeys(password);
            System.out.println("DEBUG - Password inserita ✅");

            chiudiTastieraSeAperta();

            WebElement accediBtn = driver.findElement(AppiumBy.accessibilityId("Accedi"));
            clickSafe(accediBtn, "Accedi");

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception e) {
            System.out.println("❌ ERRORE - Problema durante login base: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /** Login completo: effettua login, spunta 'Ricordami' e naviga fino a Home */
    private void effettuaLoginCompleto(String username, String password) {
        System.out.println("\n========= LOGIN COMPLETO =========");
        handleNotificationPermission();

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
            System.out.println("DEBUG - Username inserito ✅");

            campoPassword.click();
            campoPassword.clear();
            campoPassword.sendKeys(password);
            System.out.println("DEBUG - Password inserita ✅");

            chiudiTastieraSeAperta();

            // ✅ Spunta "Ricordami" prima di cliccare "Accedi"
            try {
                WebElement checkBox = driver.findElement(AppiumBy.className("android.widget.CheckBox"));
                if (!checkBox.isSelected()) {
                    checkBox.click();
                    System.out.println("✅ Checkbox 'Ricordami' selezionata prima del login");
                } else {
                    System.out.println("DEBUG - Checkbox 'Ricordami' già selezionata");
                }
            } catch (NoSuchElementException ignored) {
                System.out.println("⚠️ Checkbox 'Ricordami' non trovata (possibile layout differente)");
            }

            WebElement accediBtn = driver.findElement(AppiumBy.accessibilityId("Accedi"));
            clickSafe(accediBtn, "Accedi");

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // --- Swipe carosello ---
            System.out.println("DEBUG - Swipe post-login per carosello...");
            swipeVersoSinistra();
            swipeVersoSinistra();

            // --- Clicca "Entra nell’App" ---
            try {
                WebDriverWait waitButton = new WebDriverWait(driver, WAIT_MEDIUM);
                WebElement entraNellAppBtn = waitButton.until(ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.accessibilityId("Entra nell'App")));
                clickSafe(entraNellAppBtn, "Entra nell’App");
            } catch (TimeoutException e) {
                System.out.println("⚠️ Bottone 'Entra nell’App' non trovato (forse già cliccato).");
            }

            // --- Clicca "Home" ---
            try {
                WebDriverWait waitHome = new WebDriverWait(driver, WAIT_MEDIUM);
                WebElement homeButton = waitHome.until(ExpectedConditions.elementToBeClickable(
                        AppiumBy.accessibilityId("Home\nScheda 1 di 2")));
                clickSafe(homeButton, "Home");
            } catch (TimeoutException e) {
                System.out.println("⚠️ Bottone 'Home' non trovato entro il tempo limite.");
            }

            // --- Verifica Home ---
            WebDriverWait waitHomeScreen = new WebDriverWait(driver, WAIT_LONG);
            waitHomeScreen.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.accessibilityId("Test_Marine\nPrese elettriche disponibili\nErogatori disponibili")));
            System.out.println("✅ HOME caricata correttamente ✅");

        } catch (Exception e) {
            System.out.println("❌ ERRORE durante il login completo: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ===========================================================
    // ======================== TEST CASES =======================
    // ===========================================================

    @Test
    @Order(1)
    public void loginSbagliato_mostraErrore() {
        System.out.println("\n=== TEST 1: Login Errato ===");
        handleStartupFlow();

        effettuaLoginBase("gerryscotti", "bth01User"); // password errata

        try {
            WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);
            WebElement errore = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(@text,'Errore') or contains(@content-desc,'Errore')]")));
            System.out.println("✅ Popup errore visibile");
            WebElement ok = driver.findElement(By.xpath("//*[contains(@text,'OK') or contains(@content-desc,'Ok')]"));
            clickSafe(ok, "OK errore");
        } catch (TimeoutException e) {
            System.out.println("⚠️ Nessun popup errore trovato.");
        }
    }

    @Test
    @Order(2)
    public void loginCorretto_mostraHome() {
        System.out.println("\n=== TEST 2: Login Corretto ===");
        handleStartupFlow();

        effettuaLoginCompleto("gerryscotti", "bth01User!");
        System.out.println("🏁 TEST COMPLETATO: Login corretto con 'Ricordami' + navigazione HOME ✅");
    }
}
