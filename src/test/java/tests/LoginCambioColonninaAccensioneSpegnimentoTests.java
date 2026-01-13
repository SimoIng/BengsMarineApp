package tests;

import base.BaseAppiumTest;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginCambioColonninaAccensioneSpegnimentoTests extends BaseAppiumTest {

    // =====================================================================================
    // COSTANTI
    // =====================================================================================

    private static final Duration WAIT_SHORT = Duration.ofSeconds(3);
    private static final Duration WAIT_MEDIUM = Duration.ofSeconds(8);
    private static final Duration WAIT_LONG = Duration.ofSeconds(15);
    private static final long ANIM_DELAY = 1500;
    private static final long ACTION_DELAY = 500;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter REPORT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // Soglie HSV per rilevamento colori
    private static final float HUE_GREEN_MIN = 0.25f;
    private static final float HUE_GREEN_MAX = 0.45f;
    private static final float HUE_BLUE_MIN = 0.55f;
    private static final float HUE_BLUE_MAX = 0.75f;
    private static final float SATURATION_MIN = 0.3f;

    // =====================================================================================
    // VARIABILI STATICHE E DI ISTANZA
    // =====================================================================================

    private static File logFile;
    private static BufferedWriter writer;
    private static final File SCREENSHOT_DIR = new File("logs/screenshots");
    private String currentTotem = "-";

    // Configurazioni
    private static String username;
    private static String password;
    private static int totaleTotem;

    // =====================================================================================
    // ANSI COLORS
    // =====================================================================================

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String RED = "\u001B[31m";
    private static final String WHITE_BOLD = "\u001B[1;37m";

    // =====================================================================================
    // CONFIGURAZIONE
    // =====================================================================================

    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("test.properties")) {
            props.load(input);
            username = props.getProperty("test.username", "collaudolamaddalena");
            password = props.getProperty("test.password", "bth01User!");
            totaleTotem = Integer.parseInt(props.getProperty("test.totale_totem", "7"));
            System.out.println("⚙️ Configurazione caricata da test.properties");
        } catch (IOException e) {
            username = "collaudolamaddalena";
            password = "bth01User!";
            totaleTotem = 7;
            System.out.println("⚙️ Configurazione default applicata");
        }
    }

    // =====================================================================================
    // LOGGER CSV
    // =====================================================================================

    private static void initCsvLogger() {
        try {
            File dir = new File("logs");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (!SCREENSHOT_DIR.exists()) {
                SCREENSHOT_DIR.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(REPORT_FORMAT);
            logFile = new File(dir, "report_log_" + timestamp + ".csv");
            writer = new BufferedWriter(new FileWriter(logFile, false));
            writer.write("Timestamp;Tipo;Totem;Messaggio;Screenshot");
            writer.newLine();
            writer.flush();
            System.out.println("📘 CSV logger inizializzato → " + logFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("⚠️ Errore inizializzazione CSV: " + e.getMessage());
        }
    }

    private void writeCsvLog(String tipo, String msg, String screenshotPath) {
        try {
            if (writer != null) {
                String cleanMsg = msg.replace(";", ",").replace("\n", " ");
                String line = String.format("%s;%s;%s;%s;%s",
                        LocalTime.now().format(TIME_FORMAT),
                        tipo,
                        currentTotem,
                        cleanMsg,
                        (screenshotPath != null ? screenshotPath : "-"));
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.out.println("⚠️ Errore scrittura CSV: " + e.getMessage());
        }
    }

    private static void closeCsvLogger() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                System.out.println("💾 File CSV salvato: " + logFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("⚠️ Errore chiusura CSV: " + e.getMessage());
        }
    }

    private String takeScreenshot(String name) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File(SCREENSHOT_DIR, timestamp + "_" + name + ".png");
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            writeCsvLog("SCREENSHOT", "Screenshot: " + name, dest.getAbsolutePath());
            return dest.getAbsolutePath();
        } catch (Exception e) {
            System.out.println("⚠️ Errore screenshot: " + e.getMessage());
            return null;
        }
    }

    // =====================================================================================
    // LOGGING COLORATO
    // =====================================================================================

    private void log(String tipo, String icona, String color, String msg) {
        String timestamp = LocalTime.now().format(TIME_FORMAT);
        String formatted = String.format("%s %s%s [%s]%s %s",
                timestamp, color, icona, tipo, RESET, msg);
        System.out.println(formatted);
        writeCsvLog(tipo, msg, null);
    }

    private void logHeader(String msg) { log("HEADER", "🧩", WHITE_BOLD, msg); }
    private void logStep(String msg) { log("STEP", "⚙️", BLUE, msg); }
    private void logInfo(String msg) { log("INFO", "💬", CYAN, msg); }
    private void logAction(String msg) { log("ACTION", "🎯", MAGENTA, msg); }
    private void logSuccess(String msg) { log("SUCCESS", "🟢", GREEN, msg); }
    private void logWarn(String msg) { log("WARN", "🟠", YELLOW, msg); }
    private void logError(String msg) { log("ERROR", "🔴", RED, msg); }

    // =====================================================================================
    // UTILITY METHODS
    // =====================================================================================

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =====================================================================================
    // LOGIN (dalla vecchia classe funzionante + miglioramenti)
    // =====================================================================================

    private void effettuaLogin(String username, String password) {
        logHeader("LOGIN");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_LONG);

        try {
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                    By.className("android.widget.EditText"), 1));

            List<WebElement> campi = driver.findElements(By.className("android.widget.EditText"));
            if (campi.size() < 2) {
                throw new RuntimeException("Campi login non trovati");
            }

            WebElement campoUsername = campi.get(0);
            WebElement campoPassword = campi.get(1);

            // Tap nativo per focus stabile (dalla vecchia classe)
            Map<String, Object> tapUser = new HashMap<>();
            Point pUser = campoUsername.getLocation();
            tapUser.put("x", pUser.getX() + campoUsername.getSize().getWidth() / 2);
            tapUser.put("y", pUser.getY() + campoUsername.getSize().getHeight() / 2);
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", tapUser);

            sleep(600);
            campoUsername.clear();
            campoUsername.sendKeys(username);
            logInfo("👤 Username: " + username);

            Map<String, Object> tapPass = new HashMap<>();
            Point pPass = campoPassword.getLocation();
            tapPass.put("x", pPass.getX() + campoPassword.getSize().getWidth() / 2);
            tapPass.put("y", pPass.getY() + campoPassword.getSize().getHeight() / 2);
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", tapPass);

            sleep(600);
            campoPassword.clear();
            campoPassword.sendKeys(password);
            logInfo("🔑 Password inserita");

            // Chiudi tastiera
            try {
                ((AndroidDriver) driver).hideKeyboard();
                sleep(300);
                logInfo("⌨️ Tastiera chiusa");
            } catch (Exception ignored) {
                logInfo("ℹ️ Tastiera già chiusa");
            }

            // Gestisci checkbox "Ricordami"
            try {
                WebElement checkBox = driver.findElement(AppiumBy.className("android.widget.CheckBox"));
                if (!checkBox.isSelected()) {
                    checkBox.click();
                    sleep(1000);
                    logSuccess("✅ Checkbox 'Ricordami' selezionata");
                } else {
                    logInfo("ℹ️ Checkbox 'Ricordami' già selezionata");
                }
            } catch (NoSuchElementException e) {
                logWarn("⚠️ Checkbox 'Ricordami' non trovata, proseguo...");
            }

            // Click su Accedi
            WebElement accediBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.xpath("//*[contains(@text,'Accedi') or contains(@content-desc,'Accedi')]")));
            accediBtn.click();
            logAction("🎯 Click su 'Accedi'");

            // Gestione swipe post-login
            swipeFincheNonCompareBottoneEntra();

            logSuccess("✅ Login completato");
            takeScreenshot("login_success");

        } catch (Exception e) {
            logError("❌ Errore login: " + e.getMessage());
            takeScreenshot("login_error");
            throw new RuntimeException("Login fallito", e);
        }
    }

    private void swipeFincheNonCompareBottoneEntra() {
        for (int i = 0; i < 6; i++) {
            try {
                WebElement entra = driver.findElement(AppiumBy.accessibilityId("Entra nell'App"));
                if (entra.isDisplayed()) {
                    entra.click();
                    logSuccess("🟢 'Entra nell'App' cliccato (tentativo " + (i + 1) + ")");
                    return;
                }
            } catch (Exception ignored) {
                logInfo("👆 Swipe " + (i + 1));
                Dimension size = driver.manage().window().getSize();
                Map<String, Object> swipe = new HashMap<>();
                swipe.put("left", (int) (size.width * 0.1));
                swipe.put("top", (int) (size.height * 0.5));
                swipe.put("width", (int) (size.width * 0.8));
                swipe.put("height", (int) (size.height * 0.2));
                swipe.put("direction", "left");
                swipe.put("percent", 0.85);
                ((JavascriptExecutor) driver).executeScript("mobile: swipeGesture", swipe);
                sleep(ACTION_DELAY);
            }
        }
    }

    // =====================================================================================
    // NAVIGAZIONE
    // =====================================================================================

    private void vaiAllaHome() {
        logStep("Ritorno alla Home");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_MEDIUM);
        try {
            WebElement homeButton = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.accessibilityId("Home\nScheda 1 di 2")));
            homeButton.click();
            sleep(500);
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//android.widget.Button[contains(@content-desc,'Totem_')]")));
            logSuccess("🏠 Home caricata");
        } catch (Exception e) {
            logWarn("⚠️ Errore ritorno Home: " + e.getMessage());
            takeScreenshot("home_error");
        }
    }

    private void selezionaColonninaDalMenu(String nomeTotem) {
        currentTotem = nomeTotem;
        logStep("🔍 Selezione colonnina: " + nomeTotem);
        WebDriverWait waitShort = new WebDriverWait(driver, Duration.ofSeconds(2));
        WebDriverWait waitMedium = new WebDriverWait(driver, WAIT_MEDIUM);

        try {
            WebElement indicatore = waitShort.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//*[contains(@content-desc,'" + nomeTotem + "') or contains(@text,'" + nomeTotem + "')]")));
            if (indicatore.isDisplayed()) {
                logInfo("ℹ️ Già in " + nomeTotem + ", skip");
                return;
            }
        } catch (TimeoutException ignored) {}

        try {
            logInfo("🔽 Apro dropdown totem");
            WebElement dropdown = waitMedium.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.xpath("//android.widget.Button[contains(@content-desc,'Totem_')]")));
            dropdown.click();
            sleep(500);

            logInfo("👆 Click su " + nomeTotem);
            WebElement voce = waitMedium.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.xpath("//android.widget.Button[@content-desc='" + nomeTotem + "']")));
            voce.click();
            sleep(1000);

            logAction("✅ " + nomeTotem + " selezionato");

        } catch (Exception e) {
            logWarn("⚠️ Errore selezione " + nomeTotem + ": " + e.getMessage());
            takeScreenshot("selezione_error_" + nomeTotem);
        }

        gestisciPopupColonninaNonAlimentata();
    }

    // =====================================================================================
    // POPUP HANDLER
    // =====================================================================================

    private boolean gestisciPopupColonninaNonAlimentata() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, WAIT_SHORT);
            WebElement popup = wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//*[contains(@text,'non è alimentata') or contains(@content-desc,'non è alimentata')]")));

            String messaggioCompleto = popup.getAttribute("text");
            if (messaggioCompleto == null || messaggioCompleto.isEmpty()) {
                messaggioCompleto = popup.getAttribute("content-desc");
            }

            logWarn("⚠️ POPUP COLONNINA NON ALIMENTATA");
            logWarn("📄 Messaggio: " + messaggioCompleto);
            logWarn("🔌 Colonnina: " + currentTotem);
            takeScreenshot("popup_non_alimentata_" + currentTotem);

            WebElement okBtn = driver.findElement(AppiumBy.accessibilityId("Ok"));
            okBtn.click();
            logAction("👆 Click su bottone 'Ok' per chiudere popup");
            sleep(500);

            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean gestisciPopupPresaAttiva() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, WAIT_SHORT);
            WebElement popup = wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//*[contains(@text,'presa attiva') or contains(@content-desc,'presa attiva')]")));

            String messaggioCompleto = popup.getAttribute("text");
            if (messaggioCompleto == null || messaggioCompleto.isEmpty()) {
                messaggioCompleto = popup.getAttribute("content-desc");
            }

            logWarn("⚠️ POPUP PRESA ATTIVA");
            logWarn("📄 Messaggio: " + messaggioCompleto);
            logWarn("🔌 Colonnina: " + currentTotem);
            takeScreenshot("popup_presa_attiva_" + currentTotem);

            WebElement okBtn = driver.findElement(AppiumBy.accessibilityId("Ok"));
            okBtn.click();
            logAction("👆 Click su bottone 'Ok' per chiudere popup");
            sleep(500);

            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // =====================================================================================
    // GESTIONE COLONNINE (CON ANALISI COLORE)
    // =====================================================================================

    private void gestisciColonnine(String gruppo, boolean accendi) {
        logHeader((accendi ? "ACCENSIONE " : "SPEGNIMENTO ") + gruppo);
        WebDriverWait wait = new WebDriverWait(driver, WAIT_MEDIUM);

        for (int i = 1; i <= 4; i++) {
            String xpath = "(//android.widget.Button[@content-desc='" + i + "'])["
                    + (gruppo.contains("Elettrica") ? 1 : 2) + "]";

            try {
                gestisciSingolaColonnina(wait, xpath, gruppo, i, accendi);
            } catch (Exception e) {
                logWarn("❌ Errore " + gruppo + " presa " + i + ": " + e.getMessage());
                takeScreenshot("errore_" + gruppo.replace(" ", "_") + "_" + i);
            }
        }

        takeScreenshot(gruppo.replace(" ", "_") + "_" + (accendi ? "on" : "off") + "_" + currentTotem);
    }

    private void gestisciSingolaColonnina(WebDriverWait wait, String xpath,
                                          String gruppo, int numero, boolean accendi) throws Exception {
        String nomeCompleto = gruppo + " → Presa " + numero + " (" + currentTotem + ")";

        logInfo("🔍 Analizzo: " + nomeCompleto);

        WebElement colonnina = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.xpath(xpath)));

        boolean statoIniziale = rilevaStatoColonnina(colonnina, gruppo, numero);

        if (accendi && statoIniziale) {
            logInfo("⏩ " + nomeCompleto + " già ACCESA, skip");
            return;
        } else if (!accendi && !statoIniziale) {
            logInfo("⏩ " + nomeCompleto + " già SPENTA, skip");
            return;
        }

        logAction("👆 Click su: " + nomeCompleto);
        colonnina.click();
        sleep(1000);

        if (gestisciPopupColonninaNonAlimentata() || gestisciPopupPresaAttiva()) {
            return;
        }

        boolean statoFinale = rilevaStatoColonnina(colonnina, gruppo, numero);
        logSuccess(nomeCompleto + (statoFinale ? " ✅ ACCESA" : " ⚫ SPENTA"));

        sleep(800);
    }

    // =====================================================================================
    // ANALISI COLORE SCREENSHOT
    // =====================================================================================

    private boolean rilevaStatoColonnina(WebElement colonnina, String gruppo, int numeroPresa) throws IOException {
        Rectangle rect = colonnina.getRect();
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        BufferedImage fullImg = ImageIO.read(screenshot);

        BufferedImage crop = fullImg.getSubimage(
                rect.getX(),
                rect.getY(),
                rect.getWidth(),
                rect.getHeight()
        );

        Color avgColor = calcolaColoreMedio(crop);
        boolean isOn = verificaStatoDaColore(avgColor, gruppo);

        String nomeCompleto = gruppo + " → Presa " + numeroPresa + " (" + currentTotem + ")";
        logInfo("📊 " + nomeCompleto + " - Stato: " + (isOn ? "ON ✅" : "OFF ⚫") +
                " [RGB: " + avgColor.getRed() + "," + avgColor.getGreen() + "," + avgColor.getBlue() + "]");

        return isOn;
    }

    private Color calcolaColoreMedio(BufferedImage img) {
        long r = 0, g = 0, b = 0;
        int count = 0;

        for (int x = 0; x < img.getWidth(); x += 3) {
            for (int y = 0; y < img.getHeight(); y += 3) {
                Color c = new Color(img.getRGB(x, y));
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
                count++;
            }
        }

        return new Color(
                (int) (r / count),
                (int) (g / count),
                (int) (b / count)
        );
    }

    private boolean verificaStatoDaColore(Color color, String gruppo) {
        float[] hsv = Color.RGBtoHSB(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                null
        );

        boolean isGreen = hsv[0] > HUE_GREEN_MIN && hsv[0] < HUE_GREEN_MAX
                && hsv[1] > SATURATION_MIN;
        boolean isBlue = hsv[0] > HUE_BLUE_MIN && hsv[0] < HUE_BLUE_MAX
                && hsv[1] > SATURATION_MIN;

        return gruppo.toLowerCase().contains("elettrica") ? isGreen : isBlue;
    }

    // =====================================================================================
    // TEST PRINCIPALE
    // =====================================================================================

    @BeforeAll
    public static void setupLogger() {
        loadConfiguration();
        initCsvLogger();
    }

    @Test
    @Order(1)
    public void testCambioColonninaEAccensioneSpegnimento() throws Exception {
        logHeader("═══════════════════════════════════════════════════════════");
        logHeader("   TEST CICLO COMPLETO COLONNINE - ANALISI COLORE");
        logHeader("═══════════════════════════════════════════════════════════");

        handleStartupFlow();
        effettuaLogin(username, password);
        vaiAllaHome();

        for (int i = 1; i <= totaleTotem; i++) {
            String nomeTotem = "Totem_" + i;
            currentTotem = nomeTotem;

            logHeader("═══════════════════════════════════════════════════════════");
            logHeader("🔌 TEST " + nomeTotem + " (" + i + "/" + totaleTotem + ")");
            logHeader("═══════════════════════════════════════════════════════════");

            try {
                eseguiTestTotem(nomeTotem);
            } catch (Exception e) {
                logError("❌ Errore test " + nomeTotem + ": " + e.getMessage());
                takeScreenshot("errore_" + nomeTotem);
            }

            vaiAllaHome();
        }

        logHeader("═══════════════════════════════════════════════════════════");
        logHeader("🏁 TUTTI I TEST COMPLETATI!");
        logHeader("═══════════════════════════════════════════════════════════");
    }

    private void eseguiTestTotem(String nomeTotem) throws Exception {
        selezionaColonninaDalMenu(nomeTotem);

        WebElement tabColonnina = new WebDriverWait(driver, WAIT_MEDIUM)
                .until(ExpectedConditions.elementToBeClickable(
                        AppiumBy.accessibilityId("Colonnina\nScheda 2 di 2")));
        tabColonnina.click();
        sleep(800);
        logStep("⚙️ Tab Colonnina aperto");

        // ⚡ CICLO COMPLETO ELETTRICO
        logHeader("⚡⚡⚡ CICLO COMPLETO PRESE ELETTRICHE (" + nomeTotem + ") ⚡⚡⚡");
        gestisciColonnine("Presa Elettrica", true);
        sleep(1000);
        gestisciColonnine("Presa Elettrica", false);

        // 💧 CICLO COMPLETO IDRICO
        logHeader("💧💧💧 CICLO COMPLETO EROGATORI IDRICI (" + nomeTotem + ") 💧💧💧");
        gestisciColonnine("Erogatore Idrico", true);
        sleep(1000);
        gestisciColonnine("Erogatore Idrico", false);

        logSuccess("✅✅✅ Test completato su " + nomeTotem + " ✅✅✅");
        takeScreenshot("completato_" + nomeTotem);
    }

    @AfterAll
    public static void chiudiLogger() {
        closeCsvLogger();
        System.out.println("\n📊 Report: " + logFile.getAbsolutePath());
        System.out.println("📸 Screenshots: " + SCREENSHOT_DIR.getAbsolutePath());
    }
}