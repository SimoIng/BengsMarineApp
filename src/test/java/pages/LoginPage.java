package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LoginPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    // ===== Locators (dalla tua App Source) =====
    private final By loginButton = AppiumBy.accessibilityId("Accedi");
    private final By rememberMeCheckbox = By.className("android.widget.CheckBox");
    private final By editText = By.className("android.widget.EditText");

    // Dialog errore (da screenshot: bottone "Ok" con content-desc)
    private final By errorOkBtn = AppiumBy.accessibilityId("Ok");
    private final By errorOKBtn = AppiumBy.accessibilityId("OK");
    private final By errorOkXpath = By.xpath("//*[@content-desc='Ok' or @content-desc='OK']");

    public LoginPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    // ======================
    // API pubbliche
    // ======================

    public void faiLogin(String username, String password) {
        faiLogin(username, password, true);
    }

    public void faiLogin(String username, String password, boolean spuntaRicordami) {
        debug("LOGINPAGE - START faiLogin (Ricordami=" + spuntaRicordami + ")");

        // Chiudi eventuali dialog che bloccano
        dismissBlockingDialogsFast();

        WebElement userField = getUsernameField();
        WebElement passField = getPasswordField();

        safeClearAndType(userField, username);
        safeClearAndType(passField, password);

        // chiudi tastiera / prova "Completato"
        closeKeyboardIfPossible();

        if (spuntaRicordami) {
            checkRememberMe();
        }

        WebElement accedi = mustFindClickable(loginButton, "Bottone Accedi");
        accedi.click();

        debug("LOGINPAGE - END faiLogin");
    }

    /**
     * ✅ Compatibilità col tuo test:
     * aspetta che compaia il dialog di errore (bottone Ok).
     */
    public void waitForErroreDialog() {
        debug("Attendo dialog errore (Ok)...");
        // Il popup può comparire velocemente: aspetta visibilità/clickabilità
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(errorOkBtn),
                    ExpectedConditions.visibilityOfElementLocated(errorOKBtn),
                    ExpectedConditions.visibilityOfElementLocated(errorOkXpath)
            ));
        } catch (TimeoutException e) {
            throw new AssertionError("Dialog errore non comparso entro timeout", e);
        }
    }

    /**
     * ✅ Compatibilità col tuo test:
     * aspetta che Ok sia cliccabile e lo chiude.
     */
    public void waitAndCloseErroreDialog() {
        debug("Chiudo dialog errore (Ok)...");
        // retry: a volte cambia schermata troppo veloce
        for (int i = 0; i < 8; i++) {
            boolean closed = false;

            closed |= clickIfClickable(errorOkBtn, "Dialog Errore Ok");
            closed |= clickIfClickable(errorOKBtn, "Dialog Errore OK");
            closed |= clickIfClickable(errorOkXpath, "Dialog Errore Ok/OK (xpath)");

            if (closed) return;
            sleep(200);
        }
        throw new AssertionError("Impossibile chiudere dialog errore (Ok) entro retry");
    }

    /**
     * Alternativa "soft": prova a chiudere se presente senza fallire.
     */
    public void chiudiPopupErroreSePresente() {
        for (int i = 0; i < 8; i++) {
            boolean closed = false;
            closed |= clickIfPresent(errorOkBtn, "Dialog Errore Ok");
            closed |= clickIfPresent(errorOKBtn, "Dialog Errore OK");
            closed |= clickIfPresent(errorOkXpath, "Dialog Errore Ok/OK (xpath)");
            if (closed) return;
            sleep(200);
        }
    }

    // ======================
    // Campi pagina
    // ======================

    private WebElement getUsernameField() {
        List<WebElement> fields = driver.findElements(editText);
        if (fields.size() < 1) {
            throw new AssertionError("Elemento non trovato: Campo username | locator=By.className(android.widget.EditText)");
        }
        return fields.get(0);
    }

    private WebElement getPasswordField() {
        List<WebElement> fields = driver.findElements(editText);
        if (fields.size() < 2) {
            throw new AssertionError("Elemento non trovato: Campo password | locator=By.className(android.widget.EditText)");
        }
        return fields.get(1);
    }

    // ======================
    // Ricordami
    // ======================

    private void checkRememberMe() {
        try {
            List<WebElement> cbs = driver.findElements(rememberMeCheckbox);
            if (cbs.isEmpty()) {
                debug("Checkbox Ricordami non trovata (nessun CheckBox in pagina)");
                return;
            }

            WebElement cb = cbs.get(0);

            String checkedAttr = "";
            try { checkedAttr = cb.getAttribute("checked"); } catch (Exception ignored) {}

            boolean checked = "true".equalsIgnoreCase(checkedAttr) || cb.isSelected();

            if (!checked) {
                debug("Spunto checkbox Ricordami");
                cb.click();
                sleep(150);
            } else {
                debug("Checkbox Ricordami già spuntata");
            }
        } catch (Exception e) {
            debug("checkRememberMe EX: " + e.getMessage());
        }
    }

    // ======================
    // Helper
    // ======================

    private WebElement mustFindClickable(By by, String name) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(by));
        } catch (TimeoutException te) {
            throw new AssertionError("Elemento non trovato/cliccabile: " + name + " | locator=" + by, te);
        }
    }

    private boolean clickIfPresent(By by, String name) {
        try {
            List<WebElement> els = driver.findElements(by);
            if (!els.isEmpty() && els.get(0).isDisplayed()) {
                debug("Click: " + name + " | " + by);
                els.get(0).click();
                return true;
            }
        } catch (Exception e) {
            debug("clickIfPresent EX (" + name + "): " + e.getMessage());
        }
        return false;
    }

    private boolean clickIfClickable(By by, String name) {
        try {
            WebElement el = new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(by));
            debug("Click (clickable): " + name + " | " + by);
            el.click();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void dismissBlockingDialogsFast() {
        for (int i = 0; i < 3; i++) {
            boolean closed = false;
            closed |= clickIfPresent(errorOkBtn, "Dialog Errore Ok");
            closed |= clickIfPresent(errorOKBtn, "Dialog Errore OK");
            if (!closed) return;
            sleep(200);
        }
    }

    private void closeKeyboardIfPossible() {
        // prova a cliccare "Completato/Done"
        boolean pressed = clickIfPresent(By.xpath(
                "//*[@text='Completato' or @text='Done' or @content-desc='Done' or @content-desc='Completato']"
        ), "Tastiera -> Completato/Done");

        if (pressed) return;

        // fallback hideKeyboard
        try {
            driver.hideKeyboard();
            sleep(150);
        } catch (Exception ignored) { }
    }

    private void safeClearAndType(WebElement el, String text) {
        try {
            el.click();
            sleep(100);
            try { el.clear(); } catch (Exception ignored) {}
            el.sendKeys(text);
            sleep(150);
        } catch (Exception e) {
            throw new AssertionError("Impossibile scrivere nel campo: " + e.getMessage(), e);
        }
    }

    private void debug(String msg) {
        System.out.println("DEBUG - " + msg);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
