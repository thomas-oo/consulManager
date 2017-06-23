package com.thomas.oo.consul.ServiceTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulTemplateService;
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
public class ConsulTemplateServiceTest {
    @Autowired
    ConsulTemplateService consulTemplateService;

    @After
    public void tearDown() throws Exception {
        consulTemplateService.stopProcess();
    }

    @Test
    public void startConsulTemplateTest() throws Exception {
        assertTrue(confirmConsulTemplateIsRunning());
    }

    public boolean confirmConsulTemplateIsRunning() throws Exception {
        return consulTemplateService.getProcess().isAlive();
    }
}
