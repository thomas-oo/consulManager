import com.google.common.collect.Sets;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.catalog.CatalogService;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ConsulClientTest {

    ConsulClient consulClient;
    static ConsulService consulService;
    //Consul
    static String consulPath = "/usr/local/bin/consul";
    static String consulConfPath = "/root/Documents/consulProto/web.json";

    String testServiceName = "testService";
    String testServiceId = "test";
    int testPort = 7070;

    @BeforeClass
    public static void setUpClass() throws Exception {
        consulService = new ConsulService(consulPath,consulConfPath);
        consulService.startProcess();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        consulService.stopProcess();
    }

    @Before
    public void setUp() throws Exception {
        consulClient = new ConsulClient(Consul.builder().build());
    }

    @After
    public void tearDown() throws Exception {
        List<CatalogService> services = consulClient.queryForService(testServiceName);
        for (CatalogService service : services){
            consulClient.deregisterLocalService(service.getServiceId());
        }
    }

    @Test
    public void registerLocalServiceWithNoTagsTest() throws Exception {
        consulClient.registerLocalService(testPort, testServiceName, testServiceId, Collections.<String>emptySet());
        consulClient.addNewTTLCheck(testServiceId, 10);
        consulClient.passTTLCheck(testServiceId);
        assertTrue(consulClient.checkLocalService(testServiceName, testServiceId));
    }

    @Test
    public void registerLocalServiceWithTagsTest() throws Exception {
        Set<String> tags = Sets.newHashSet("tag1","tag2","tag3","tag4","tag5");
        consulClient.registerLocalService(testPort, testServiceName, testServiceId, tags);
        consulClient.addNewTTLCheck(testServiceId, 10);
        consulClient.passTTLCheck(testServiceId);
        assertTrue(consulClient.checkLocalService(testServiceName, testServiceId));
    }

    @Test
    public void queryForServiceWithNoTagsTest() throws Exception {
        consulClient.registerLocalService(testPort, testServiceName, testServiceId, Collections.<String>emptySet());
        assertTrue(consulClient.queryForService(testServiceName, "active").size() == 0);
        List<CatalogService> catalogServices = consulClient.queryForService(testServiceName);
        assertTrue(catalogServices.size() == 1);
        assertTrue(catalogServices.get(0).getAddress().equalsIgnoreCase("127.0.0.1"));
        assertTrue(catalogServices.get(0).getServicePort()==testPort);
    }

    @Test
    public void queryForServiceWithTagsTest() throws Exception {
        consulClient.registerLocalService(testPort, testServiceName, testServiceId, new HashSet<String>(Arrays.asList("active", "version1")));
        consulClient.registerLocalService(testPort + 1, testServiceName, testServiceId +"1", new HashSet<String>(Arrays.asList("active", "version2")));
        consulClient.registerLocalService(testPort + 2, testServiceName, testServiceId +"2", new HashSet<String>(Arrays.asList("disabled", "version1")));

        assertTrue(consulClient.queryForService(testServiceName, "active").size() == 2);
        assertTrue(consulClient.queryForService(testServiceName, "version1").size() == 2);
        assertTrue(consulClient.queryForService(testServiceName, "version1", "active").size() == 1);
    }

    @Test
    public void addNewTCPCheckTest() throws Exception {
        consulClient.registerLocalService(testPort, testServiceName, testServiceId, new HashSet<String>(Arrays.asList("active", "version1")));
        //connect to consul port, one that is already open.
        consulClient.addNewTCPCheck(testServiceId, "localhost:8500", 1);
        Thread.sleep(1000);
        assertTrue(consulClient.checkLocalService(testServiceName, testServiceId));
    }

    @Test
    public void removeANDTagsFromTest() throws Exception {
        List<String> tags = Arrays.asList("active", "version1");
        consulClient.registerLocalService(testPort, testServiceName, testServiceId, new HashSet<String>(tags));
        List<CatalogService> resultCatalogServices = consulClient.queryForService(testServiceName);
        assertTrue(resultCatalogServices.size()==1);
        List<String> resultTags = resultCatalogServices.get(0).getServiceTags();
        assertTrue(resultTags.containsAll(tags));
        assertTrue(resultTags.size()==tags.size());
    }
}
