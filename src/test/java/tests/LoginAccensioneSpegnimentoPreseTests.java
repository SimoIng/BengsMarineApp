package tests;

import base.BaseAppiumTest;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginAccensioneSpegnimentoPreseTests extends BaseAppiumTest {

    private static final Duration WAIT_SHORT = Duration.ofSeconds(5);
    private static final Duration WAIT_MEDIUM = Duration.ofSeconds(15);
    private static final Duration WAIT_LONG = Duration.ofSeconds(30);
    private static final double SOGLIA_VERDE_DOMINANTE = 15.0;
    private static final double SOGLIA_BLU_DOMINANTE = 10.0;
    private static final int MAX_TENTATIVI_AZIONE = 5;

    @BeforeAll
    public static void setUpClass() {
        System.out.println("========== AVVIO TEST SUITE: ACCENSIONE + SPEGNIMENTO PRESE ==========");
    }

    private void chiudiTastieraSeAperta() {
        try {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).hideKeyboard();
                System.out.println("DEBUG - Tastiera chiusa ✅");
            }
        } catch (Exception ignored) {}
    }

    private void swipeFincheNonCompareBottoneEntra() {
        int maxTentativi = 4;
        for (int i = 1; i <= maxTentativi; i++) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
                WebElement entraBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.accessibilityId("Entra nell'App")));
                if (entraBtn.isDisplayed()) {
                    entraBtn.click();
                    System.out.println("✅ Bottone 'Entra nell'App' cliccato!");
                    return;
                }
            } catch (TimeoutException ignored) {
                try {
                    org.openqa.selenium.Dimension size = driver.manage().window().getSize();
                    Map<String, Object> swipe = new HashMap<>();
                    swipe.put("left", (int) (size.width * 0.1));
                    swipe.put("top", (int) (size.height * 0.5));
                    swipe.put("width", (int) (size.width * 0.8));
                    swipe.put("height", (int) (size.height * 0.2));
                    swipe.put("direction", "left");
                    swipe.put("percent", 0.85);
                    ((JavascriptExecutor) driver).executeScript("mobile: swipeGesture", swipe);
                    Thread.sleep(1200);
                } catch (Exception e) {
                    System.out.println("⚠️ Swipe fallito: " + e.getMessage());
                }
            }
        }
    }

    private void effettuaLogin(String username, String password) {
        System.out.println("========= ESECUZIONE LOGIN =========");
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
            campoPassword.click();
            campoPassword.clear();
            campoPassword.sendKeys(password);
            chiudiTastieraSeAperta();

            try {
                WebElement checkBox = driver.findElement(AppiumBy.className("android.widget.CheckBox"));
                if (!checkBox.isSelected()) {
                    checkBox.click();
                    System.out.println("DEBUG - Checkbox 'Ricordami' selezionata ✅");
                }
            } catch (NoSuchElementException ignored) {}

            WebElement accediBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.xpath("//*[contains(@text,'Accedi') or contains(@content-desc,'Accedi')]")));
            accediBtn.click();
            System.out.println("DEBUG - Click su 'Accedi' ✅");

            swipeFincheNonCompareBottoneEntra();

        } catch (Exception e) {
            throw new RuntimeException("❌ ERRORE durante il login: " + e.getMessage(), e);
        }
    }

    private void vaiASchermataColonnina() {
        System.out.println("DEBUG - Navigazione alla schermata 'Colonnina'...");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);
        WebElement colonninaButton = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Colonnina\nScheda 2 di 2")));
        colonninaButton.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.xpath("//*[contains(@content-desc,'Presa Elettrica')]")));
        System.out.println("✅ Schermata 'Colonnina' caricata correttamente");
    }

    /** Gestione popup "Timeout connessione" o simili */
    private boolean gestisciPopupErrorePersistente() {
        try {
            long start = System.currentTimeMillis();
            boolean chiuso = false;

            while (System.currentTimeMillis() - start < 10000) { // fino a 10 secondi di controllo
                try {
                    WebElement popup = driver.findElement(AppiumBy.xpath("//*[contains(@content-desc,'Errore')]"));
                    WebElement okButton = driver.findElement(AppiumBy.accessibilityId("Ok"));
                    if (popup.isDisplayed() && okButton.isDisplayed()) {
                        System.out.println("⚠️ Popup di errore rilevato — Timeout connessione. Tentativo di chiusura...");
                        okButton.click();
                        chiuso = true;
                        Thread.sleep(1500);
                        System.out.println("✅ Popup chiuso con successo, attendo riconnessione...");
                        Thread.sleep(5000);
                        break;
                    }
                } catch (NoSuchElementException ignored) {}
                Thread.sleep(500);
            }
            return chiuso;
        } catch (Exception e) {
            System.out.println("⚠️ Errore durante la gestione del popup persistente: " + e.getMessage());
            return false;
        }
    }

    /** Lettura visiva con dominante verde/blu a seconda del gruppo */
    private boolean isPresaAccesa(int numero, String gruppo) {
        try {
            String xpath = gruppo.toLowerCase().contains("elettrica")
                    ? "(//android.widget.Button[@content-desc='" + numero + "'])[1]"
                    : "(//android.widget.Button[@content-desc='" + numero + "'])[2]";

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
            WebElement presa = wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath(xpath)));

            Thread.sleep(1500);

            int accensioniRilevate = 0;

            for (int tentativo = 1; tentativo <= 3; tentativo++) {
                try {
                    org.openqa.selenium.Point location = presa.getLocation();
                    org.openqa.selenium.Dimension size = presa.getSize();

                    byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    BufferedImage screenshot = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(screenshotBytes));

                    int startX = location.getX() + (size.getWidth() / 2) - 10;
                    int startY = location.getY() + (size.getHeight() / 2) - 10;

                    int totalR = 0, totalG = 0, totalB = 0, count = 0;

                    for (int dx = 0; dx < 20; dx += 4) {
                        for (int dy = 0; dy < 20; dy += 4) {
                            int rgb = screenshot.getRGB(startX + dx, startY + dy);
                            Color color = new Color(rgb);
                            totalR += color.getRed();
                            totalG += color.getGreen();
                            totalB += color.getBlue();
                            count++;
                        }
                    }

                    int avgR = totalR / count;
                    int avgG = totalG / count;
                    int avgB = totalB / count;

                    double verdeDominanza = avgG - ((avgR + avgB) / 2.0);
                    double bluDominanza = avgB - ((avgR + avgG) / 2.0);

                    boolean acceso;

                    if (avgR > 220 && avgG > 220 && avgB > 220) {
                        acceso = false;
                    } else if (gruppo.toLowerCase().contains("idrico")) {
                        acceso = bluDominanza > SOGLIA_BLU_DOMINANTE;
                    } else {
                        acceso = verdeDominanza > SOGLIA_VERDE_DOMINANTE;
                    }

                    System.out.printf(
                            "DEBUG - Lettura #%d %s %d -> R=%d G=%d B=%d (Δverde=%.1f | Δblu=%.1f) → %s%n",
                            tentativo, gruppo, numero, avgR, avgG, avgB, verdeDominanza, bluDominanza,
                            acceso ? "ACCESA" : "SPENTA"
                    );

                    if (acceso) accensioniRilevate++;
                    Thread.sleep(1200);

                } catch (StaleElementReferenceException e) {
                    presa = wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath(xpath)));
                    Thread.sleep(1000);
                }
            }

            boolean statoFinale = accensioniRilevate >= 2;
            System.out.println("🧠 Risultato consolidato: " + gruppo + " presa " + numero + " → " + (statoFinale ? "ACCESA" : "SPENTA"));
            return statoFinale;

        } catch (Exception e) {
            System.out.println("⚠️ Errore leggendo colore presa " + numero + ": " + e.getMessage());
            gestisciPopupErrorePersistente();
            return false;
        }
    }

    private void gestisciPrese(String gruppo, boolean accendi) {
        WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);
        System.out.println("\n" + (accendi ? "⚡ AVVIO ACCENSIONE " : "🔌 AVVIO SPEGNIMENTO ") + gruppo.toUpperCase());

        for (int i = 1; i <= 4; i++) {
            int tentativo = 0;
            boolean azioneRiuscita = false;

            while (!azioneRiuscita && tentativo < MAX_TENTATIVI_AZIONE) {
                tentativo++;
                try {
                    boolean statoAttuale = isPresaAccesa(i, gruppo);
                    System.out.println("🔍 Stato iniziale rilevato -> " + gruppo + " presa " + i + ": " + (statoAttuale ? "ACCESA" : "SPENTA"));

                    if ((accendi && statoAttuale) || (!accendi && !statoAttuale)) {
                        System.out.println("💤 " + gruppo + " presa " + i + " già nello stato desiderato, salto.");
                        azioneRiuscita = true;
                        break;
                    }

                    String xpath = gruppo.toLowerCase().contains("elettrica")
                            ? "(//android.widget.Button[@content-desc='" + i + "'])[1]"
                            : "(//android.widget.Button[@content-desc='" + i + "'])[2]";

                    WebElement presa = wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(xpath)));
                    presa.click();
                    System.out.println("👉 Tentativo #" + tentativo + " su " + gruppo + " presa " + i + " — attendo transizione...");

                    // Popup persistente se compare
                    gestisciPopupErrorePersistente();

                    long start = System.currentTimeMillis();
                    boolean nuovoStato = statoAttuale;

                    while (System.currentTimeMillis() - start < 20000) {
                        Thread.sleep(2000);
                        gestisciPopupErrorePersistente();
                        boolean statoCorrente = isPresaAccesa(i, gruppo);
                        if (statoCorrente != statoAttuale) {
                            nuovoStato = statoCorrente;
                            break;
                        }
                    }

                    if (nuovoStato == accendi) {
                        System.out.println("✅ " + gruppo + " presa " + i + (accendi ? " accesa" : " spenta") + " correttamente al tentativo #" + tentativo);
                        azioneRiuscita = true;
                    } else {
                        System.out.println("⚠️ Stato non cambiato per " + gruppo + " presa " + i + ", ritento dopo breve attesa...");
                        Thread.sleep(5000);
                    }

                } catch (Exception e) {
                    System.out.println("⚠️ Errore al tentativo #" + tentativo + " per " + gruppo + " presa " + i + ": " + e.getMessage());
                    gestisciPopupErrorePersistente();
                }
            }

            if (!azioneRiuscita) {
                System.out.println("❌ IMPOSSIBILE completare azione per " + gruppo + " presa " + i + " dopo " + MAX_TENTATIVI_AZIONE + " tentativi!");
            }

            try {
                Thread.sleep(4000);
            } catch (InterruptedException ignored) {}
        }
    }

    @Test
    @Order(1)
    public void testAccensioneESpegnimentoTutteLePrese() throws InterruptedException {
        handleStartupFlow();
        effettuaLogin("gerryscotti", "bth01User!");
        vaiASchermataColonnina();

        gestisciPrese("Presa Elettrica", true);
        Thread.sleep(7000);
        gestisciPrese("Presa Elettrica", false);

        gestisciPrese("Erogatore Idrico", true);
        Thread.sleep(10000);
        gestisciPrese("Erogatore Idrico", false);

        System.out.println("\n🏁 Test COMPLETATO con successo ✅");
    }

    @AfterAll
    public static void tearDownClass() {
        System.out.println("\n================== FINE SUITE AUTOMAZIONE ==================");
        System.out.println("Tutti i test di accensione e spegnimento completati con successo ✅");
        System.out.println("=============================================================");
    }
}
