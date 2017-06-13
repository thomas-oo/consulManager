import com.thomas.oo.consul.consul.ConsulService;
import com.thomas.oo.consul.consul.ConsulTemplateService;
import com.thomas.oo.consul.loadBalancer.HAProxyService;
import com.thomas.oo.consul.util.DummyWebAppService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testing whole stack from haproxy to consul and service. Testing by passing http requests to proxy
 */
public class ConsulIntegrationTest {
    static ConsulService consulController;
    static ConsulTemplateService consulTemplateController;
    static HAProxyService haProxyController;
    static DummyWebAppService[] dummyWebAppControllers = new DummyWebAppService[4];

    //Consul
    static String consulPath = "/usr/local/bin/consul";
    static String consulConfPath = "/root/Documents/consulProto/web.json";

    //Consul Template
    static String consulTemplatePath = "/usr/local/bin/consul-template";
    static String consulTemplateconfFilePath = "/root/Documents/consulProto/haproxy.json";
    static String consulAddressAndPort = "localhost:8500";

    //HAProxy
    static String haproxyPath = "/usr/local/bin/haproxy";
    static String haproxyConfFilePath = "/root/Documents/consulProto/haproxy.conf";
    static int haproxyListeningPort = 8000;

    //DummyWebApp
    static String mavenPath = "/root/bin/maven/3.0.5/bin/mvn";
    static String webAppPath = "/git/consulPrototype/simpleapp/pom.xml";

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Start up consul, 4 dummy webapps, consulTemplate, and HAProxy
        consulController = new ConsulService(consulPath, consulConfPath);
        consulTemplateController = new ConsulTemplateService(consulTemplatePath, consulTemplateconfFilePath, consulAddressAndPort);
        haProxyController = new HAProxyService(haproxyPath, haproxyConfFilePath);
        dummyWebAppControllers[0] = new DummyWebAppService(mavenPath, webAppPath, 8080);
        dummyWebAppControllers[1] = new DummyWebAppService(mavenPath, webAppPath, 8081);
        dummyWebAppControllers[2] = new DummyWebAppService(mavenPath, webAppPath, 8082);
        dummyWebAppControllers[3] = new DummyWebAppService(mavenPath, webAppPath, 8083);

        //Start
        consulController.startProcess();
        consulTemplateController.startProcess();
        haProxyController.startProcess();
        for(DummyWebAppService dummyWebAppController : dummyWebAppControllers){
            dummyWebAppController.startProcess();
        }
        waitUntilProcessesStart();
        //time for new conf file to be written and haproxy to be reloaded
        Thread.sleep(5000);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        //Stop
        for(DummyWebAppService dummyWebAppController : dummyWebAppControllers){
            dummyWebAppController.stopProcess();
        }
        haProxyController.stopProcess();
        consulTemplateController.stopProcess();
        consulController.stopProcess();
    }

    private static void waitUntilProcessesStart() throws Exception {
        boolean processesStarted = false;
        while(!processesStarted){
           processesStarted = true;
           int[] ports = {8500,8000,8080,8081,8082,8083};
           for(int port : ports){
               Runtime r = Runtime.getRuntime();
               Process p = r.exec("lsof -i :"+port);
               BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
               if(stdInput.lines().count()==0){
                   processesStarted = false;
               }
           }
           Thread.sleep(1000);
        }
    }

    //Test that given a request to an end point with headers that select the desired node, that it is redirected to that node
    @Test
    public void routingForWeb_ActiveAndSpringTest() throws Exception {
        String url = "http://localhost:"+haproxyListeningPort+"/greeting";
        String serviceNameHeader = "X-service-name";
        String tagNameHeader = "X-tag-name";
        //Try to route to active and spring web service
        //TODO: wrap this in a class that handles this
        String service = "web";
        String tag = "activeANDspring";
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        request.addHeader(serviceNameHeader, service);
        request.addHeader(tagNameHeader, tag);
        Set<Integer> expectedPorts = new HashSet<>();
        expectedPorts.add(8080); expectedPorts.add(8081);
        for(int i=0; i<expectedPorts.size(); i++){
            HttpResponse response = httpClient.execute(request);
            assertEquals(200,response.getStatusLine().getStatusCode());
            int routedPort;
            String routedServerIp = response.getFirstHeader("X-Forwarded-Host").getValue();
            String routedServerPort = routedServerIp.substring(routedServerIp.indexOf(":")+1).trim();
            routedPort = Integer.parseInt(routedServerPort);
            assertTrue(expectedPorts.contains(routedPort));
            expectedPorts.remove(routedPort);
            request.releaseConnection();
        }
    }

    @Test
    public void routingForWeb_SpringTest() throws Exception {
        String url = "http://localhost:"+haproxyListeningPort+"/greeting";
        String serviceNameHeader = "X-service-name";
        String tagNameHeader = "X-tag-name";
        //Try to route to active and spring web service
        //TODO: wrap this in a class that handles this
        String service = "web";
        String tag = "spring";
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        request.addHeader(serviceNameHeader, service);
        request.addHeader(tagNameHeader, tag);

        Set<Integer> expectedPorts = new HashSet<>();
        expectedPorts.add(8080); expectedPorts.add(8081); expectedPorts.add(8082);
        for(int i=0; i<expectedPorts.size(); i++){
            HttpResponse response = httpClient.execute(request);
            assertEquals(200,response.getStatusLine().getStatusCode());
            int routedPort;
            String routedServerIp = response.getFirstHeader("X-Forwarded-Host").getValue();
            String routedServerPort = routedServerIp.substring(routedServerIp.indexOf(":")+1).trim();
            routedPort = Integer.parseInt(routedServerPort);
            assertTrue(expectedPorts.contains(routedPort));
            expectedPorts.remove(routedPort);
            request.releaseConnection();
        }
    }


}
