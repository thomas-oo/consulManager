import consul.ConsulController;
import consul.ConsulTemplateController;
import loadBalancer.HAProxyController;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.DummyWebAppController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by root on 6/12/17.
 */
public class ConsulIntegrationTest {
    ConsulController consulController;
    ConsulTemplateController consulTemplateController;
    HAProxyController haProxyController;
    DummyWebAppController[] dummyWebAppControllers = new DummyWebAppController[4];

    //Consul
    String consulPath = "/usr/local/bin/consul";
    String consulConfPath = "/root/Documents/consulProto/web.json";

    //Consul Template
    String consulTemplatePath = "/usr/local/bin/consul-template";
    String consulTemplateconfFilePath = "/root/Documents/consulProto/haproxy.json";
    String consulAddressAndPort = "localhost:8500";

    //HAProxy
    String haproxyPath = "/usr/local/bin/haproxy";
    String haproxyConfFilePath = "/root/Documents/consulProto/haproxy.conf";
    int haproxyListeningPort = 8000;

    //DummyWebApp
    String mavenPath = "/root/bin/maven/3.0.5/bin/mvn";
    String webAppPath = "/git/consulPrototype/simpleapp/pom.xml";

    @Before
    public void setUp() throws Exception {
        //Start up consul, 4 dummy webapps, consulTemplate, and HAProxy
        consulController = new ConsulController(consulPath, consulConfPath);
        consulTemplateController = new ConsulTemplateController(consulTemplatePath, consulTemplateconfFilePath, consulAddressAndPort);
        haProxyController = new HAProxyController(haproxyPath, haproxyListeningPort, haproxyConfFilePath);
        dummyWebAppControllers[0] = new DummyWebAppController(mavenPath, webAppPath, 8080);
        dummyWebAppControllers[1] = new DummyWebAppController(mavenPath, webAppPath, 8081);
        dummyWebAppControllers[2] = new DummyWebAppController(mavenPath, webAppPath, 8082);
        dummyWebAppControllers[3] = new DummyWebAppController(mavenPath, webAppPath, 8083);

        //Start
        consulController.startConsul();
        for(DummyWebAppController dummyWebAppController : dummyWebAppControllers){
            dummyWebAppController.startWebApp();
        }
        consulTemplateController.startConsulTemplate();
        haProxyController.startHAProxy();
        Thread.sleep(10000);
    }

    //Test that given a request to an end point with headers that select the desired node, that it is redirected to that node
    @Test
    public void routingTest() throws Exception {
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
        HttpResponse response = httpClient.execute(request);
        assertEquals(200,response.getStatusLine().getStatusCode());
        boolean nextIs8082 = false;
        if(response.getFirstHeader("X-Forwarded-Host").getValue().equals("127.0.0.1:8080")){
            //first server was 8080, next should be 8082 as consulConfPath
            nextIs8082 = true;
        }else{
            //if it isn't 8080, make sure it is 8082
            assertTrue(response.getFirstHeader("X-Forwarded-Host").getValue().equals("127.0.0.1:8082"));
        }
        //send a second request to see if loadbalancing is working
        httpClient = HttpClientBuilder.create().build();
        response = httpClient.execute(request);
        if(nextIs8082){
            assertTrue("Got header: "+response.getFirstHeader("X-Forwarded-Host").getValue(),response.getFirstHeader("X-Forwarded-Host").getValue().equals("127.0.0.1:8082"));
        }else{
            assertTrue(response.getFirstHeader("X-Forwarded-Host").getValue().equals("127.0.0.1:8080"));
        }
    }

    @After
    public void tearDown() throws Exception {
        //Stop
        haProxyController.stopHAProxy();
        consulTemplateController.stopConsulTemplate();
        for(DummyWebAppController dummyWebAppController : dummyWebAppControllers){
            dummyWebAppController.stopWebApp();
        }
        consulController.stopConsul();
    }
}
