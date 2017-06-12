import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.DummyWebAppController;

import static org.junit.Assert.assertTrue;

public class DummyWebAppControllerTest {
    String mavenPath = "/root/bin/maven/3.0.5/bin/mvn";
    String webAppPath = "/git/consulPrototype/simpleapp/pom.xml";
    DummyWebAppController dummyWebAppController;//on 8080

    @Before
    public void setUp() throws Exception {
        dummyWebAppController = new DummyWebAppController(mavenPath, webAppPath, 8080);
    }

    @After
    public void tearDown() throws Exception {
        dummyWebAppController.stopProcess();
    }

    @Test
    public void startWebAppTest() throws Exception {
        dummyWebAppController.startProcess();
        assertTrue(confirmWebAppIsRunning(dummyWebAppController));
    }

    private boolean confirmWebAppIsRunning(DummyWebAppController dummyWebAppController) {
        return (dummyWebAppController.getProcess().isAlive());
    }
}
