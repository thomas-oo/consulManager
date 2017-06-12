package consul;

import util.BaseController;

import java.io.File;


public class ConsulTemplateController extends BaseController{
    String executablePath;
    String consulAddressAndPort;
    String confFilePath;

    public ConsulTemplateController(String executablePath, String confFilePath, String consulAddressAndPort) {
        this.executablePath = executablePath;
        this.consulAddressAndPort = consulAddressAndPort;
        this.confFilePath = confFilePath;
    }

    public void startProcess() throws Exception {
        File conf;
        conf = new File(confFilePath);
        if (!conf.exists()) {
            System.err.println("Could not find consul-template configuration file!");
            throw new Exception();
        }

        // Start consul-template
        try {
            String command = executablePath + " -consul-addr=" + consulAddressAndPort + " -config=" + confFilePath;
            this.p = execInShell(command);
            System.out.println("Started consul-template");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void stopProcess() throws Exception {
        Runtime r = Runtime.getRuntime();
        r.exec("pkill consul-template");
    }
}
