package com.thomas.oo.consul.util;

import java.io.IOException;

public abstract class BaseService {
    protected Process p;
    public abstract void startProcess() throws Exception;
    public void stopProcess() throws Exception {
        p.destroyForcibly();
    }
    protected Process execInShell(String command) throws IOException {
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(new String[] {"bash", "-c", command});
        return p;
    }

    public Process getProcess(){
        return p;
    }
}
