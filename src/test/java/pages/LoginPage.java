package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LoginPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private final By accediA11y = AppiumBy.accessibilityId("Accedi");
    private final By accediText = AppiumBy.androidUIAutomator("new UiSelector().text(\"Accedi\")");

    public LoginPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    private void debug(String msg) {
        System.out.println("DEBUG - " + msg);
    }

    private List<WebElement> editTexts() {
        return driver.findElements(By.className("android.widget.EditText"));
    }

    // ✅ OVERLOAD: mantiene compatibilità coi test che passano 2 argomenti
    public void faiLogin(String username, String password) {
        faiLogin(username, password, true); // default: Ricordami = true
    }

    public void faiLogin(String username, String password, boolean ricordami) {
        debug("LOGINPAGE - START faiLogin");

        wait.until(d -> editTexts().size() >= 2);

        List<WebElement> fields = editTexts();
        WebElement userField = fields.get(0);
        WebElement passField = fields.get(1);

        userField.click();
        userField.clear();
        userField.sendKeys(username);
        debug("Inserito USERNAME");

        passField.click();
        passField.clear();
        passField.sendKeys(password);
        debug("Inserito PASSWORD");

        if (ricordami) {
            ensureRicordamiChecked();
        }

        try { driver.hideKeyboard(); } catch (Exception ignored) {}

        clickAccediRobusto();

        debug("LOGINPAGE - END faiLogin");
    }


    /**
     * Compatibilità con i test:
     * chiude eventuale dialog di errore login (Ok)
     */
    public void waitAndCloseErroreDialog() {
        try {
            System.out.println("DEBUG - Attendo e chiudo dialog errore (Ok)");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            By ok = AppiumBy.accessibilityId("Ok");

            wait.until(ExpectedConditions.presenceOfElementLocated(ok));
            driver.findElement(ok).click();

            try { Thread.sleep(300); } catch (InterruptedException ignored) {}

        } catch (TimeoutException e) {
            System.out.println("DEBUG - Nessun dialog errore da chiudere");
        }
    }


    /** Prova a spuntare "Ricordami" in modo robusto */
    private void ensureRicordamiChecked() {
        debug("Ricordami -> richiesto true");

        By label = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Ricordami\")");
        By checkbox = By.className("android.widget.CheckBox");
        By switchCls = By.className("android.widget.Switch");

        try {
            List<WebElement> labels = driver.findElements(label);
            if (!labels.isEmpty() && labels.get(0).isDisplayed()) {
                WebElement l = labels.get(0);

                // click su label
                try {
                    l.click();
                    debug("Spunto checkbox Ricordami (click su label)");
                    return;
                } catch (Exception ignored) {}

                // click su parent
                try {
                    WebElement parent = l.findElement(By.xpath(".."));
                    parent.click();
                    debug("Spunto checkbox Ricordami (click su parent label)");
                    return;
                } catch (Exception ignored) {}

                // click su sibling
                try {
                    WebElement sibling = l.findElement(By.xpath("../following-sibling::*[1]"));
                    sibling.click();
                    debug("Spunto checkbox Ricordami (click su sibling)");
                    return;
                } catch (Exception ignored) {}
            }

            // fallback: prima checkbox visibile
            List<WebElement> cbs = driver.findElements(checkbox);
            for (WebElement cb : cbs) {
                if (cb.isDisplayed()) {
                    cb.click();
                    debug("Spunto checkbox Ricordami (fallback CheckBox)");
                    return;
                }
            }

            // fallback: primo switch visibile
            List<WebElement> sws = driver.findElements(switchCls);
            for (WebElement sw : sws) {
                if (sw.isDisplayed()) {
                    sw.click();
                    debug("Spunto checkbox Ricordami (fallback Switch)");
                    return;
                }
            }

            debug("Ricordami: non trovata (nessuna azione)");
        } catch (Exception e) {
            debug("ensureRicordamiChecked EX: " + e.getMessage());
        }
    }

    /** Click "Accedi" robusto con locator multipli + fallback tap */
    public void clickAccediRobusto() {
        debug("Click ACCEDI (robusto, multi-locator)");

        if (tryClickClickable(accediA11y, "Accedi (a11y)")) return;
        if (tryClickClickable(accediText, "Accedi (text)")) return;

        By accediXpath = By.xpath("//android.widget.Button[@text='Accedi' or @content-desc='Accedi']");
        if (tryClickClickable(accediXpath, "Accedi (xpath)")) return;

        WebElement el = findFirstPresent(accediA11y, accediText, accediXpath);
        if (el != null) {
            debug("Clickable fallito, fallback tap");
            tapCenter(el);
            return;
        }

        throw new NoSuchElementException("Bottone Accedi non trovato con nessun locator");
    }

    private boolean tryClickClickable(By by, String name) {
        try {
            List<WebElement> els = driver.findElements(by);
            if (els.isEmpty()) return false;
            WebElement el = els.get(0);
            if (!el.isDisplayed()) return false;

            wait.until(ExpectedConditions.elementToBeClickable(by)).click();
            return true;
        } catch (Exception e) {
            debug(name + " click fallito: " + e.getMessage());
            return false;
        }
    }

    private WebElement findFirstPresent(By... locators) {
        for (By by : locators) {
            try {
                List<WebElement> els = driver.findElements(by);
                if (!els.isEmpty() && els.get(0).isDisplayed()) return els.get(0);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void tapCenter(WebElement el) {
        Rectangle r = el.getRect();
        int x = r.x + (r.width / 2);
        int y = r.y + (r.height / 2);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(tap));
    }
}
