import consul.ConsulController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
        consulController.stopConsul();
    }

    @Test
    public void startConsulTest() throws Exception {
        consulController.startConsul();
        assertTrue(confirmConsulIsRunning());
    }

    public boolean confirmConsulIsRunning() throws Exception {
        String findPid = "pidof consul";
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(findPid);
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String out = "";
        String pid = "";
        while((out = output.readLine()) != null){
            pid+=out;
        }
        if(pid.equals("")){
            System.out.println("consul is not running");
            return false;
        }else{
            System.out.println("consul is running, pid: "+ pid);
            return true;
        }
    }
}
