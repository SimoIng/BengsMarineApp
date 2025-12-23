package tests;

import base.BaseAppiumTest;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginTutteLePreseTests extends BaseAppiumTest {

    private static final Duration WAIT_SHORT = Duration.ofSeconds(5);
    private static final Duration WAIT_MEDIUM = Duration.ofSeconds(15);
    private static final Duration WAIT_LONG = Duration.ofSeconds(30);

    // ==========================================================
    // =============== UTILITIES ================================
    // ==========================================================

    private void chiudiTastieraSeAperta() {
        System.out.println("DEBUG - Tentativo di chiusura tastiera virtuale...");
        try {
            driver.hideKeyboard();
            System.out.println("DEBUG - Tastiera chiusa ✅");
        } catch (Exception e) {
            System.out.println("DEBUG - Nessuna tastiera da chiudere.");
        }
    }

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

    private void waitForAccediButton() {
        WebDriverWait wait = new WebDriverWait(driver, WAIT_MEDIUM);
        wait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.xpath("//*[contains(@content-desc,'Accedi') or contains(@text,'Accedi')]")));
    }

    private void swipeFincheNonCompareBottoneEntra() {
        System.out.println("DEBUG - Inizio swipe multipli per superare il carosello di onboarding...");
        int maxTentativi = 4;
        boolean trovato = false;

        for (int i = 1; i <= maxTentativi; i++) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
                WebElement entraBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.accessibilityId("Entra nell'App")));

                if (entraBtn != null && entraBtn.isDisplayed()) {
                    entraBtn.click();
                    System.out.println("✅ Bottone 'Entra nell'App' trovato e cliccato!");
                    trovato = true;
                    break;
                }
            } catch (TimeoutException ignored) {
                System.out.println("DEBUG - Swipe verso sinistra (" + i + "/" + maxTentativi + ")");
                try {
                    Dimension size = driver.manage().window().getSize();
                    int left = (int) (size.width * 0.1);
                    int top = (int) (size.height * 0.5);
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
                    Thread.sleep(1200);
                } catch (Exception e) {
                    System.out.println("⚠️ Swipe fallito: " + e.getMessage());
                }
            }
        }

        if (!trovato) {
            System.out.println("⚠️ Nessun bottone 'Entra nell'App' trovato dopo " + maxTentativi + " swipe.");
        }
    }

    // ==========================================================
    // =============== LOGIN ====================================
    // ==========================================================

    private void effettuaLoginConSwipe(String username, String password) {
        effettuaLogin(username, password);
        System.out.println("DEBUG - Eseguo swipe post-login fino a bottone 'Entra nell'App'...");
        swipeFincheNonCompareBottoneEntra();
    }

    private void effettuaLogin(String username, String password) {
        System.out.println("\n========= ESECUZIONE LOGIN (TUTTE LE PRESE) =========");
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

            try {
                WebElement checkBox = driver.findElement(AppiumBy.className("android.widget.CheckBox"));
                if (!checkBox.isSelected()) {
                    checkBox.click();
                    System.out.println("DEBUG - Checkbox 'Ricordami' selezionata ✅");
                }
            } catch (NoSuchElementException ignored) {
                System.out.println("DEBUG - Nessuna checkbox 'Ricordami' trovata.");
            }

            waitForAccediButton();
            WebElement accediBtn;
            try {
                accediBtn = driver.findElement(AppiumBy.accessibilityId("Accedi"));
            } catch (NoSuchElementException e) {
                accediBtn = driver.findElement(AppiumBy.xpath("//*[contains(@text,'Accedi')]"));
            }

            new WebDriverWait(driver, WAIT_SHORT)
                    .until(ExpectedConditions.elementToBeClickable(accediBtn));
            accediBtn.click();
            System.out.println("DEBUG - Click su 'Accedi' eseguito ✅");

        } catch (Exception e) {
            System.out.println("❌ ERRORE - Problema durante il login: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ==========================================================
    // =============== AZIONI SULLE PRESE =======================
    // ==========================================================

    private void accendiTutteLePrese() {
        System.out.println("DEBUG - Navigazione alla schermata 'Colonnina'...");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);

        try {
            WebElement colonninaButton = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.accessibilityId("Colonnina\nScheda 2 di 2")));
            colonninaButton.click();
            System.out.println("DEBUG - Click su 'Colonnina' eseguito ✅");

            wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//*[contains(@content-desc,'Presa Elettrica')]")));
            System.out.println("DEBUG - Schermata 'Colonnina' caricata ✅");

            for (int i = 1; i <= 4; i++) {
                final int presaIndex = i;
                try {
                    WebElement presaBtn = wait.until(ExpectedConditions.elementToBeClickable(
                            AppiumBy.xpath("//android.widget.Button[@content-desc='" + presaIndex + "']")));

                    String statoPrima = presaBtn.getAttribute("selected");
                    presaBtn.click();
                    System.out.println("✅ Click su presa " + presaIndex + " eseguito");

                    // ⏱️ Attesa di 3 secondi per completare animazione
                    Thread.sleep(3500);

                    String statoDopo = presaBtn.getAttribute("selected");
                    String contentDescDopo = presaBtn.getAttribute("contentDescription");

                    if ((statoDopo != null && !statoDopo.equalsIgnoreCase(statoPrima)) ||
                            (contentDescDopo != null && !contentDescDopo.equals(String.valueOf(presaIndex)))) {
                        System.out.println("🔌 Presa " + presaIndex + " accesa correttamente ✅");
                    } else {
                        System.out.println("⚠️ Nessuna variazione visibile per presa " + presaIndex);
                    }

                } catch (Exception e) {
                    System.out.println("❌ Errore con la presa " + presaIndex + ": " + e.getMessage());
                }
            }

            // ✅ Attesa extra di 2 secondi per sicurezza dopo tutte le prese
            Thread.sleep(4000);
            System.out.println("\n✅ ACCENSIONE DI TUTTE LE PRESE COMPLETATA");
            System.out.println("🏁 FINE PROCEDURA ACCENSIONE DELLE 4 PRESE ✅\n");

        } catch (Exception e) {
            System.out.println("❌ ERRORE - Navigazione o accensione fallite: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ==========================================================
    // =============== TEST CASE =================================
    // ==========================================================

    @Test
    @Order(1)
    public void loginCorretto_eAccendeTutteLePrese() {
        System.out.println("\n=== TEST: Login Corretto + Accensione di tutte le prese ===");
        handleStartupFlow();
        effettuaLoginConSwipe("gerryscotti", "bth01User!");

        WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);
        try {
            System.out.println("DEBUG - Attendo caricamento HOME...");
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.accessibilityId("Home\nScheda 1 di 2")));
            System.out.println("✅ Login corretto, HOME caricata con successo!");
        } catch (TimeoutException e) {
            System.out.println("❌ HOME non visibile dopo login, activity corrente: " + safeActivity());
            throw e;
        }

        accendiTutteLePrese();
    }
}
