package com.thomas.oo.consul.consul;

import com.thomas.oo.consul.util.AbstractService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class ConsulTemplateService extends AbstractService {
    String executablePath;
    String consulAddressAndPort;
    String confFilePath;

    public ConsulTemplateService(@Value("${consulTemplate.execPath}")String executablePath, @Value("${consulTemplate.confPath}")String confFilePath, @Value("${consulTemplate.consulAddressAndPort}")String consulAddressAndPort) {
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
        System.out.println("Stopped consul-template");
    }
}
