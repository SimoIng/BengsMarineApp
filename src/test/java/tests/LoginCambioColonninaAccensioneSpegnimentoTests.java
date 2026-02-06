package tests;

import base.BaseAppiumTest;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.*;
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
import java.awt.Desktop;
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

@Epic("Test Colonnine")
@Feature("Login, Cambio Colonnina, Accensione/Spegnimento")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginCambioColonninaAccensioneSpegnimentoTests extends BaseAppiumTest {

    // =====================================================================================
    // COSTANTI
    // =====================================================================================

    private static final Duration WAIT_SHORT = Duration.ofSeconds(5);
    private static final Duration WAIT_MEDIUM = Duration.ofSeconds(10);
    private static final Duration WAIT_LONG = Duration.ofSeconds(20);
    private static final long ANIM_DELAY = 2000;
    private static final long ACTION_DELAY = 1000;

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

    @Attachment(value = "{name}", type = "image/png")
    private byte[] takeScreenshotForAllure(String name) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            System.out.println("⚠️ Errore screenshot Allure: " + e.getMessage());
            return new byte[0];
        }
    }

    private String takeScreenshot(String name) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File(SCREENSHOT_DIR, timestamp + "_" + name + ".png");
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            writeCsvLog("SCREENSHOT", "Screenshot: " + name, dest.getAbsolutePath());

            // Aggiungi screenshot anche ad Allure
            takeScreenshotForAllure(name);

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

    @Step("{msg}")
    private void logStep(String msg) { log("STEP", "⚙️", BLUE, msg); }

    private void logInfo(String msg) { log("INFO", "💬", CYAN, msg); }

    @Step("{msg}")
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
    // LOGIN
    // =====================================================================================

    @Step("Effettua login con username: {username}")
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

            try {
                ((AndroidDriver) driver).hideKeyboard();
                sleep(300);
                logInfo("⌨️ Tastiera chiusa");
            } catch (Exception ignored) {
                logInfo("ℹ️ Tastiera già chiusa");
            }

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

            WebElement accediBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.xpath("//*[contains(@text,'Accedi') or contains(@content-desc,'Accedi')]")));
            accediBtn.click();
            logAction("🎯 Click su 'Accedi'");

            swipeFincheNonCompareBottoneEntra();

            logSuccess("✅ Login completato");
            takeScreenshot("login_success");

        } catch (Exception e) {
            logError("❌ Errore login: " + e.getMessage());
            takeScreenshot("login_error");
            throw new RuntimeException("Login fallito", e);
        }
    }

    @Step("Swipe fino a comparsa bottone 'Entra'")
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

    @Step("Ritorno alla Home")
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

    @Step("Selezione colonnina: {nomeTotem}")
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

        String popupMsg = gestisciPopupColonninaNonAlimentata();
        if (popupMsg != null) {
            logWarn("⚠️ Colonnina " + nomeTotem + " risulta non alimentata al cambio");
        }
    }

    // =====================================================================================
    // POPUP HANDLER - ✅ CORRETTO
    // =====================================================================================

    @Step("Gestione popup colonnina non alimentata")
    private String gestisciPopupColonninaNonAlimentata() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, WAIT_SHORT);

            WebElement popup = wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//android.view.View[contains(@content-desc, 'non è alimentata')]")));

            String messaggioCompleto = popup.getAttribute("content-desc");

            logWarn("⚠️ POPUP COLONNINA NON ALIMENTATA RILEVATO");
            logWarn("📄 Messaggio: " + messaggioCompleto);
            logWarn("🔌 Colonnina: " + currentTotem);
            takeScreenshot("popup_non_alimentata_" + currentTotem);

            WebElement okBtn = driver.findElement(AppiumBy.accessibilityId("Ok"));
            okBtn.click();
            logAction("👆 Click su bottone 'Ok' per chiudere popup");
            sleep(500);

            return messaggioCompleto;

        } catch (TimeoutException e) {
            return null;
        } catch (NoSuchElementException e) {
            logWarn("⚠️ Popup trovato ma bottone Ok non trovato");
            takeScreenshot("popup_ok_button_missing");
            try {
                driver.findElement(AppiumBy.xpath("//android.widget.Button[@content-desc='Ok']")).click();
                logAction("👆 Click su bottone 'Ok' (metodo alternativo)");
                sleep(500);
                return "Popup chiuso con metodo alternativo";
            } catch (Exception ex) {
                logError("❌ Impossibile chiudere il popup");
                return null;
            }
        } catch (Exception e) {
            logWarn("⚠️ Errore durante la gestione del popup: " + e.getMessage());
            takeScreenshot("errore_popup_generico");
            return null;
        }
    }

    @Step("Gestione popup presa attiva")
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
    // GESTIONE COLONNINE
    // =====================================================================================

    @Step("Gestione colonnine {gruppo} - Accensione: {accendi}")
    private void gestisciColonnine(String gruppo, boolean accendi) {
        logHeader((accendi ? "ACCENSIONE " : "SPEGNIMENTO ") + gruppo);
        WebDriverWait wait = new WebDriverWait(driver, WAIT_MEDIUM);

        int fallimenti = 0;
        int presaNonAlimentate = 0;
        StringBuilder reportFallimenti = new StringBuilder();

        for (int i = 1; i <= 4; i++) {
            String xpath = "(//android.widget.Button[@content-desc='" + i + "'])["
                    + (gruppo.contains("Elettrica") ? 1 : 2) + "]";

            try {
                gestisciSingolaColonninaConRetry(wait, xpath, gruppo, i, accendi);

            } catch (ColonninaNonAlimentataException e) {
                presaNonAlimentate++;
                logWarn("⚠️ " + gruppo + " presa " + i + " - NON ALIMENTATA");
                reportFallimenti.append(String.format("⚡ Presa %d - NON ALIMENTATA\n", i));

            } catch (Exception e) {
                fallimenti++;
                String errorMsg = "❌ FALLIMENTO " + gruppo + " presa " + i + ": " + e.getMessage();
                logError(errorMsg);
                takeScreenshot("ERRORE_" + gruppo.replace(" ", "_") + "_" + i);

                String dettaglio = String.format("Presa %d - %s", i, e.getClass().getSimpleName());
                reportFallimenti.append(dettaglio).append("\n");
                Allure.addAttachment(
                        "❌ Errore Presa " + i,
                        "text/plain",
                        e.getMessage(),
                        ".txt"
                );

                logWarn("⚠️ Continuo con le altre prese...");
            }
        }

        takeScreenshot(gruppo.replace(" ", "_") + "_" + (accendi ? "on" : "off") + "_" + currentTotem);

        int totaleProblemi = fallimenti + presaNonAlimentate;
        if (totaleProblemi > 0) {
            String summary = String.format(
                    "⚠️ %s: %d/%d prese con problemi (%d non alimentate, %d errori)",
                    gruppo, totaleProblemi, 4, presaNonAlimentate, fallimenti
            );
            logWarn(summary);

            Allure.addAttachment(
                    "⚠️ Riepilogo " + gruppo,
                    "text/plain",
                    reportFallimenti.toString(),
                    ".txt"
            );
        } else {
            String success = "✅ " + gruppo + ": tutte le 4 prese OK";
            logSuccess(success);
            Allure.step(success, () -> {});
        }
    }

    private static class ColonninaNonAlimentataException extends Exception {
        public ColonninaNonAlimentataException(String message) {
            super(message);
        }
    }

    private void gestisciSingolaColonninaConRetry(WebDriverWait wait, String xpath,
                                                  String gruppo, int numero, boolean accendi) throws Exception {
        int maxRetry = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                if (attempt > 1) {
                    logWarn("🔄 Tentativo " + attempt + "/" + maxRetry + " per " + gruppo + " → Presa " + numero);
                    sleep(1500);
                }

                gestisciSingolaColonnina(wait, xpath, gruppo, numero, accendi);
                return;

            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                lastException = e;
                logWarn("⚠️ StaleElement su tentativo " + attempt + " - " + gruppo + " → Presa " + numero);

                if (attempt == maxRetry) {
                    logError("❌ StaleElement persistente dopo " + maxRetry + " tentativi");
                    throw new Exception("StaleElement dopo " + maxRetry + " tentativi: " + e.getMessage(), e);
                }

            } catch (Exception e) {
                throw e;
            }
        }

        throw lastException != null ? lastException : new Exception("Errore sconosciuto");
    }

    @Step("Gestione {gruppo} → Presa {numero} ({currentTotem}) - Accensione: {accendi}")
    private void gestisciSingolaColonnina(WebDriverWait wait, String xpath,
                                          String gruppo, int numero, boolean accendi) throws Exception {
        String nomeCompleto = gruppo + " → Presa " + numero + " (" + currentTotem + ")";

        logInfo("🔍 Analizzo: " + nomeCompleto);

        try {
            WebElement colonnina = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.xpath(xpath)));

            boolean statoIniziale = rilevaStatoColonnina(colonnina, gruppo, numero);

            if (accendi && statoIniziale) {
                logInfo("⏩ " + nomeCompleto + " già ACCESA, skip");
                Allure.step(nomeCompleto + " - Skip (già accesa)", () -> {});
                return;
            } else if (!accendi && !statoIniziale) {
                logInfo("⏩ " + nomeCompleto + " già SPENTA, skip");
                Allure.step(nomeCompleto + " - Skip (già spenta)", () -> {});
                return;
            }

            logAction("👆 Click su: " + nomeCompleto);
            colonnina = driver.findElement(AppiumBy.xpath(xpath));
            colonnina.click();
            sleep(2000);

            String popupMessaggio = gestisciPopupColonninaNonAlimentata();
            if (popupMessaggio != null) {
                String dettaglioErrore = String.format(
                        "🔌 COLONNINA NON ALIMENTATA\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "📍 Totem: %s\n" +
                                "⚡ Gruppo: %s\n" +
                                "🔢 Presa: %d\n" +
                                "🔄 Azione: %s\n" +
                                "💬 Popup: \"%s\"\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━",
                        currentTotem,
                        gruppo,
                        numero,
                        (accendi ? "Tentativo ACCENSIONE" : "Tentativo SPEGNIMENTO"),
                        popupMessaggio
                );

                logWarn("⚠️ " + nomeCompleto + " → COLONNINA NON ALIMENTATA");

                Allure.step(nomeCompleto + " - ⚠️ NON ALIMENTATA", () -> {
                    Allure.addAttachment(
                            "Dettagli Colonnina Non Alimentata",
                            "text/plain",
                            dettaglioErrore,
                            ".txt"
                    );
                });

                throw new ColonninaNonAlimentataException(
                        "Colonnina non alimentata: " + currentTotem + " - " + gruppo + " presa " + numero
                );
            }

            if (gestisciPopupPresaAttiva()) {
                logWarn("⚠️ " + nomeCompleto + " → PRESA ATTIVA");
                Allure.step(nomeCompleto + " - ⚠️ Presa attiva", () -> {});
                return;
            }

            colonnina = driver.findElement(AppiumBy.xpath(xpath));
            boolean statoFinale = rilevaStatoColonnina(colonnina, gruppo, numero);

            String risultato = statoFinale ? " ✅ ACCESA" : " ⚫ SPENTA";
            logSuccess(nomeCompleto + risultato);

            Allure.step(nomeCompleto + risultato, () -> {});

            sleep(1500);

        } catch (ColonninaNonAlimentataException e) {
            throw e;
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            logError("❌ StaleElement su " + nomeCompleto);
            throw e;
        } catch (Exception e) {
            logError("❌ Errore generico su " + nomeCompleto + ": " + e.getMessage());
            throw e;
        }
    }

    // =====================================================================================
    // ANALISI COLORE
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
    @Epic("Test Colonnine")
    @Feature("Ciclo Completo")
    @Story("Cambio colonnina, accensione e spegnimento prese")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test completo che esegue il ciclo di cambio colonnina, accensione e spegnimento di tutte le prese elettriche e idriche per tutti i totem configurati")
    public void testCambioColonninaEAccensioneSpegnimento() throws Exception {
        logHeader("═══════════════════════════════════════════════════════════");
        logHeader("   TEST CICLO COMPLETO COLONNINE - ANALISI COLORE");
        logHeader("═══════════════════════════════════════════════════════════");

        handleStartupFlow();
        effettuaLogin(username, password);
        vaiAllaHome();

        int totemCompletati = 0;
        int totemFalliti = 0;
        StringBuilder reportGlobale = new StringBuilder();
        reportGlobale.append("REPORT GLOBALE TEST\n");
        reportGlobale.append("===================\n\n");

        for (int i = 1; i <= totaleTotem; i++) {
            String nomeTotem = "Totem_" + i;
            currentTotem = nomeTotem;

            logHeader("═══════════════════════════════════════════════════════════");
            logHeader("🔌 TEST " + nomeTotem + " (" + i + "/" + totaleTotem + ")");
            logHeader("═══════════════════════════════════════════════════════════");

            try {
                eseguiTestTotem(nomeTotem);
                totemCompletati++;
                reportGlobale.append("✅ ").append(nomeTotem).append(": COMPLETATO\n");
            } catch (Exception e) {
                totemFalliti++;
                logError("❌ Errore critico test " + nomeTotem + ": " + e.getMessage());
                takeScreenshot("errore_critico_" + nomeTotem);
                reportGlobale.append("❌ ").append(nomeTotem).append(": ERRORE CRITICO - ").append(e.getMessage()).append("\n");

                Allure.addAttachment(
                        "❌ Errore Critico " + nomeTotem,
                        "text/plain",
                        e.getMessage(),
                        ".txt"
                );
            }

            vaiAllaHome();
        }

        logHeader("═══════════════════════════════════════════════════════════");
        logHeader("🏁 TUTTI I TEST COMPLETATI!");
        logHeader("═══════════════════════════════════════════════════════════");

        reportGlobale.append("\n===================\n");
        reportGlobale.append(String.format("Totem testati: %d/%d\n", totemCompletati + totemFalliti, totaleTotem));
        reportGlobale.append(String.format("✅ Completati: %d\n", totemCompletati));
        reportGlobale.append(String.format("❌ Falliti: %d\n", totemFalliti));

        String summary = reportGlobale.toString();
        logHeader(summary);

        Allure.addAttachment(
                "📊 Report Globale Test",
                "text/plain",
                summary,
                ".txt"
        );
    }

    @Step("Esecuzione test completo per {nomeTotem}")
    private void eseguiTestTotem(String nomeTotem) throws Exception {
        selezionaColonninaDalMenu(nomeTotem);

        WebElement tabColonnina = new WebDriverWait(driver, WAIT_MEDIUM)
                .until(ExpectedConditions.elementToBeClickable(
                        AppiumBy.accessibilityId("Colonnina\nScheda 2 di 2")));
        tabColonnina.click();
        sleep(800);
        logStep("⚙️ Tab Colonnina aperto");

        logHeader("⚡⚡⚡ CICLO COMPLETO PRESE ELETTRICHE (" + nomeTotem + ") ⚡⚡⚡");
        gestisciColonnine("Presa Elettrica", true);
        sleep(3000);
        gestisciColonnine("Presa Elettrica", false);

        logHeader("💧💧💧 CICLO COMPLETO EROGATORI IDRICI (" + nomeTotem + ") 💧💧💧");
        gestisciColonnine("Erogatore Idrico", true);
        sleep(3000);
        gestisciColonnine("Erogatore Idrico", false);

        logSuccess("✅✅✅ Test completato su " + nomeTotem + " ✅✅✅");
        takeScreenshot("completato_" + nomeTotem);
    }

    // =====================================================================================
    // ✨ GENERAZIONE REPORT ALLURE E PDF CON PULSANTE DOWNLOAD
    // =====================================================================================

    @AfterAll
    public static void chiudiLoggerEGeneraReportCompleto() {
        closeCsvLogger();

        System.out.println("\n📊 Report CSV: " + logFile.getAbsolutePath());
        System.out.println("📸 Screenshots: " + SCREENSHOT_DIR.getAbsolutePath());

        // Genera report Allure HTML statico
        generaReportAllureStatic();

        // Genera PDF e aggiungi pulsante download
        generaPDFConPulsante();

        // Apri il report nel browser
        apriReportNelBrowser();
    }

    /**
     * Genera il report Allure HTML statico
     */
    private static void generaReportAllureStatic() {
        System.out.println("\n🔄 Generazione report Allure HTML...");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "allure", "generate",
                    "build/allure-results", "--clean",
                    "-o", "build/allure-report"
            );
            pb.inheritIO();
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Report HTML generato: build/allure-report/index.html");
            } else {
                System.out.println("⚠️ Errore generazione report HTML (exit code: " + exitCode + ")");
            }

        } catch (Exception e) {
            System.out.println("⚠️ Errore generazione report: " + e.getMessage());
        }
    }

    /**
     * Genera PDF e aggiunge il pulsante di download al report
     */
    private static void generaPDFConPulsante() {
        System.out.println("\n📄 Generazione PDF e aggiunta pulsante download...");

        try {
            // Attendi che il report HTML sia pronto
            Thread.sleep(2000);

            // Verifica che il report esista
            File reportIndex = new File("build/allure-report/index.html");
            if (!reportIndex.exists()) {
                System.out.println("⚠️ Report HTML non trovato, impossibile generare PDF");
                return;
            }

            // Esegui script Node.js
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "node", "generate-pdf-with-button.js"
            );
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                File pdfFile = new File("build/allure-report/report.pdf");
                System.out.println("✅ PDF generato: " + pdfFile.getAbsolutePath());
                System.out.println("✅ Pulsante download aggiunto al report");
                System.out.println("💡 Apri il report per vedere il pulsante 'PDF' accanto a 'CSV'");
            } else {
                System.out.println("⚠️ Errore durante la generazione del PDF (exit code: " + exitCode + ")");
                System.out.println("💡 Verifica che Node.js e Puppeteer siano installati:");
                System.out.println("   npm install puppeteer");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("⚠️ Processo interrotto");
        } catch (Exception e) {
            System.out.println("⚠️ Impossibile generare PDF: " + e.getMessage());
            System.out.println("💡 Setup richiesto:");
            System.out.println("   1. Installa Node.js: https://nodejs.org/");
            System.out.println("   2. Esegui: npm install puppeteer");
            System.out.println("   3. Copia generate-pdf-with-button.js nella root del progetto");
        }
    }

    /**
     * Apre il report nel browser predefinito
     */
    private static void apriReportNelBrowser() {
        System.out.println("\n🌐 Apertura report nel browser...");

        try {
            File reportFile = new File("build/allure-report/index.html");

            if (Desktop.isDesktopSupported() && reportFile.exists()) {
                Desktop.getDesktop().browse(reportFile.toURI());
                System.out.println("✅ Report aperto nel browser");
                System.out.println("💡 Cerca il pulsante 'PDF' nella barra di navigazione");
            }

        } catch (Exception e) {
            System.out.println("⚠️ Impossibile aprire automaticamente il browser");
            System.out.println("💡 Apri manualmente: build/allure-report/index.html");
        }
    }
}
