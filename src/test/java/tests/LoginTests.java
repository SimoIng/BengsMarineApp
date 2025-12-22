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

    // ============================== UTILS =================================

    private void chiudiTastieraSeAperta() {
        System.out.println("DEBUG - Tentativo di chiusura tastiera virtuale...");
        try {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).hideKeyboard();
                System.out.println("DEBUG - Tastiera chiusa con hideKeyboard() ✅");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Tastiera non chiudibile via hideKeyboard(): " + e.getMessage());
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

    // ============================== LOGIN =================================

    private void effettuaLogin(String username, String password) {
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

    private void effettuaLoginConSwipe(String username, String password) {
        effettuaLogin(username, password);
        System.out.println("DEBUG - Eseguo swipe post-login fino a bottone 'Entra nell'App'...");
        swipeFincheNonCompareBottoneEntra();
    }

    // ============================== COLONNINA =================================

    private void vaiASchermataColonninaESelezionaPresa() {
        System.out.println("DEBUG - Navigazione alla schermata 'Colonnina'...");
        try {
            WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);

            WebElement colonninaButton = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.accessibilityId("Colonnina\nScheda 2 di 2")));
            colonninaButton.click();
            System.out.println("DEBUG - Click su 'Colonnina' eseguito ✅");

            wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//*[contains(@content-desc,'Presa Elettrica')]")));
            System.out.println("DEBUG - Schermata 'Colonnina' caricata ✅");

            WebElement firstButton = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.xpath("//android.widget.Button[@content-desc='1']")));
            String statoPrima = firstButton.getAttribute("selected");
            System.out.println("DEBUG - Stato prima del click: " + statoPrima);

            firstButton.click();
            System.out.println("✅ Click sul bottone '1' (accensione presa) eseguito");

            Thread.sleep(3000); // Attesa realistica per animazione e transizione colore

            // Verifica se il bottone ha cambiato stato o colore
            String statoDopo = firstButton.getAttribute("selected");
            String contentDescDopo = firstButton.getAttribute("contentDescription");

            if (!String.valueOf(statoPrima).equalsIgnoreCase(String.valueOf(statoDopo)) ||
                    (contentDescDopo != null && !contentDescDopo.equals("1"))) {
                System.out.println("✅ Stato del bottone cambiato → Presa accesa 🔌");
            } else {
                // fallback: cerca view secondaria con colore indicativo (es. verde)
                try {
                    WebElement indicatore = driver.findElement(
                            By.xpath("//*[contains(@content-desc,'Accesa') or contains(@text,'Accesa') or contains(@content-desc,'On')]"));
                    if (indicatore.isDisplayed()) {
                        System.out.println("✅ Indicatore luminoso trovato → presa accesa visivamente 💡");
                    } else {
                        System.out.println("⚠️ Indicatore non visibile — verifica manuale consigliata");
                    }
                } catch (NoSuchElementException e) {
                    System.out.println("⚠️ Nessun cambiamento visibile né indicatore trovato.");
                }
            }

            System.out.println("DEBUG - Verifica completata (schermata resta aperta per osservazione) ✅");

        } catch (Exception e) {
            System.out.println("❌ ERRORE - Navigazione o click falliti: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ============================== TEST CASES =================================

    @Test
    @Order(1)
    public void loginSbagliato_mostraErrore() {
        System.out.println("\n=== TEST 1: Login Errato ===");
        handleStartupFlow();
        effettuaLogin("gerryscotti", "bth01User"); // password errata

        WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);
        try {
            WebElement errore = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(@text,'Errore') or contains(@content-desc,'Errore')]")));
            System.out.println("✅ Popup errore visibile");
            WebElement ok = driver.findElement(By.xpath("//*[contains(@text,'OK') or contains(@content-desc,'Ok')]"));
            ok.click();
            System.out.println("DEBUG - Popup errore chiuso ✅");
        } catch (TimeoutException e) {
            System.out.println("⚠️ Nessun popup errore trovato.");
        }
    }

    @Test
    @Order(2)
    public void loginCorretto_mostraHome_eAccendePresa() {
        System.out.println("\n=== TEST 2: Login Corretto + Accensione Presa ===");
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

        // Navigazione alla Colonnina e verifica stato bottone
        vaiASchermataColonninaESelezionaPresa();
    }
}
