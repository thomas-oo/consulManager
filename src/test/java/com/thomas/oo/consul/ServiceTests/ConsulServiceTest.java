package com.thomas.oo.consul.ServiceTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulService;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
@TestPropertySource("classpath:testConfig.properties")
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void failJoinNodeTest() throws Exception {
        String invalidNodeAddress = "invalidNodeAddress";
        thrown.expect(Exception.class);
        thrown.expectMessage(String.format("Unable to join cluster at address: %s", invalidNodeAddress));
        consulService.joinCluster(invalidNodeAddress);
    }

    @Test
    public void joinNodeTest() throws Exception {
        //Todo: figure out how to start consul server on a remote server
    }
}
