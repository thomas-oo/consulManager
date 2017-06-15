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

    protected Process execInShell(String command) throws IOException {
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(new String[] {"bash", "-c", command});
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
