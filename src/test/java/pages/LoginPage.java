package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LoginPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private final By accediBtn = AppiumBy.accessibilityId("Accedi");
    private final By erroreOkBtn = AppiumBy.accessibilityId("Ok");

    public LoginPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void faiLogin(String username, String password) {
        System.out.println("LOGINPAGE - START faiLogin");

        List<WebElement> fields = driver.findElements(By.className("android.widget.EditText"));
        if (fields.size() < 2) {
            throw new AssertionError("Attesi >=2 EditText, trovati=" + fields.size());
        }

        WebElement user = fields.get(0);
        WebElement pass = fields.get(1);

        // Username
        user.click();
        user.clear();
        user.sendKeys(username);

        // Password
        pass.click();
        pass.clear();
        pass.sendKeys(password);

        // ✅ chiude tastiera / done
        pressDoneOrHideKeyboard();

        // ✅ click robusto su Accedi
        wait.until(ExpectedConditions.elementToBeClickable(accediBtn)).click();
    }

    /** Attende che compaia il dialog errore (Ok visibile) */
    public void waitForErroreDialog() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(erroreOkBtn));
    }

    /** Attende che OK sia cliccabile e lo clicca */
    public void waitAndCloseErroreDialog() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(erroreOkBtn))
                .click();
    }

    private void pressDoneOrHideKeyboard() {
        // 1) ENTER (Done/Completato in molti casi)
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
            Thread.sleep(250);
            return;
        } catch (Exception ignored) {}

        // 2) hideKeyboard
        try {
            driver.hideKeyboard();
            Thread.sleep(250);
            return;
        } catch (Exception ignored) {}

        // 3) BACK chiude tastiera
        try {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
            Thread.sleep(250);
        } catch (Exception ignored) {}
    }
}
