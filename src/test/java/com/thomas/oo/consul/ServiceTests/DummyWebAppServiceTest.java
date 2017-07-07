package com.thomas.oo.consul.ServiceTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.util.DummyWebAppService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
@TestPropertySource("classpath:testConfig.properties")
public class DummyWebAppServiceTest {
    @Value("${dummyWebApp.mvnPath}")
    String mavenPath;
    @Value("${dummyWebApp.webAppPath}")
    String webAppPath;
    DummyWebAppService dummyWebAppService;//on 8080

    @Before
    public void setUp() throws Exception {
        dummyWebAppService = new DummyWebAppService(mavenPath, webAppPath, 8080);
    }

    @After
    public void tearDown() throws Exception {
        dummyWebAppService.stopProcess();
    }

    @Test
    public void startWebAppTest() throws Exception {
        dummyWebAppService.startProcess();
        assertTrue(confirmWebAppIsRunning(dummyWebAppService));
    }

    private boolean confirmWebAppIsRunning(DummyWebAppService dummyWebAppController) {
        return (dummyWebAppController.getProcess().isAlive());
    }
}
