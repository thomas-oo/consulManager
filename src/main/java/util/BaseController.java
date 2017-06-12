package util;

import java.io.IOException;

/**
 * Created by root on 6/12/17.
 */
public abstract class BaseController {
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
