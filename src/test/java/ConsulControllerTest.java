import consul.ConsulController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConsulControllerTest {
    String consulPath = "/usr/local/bin/consul";
    String consulConfPath = "/root/Documents/consulProto/web.json";
    ConsulController consulController;

    @Before
    public void setUp() throws Exception {
        consulController = new ConsulController(consulPath, consulConfPath);
    }

    @After
    public void tearDown() throws Exception {
        consulController.stopProcess();
    }

    @Test
    public void startConsulTest() throws Exception {
        consulController.startProcess();
        assertTrue(confirmConsulIsRunning());
    }

    public boolean confirmConsulIsRunning() throws Exception {
        return consulController.getProcess().isAlive();
    }
}
