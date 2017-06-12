package consul;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by root on 6/9/17.
 */
//This is a controller for consul but in DEV MODE. This is only for prototyping
public class ConsulController {
    String executablePath;
    String confFilePath;

    public ConsulController(String consulPath, String confFilePath) {
        this.executablePath = consulPath;
        this.confFilePath = confFilePath;
    }

    public void startConsul() throws Exception {
        File conf;
        conf = new File(confFilePath);
        if (!conf.exists()) {
            System.err.println("Could not find consul configuration file!");
            throw new Exception();
        }

        // Start consul
        try {
            String command = executablePath + " agent -dev -config-dir=" + confFilePath;
            Process p = execInShell(command);
            System.out.println("Started consul");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopConsul() throws Exception {
        String findPid = "pidof consul";
        Process process = execInShell(findPid);
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String out = "";
        String pid = "";
        while((out = output.readLine()) != null){
            pid+=out;
        }
        String killHaproxy = "kill -9 "+pid;
        execInShell(killHaproxy);
        System.out.println("stopped consul");
    }

    private Process execInShell(String command) throws IOException {
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(new String[] {"bash", "-c", command});
        return p;
    }

}
