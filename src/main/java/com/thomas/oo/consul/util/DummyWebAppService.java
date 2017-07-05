package com.thomas.oo.consul.util;

import java.io.File;

//Meant to start dummy spring-boot apps. This is only for prototyping
public class DummyWebAppService extends AbstractService {
    String mavenPath;
    String webAppPath;
    int port;
    public DummyWebAppService(String mavenPath, String webAppPath, int port) {
        this.mavenPath = mavenPath;
        this.webAppPath = webAppPath;
        this.port = port;
    }

    public void startProcess() throws Exception {
        File webApp;
        webApp = new File(webAppPath);
        if (!webApp.exists()) {
            System.err.println("Could not find web app file!");
            throw new Exception();
        }
        // Start web app
        try {
            String command = mavenPath + " -f " + webAppPath + " spring-boot:run -Dserver.port=" + port;
            this.p = execInShell(command);
            System.out.println("Started web app on port: "+port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopProcess() throws Exception {
        this.p.destroyForcibly();
        System.out.println("Stopped web app on port: "+port);
    }
}