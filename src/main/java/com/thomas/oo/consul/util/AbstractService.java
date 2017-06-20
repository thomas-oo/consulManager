package com.thomas.oo.consul.util;

import javax.annotation.PreDestroy;
import java.io.IOException;

public abstract class AbstractService {
    protected Process p;
    public Process getProcess(){
        return p;
    }

    public abstract void startProcess() throws Exception;
    public abstract void stopProcess() throws Exception;

    //Very important: you must redirect the output of these processes somewhere or else the buffer will fill and just hang with no indiction of what is wrong
    //Here, we direct it to this java process's io
    protected Process execInShell(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder().command("bash", "-c", command).inheritIO();
        p = processBuilder.start();
        return p;
    }

    @PreDestroy
    public void destroy(){
        try {
            stopProcess();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
