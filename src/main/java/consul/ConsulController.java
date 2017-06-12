package consul;

import util.BaseController;

import java.io.File;

/**
 * Created by root on 6/9/17.
 */
//This is a controller for consul but in DEV MODE. This is only for prototyping
public class ConsulController extends BaseController{
    String executablePath;
    String confFilePath;

    public ConsulController(String consulPath, String confFilePath) {
        this.executablePath = consulPath;
        this.confFilePath = confFilePath;
    }

    public void startProcess() throws Exception {
        File conf;
        conf = new File(confFilePath);
        if (!conf.exists()) {
            System.err.println("Could not find consul configuration file!");
            throw new Exception();
        }

        // Start consul
        try {
            String command = executablePath + " agent -dev -config-dir=" + confFilePath;
            this.p = execInShell(command);
            System.out.println("Started consul");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopProcess() throws Exception {
        Runtime r = Runtime.getRuntime();
        r.exec("pkill consul");
    }
}
