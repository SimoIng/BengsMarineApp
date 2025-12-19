package tests;

import base.BaseAppiumTest;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

public class SmokeTest extends BaseAppiumTest {

    @Test
    @Order(1)
    public void lAppSiAvviaEMostraUnaActivityQualsiasi() {
        System.out.println("DEBUG - @BeforeAll setUpClass (gestito da BaseAppiumTest)");

        handleStartupFlow();

        String act = safeActivity();
        System.out.println("Activity corrente: " + act);

        assert act != null && !act.isEmpty() : "Activity corrente non valida!";
    }
}
