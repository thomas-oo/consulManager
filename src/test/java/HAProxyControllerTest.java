import loadBalancer.HAProxyController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HAProxyControllerTest {
    String haproxyPath = "/usr/local/bin/haproxy";
    String confFilePath = "/root/Documents/consulProto/haproxy.conf";
    HAProxyController haProxyController;
    @Before
    public void setUp() throws Exception {
        haProxyController = new HAProxyController(haproxyPath, confFilePath);
    }

    @After
    public void stopHaproxyTest() throws Exception {
        haProxyController.stopProcess();
    }

    @Test
    public void startHAProxyTest() throws Exception {
        haProxyController.startProcess();
        assertTrue(confirmHAProxyIsRunning());
    }

    public boolean confirmHAProxyIsRunning() throws Exception {
        return haProxyController.getProcess().isAlive();
    }
}
