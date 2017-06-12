package loadBalancer;

import util.BaseController;

import java.io.File;

public class HAProxyController extends BaseController{

    private String executablePath;
    private String confFilePath;

    public HAProxyController(String executablePath, String confFilePath) {
        this.executablePath = executablePath;
        this.confFilePath = confFilePath;
    }

    public void startProcess() throws Exception {
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
            this.p = execInShell(command);
            System.out.println("Started HAProxy");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopProcess() throws Exception {
        Runtime r = Runtime.getRuntime();
        r.exec("pkill haproxy");
    }
}