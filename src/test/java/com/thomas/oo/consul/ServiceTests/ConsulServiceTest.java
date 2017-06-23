package com.thomas.oo.consul.ServiceTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulService;
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
public class ConsulServiceTest {
    @Autowired
    ConsulService consulService;

    @After
    public void tearDown() throws Exception {
        consulService.stopProcess();
    }

    @Test
    public void startConsulTest() throws Exception {
        assertTrue(confirmConsulIsRunning());
    }

    public boolean confirmConsulIsRunning() throws Exception {
        return consulService.getProcess().isAlive();
    }
}
