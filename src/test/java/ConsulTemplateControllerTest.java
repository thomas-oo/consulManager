import consul.ConsulTemplateController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.assertTrue;

public class ConsulTemplateControllerTest {
    String consulTemplatePath = "/usr/local/bin/consul-template";
    String confFilePath = "/root/Documents/consulProto/haproxy.json";
    String consulAddressAndPort = "localhost:8500";
    ConsulTemplateController consulTemplateController;

    @Before
    public void setUp() throws Exception {
        consulTemplateController = new ConsulTemplateController(consulTemplatePath, confFilePath, consulAddressAndPort);
    }

    @After
    public void tearDown() throws Exception {
        consulTemplateController.stopConsulTemplate();
    }

    @Test
    public void startConsulTemplateTest() throws Exception {
        consulTemplateController.startConsulTemplate();
        assertTrue(confirmConsulTemplateIsRunning());
    }

    public boolean confirmConsulTemplateIsRunning() throws Exception {
        String findPid = "pidof consul-template";
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(findPid);
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String out = "";
        String pid = "";
        while((out = output.readLine()) != null){
            pid+=out;
        }
        if(pid.equals("")){
            System.out.println("consul-template is not running");
            return false;
        }else{
            System.out.println("consul-template is running, pid: "+ pid);
            return true;
        }
    }
}
