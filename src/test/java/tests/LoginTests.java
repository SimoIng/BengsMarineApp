package tests;

import base.BaseAppiumTest;
import org.junit.jupiter.api.Test;
import pages.LoginPage;

public class LoginTests extends BaseAppiumTest {

    @Test
    void loginSbagliato_mostraErrore() {
        handleStartupFlow();      // ✅ gestisce Termini & Condizioni
        waitForLoginPageReady();  // ✅ login pronta

        LoginPage loginPage = new LoginPage(driver);

        // submit
        loginPage.faiLogin("collaudodemo", "bth01Us");

        // ✅ robusto: aspetta davvero il dialog e poi clicca Ok quando è cliccabile
        loginPage.waitForErroreDialog();
        loginPage.waitAndCloseErroreDialog();
    }

    @Test
    void loginCorretto_mostraHome() {
        handleStartupFlow();
        waitForLoginPageReady();

        LoginPage loginPage = new LoginPage(driver);
        loginPage.faiLogin("collaudodemo", "bth01User!");

        // TODO: qui inserisci un assert reale di Home
    }
}
