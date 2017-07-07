package com.thomas.oo.consul.consul;

import com.thomas.oo.consul.util.AbstractService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

//This is a consul service but in DEV MODE. This is only for prototyping
@Service
public class ConsulService extends AbstractService {
    String executablePath;
    String confFilePath;
    String dataDirPath;
    String logLevel;

    public ConsulService(@Value("${consul.execPath}") String consulPath, @Value("${consul.confPath}")String confFilePath, @Value("${consul.dataDirPath}") String dataDirPath, @Value("${consul.logLevel}")String logLevel) {
        this.executablePath = consulPath;
        this.confFilePath = confFilePath;
        this.logLevel = logLevel;
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
            String command = String.format("%s agent -config-dir=%s -data-dir=%s -log-level=%s", executablePath, confFilePath, dataDirPath, logLevel);
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

    public void joinCluster(String nodeAddress) throws InvalidParameterException,IOException {
        //consul join <nodeAddress>
        Process process = execInShell(String.format("consul join %s", nodeAddress));
        while(process.isAlive()){

        }
        int exitValue = process.exitValue();
        if(exitValue != 0){
            throw new InvalidParameterException(String.format("Unable to join cluster at address: %s", nodeAddress));
        }
    }
}
