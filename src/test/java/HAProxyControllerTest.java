import loadBalancer.HAProxyController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.assertTrue;

public class HAProxyControllerTest {
    String haproxyPath = "/usr/local/bin/haproxy";
    String confFilePath = "/root/Documents/consulProto/haproxy.conf";
    int listeningPort = 8000;
    HAProxyController haProxyController;
    @Before
    public void setUp() throws Exception {
        haProxyController = new HAProxyController(haproxyPath, listeningPort, confFilePath);
    }

    @After
    public void stopHaproxyTest() throws Exception {
        haProxyController.stopHAProxy();
    }

    @Test
    public void startHAProxyTest() throws Exception {
        haProxyController.startHAProxy();
        assertTrue(confirmHAProxyIsRunning());
    }

    public boolean confirmHAProxyIsRunning() throws Exception {
        String findPid = "pidof haproxy";
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(findPid);
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String out = "";
        String pid = "";
        while((out = output.readLine()) != null){
            pid+=out;
        }
        if(pid.equals("")){
            System.out.println("HAProxy is not running");
            return false;
        }else{
            System.out.println("HAProxy is running, pid: "+ pid);
            return true;
        }
    }
}
