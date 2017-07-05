package com.thomas.oo.consul.IntegrationTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulService;
import com.thomas.oo.consul.consul.ConsulTemplateService;
import com.thomas.oo.consul.loadBalancer.HAProxyService;
import com.thomas.oo.consul.util.DummyWebAppService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testing whole stack from haproxy to consul and service. Testing by passing http requests to proxy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
@Order(value = Ordered.LOWEST_PRECEDENCE)
@TestExecutionListeners(listeners = {LoadBalancingIntegrationTest.class, DependencyInjectionTestExecutionListener.class})
public class LoadBalancingIntegrationTest extends AbstractTestExecutionListener{
    @Autowired
    ConsulService consulController;
    @Autowired
    ConsulTemplateService consulTemplateController;
    @Autowired
    HAProxyService haProxyController;

    DummyWebAppService[] dummyWebAppControllers = new DummyWebAppService[4];
    int haproxyListeningPort = 8000;
    @Value("${dummyWebApp.mvnPath}")
    String mavenPath;

    @Value("${dummyWebApp.webAppPath}")
    String webAppPath;

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        testContext.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);
        dummyWebAppControllers[0] = new DummyWebAppService(mavenPath, webAppPath, 8080);
        dummyWebAppControllers[1] = new DummyWebAppService(mavenPath, webAppPath, 8081);
        dummyWebAppControllers[2] = new DummyWebAppService(mavenPath, webAppPath, 8082);
        dummyWebAppControllers[3] = new DummyWebAppService(mavenPath, webAppPath, 8083);

        for(DummyWebAppService dummyWebAppController : dummyWebAppControllers){
            dummyWebAppController.startProcess();
        }
        waitUntilWebAppsStart();
        //time for new conf file to be written and haproxy to be reloaded
        Thread.sleep(500);
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        for(DummyWebAppService dummyWebAppController : dummyWebAppControllers){
            dummyWebAppController.stopProcess();
        }
    }

    //Test that given a request to an end point with headers that select the desired node, that it is redirected to that node
    @Test
    public void routingForWeb_ActiveAndSpringTest() throws Exception {
        String url = "http://localhost:"+haproxyListeningPort+"/greeting";
        String serviceNameHeader = "X-service-name";
        String tagNameHeader = "X-tag-name";
        //Try to route to active and spring web service
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

    private static void waitUntilWebAppsStart() throws Exception {
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
            Thread.sleep(100);
        }
    }

}
