package tests;

import base.BaseAppiumTest;
import org.junit.jupiter.api.Test;

public class SmokeTest extends BaseAppiumTest {

    @Test
    void lAppSiAvviaEMostraUnaActivityQualsiasi() {
        handleStartupFlow();
        String act = safeActivity();
        System.out.println("Activity corrente: " + act);
    }
}
