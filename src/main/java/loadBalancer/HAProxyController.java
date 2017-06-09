package loadBalancer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class HAProxyController {

    private String executablePath;
    private String configuration;
    private int listeningPort;
    private String confFilePath;
    private String shell = "bash -c";

    public HAProxyController(String executablePath, int listeningPort, String confFilePath) {
        this.executablePath = executablePath;
        if (!this.executablePath.endsWith("haproxy")) {
            if (!this.executablePath.endsWith("/")) {
                this.executablePath += "/";
            }
            this.executablePath += "haproxy";
        }

        this.listeningPort = listeningPort;
        this.confFilePath = confFilePath;
    }

    public void startLoadBalancer() throws Exception {
        // Check for configuration file
        File conf;
        conf = new File(confFilePath);
        if (!conf.exists()) {
            System.err.println("Could not find haproxy configuration file!");
            throw new Exception();
        }

        // Start haproxy
        try {
            String command = executablePath + " -f " + confFilePath;
            Process p = execInShell(command);
            System.out.println("Started HAProxy");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopLoadBalancer() throws Exception {
        // Kill haproxy process
        String findPid = "pidof haproxy";
        Process process = execInShell(findPid);
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String out = "";
        String pid = "";
        while((out = output.readLine()) != null){
            pid+=out;
        }
        String killHaproxy = "kill -9 "+pid;
        execInShell(killHaproxy);
        System.out.println("stopped HAProxy");
    }

    private Process execInShell(String command) throws IOException {
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(new String[] {"bash", "-c", command});
        return p;
    }
}