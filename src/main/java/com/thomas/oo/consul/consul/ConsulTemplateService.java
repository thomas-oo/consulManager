package com.thomas.oo.consul.consul;

import com.thomas.oo.consul.util.AbstractService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

@Service
public class ConsulTemplateService extends AbstractService {
    String executablePath;
    String consulAddressAndPort;
    String confFilePath;

    //Optional
    String sourceTemplatePath;
    String destinationPath;

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

    public void reloadConfig() {
        long pid = -1;
        try{
            if(this.p.getClass().getName().equals("java.lang.UNIXProcess")){
                Field f = this.p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(this.p);
                f.setAccessible(false);
            }
        }catch (Exception e){
            pid = -1;
            return;
        }
        try {
            execInShell(String.format("kill -HUP %s",pid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopProcess() throws Exception {
        Runtime r = Runtime.getRuntime();
        r.exec("killall -9 consul-template");
        System.out.println("Stopped consul-template");
    }

    public String getConfFilePath() {
        return confFilePath;
    }
}
