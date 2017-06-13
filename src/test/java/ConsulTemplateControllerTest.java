import com.thomas.oo.consul.consul.ConsulTemplateService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConsulTemplateControllerTest {
    String consulTemplatePath = "/usr/local/bin/consul-template";
    String confFilePath = "/root/Documents/consulProto/haproxy.json";
    String consulAddressAndPort = "localhost:8500";
    ConsulTemplateService consulTemplateController;

    @Before
    public void setUp() throws Exception {
        consulTemplateController = new ConsulTemplateService(consulTemplatePath, confFilePath, consulAddressAndPort);
    }

    @After
    public void tearDown() throws Exception {
        consulTemplateController.stopProcess();
    }

    @Test
    public void startConsulTemplateTest() throws Exception {
        consulTemplateController.startProcess();
        assertTrue(confirmConsulTemplateIsRunning());
    }

    public boolean confirmConsulTemplateIsRunning() throws Exception {
        return consulTemplateController.getProcess().isAlive();
    }
}
