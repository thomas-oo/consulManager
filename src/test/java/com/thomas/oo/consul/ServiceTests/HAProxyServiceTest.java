package com.thomas.oo.consul.ServiceTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.loadBalancer.HAProxyService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
public class HAProxyServiceTest {
    @Autowired
    HAProxyService haProxyController;

    @After
    public void stopHaproxyTest() throws Exception {
        haProxyController.stopProcess();
    }

    @Test
    public void startHAProxyTest() throws Exception {
        assertTrue(confirmHAProxyIsRunning());
    }

    public boolean confirmHAProxyIsRunning() throws Exception {
        return haProxyController.getProcess().isAlive();
    }
}
