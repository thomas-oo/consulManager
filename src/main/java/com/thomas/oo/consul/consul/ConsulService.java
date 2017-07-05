package com.thomas.oo.consul.consul;

import com.thomas.oo.consul.util.AbstractService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

//This is a consul service but in DEV MODE. This is only for prototyping
@Service
public class ConsulService extends AbstractService {
    String executablePath;
    String confFilePath;

    public ConsulService(@Value("${consul.execPath}") String consulPath, @Value("${consul.confPath}")String confFilePath) {
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
        //TODO: put log level into config
        try {
            String command = executablePath + " agent -dev -config-dir=" + confFilePath + " -log-level=err";
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
        System.out.println("Stopped consul");
    }
}
