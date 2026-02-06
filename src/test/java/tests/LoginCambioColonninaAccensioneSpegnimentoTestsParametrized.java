package tests;

import base.BaseAppiumTest;
import config.UserConfig;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Stream;
import java.util.Comparator;

@Epic("Test Colonnine")
@Feature("Login, Cambio Colonnina, Accensione/Spegnimento")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginCambioColonninaAccensioneSpegnimentoTestsParametrized extends BaseAppiumTest {

    // =====================================================================================
    // COSTANTI
    // =====================================================================================

    private static final Duration WAIT_SHORT = Duration.ofSeconds(5);
    private static final Duration WAIT_MEDIUM = Duration.ofSeconds(10);
    private static final Duration WAIT_LONG = Duration.ofSeconds(20);
    private static final long ACTION_DELAY = 1000;
    private static final long INTER_ACTION_DELAY = 1500;

    private static final long ANIMATION_CHECK_INTERVAL = 500;
    private static final int ANIMATION_MAX_CHECKS = 20;

    // ⭐ DELAY per stabilizzazione colore dopo animazione
    private static final long COLOR_STABILIZATION_DELAY = 3000;

    // ⭐ NUOVO: Configurazione ricreazione sessione
    // ⭐ CONFIGURAZIONE RICREAZIONE SESSIONE ⭐
    // Ricrea la sessione Appium ogni N cicli per pulire la memoria UIAutomator2
    // Valore 1 = Ricrea SEMPRE (consigliato per device con poca RAM come OUKITEL C5)
    // Valore 5 = Ricrea ogni 5 cicli (per device più potenti)
    // Valore 10 = Ricrea ogni 10 cicli (solo device molto potenti)
    private static final int CICLI_PER_RICREAZIONE_SESSIONE = 1;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter REPORT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

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
    private String currentUser = "-";

    // ⭐ GESTIONE CICLI MULTIPLI
    private static int numeroCicli = 1;
    private static int pausaTraCicli = 5;
    private static int cicloCorrente = 0;

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String RED = "\u001B[31m";
    private static final String WHITE_BOLD = "\u001B[1;37m";

    // =====================================================================================
    // PROVIDER PER TEST PARAMETRIZZATI CON CICLI MULTIPLI
    // =====================================================================================

    /**
     * ⭐ PROVIDER AVANZATO CON SUPPORTO CICLI MULTIPLI
     * Legge numero.cicli dal file test.properties e crea N copie di ogni utente
     */
    static Stream<Map<String, Object>> allEnabledUsersProviderWithCycles() {
        // Carica configurazione cicli
        caricaConfigurazioneCicli();

        List<UserConfig> users = UserConfig.loadAllEnabledUsers();

        if (users.isEmpty()) {
            System.out.println("⚠️ Nessun utente abilitato, uso configurazione di default");
            users.add(UserConfig.loadDefault());
        }

        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║           📋 CONFIGURAZIONE TEST CICLI MULTIPLI          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println("🔄 Numero cicli configurati: " + numeroCicli);
        System.out.println("⏱️  Pausa tra cicli: " + pausaTraCicli + " secondi");
        System.out.println("🔄 Ricreazione sessione ogni: " + CICLI_PER_RICREAZIONE_SESSIONE + " cicli");
        System.out.println("👥 Utenti da testare: " + users.size());
        System.out.println("═══════════════════════════════════════════════════════════");

        for (int i = 0; i < users.size(); i++) {
            System.out.println((i + 1) + ". " + users.get(i).toStringSafe());
        }

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("📊 TEST TOTALI DA ESEGUIRE: " + (users.size() * numeroCicli));
        System.out.println("   └─ " + users.size() + " utenti × " + numeroCicli + " cicli ciascuno");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // Crea stream con cicli multipli
        List<Map<String, Object>> testsWithCycles = new ArrayList<>();

        for (int ciclo = 1; ciclo <= numeroCicli; ciclo++) {
            for (UserConfig user : users) {
                Map<String, Object> testData = new HashMap<>();
                testData.put("user", user);
                testData.put("ciclo", ciclo);
                testData.put("totaleCicli", numeroCicli);
                testsWithCycles.add(testData);
            }
        }

        return testsWithCycles.stream();
    }

    /**
     * Carica configurazione cicli da test.properties
     */
    private static void caricaConfigurazioneCicli() {
        try (InputStream input = new FileInputStream("test.properties")) {
            Properties props = new Properties();
            props.load(input);

            numeroCicli = Integer.parseInt(props.getProperty("numero.cicli", "1"));
            pausaTraCicli = Integer.parseInt(props.getProperty("pausa.tra.cicli", "5"));

            // Validazione
            if (numeroCicli < 1) {
                System.out.println("⚠️ numero.cicli non valido (" + numeroCicli + "), uso default=1");
                numeroCicli = 1;
            }
            if (pausaTraCicli < 0) {
                System.out.println("⚠️ pausa.tra.cicli non valida (" + pausaTraCicli + "), uso default=5");
                pausaTraCicli = 5;
            }

        } catch (Exception e) {
            System.out.println("⚠️ Errore lettura configurazione cicli, uso valori default");
            System.out.println("   Errore: " + e.getMessage());
            numeroCicli = 1;
            pausaTraCicli = 5;
        }
    }

    // =====================================================================================
    // ⭐ NUOVA SEZIONE: RICREAZIONE SESSIONE E PULIZIA MEMORIA
    // =====================================================================================

    /**
     * ⭐ RICREA LA SESSIONE APPIUM
     * Chiude la sessione corrente, pulisce memoria, e crea una nuova sessione fresca
     */
    private void ricreaSessioneAppium() throws Exception {
        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║         🔄 RICREAZIONE SESSIONE APPIUM                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");

        try {
            // STEP 1: Chiudi sessione vecchia
            System.out.println("📱 [1/4] Chiusura sessione vecchia...");
            if (driver != null) {
                try {
                    driver.quit();
                    driver = null;
                    System.out.println("   ✅ Sessione chiusa");
                } catch (Exception e) {
                    System.out.println("   ⚠️ Errore chiusura: " + e.getMessage());
                }
            }

            // STEP 2: Pausa per pulizia completa
            System.out.println("⏳ [2/4] Attesa pulizia (5 secondi)...");
            Thread.sleep(5000);

            // STEP 3: Pulisci memoria telefono
            System.out.println("🧹 [3/4] Pulizia memoria telefono...");
            pulisciMemoriaTelefono();

            // STEP 4: Ricrea driver con nuova sessione
            System.out.println("✨ [4/4] Creazione nuova sessione...");
            ricreaDriver(); // ⭐ Usa il metodo della classe base

            System.out.println("╔═══════════════════════════════════════════════════════════╗");
            System.out.println("║         ✅ NUOVA SESSIONE CREATA CON SUCCESSO            ║");
            System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        } catch (Exception e) {
            System.out.println("╔═══════════════════════════════════════════════════════════╗");
            System.out.println("║         ❌ ERRORE RICREAZIONE SESSIONE                   ║");
            System.out.println("╚═══════════════════════════════════════════════════════════╝");
            System.out.println("Errore: " + e.getMessage());
            throw new Exception("Impossibile ricreare sessione Appium", e);
        }
    }

    /**
     * ⭐ PULISCE LA MEMORIA DEL TELEFONO ANDROID
     * Esegue comandi ADB per liberare risorse e chiudere processi in background
     */
    private void pulisciMemoriaTelefono() {
        try {
            // Ottieni l'UDID del dispositivo dalle capabilities
            String udid = getDeviceUdid();

            if (udid == null || udid.isEmpty()) {
                System.out.println("   ⚠️ UDID non disponibile, skip pulizia memoria");
                return;
            }

            System.out.println("   📱 Dispositivo: " + udid);

            // Comando 1: Chiudi tutte le app in background
            System.out.println("   🔄 Chiusura app in background...");
            ProcessBuilder pb1 = new ProcessBuilder("adb", "-s", udid, "shell", "am", "kill-all");
            Process p1 = pb1.start();
            p1.waitFor();
            System.out.println("      ✅ App chiuse");

            Thread.sleep(1000);

            // Comando 2: Pulisci cache UIAutomator2
            System.out.println("   🗑️ Pulizia cache UIAutomator2...");
            ProcessBuilder pb2 = new ProcessBuilder("adb", "-s", udid, "shell", "pm", "clear",
                    "io.appium.uiautomator2.server");
            Process p2 = pb2.start();
            p2.waitFor();
            System.out.println("      ✅ Cache pulita");

            Thread.sleep(1000);

            // Comando 3: Forza garbage collection
            System.out.println("   🧹 Garbage collection...");
            ProcessBuilder pb3 = new ProcessBuilder("adb", "-s", udid, "shell", "am", "broadcast",
                    "-a", "com.android.server.ActivityManagerService.action.TRIM_MEMORY");
            Process p3 = pb3.start();
            p3.waitFor();
            System.out.println("      ✅ GC completato");

            System.out.println("   ✅ Pulizia memoria completata");

        } catch (Exception e) {
            System.out.println("   ⚠️ Errore pulizia memoria: " + e.getMessage());
            System.out.println("   ℹ️ Continuo comunque...");
        }
    }

    /**
     * Ottiene l'UDID del dispositivo dalla classe base
     */
    private String getDeviceUdid() {
        return getUdid();
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
            writer.write("Timestamp;Tipo;Utente;Totem;Ciclo;Messaggio;Screenshot");
            writer.newLine();
            writer.flush();
            System.out.println("📘 CSV logger inizializzato → " + logFile.getAbsolutePath());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (writer != null) {
                        writer.close();
                        System.out.println("💾 CSV chiuso via shutdown hook");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

        } catch (IOException e) {
            System.out.println("⚠️ Errore inizializzazione CSV: " + e.getMessage());
        }
    }

    private void writeCsvLog(String tipo, String msg, String screenshotPath) {
        try {
            if (writer != null) {
                String cleanMsg = msg.replace(";", ",").replace("\n", " ");
                String line = String.format("%s;%s;%s;%s;%d/%d;%s;%s",
                        LocalTime.now().format(TIME_FORMAT),
                        tipo,
                        currentUser,
                        currentTotem,
                        cicloCorrente,
                        numeroCicli,
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
            File dest = new File(SCREENSHOT_DIR, timestamp + "_C" + cicloCorrente + "_" + currentUser + "_" + name + ".png");
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            writeCsvLog("SCREENSHOT", "Screenshot: " + name, dest.getAbsolutePath());

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
        String formatted = String.format("%s %s%s [%s - C%d/%d - %s]%s %s",
                timestamp, color, icona, currentUser, cicloCorrente, numeroCicli, tipo, RESET, msg);
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

    @Step("Reinizializzazione app per nuovo utente")
    private void reinizializzaApp() {
        try {
            logStep("🔄 Reinizializzazione app per nuovo utente...");

            if (driver != null) {
                try {
                    driver.terminateApp("com.bithiatec.bengsMarine");
                    logInfo("📱 App terminata");
                    sleep(2000);
                } catch (Exception e) {
                    logWarn("⚠️ Impossibile terminare l'app: " + e.getMessage());
                }
            }

            driver.activateApp("com.bithiatec.bengsMarine");
            logSuccess("✅ App riavviata");
            sleep(3000);

        } catch (Exception e) {
            logError("❌ Errore reinizializzazione app: " + e.getMessage());
        }
    }

    // =====================================================================================
    // LOGIN
    // =====================================================================================

    @Step("Effettua login - User: {userConfig.descrizione}")
    private void effettuaLogin(UserConfig userConfig) {
        logHeader("LOGIN - " + userConfig.getDescrizione());
        logInfo("👤 Username: " + userConfig.getUsername());
        logInfo("🔢 Totem disponibili: " + userConfig.getTotaleTotem());

        // ⭐ CONTROLLA SE GIÀ LOGGATO (RICORDAMI ATTIVO)
        try {
            WebDriverWait waitShort = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement homeButton = waitShort.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.accessibilityId("Home\nScheda 1 di 2")));
            if (homeButton.isDisplayed()) {
                logSuccess("✅ Utente già loggato (Ricordami attivo), skip login");
                Allure.step("Login automatico - Ricordami attivo", () -> {});
                takeScreenshot("gia_loggato");
                return;  // ⬅️ ESCI SUBITO, NON FARE LOGIN!
            }
        } catch (TimeoutException e) {
            logInfo("🔐 Schermata di login rilevata, procedo con login manuale");
        }

        // ⬇️ DA QUI IN POI IL CODICE RIMANE IDENTICO
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
            campoUsername.sendKeys(userConfig.getUsername());
            logInfo("✅ Username inserito");

            Map<String, Object> tapPass = new HashMap<>();
            Point pPass = campoPassword.getLocation();
            tapPass.put("x", pPass.getX() + campoPassword.getSize().getWidth() / 2);
            tapPass.put("y", pPass.getY() + campoPassword.getSize().getHeight() / 2);
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", tapPass);

            sleep(600);
            campoPassword.clear();
            campoPassword.sendKeys(userConfig.getPassword());
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

            logSuccess("✅ Login completato per: " + userConfig.getDescrizione());
            takeScreenshot("login_success");

        } catch (Exception e) {
            logError("❌ Errore login: " + e.getMessage());
            takeScreenshot("login_error");
            throw new RuntimeException("Login fallito per " + userConfig.getDescrizione(), e);
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

        // ⭐ Chiudi eventuali modal aperti PRIMA di tornare alla Home
        chiudiEventualiModal();

        WebDriverWait wait = new WebDriverWait(driver, WAIT_MEDIUM);
        try {
            WebElement homeButton = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.accessibilityId("Home\nScheda 1 di 2")));
            homeButton.click();
            sleep(500);

            // Aspetta che appaia almeno UN totem (Button che non sia Home/Scheda)
            wait.until(driver -> {
                List<WebElement> buttons = driver.findElements(AppiumBy.className("android.widget.Button"));
                for (WebElement btn : buttons) {
                    try {
                        if (btn.isDisplayed()) {
                            String desc = btn.getAttribute("content-desc");
                            if (desc != null && !desc.isEmpty() &&
                                    !desc.contains("Home") && !desc.contains("Scheda") &&
                                    !desc.contains("Apri il menu")) {
                                return true; // Trovato almeno un totem!
                            }
                        }
                    } catch (Exception ignored) {}
                }
                return false;
            });

            logSuccess("🏠 Home caricata");
        } catch (Exception e) {
            logWarn("⚠️ Errore ritorno Home: " + e.getMessage());
            takeScreenshot("home_error");
        }
    }

    /**
     * Valida e ottiene il numero REALE di totem disponibili per l'utente.
     * Se il numero configurato nel properties è sbagliato, lo corregge automaticamente.
     *
     * @param numeroConfigurato Numero totem configurato nel test.properties
     * @return Numero REALE di totem disponibili
     */
    private int validaNumeroTotem(int numeroConfigurato) {
        logInfo("🔍 Validazione numero totem disponibili...");
        logInfo("   📋 Configurato nel properties: " + numeroConfigurato);

        try {
            WebDriverWait waitMedium = new WebDriverWait(driver, WAIT_MEDIUM);

            // STEP 1: Chiudi eventuali modal/overlay
            chiudiEventualiModal();

            // STEP 2: Trova il dropdown
            Dimension screenSize = driver.manage().window().getSize();
            int screenHeight = screenSize.getHeight();
            int screenWidth = screenSize.getWidth();
            int topBarHeight = (int) (screenHeight * 0.15);
            int bottomBarHeight = (int) (screenHeight * 0.85);

            List<WebElement> allButtons = driver.findElements(AppiumBy.className("android.widget.Button"));
            WebElement dropdown = null;

            for (WebElement btn : allButtons) {
                try {
                    if (btn.isDisplayed()) {
                        Point location = btn.getLocation();
                        Dimension size = btn.getSize();
                        int centerY = location.getY() + (size.getHeight() / 2);
                        boolean isInCentralArea = centerY > topBarHeight && centerY < bottomBarHeight;
                        boolean isWideEnough = size.getWidth() > (screenWidth * 0.3);

                        if (isInCentralArea && isWideEnough) {
                            dropdown = btn;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (dropdown == null) {
                logWarn("   ⚠️ Dropdown non trovato, uso numero configurato");
                return numeroConfigurato;
            }

            // STEP 3: Apri dropdown e conta totem
            dropdown.click();
            sleep(2000);

            List<WebElement> colonnineValide = waitMedium.until(driver -> {
                List<WebElement> buttons = driver.findElements(AppiumBy.className("android.widget.Button"));
                List<WebElement> validi = new ArrayList<>();

                for (WebElement btn : buttons) {
                    try {
                        if (btn.isDisplayed()) {
                            String desc = btn.getAttribute("content-desc");
                            if (desc == null || desc.isEmpty() || desc.equals("null")) continue;
                            if (desc.contains("Apri il menu") || desc.contains("Home") ||
                                    desc.contains("Scheda") || desc.contains("Cerca")) continue;
                            if (desc.matches("^[0-9]$")) continue;
                            if (desc.matches("^[0-9]+\\s*/\\s*[0-9]+$")) continue;
                            if (desc.length() < 3) continue;
                            validi.add(btn);
                        }
                    } catch (Exception ignored) {}
                }
                return validi.size() >= 2 ? validi : null;
            });

            int numeroReale = colonnineValide != null ? colonnineValide.size() : numeroConfigurato;

            // STEP 4: Chiudi dropdown
            driver.navigate().back();
            sleep(1500);

            // STEP 5: Validazione e warning se necessario
            logInfo("   📊 Totem reali trovati: " + numeroReale);

            if (numeroReale != numeroConfigurato) {
                logWarn("⚠️ ═══════════════════════════════════════════════════════════");
                logWarn("⚠️ ATTENZIONE: NUMERO TOTEM NON CORRETTO!");
                logWarn("⚠️ ═══════════════════════════════════════════════════════════");
                logWarn("⚠️ Configurato in test.properties: " + numeroConfigurato);
                logWarn("⚠️ Totem reali nel dropdown:       " + numeroReale);
                logWarn("⚠️ → Il test userà il numero REALE: " + numeroReale);
                logWarn("⚠️ → AGGIORNA test.properties con: userX.totale_totem=" + numeroReale);
                logWarn("⚠️ ═══════════════════════════════════════════════════════════");
            } else {
                logInfo("   ✅ Numero totem validato correttamente");
            }

            return numeroReale;

        } catch (Exception e) {
            logWarn("   ⚠️ Errore validazione: " + e.getMessage());
            logWarn("   → Uso numero configurato: " + numeroConfigurato);
            return numeroConfigurato;
        }
    }

    @Step("Selezione colonnina per posizione: #{indiceTotem}")
    private void selezionaColonninaDalMenu(int indiceTotem) {
        String nomeVisualizzato = "Colonnina #" + indiceTotem;
        currentTotem = nomeVisualizzato;
        logStep("🔍 Selezione colonnina per posizione: #" + indiceTotem);

        WebDriverWait waitMedium = new WebDriverWait(driver, WAIT_MEDIUM);

        try {
            // STEP 0: Chiudi eventuali modal/overlay aperti per errore
            chiudiEventualiModal();

            // STEP 1: Ottieni dimensioni schermo
            Dimension screenSize = driver.manage().window().getSize();
            int screenHeight = screenSize.getHeight();
            int screenWidth = screenSize.getWidth();

            // Zone da escludere (barre di navigazione)
            int topBarHeight = (int) (screenHeight * 0.15);
            int bottomBarHeight = (int) (screenHeight * 0.85);

            logInfo("🔽 Cerco dropdown nella zona centrale dello schermo...");

            // STEP 2: Trova il dropdown per posizione
            List<WebElement> allButtons = driver.findElements(AppiumBy.className("android.widget.Button"));
            WebElement dropdown = null;

            for (WebElement btn : allButtons) {
                try {
                    if (btn.isDisplayed()) {
                        Point location = btn.getLocation();
                        Dimension size = btn.getSize();

                        int centerY = location.getY() + (size.getHeight() / 2);
                        boolean isInCentralArea = centerY > topBarHeight && centerY < bottomBarHeight;
                        boolean isWideEnough = size.getWidth() > (screenWidth * 0.3);

                        if (isInCentralArea && isWideEnough) {
                            dropdown = btn;
                            String desc = btn.getAttribute("content-desc");
                            logInfo("   📍 Trovato dropdown: " + (desc != null ? desc : "[senza nome]"));
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (dropdown == null) {
                throw new RuntimeException("Dropdown non trovato");
            }

            // STEP 3: Clicca per aprire il dropdown
            dropdown.click();
            logInfo("   ⏳ Attesa apertura dropdown...");
            sleep(2000);

            // STEP 4: Recupera SOLO i totem validi (filtro intelligente)
            logInfo("🔍 Recupero lista totem dal dropdown aperto...");

            List<WebElement> colonnineValide = waitMedium.until(driver -> {
                List<WebElement> buttons = driver.findElements(AppiumBy.className("android.widget.Button"));
                List<WebElement> validi = new ArrayList<>();

                for (WebElement btn : buttons) {
                    try {
                        if (btn.isDisplayed()) {
                            String desc = btn.getAttribute("content-desc");

                            // ⭐ FILTRO INTELLIGENTE: Escludi spazzatura
                            if (desc == null || desc.isEmpty() || desc.equals("null")) {
                                continue; // ❌ null o vuoto
                            }

                            // ❌ Escludi controlli UI
                            if (desc.contains("Apri il menu") || desc.contains("Home") ||
                                    desc.contains("Scheda") || desc.contains("Cerca")) {
                                continue;
                            }

                            // ❌ Escludi numeri singoli (1, 2, 3, 4)
                            if (desc.matches("^[0-9]$")) {
                                continue;
                            }

                            // ❌ Escludi contatori (es: "0 / 4", "4 / 4")
                            if (desc.matches("^[0-9]+\\s*/\\s*[0-9]+$")) {
                                continue;
                            }

                            // ❌ Escludi elementi troppo corti (probabilmente numeri o simboli)
                            if (desc.length() < 3) {
                                continue;
                            }

                            // ✅ Elemento valido!
                            validi.add(btn);
                        }
                    } catch (Exception ignored) {}
                }

                // Dropdown aperto quando ci sono 2+ totem validi
                return validi.size() >= 2 ? validi : null;
            });

            if (colonnineValide == null || colonnineValide.isEmpty()) {
                throw new RuntimeException("Nessun totem valido trovato dopo apertura dropdown");
            }

            logInfo("📋 Trovati " + colonnineValide.size() + " totem validi:");

            // Debug: stampa tutti i totem trovati
            for (int i = 0; i < colonnineValide.size(); i++) {
                String desc = colonnineValide.get(i).getAttribute("content-desc");
                logInfo("   [" + (i + 1) + "] " + desc);
            }

            if (indiceTotem > colonnineValide.size()) {
                throw new RuntimeException("Indice " + indiceTotem + " fuori range (max: " + colonnineValide.size() + ")");
            }

            // STEP 5: Seleziona per indice (0-based)
            WebElement colonninaDaSelezionare = colonnineValide.get(indiceTotem - 1);
            String nomeReale = colonninaDaSelezionare.getAttribute("content-desc");

            logInfo("👆 Click su posizione #" + indiceTotem + " (" + nomeReale + ")");
            colonninaDaSelezionare.click();
            sleep(1000);

            currentTotem = nomeReale != null ? nomeReale : nomeVisualizzato;
            logAction("✅ Totem #" + indiceTotem + " selezionato: " + currentTotem);

        } catch (Exception e) {
            logWarn("⚠️ Errore selezione totem #" + indiceTotem + ": " + e.getMessage());
            takeScreenshot("selezione_error_totem_" + indiceTotem);
            currentTotem = nomeVisualizzato;
        }
    }

    /**
     * ⭐ CHIUDE EVENTUALI MODAL/OVERLAY APERTI PER ERRORE
     * Durante il test delle prese, a volte si apre per errore un overlay "PRESE TERMINATE"
     * o altri modal che bloccano l'interazione. Questo metodo li chiude.
     *
     * OVERLAY RILEVATI:
     * 1. "PRESE TERMINATE" - Modal che si apre cambiando colonnina
     * 2. "Hai collegato tutte le prese" - Popup informativo
     * 3. Overlay prese totali - Si apre cliccando su "X / Y" per errore
     */
    private void chiudiEventualiModal() {
        try {
            boolean modalChiuso = false;
            logInfo("🔍 Verifico presenza modal/overlay...");

            // ═══════════════════════════════════════════════════════
            // 1. CERCA BLUR/EDIT VIEW (overlay generico)
            // ═══════════════════════════════════════════════════════
            try {
                List<WebElement> blurViews = driver.findElements(
                        AppiumBy.xpath("//*[contains(@resource-id, 'blur') or contains(@resource-id, 'edit')]"));

                logInfo("   Control 1 (blur views): trovate " + blurViews.size());

                if (!blurViews.isEmpty()) {
                    logWarn("⚠️ Rilevato modal/overlay (blur view), lo chiudo...");
                    driver.navigate().back();
                    sleep(1500); // Attesa per stabilizzazione UI
                    logInfo("   ✓ Modal chiuso con tasto BACK");
                    modalChiuso = true;
                    return;
                }
            } catch (Exception e) {
                logInfo("   Controllo 1 (blur) - errore: " + e.getMessage());
            }

            // ═══════════════════════════════════════════════════════
            // 2. CERCA TESTO "PRESE TERMINATE" / "Cambia entrambe"
            // ═══════════════════════════════════════════════════════
            try {
                WebElement modalText = driver.findElement(
                        AppiumBy.xpath("//*[contains(@text, 'PRESE TERMINE') or contains(@content-desc, 'PRESE TERMINE')]"));

                logInfo("   Controllo 2 (text): trovato testo modal");

                if (modalText.isDisplayed()) {
                    logWarn("⚠️ Rilevato modal 'PRESE TERMINATE', lo chiudo...");
                    driver.navigate().back();
                    sleep(1500); // Attesa per stabilizzazione UI
                    logInfo("   ✓ Modal chiuso con tasto BACK");
                    modalChiuso = true;
                    return;
                }
            } catch (Exception e) {
                logInfo("   Controllo 2 (text) - nessun testo trovato");
            }

            // ═══════════════════════════════════════════════════════
            // 3. CERCA OVERLAY PRESE TOTALI (apertura accidentale)
            // ═══════════════════════════════════════════════════════
            // Questo overlay si apre quando si clicca per errore sul contatore "X / Y"
            // Sintomo: I bottoni delle prese non sono più cliccabili
            try {
                // Cerca se ci sono TROPPI button visibili contemporaneamente
                // (segno che l'overlay è aperto con tutte le prese visibili)
                List<WebElement> allButtons = driver.findElements(AppiumBy.className("android.widget.Button"));
                int buttonCount = 0;

                for (WebElement btn : allButtons) {
                    try {
                        if (btn.isDisplayed()) {
                            buttonCount++;
                        }
                    } catch (Exception ignored) {}
                }

                logInfo("   Controllo 3 (button count): " + buttonCount + " bottoni visibili");

                // Se ci sono più di 15 bottoni visibili, probabilmente l'overlay è aperto
                if (buttonCount > 15) {
                    logWarn("⚠️ Rilevato possibile overlay prese totali (" + buttonCount + " bottoni), lo chiudo...");
                    driver.navigate().back();
                    sleep(1500); // Attesa per stabilizzazione UI
                    logInfo("   ✓ Overlay chiuso con tasto BACK");
                    modalChiuso = true;
                    return;
                }
            } catch (Exception e) {
                logInfo("   Controllo 3 (buttons) - errore: " + e.getMessage());
            }

            logInfo("   ✅ Nessun modal/overlay rilevato");

        } catch (Exception e) {
            logInfo("   ⚠️ Errore generale chiudiEventualiModal: " + e.getMessage());
        }
    }

    // =====================================================================================
    // ATTESA ADATTIVA E GESTIONE POPUP
    // =====================================================================================

    private static class AnimationResult {
        public enum Type {
            POPUP_ERRORE,
            POPUP_INFO,
            PRESA_VISIBILE,
            TIMEOUT
        }

        public final Type type;
        public final String message;
        public final WebElement element;

        public AnimationResult(Type type, String message, WebElement element) {
            this.type = type;
            this.message = message;
            this.element = element;
        }
    }

    @Step("Attesa adattiva fine animazione")
    private AnimationResult aspettaFineAnimazioneAdattiva(String xpath) {
        logInfo("⏳ Attendo fine animazione (controllo adattivo ogni " + ANIMATION_CHECK_INTERVAL + "ms)...");

        for (int check = 1; check <= ANIMATION_MAX_CHECKS; check++) {
            sleep(ANIMATION_CHECK_INTERVAL);

            try {
                WebElement popupErrore = driver.findElement(AppiumBy.xpath(
                        "//*[contains(@content-desc, 'Errore')]"));
                String msg = popupErrore.getAttribute("content-desc");
                logWarn("⚠️ Popup ERRORE rilevato (check " + check + "/" + ANIMATION_MAX_CHECKS + ")");
                return new AnimationResult(AnimationResult.Type.POPUP_ERRORE, msg, popupErrore);
            } catch (NoSuchElementException e1) {
                // Nessun popup errore, continua
            }

            try {
                List<WebElement> elements = driver.findElements(AppiumBy.xpath(
                        "//*[contains(@content-desc, 'non è alimentata') or contains(@content-desc, 'non alimentata')]"));

                for (WebElement el : elements) {
                    String desc = el.getAttribute("content-desc");

                    if (desc != null &&
                            !desc.contains("Scheda") &&
                            !desc.equals("Colonnina") &&
                            !desc.contains("Home") &&
                            desc.length() > 20) {

                        logWarn("⚠️ Popup 'non alimentata' rilevato (check " + check + "/" + ANIMATION_MAX_CHECKS + ")");
                        return new AnimationResult(AnimationResult.Type.POPUP_ERRORE, desc, el);
                    }
                }
            } catch (Exception e2) {
                // Nessun popup, continua
            }

            try {
                WebElement popupInfo = driver.findElement(AppiumBy.xpath(
                        "//*[contains(@content-desc, 'Hai collegato')]"));
                String msg = popupInfo.getAttribute("content-desc");
                logWarn("⚠️ Popup 'Hai collegato' rilevato (check " + check + "/" + ANIMATION_MAX_CHECKS + ")");
                return new AnimationResult(AnimationResult.Type.POPUP_INFO, msg, popupInfo);
            } catch (NoSuchElementException e3) {
                // Nessun popup info, continua
            }

            try {
                WebElement presa = driver.findElement(AppiumBy.xpath(xpath));
                if (presa.isDisplayed() && presa.isEnabled()) {
                    logSuccess("✅ Animazione completata (check " + check + "/" + ANIMATION_MAX_CHECKS + ")");
                    return new AnimationResult(AnimationResult.Type.PRESA_VISIBILE, "Presa visibile", presa);
                }
            } catch (Exception e4) {
                // Presa non ancora visibile, animazione in corso
            }

            if (check % 5 == 0) {
                logInfo("   ⏱️ Check " + check + "/" + ANIMATION_MAX_CHECKS + " - animazione in corso...");
            }
        }

        logWarn("⚠️ Timeout attesa animazione dopo " + (ANIMATION_MAX_CHECKS * ANIMATION_CHECK_INTERVAL / 1000) + " secondi");
        return new AnimationResult(AnimationResult.Type.TIMEOUT, "Timeout", null);
    }

    private boolean chiudiPopup() {
        logAction("🔘 Chiusura popup...");

        sleep(1000);

        try {
            WebElement okBtn = new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Ok")));
            okBtn.click();
            sleep(1000);
            logInfo("  ✓ Click 'Ok' (AccessibilityId)");
            return true;
        } catch (TimeoutException e) {
            logInfo("  ✗ 'Ok' non trovato (AccessibilityId)");
        }

        try {
            WebElement okBtn = driver.findElement(
                    AppiumBy.xpath("//android.widget.Button[@content-desc='Ok' or @content-desc='OK']"));
            if (okBtn.isDisplayed()) {
                okBtn.click();
                sleep(1000);
                logInfo("  ✓ Click 'Ok' (XPath)");
                return true;
            }
        } catch (NoSuchElementException e) {
            logInfo("  ✗ 'Ok' non trovato (XPath)");
        }

        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("android.widget.Button"));

            for (WebElement btn : buttons) {
                if (!btn.isDisplayed()) continue;

                String desc = btn.getAttribute("content-desc");
                String text = btn.getAttribute("text");

                if ((desc != null && (desc.equalsIgnoreCase("Ok") ||
                        desc.equalsIgnoreCase("OK") ||
                        desc.equalsIgnoreCase("Chiudi") ||
                        desc.equalsIgnoreCase("Conferma"))) ||
                        (text != null && (text.equalsIgnoreCase("Ok") ||
                                text.equalsIgnoreCase("OK")))) {

                    btn.click();
                    sleep(1000);
                    logInfo("  ✓ Click: " + (desc != null ? desc : text));
                    return true;
                }
            }
            logInfo("  ✗ Nessun bottone OK trovato");

        } catch (Exception e) {
            logInfo("  ✗ Errore scansione bottoni");
        }

        try {
            logInfo("  ⚠️ Fallback: TAP coordinate centrali");
            Dimension size = driver.manage().window().getSize();
            int x = size.width / 2;
            // ⭐ FIX: TAP più in alto (42% invece di 58%) per evitare di colpire
            // il dropdown colonnine che si trova nella parte centrale dello schermo
            int y = (int) (size.height * 0.42);

            ((JavascriptExecutor) driver).executeScript(
                    "mobile: clickGesture",
                    Map.of("x", x, "y", y)
            );

            sleep(1000);
            logInfo("  ✓ TAP eseguito");
            return true;

        } catch (Exception e) {
            logError("  ✗ TAP fallito: " + e.getMessage());
            return false;
        }
    }

    // =====================================================================================
    // GESTIONE COLONNINE
    // =====================================================================================

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

        // ⭐ Chiudi eventuali modal aperti per errore PRIMA di iniziare
        chiudiEventualiModal();

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

            AnimationResult result = aspettaFineAnimazioneAdattiva(xpath);

            switch (result.type) {
                case POPUP_ERRORE:
                    takeScreenshot("popup_errore_" + System.currentTimeMillis());

                    logWarn("⚠️ ════════════════════════════════════════");
                    logWarn("⚠️ COLONNINA NON ALIMENTATA");
                    logWarn("⚠️ Messaggio: " + result.message);
                    logWarn("⚠️ ════════════════════════════════════════");

                    chiudiPopup();
                    takeScreenshot("popup_chiuso_" + System.currentTimeMillis());

                    String dettaglioErrore = String.format(
                            "🔌 COLONNINA NON ALIMENTATA\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                    "👤 Utente: %s\n" +
                                    "📍 Totem: %s\n" +
                                    "🔄 Ciclo: %d/%d\n" +
                                    "⚡ Gruppo: %s\n" +
                                    "🔢 Presa: %d\n" +
                                    "🔄 Azione: %s\n" +
                                    "💬 Popup: \"%s\"\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━━━",
                            currentUser,
                            currentTotem,
                            cicloCorrente,
                            numeroCicli,
                            gruppo,
                            numero,
                            (accendi ? "Tentativo ACCENSIONE" : "Tentativo SPEGNIMENTO"),
                            result.message
                    );

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

                case POPUP_INFO:
                    takeScreenshot("popup_info_" + System.currentTimeMillis());
                    logWarn("⚠️ POPUP INFO: " + result.message);
                    chiudiPopup();

                    logInfo("⏱️ Attesa stabilizzazione UI dopo chiusura popup (1500ms)...");
                    sleep(1500); // Stabilizzazione UI

                    logInfo("🔍 Verifico se dropdown colonnine rimasto aperto...");
                    // Verifica se il dropdown è aperto (ricerca testo "Cambia entrambe le colonnine")
                    try {
                        WebElement dropdownAperto = driver.findElement(
                                AppiumBy.xpath("//*[contains(@content-desc, 'Cambia entrambe le colonnine')]"));
                        if (dropdownAperto.isDisplayed()) {
                            logWarn("⚠️ DROPDOWN COLONNINE APERTO dopo chiusura popup, lo chiudo con BACK...");
                            driver.navigate().back();
                            sleep(1500);
                            logInfo("   ✓ Dropdown chiuso");
                        }
                    } catch (NoSuchElementException e) {
                        logInfo("   ✅ Dropdown NON aperto");
                    }

                    // Verifica anche altri overlay aperti
                    chiudiEventualiModal();

                    logWarn("⚠️ " + nomeCompleto + " → PRESA GIÀ ATTIVA");
                    Allure.step(nomeCompleto + " - ⚠️ Presa già attiva", () -> {});
                    return;

                case PRESA_VISIBILE:
                    logSuccess("✅ Animazione completata");

                    // ⭐ FIX: DELAY STABILIZZAZIONE COLORE
                    // Aspetta che il colore della presa si aggiorni visivamente dopo l'animazione
                    logInfo("⏳ Attesa stabilizzazione colore (" + COLOR_STABILIZATION_DELAY + "ms)...");
                    sleep(COLOR_STABILIZATION_DELAY);

                    colonnina = driver.findElement(AppiumBy.xpath(xpath));
                    boolean statoFinale = rilevaStatoColonnina(colonnina, gruppo, numero);
                    String risultato = statoFinale ? " ✅ ACCESA" : " ⚫ SPENTA";
                    logSuccess(nomeCompleto + risultato);
                    Allure.step(nomeCompleto + risultato, () -> {});
                    break;

                case TIMEOUT:
                    logError("❌ Timeout attesa animazione per " + nomeCompleto);
                    takeScreenshot("timeout_animazione_" + System.currentTimeMillis());
                    throw new Exception("Timeout attesa animazione: " + nomeCompleto);
            }

            sleep(INTER_ACTION_DELAY);

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
    // ⭐ GESTIONE BACKUP RISULTATI CICLI
    // =====================================================================================

    /**
     * ⭐ GENERA REPORT HTML PER UN SINGOLO CICLO
     * Chiamato subito dopo ogni ciclo per catturare i risultati
     */
    private static void generaReportCiclo(int numeroCiclo, int totaleCicli) {
        try {
            System.out.println("💾 Salvataggio risultati ciclo " + numeroCiclo + "...");

            // Chiama lo script Node.js per generare il report
            ProcessBuilder pb = new ProcessBuilder(
                    "node",
                    "generate-embedded-html-report.js",
                    String.valueOf(numeroCiclo),
                    String.valueOf(totaleCicli)
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Leggi output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Report ciclo " + numeroCiclo + " generato!");
            } else {
                System.out.println("⚠️ Errore generazione report ciclo " + numeroCiclo);
            }

        } catch (Exception e) {
            System.out.println("⚠️ Errore: " + e.getMessage());
        }
    }

    // =====================================================================================
    // TEST PARAMETRIZZATI CON CICLI MULTIPLI
    // =====================================================================================

    @BeforeAll
    public static void setupLogger() {
        pulisciBackupVecchi(); // ⭐ Pulisci backup di test precedenti
        initCsvLogger();
    }

    /**
     * ⭐ PULIZIA BACKUP VECCHI
     * Cancella tutte le directory allure-results-ciclo-* prima di iniziare
     * Questo previene che risultati di test precedenti vengano sommati
     */
    private static void pulisciBackupVecchi() {
        try {
            Path buildDir = Paths.get("build");
            if (!Files.exists(buildDir)) {
                return;
            }

            System.out.println("\n🗑️ Pulizia backup vecchi...");

            Files.list(buildDir)
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("allure-results-ciclo-"))
                    .forEach(dir -> {
                        try {
                            System.out.println("   🗑️ " + dir.getFileName());
                            Files.walk(dir)
                                    .sorted(Comparator.reverseOrder())
                                    .forEach(path -> {
                                        try {
                                            Files.delete(path);
                                        } catch (IOException e) {
                                            // Ignora errori durante la cancellazione
                                        }
                                    });
                        } catch (Exception e) {
                            System.out.println("   ⚠️ Errore: " + e.getMessage());
                        }
                    });

            System.out.println("✅ Backup vecchi cancellati\n");

        } catch (Exception e) {
            System.out.println("⚠️ Errore pulizia backup: " + e.getMessage());
        }
    }

    /**
     * ⭐ BACKUP RISULTATI ALLURE PER SINGOLO CICLO
     * Copia i file JSON da build/allure-results in build/allure-results-ciclo-X
     * DEVE ESSERE CHIAMATO SUBITO dopo ogni ciclo, PRIMA che il successivo lo sovrascriva
     */
    private static void backupAllureResultsCiclo(int numeroCiclo) {
        try {
            Path sourceDir = Paths.get("build/allure-results");
            Path targetDir = Paths.get("build/allure-results-ciclo-" + numeroCiclo);

            if (!Files.exists(sourceDir)) {
                System.out.println("⚠️ Directory allure-results non trovata, skip backup ciclo " + numeroCiclo);
                return;
            }

            // Crea directory di destinazione
            Files.createDirectories(targetDir);

            // Copia TUTTI i file dalla directory sorgente
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .forEach(source -> {
                        try {
                            Path destination = targetDir.resolve(sourceDir.relativize(source));
                            Files.createDirectories(destination.getParent());
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            System.out.println("⚠️ Errore copia file " + source.getFileName() + ": " + e.getMessage());
                        }
                    });

            // Conta i file copiati
            long fileCount = Files.walk(targetDir)
                    .filter(Files::isRegularFile)
                    .count();

            System.out.println("✅ Backup ciclo " + numeroCiclo + " completato → " + targetDir);
            System.out.println("   📁 File salvati: " + fileCount);

        } catch (Exception e) {
            System.out.println("⚠️ Errore backup ciclo " + numeroCiclo + ": " + e.getMessage());
        }
    }

    @ParameterizedTest(name = "Ciclo {1}/{2} - {0}")
    @MethodSource("allEnabledUsersProviderWithCycles")
    @Order(1)
    @Epic("Test Colonnine")
    @Feature("Ciclo Completo")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Test completo per utente abilitato con cicli multipli")
    public void testConTuttiGliUtenti(Map<String, Object> testData) {
        UserConfig userConfig = (UserConfig) testData.get("user");
        int ciclo = (int) testData.get("ciclo");
        int totaleCicli = (int) testData.get("totaleCicli");

        cicloCorrente = ciclo;

        // ⭐⭐⭐ NUOVA SEZIONE: RICREAZIONE SESSIONE OGNI 10 CICLI ⭐⭐⭐
        // Controlla se è necessario ricreare la sessione
        // NON ricrea al ciclo 1 (è la prima sessione)
        // Ricrea ai cicli 11, 21, 31, 41, ecc.
        if (ciclo > 1 && (ciclo - 1) % CICLI_PER_RICREAZIONE_SESSIONE == 0) {
            System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
            System.out.println("║   🔄 RICREAZIONE SESSIONE DOPO " + CICLI_PER_RICREAZIONE_SESSIONE + " CICLI           ║");
            System.out.println("╚═══════════════════════════════════════════════════════════╝");
            System.out.println("📊 Ciclo corrente: " + ciclo + "/" + totaleCicli);
            System.out.println("💡 Motivo: Prevenzione memory leak UIAutomator2");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            try {
                ricreaSessioneAppium();

                System.out.println("✅ Sessione ricreata con successo!");
                System.out.println("⏩ Ripresa test dal ciclo " + ciclo + "...\n");

            } catch (Exception e) {
                System.out.println("╔═══════════════════════════════════════════════════════════╗");
                System.out.println("║         ❌ ERRORE CRITICO RICREAZIONE SESSIONE           ║");
                System.out.println("╚═══════════════════════════════════════════════════════════╝");
                System.out.println("Errore: " + e.getMessage());
                System.out.println("\n⚠️ Impossibile continuare test dopo ciclo " + (ciclo - 1));

                throw new RuntimeException("Errore critico ricreazione sessione al ciclo " + ciclo, e);
            }
        }
        // ⭐⭐⭐ FINE NUOVA SEZIONE ⭐⭐⭐

        // ⭐ PAUSA TRA CICLI (solo se non è il primo ciclo)
        if (ciclo > 1 && !((ciclo - 1) % CICLI_PER_RICREAZIONE_SESSIONE == 0)) {
            // Se abbiamo appena ricreato la sessione, non fare altra pausa
            System.out.println("\n⏸️ ═══════════════════════════════════════════════════════════");
            System.out.println("⏸️  PAUSA TRA CICLI: " + pausaTraCicli + " secondi");
            System.out.println("⏸️ ═══════════════════════════════════════════════════════════\n");
            try {
                Thread.sleep(pausaTraCicli * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // ⭐ GESTIONE ERRORI: Il test continua anche se un ciclo fallisce
        try {
            eseguiTestCompleto(userConfig, ciclo, totaleCicli);

        } catch (Exception e) {
            logError("❌ CICLO " + ciclo + "/" + totaleCicli + " FALLITO: " + e.getMessage());
            takeScreenshot("ciclo_" + ciclo + "_FAILED");

            // Crea report di errore per Allure
            Allure.addAttachment(
                    "❌ Ciclo " + ciclo + " - ERRORE CRITICO",
                    "text/plain",
                    "Ciclo: " + ciclo + "/" + totaleCicli + "\n" +
                            "Utente: " + userConfig.getDescrizione() + "\n" +
                            "Errore: " + e.getMessage() + "\n\n" +
                            "Stack trace:\n" + getStackTraceAsString(e),
                    ".txt"
            );

            // ⚠️ IMPORTANTE: Rilancia l'eccezione per far fallire il test JUnit
            // (ma il backup verrà comunque eseguito nell'@AfterEach)
            throw new RuntimeException("Ciclo " + ciclo + " fallito: " + e.getMessage(), e);

        } finally {
            // ⭐ NON FARE NULLA QUI
            // Allure scrive i file SOLO dopo che il test method termina
            // Il report verrà generato nell'@AfterAll quando TUTTI i test sono finiti
            System.out.println("\n✅ CICLO " + ciclo + "/" + totaleCicli + " completato");
        }
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private void eseguiTestCompleto(UserConfig userConfig, int ciclo, int totaleCicli) throws Exception {
        currentUser = userConfig.getUserId();

        logHeader("═══════════════════════════════════════════════════════════");
        logHeader("   🔄 CICLO " + ciclo + "/" + totaleCicli + " - " + userConfig.getDescrizione());
        logHeader("═══════════════════════════════════════════════════════════");

        Allure.parameter("Utente", userConfig.getUsername());
        Allure.parameter("Descrizione", userConfig.getDescrizione());
        Allure.parameter("Totem Totali", userConfig.getTotaleTotem());
        Allure.parameter("Ciclo", ciclo + "/" + totaleCicli);

        reinizializzaApp();
        handleStartupFlow();
        effettuaLogin(userConfig);
        vaiAllaHome();

        // ⭐ VALIDAZIONE NUMERO TOTEM REALE
        int numeroTotemReale = validaNumeroTotem(userConfig.getTotaleTotem());

        int totemCompletati = 0;
        int totemFalliti = 0;
        StringBuilder reportGlobale = new StringBuilder();
        reportGlobale.append("REPORT CICLO " + ciclo + "/" + totaleCicli + " - " + userConfig.getDescrizione() + "\n");
        reportGlobale.append("===================\n\n");

        for (int i = 1; i <= numeroTotemReale; i++) {
            currentTotem = "Totem #" + i;

            logHeader("═══════════════════════════════════════════════════════════");
            logHeader("🔌 TEST Totem #" + i + " (" + i + "/" + numeroTotemReale + ")");
            logHeader("═══════════════════════════════════════════════════════════");

            try {
                eseguiTestTotem(i); // ⭐ Passa l'indice invece del nome
                totemCompletati++;
                reportGlobale.append("✅ Totem #").append(i).append(" (").append(currentTotem).append("): COMPLETATO\n");
            } catch (Exception e) {
                totemFalliti++;
                logError("❌ Errore critico test Totem #" + i + ": " + e.getMessage());
                takeScreenshot("errore_critico_totem_" + i);
                reportGlobale.append("❌ Totem #").append(i).append(" (").append(currentTotem).append("): ERRORE CRITICO - ").append(e.getMessage()).append("\n");

                Allure.addAttachment(
                        "❌ Errore Critico Totem #" + i,
                        "text/plain",
                        e.getMessage(),
                        ".txt"
                );
            }

            vaiAllaHome();
        }

        logHeader("═══════════════════════════════════════════════════════════");
        logHeader("🏁 CICLO " + ciclo + "/" + totaleCicli + " COMPLETATO PER: " + userConfig.getDescrizione());
        logHeader("═══════════════════════════════════════════════════════════");

        reportGlobale.append("\n===================\n");
        reportGlobale.append(String.format("Ciclo: %d/%d\n", ciclo, totaleCicli));
        reportGlobale.append(String.format("Utente: %s\n", userConfig.getDescrizione()));
        reportGlobale.append(String.format("Totem testati: %d/%d\n", totemCompletati + totemFalliti, numeroTotemReale));
        reportGlobale.append(String.format("✅ Completati: %d\n", totemCompletati));
        reportGlobale.append(String.format("❌ Falliti: %d\n", totemFalliti));

        String summary = reportGlobale.toString();
        logHeader(summary);

        Allure.addAttachment(
                "📊 Report Ciclo " + ciclo + " - " + userConfig.getUserId(),
                "text/plain",
                summary,
                ".txt"
        );
    }

    /**
     * Conta automaticamente il numero di prese disponibili per un gruppo specifico.
     *
     * @param gruppo "Presa Elettrica" o "Erogatore Idrico"
     * @return Numero reale di prese disponibili (default 4 se il conteggio fallisce)
     */
    /**
     * Conta automaticamente il numero di prese disponibili per un gruppo specifico.
     * Verifica EFFETTIVAMENTE se esistono le prese per quel tipo (elettriche o idriche).
     *
     * @param gruppo "Presa Elettrica" o "Erogatore Idrico"
     * @return Numero reale di prese disponibili per quel tipo (0 se non esistono)
     */
    private int contaPreseDisponibili(String gruppo) {
        logInfo("🔍 Conteggio prese " + gruppo + "...");

        try {
            // Determina quale indice cercare: [1] per elettriche, [2] per idriche
            int indiceGruppo = gruppo.contains("Elettrica") ? 1 : 2;

            // Trova tutti i bottoni numerici (content-desc="1", "2", "3", "4", etc.)
            // ESCLUDI quelli con "/" che sono i contatori (es: "0 / 4", "4 / 4")
            List<WebElement> bottoniNumerici = driver.findElements(
                    AppiumBy.xpath("//android.widget.Button[" +
                            "string-length(@content-desc) = 1 and " +
                            "number(@content-desc) = number(@content-desc) and " +
                            "not(contains(@content-desc, '/'))]")
            );

            if (bottoniNumerici.isEmpty()) {
                logWarn("   ⚠️ Nessun bottone numerico trovato");
                if (gruppo.contains("Elettrica")) {
                    logWarn("   → Uso valore default: 4");
                    return 4;
                } else {
                    logWarn("   → Probabilmente questa colonnina non ha erogatori idrici");
                    return 0;
                }
            }

            // ⭐ VERIFICA EFFETTIVA: Conta quanti bottoni esistono per QUESTO GRUPPO
            // Per elettriche: cerca bottoni "1", "2", "3", "4" con indice [1]
            // Per idriche: cerca bottoni "1", "2", "3", "4" con indice [2]

            int conteggioTrovati = 0;
            for (int i = 1; i <= 8; i++) {  // Testa fino a 8 prese (valore massimo teorico)
                try {
                    String xpath = "(//android.widget.Button[@content-desc='" + i + "' " +
                            "and not(contains(@content-desc, '/'))])[" + indiceGruppo + "]";

                    // Prova a trovare il bottone (timeout breve: 1 secondo)
                    WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(1));
                    quickWait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath(xpath)));

                    // Se arriva qui, il bottone esiste!
                    conteggioTrovati = i;

                } catch (Exception e) {
                    // Bottone non trovato → abbiamo raggiunto il massimo
                    break;
                }
            }

            if (conteggioTrovati == 0) {
                logWarn("   ⚠️ Nessuna presa " + gruppo + " trovata");
                if (gruppo.contains("Elettrica")) {
                    logWarn("   → ATTENZIONE: Colonnina senza prese elettriche?!");
                    return 4;  // Fallback sicuro
                } else {
                    logInfo("   → Questa colonnina ha solo prese elettriche (nessun erogatore idrico)");
                    return 0;
                }
            }

            logInfo("   📊 Prese " + gruppo + " rilevate: " + conteggioTrovati);
            return conteggioTrovati;

        } catch (Exception e) {
            logWarn("   ⚠️ Errore conteggio prese: " + e.getMessage());
            if (gruppo.contains("Elettrica")) {
                logWarn("   → Uso valore default: 4");
                return 4;
            } else {
                logWarn("   → Assumo nessun erogatore idrico");
                return 0;
            }
        }
    }

    @Step("Esecuzione test completo per Totem #{indiceTotem}")
    private void eseguiTestTotem(int indiceTotem) throws Exception {
        selezionaColonninaDalMenu(indiceTotem);

        WebElement tabColonnina = new WebDriverWait(driver, WAIT_MEDIUM)
                .until(ExpectedConditions.elementToBeClickable(
                        AppiumBy.accessibilityId("Colonnina\nScheda 2 di 2")));
        tabColonnina.click();
        sleep(800);
        logStep("⚙️ Tab Colonnina aperto");

        // ⭐ CONTA AUTOMATICAMENTE LE PRESE DISPONIBILI
        int numeroPreseElettriche = contaPreseDisponibili("Presa Elettrica");
        int numeroPreseIdriche = contaPreseDisponibili("Erogatore Idrico");

        int elettricheNonAlimentate = 0;
        int idricheNonAlimentate = 0;

        logHeader("⚡⚡⚡ CICLO COMPLETO PRESE ELETTRICHE (" + currentTotem + ") ⚡⚡⚡");
        elettricheNonAlimentate = gestisciColonnineConConteggio("Presa Elettrica", true, numeroPreseElettriche);

        if (elettricheNonAlimentate < numeroPreseElettriche) {
            sleep(3000);
            // ⭐ Chiudi eventuali overlay aperti dal popup "Hai collegato" prima dello spegnimento
            chiudiEventualiModal();
            gestisciColonnineConConteggio("Presa Elettrica", false, numeroPreseElettriche);
        } else {
            logWarn("⚠️ Tutte le " + numeroPreseElettriche + " prese elettriche sono NON ALIMENTATE, skip spegnimento");
        }

        // ⭐ GESTIONE EROGATORI IDRICI (solo se esistono!)
        if (numeroPreseIdriche > 0) {
            logHeader("💧💧💧 CICLO COMPLETO EROGATORI IDRICI (" + currentTotem + ") 💧💧💧");
            idricheNonAlimentate = gestisciColonnineConConteggio("Erogatore Idrico", true, numeroPreseIdriche);

            if (idricheNonAlimentate < numeroPreseIdriche) {
                sleep(3000);
                // ⭐ Chiudi eventuali overlay aperti dal popup "Hai collegato" prima dello spegnimento
                chiudiEventualiModal();
                gestisciColonnineConConteggio("Erogatore Idrico", false, numeroPreseIdriche);
            } else {
                logWarn("⚠️ Tutti i " + numeroPreseIdriche + " erogatori idrici sono NON ALIMENTATI, skip spegnimento");
            }
        } else {
            logInfo("ℹ️ Questa colonnina ha solo prese elettriche (nessun erogatore idrico)");
        }

        int totalePreseTotali = numeroPreseElettriche + numeroPreseIdriche;
        int totalePreseNonAlimentate = elettricheNonAlimentate + idricheNonAlimentate;

        // ⭐ VERIFICA COLONNINA COMPLETAMENTE NON ALIMENTATA
        // Se ci sono SOLO prese elettriche, controlla solo quelle
        // Se ci sono entrambi i tipi, controlla entrambi
        boolean colonninaNonAlimentata;
        if (numeroPreseIdriche == 0) {
            // Solo prese elettriche
            colonninaNonAlimentata = (elettricheNonAlimentate == numeroPreseElettriche);
        } else {
            // Entrambi i tipi di prese
            colonninaNonAlimentata = (elettricheNonAlimentate == numeroPreseElettriche &&
                    idricheNonAlimentate == numeroPreseIdriche);
        }

        if (colonninaNonAlimentata) {
            String messaggioErrore;
            if (numeroPreseIdriche == 0) {
                // Solo prese elettriche
                messaggioErrore = String.format(
                        "⚡🔴 COLONNINA %s COMPLETAMENTE NON ALIMENTATA 🔴⚡\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "📍 Totem: %s\n" +
                                "👤 Utente: %s\n" +
                                "🔄 Ciclo: %d/%d\n" +
                                "⚡ Prese Elettriche: %d/%d NON ALIMENTATE\n" +
                                "💧 Erogatori Idrici: N/A (colonnina solo elettrica)\n" +
                                "📊 Totale: %d/%d prese NON RISPONDONO\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "⏩ SKIP ALLA PROSSIMA COLONNINA",
                        currentTotem, currentTotem, currentUser, cicloCorrente, numeroCicli,
                        elettricheNonAlimentate, numeroPreseElettriche,
                        totalePreseNonAlimentate, totalePreseTotali
                );
            } else {
                // Entrambi i tipi di prese
                messaggioErrore = String.format(
                        "⚡🔴 COLONNINA %s COMPLETAMENTE NON ALIMENTATA 🔴⚡\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "📍 Totem: %s\n" +
                                "👤 Utente: %s\n" +
                                "🔄 Ciclo: %d/%d\n" +
                                "⚡ Prese Elettriche: %d/%d NON ALIMENTATE\n" +
                                "💧 Erogatori Idrici: %d/%d NON ALIMENTATI\n" +
                                "📊 Totale: %d/%d prese NON RISPONDONO\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "⏩ SKIP ALLA PROSSIMA COLONNINA",
                        currentTotem, currentTotem, currentUser, cicloCorrente, numeroCicli,
                        elettricheNonAlimentate, numeroPreseElettriche,
                        idricheNonAlimentate, numeroPreseIdriche,
                        totalePreseNonAlimentate, totalePreseTotali
                );
            }

            logError(messaggioErrore);
            takeScreenshot("colonnina_non_alimentata_completa_" + indiceTotem);

            Allure.step("🔴 " + currentTotem + " - COMPLETAMENTE NON ALIMENTATA", () -> {
                Allure.addAttachment(
                        "⚡ Colonnina Non Alimentata - Dettagli",
                        "text/plain",
                        messaggioErrore,
                        ".txt"
                );
            });

            throw new ColonninaNonAlimentataException(
                    "Colonnina " + currentTotem + " completamente non alimentata (" + totalePreseNonAlimentate + "/" + totalePreseTotali + " prese)"
            );
        }

        logSuccess("✅✅✅ Test completato su " + currentTotem + " ✅✅✅");
        takeScreenshot("completato_totem_" + indiceTotem);
    }

    private int gestisciColonnineConConteggio(String gruppo, boolean accendi, int numeroPrese) {
        logHeader((accendi ? "ACCENSIONE " : "SPEGNIMENTO ") + gruppo);

        // ⭐ SAFETY CHECK: Se non ci sono prese, skip tutto
        if (numeroPrese == 0) {
            logInfo("ℹ️ Nessuna presa " + gruppo + " da testare (colonnina senza questo tipo di prese)");
            return 0;
        }

        // ⭐ Chiudi eventuali modal aperti PRIMA di iniziare il ciclo
        chiudiEventualiModal();

        WebDriverWait wait = new WebDriverWait(driver, WAIT_MEDIUM);

        int fallimenti = 0;
        int presaNonAlimentate = 0;
        StringBuilder reportFallimenti = new StringBuilder();

        for (int i = 1; i <= numeroPrese; i++) {
            // ⭐ XPath migliorato: escludi bottoni con "/" (es: "0 / 4", "4 / 4")
            // Questo previene click accidentali sul contatore delle prese
            String xpath = "(//android.widget.Button[@content-desc='" + i + "' and not(contains(@content-desc, '/'))])["
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
                    gruppo, totaleProblemi, numeroPrese, presaNonAlimentate, fallimenti
            );
            logWarn(summary);

            Allure.addAttachment(
                    "⚠️ Riepilogo " + gruppo,
                    "text/plain",
                    reportFallimenti.toString(),
                    ".txt"
            );
        } else {
            String success = "✅ " + gruppo + ": tutte le " + numeroPrese + " prese OK";
            logSuccess(success);
            Allure.step(success, () -> {});
        }

        return presaNonAlimentate;
    }

    // =====================================================================================
    // GENERAZIONE REPORT
    // =====================================================================================

    @AfterAll
    public static void chiudiLoggerEGeneraReportCompleto() {
        closeCsvLogger();

        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║           📊 GENERAZIONE REPORT COMPLETI                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        System.out.println("📊 Report CSV: " + logFile.getAbsolutePath());
        System.out.println("📸 Screenshots: " + SCREENSHOT_DIR.getAbsolutePath());

        // ⭐ ATTENDI CHE ALLURE SCRIVA TUTTI I FILE (10 secondi)
        System.out.println("\n⏳ Attesa scrittura completa risultati Allure (10 secondi)...");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Step 1: Genera Report HTML Embedded (SEMPRE FUNZIONANTE)
        generaReportEmbeddedFinale();

        // Step 2: Crea ZIP del report Allure (SE DISPONIBILE)
        creaZipReportAllure();

        // Step 3: Apri report HTML embedded nel browser
        apriReportEmbedded();

        // Step 4: Pausa per consultazione
        pausaPrimaChiusura();

        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║          ✅ REPORT GENERATI CON SUCCESSO                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println("📄 Report HTML Embedded: reports/test-report-[timestamp].html");
        System.out.println("   └─ ✅ Consultabile OFFLINE, funziona ovunque");
        System.out.println("📦 Report Allure ZIP: reports/allure-report-[timestamp].zip");
        System.out.println("   └─ ✅ Da condividere via email/Teams/Slack");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
    }

    private static void generaReportEmbeddedFinale() {
        System.out.println("\n📄 Generazione report HTML embedded completo...");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "node", "generate-embedded-html-report.js", "FINAL"
            );
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Report embedded generato con successo!");
            } else {
                System.out.println("⚠️ Errore generazione report embedded (exit code: " + exitCode + ")");
            }

        } catch (Exception e) {
            System.out.println("⚠️ Impossibile generare report embedded: " + e.getMessage());
        }
    }

    private static void creaZipReportAllure() {
        System.out.println("\n📦 Creazione ZIP report Allure...");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "node", "generate-allure-zip.js"
            );
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ ZIP generato con successo!");
            } else {
                System.out.println("⚠️ Errore creazione ZIP (exit code: " + exitCode + ")");
            }

        } catch (Exception e) {
            System.out.println("⚠️ Impossibile creare ZIP: " + e.getMessage());
        }
    }

    private static void apriReportEmbedded() {
        System.out.println("\n🌐 Apertura report embedded nel browser...");

        try {
            File reportsDir = new File("reports");
            if (!reportsDir.exists()) {
                System.out.println("⚠️ Directory reports non trovata");
                return;
            }

            File[] files = reportsDir.listFiles((dir, name) ->
                    name.startsWith("test-report-") && name.endsWith(".html"));

            if (files == null || files.length == 0) {
                System.out.println("⚠️ Nessun report embedded trovato");
                return;
            }

            // Ordina per data di modifica (più recente prima)
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            File reportFile = files[0];

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(reportFile.toURI());
                System.out.println("✅ Report aperto nel browser: " + reportFile.getName());
            } else {
                System.out.println("💡 Apri manualmente: " + reportFile.getAbsolutePath());
            }

        } catch (Exception e) {
            System.out.println("⚠️ Impossibile aprire il browser: " + e.getMessage());
        }
    }

    private static void pausaPrimaChiusura() {
        boolean pausaAbilitata = true;

        try (InputStream input = new FileInputStream("test.properties")) {
            Properties props = new Properties();
            props.load(input);
            pausaAbilitata = Boolean.parseBoolean(props.getProperty("pausa.prima.chiusura", "true"));
        } catch (Exception e) {
            pausaAbilitata = true;
        }

        if (!pausaAbilitata) {
            System.out.println("\n⏩ Pausa disabilitata in test.properties");
            return;
        }

        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("⏸️  TEST COMPLETATO - PREMI INVIO PER CHIUDERE");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("📱 L'app è ancora aperta sul dispositivo");
        System.out.println("🌐 Il report HTML embedded è aperto nel browser");
        System.out.println("📄 Report embedded: reports/test-report-[timestamp].html");
        System.out.println("📦 ZIP da condividere: reports/allure-report-[timestamp].zip");
        System.out.println("\n👉 Premi INVIO quando hai finito per chiudere tutto...");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        try {
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("⚠️ Errore durante l'attesa: " + e.getMessage());
        }

        System.out.println("\n✅ Chiusura in corso...");
    }
}
