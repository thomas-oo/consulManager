package com.thomas.oo.consul;

import com.thomas.oo.consul.util.DummyWebAppService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DummyWebAppControllerTest {
    String mavenPath = "/root/bin/maven/3.0.5/bin/mvn";
    String webAppPath = "/git/consulPrototype/simpleapp/pom.xml";
    DummyWebAppService dummyWebAppController;//on 8080

    @Before
    public void setUp() throws Exception {
        dummyWebAppController = new DummyWebAppService(mavenPath, webAppPath, 8080);
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

    private boolean confirmWebAppIsRunning(DummyWebAppService dummyWebAppController) {
        return (dummyWebAppController.getProcess().isAlive());
    }
}
