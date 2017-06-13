package com.thomas.oo.consul.loadBalancer;

import com.thomas.oo.consul.util.BaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class HAProxyService extends BaseService {
    String executablePath;
    String confFilePath;

    public HAProxyService(@Value("${haproxy.execPath}")String executablePath, @Value("${haproxy.confPath}")String confFilePath) {
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
        System.out.println("Stopped HAProxy");
    }
}