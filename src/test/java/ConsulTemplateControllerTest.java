import consul.ConsulTemplateController;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.assertTrue;

/**
 * Created by root on 6/9/17.
 */
public class ConsulTemplateControllerTest {
    String consulTemplatePath = "/usr/local/bin/consul-template";
    String confFilePath = "/root/Documents/consulProto/haproxy.json";
    String consulAddressAndPort = "localhost:8500";
    @Test
    public void startConsulTemplateTest() throws Exception {
        ConsulTemplateController consulTemplateController = new ConsulTemplateController(consulTemplatePath, consulAddressAndPort, confFilePath);
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
