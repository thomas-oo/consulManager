package util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

//Meant to start dummy spring-boot apps. This is only for prototyping
public class DummyWebAppController {
    String mavenPath;
    String webAppPath;
    int port;
    Process p;
    public long pid = -1;
    public DummyWebAppController(String mavenPath, String webAppPath, int port) {
        this.mavenPath = mavenPath;
        this.webAppPath = webAppPath;
        this.port = port;
    }

    public void startWebApp() throws Exception {
        File webApp;
        webApp = new File(webAppPath);
        if (!webApp.exists()) {
            System.err.println("Could not find web app file!");
            throw new Exception();
        }
        // Start web app
        try {
            String command = mavenPath + " -f " + webAppPath + " spring-boot:run -Dserver.port=" + port;
            p = execInShell(command);
            System.out.println("Started web app on port: "+port);
            pid = getPid();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getPid() {
        long pid;
        try {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getLong(p);
            f.setAccessible(false);
        } catch (Exception e) {
            System.out.println("Could not find pid of web app");
            e.printStackTrace();
            pid = -1;
        }
        return pid;
    }

    public void stopWebApp() throws IOException {
        if(pid == -1){
            System.out.println("Cannot stop webapp as we don't know pid of web app on port: "+port);
            return;
        }
        String killWebApp = "kill -9 "+pid;
        execInShell(killWebApp);
        System.out.println("stopped web app");
    }

    private Process execInShell(String command) throws IOException {
        Runtime r = Runtime.getRuntime();
        p = r.exec(new String[] {"bash", "-c", command});
        return p;
    }

}