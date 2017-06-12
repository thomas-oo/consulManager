package consul;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsulTemplateController{
    String executablePath;
    String consulAddressAndPort;
    String confFilePath;

    public ConsulTemplateController(String executablePath, String confFilePath, String consulAddressAndPort) {
        this.executablePath = executablePath;
        this.consulAddressAndPort = consulAddressAndPort;
        this.confFilePath = confFilePath;
    }

    public void startConsulTemplate() throws Exception {
        File conf;
        conf = new File(confFilePath);
        if (!conf.exists()) {
            System.err.println("Could not find consul-template configuration file!");
            throw new Exception();
        }

        // Start consul-template
        try {
            String command = executablePath + " -consul-addr=" + consulAddressAndPort + " -config=" + confFilePath;
            Process p = execInShell(command);
            System.out.println("Started consul-template");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopConsulTemplate() throws Exception{
        // Kill haproxy process
        String findPid = "pidof consul-template";
        Process process = execInShell(findPid);
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String out = "";
        String pid = "";
        while((out = output.readLine()) != null){
            pid+=out;
        }
        String killConsulTemplate = "kill -9 "+pid;
        execInShell(killConsulTemplate);
        System.out.println("stopped consul-template");

    }

    private Process execInShell(String command) throws IOException {
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(new String[] {"bash", "-c", command});
        return p;
    }

}
