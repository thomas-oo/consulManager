import com.orbitz.consul.Consul;
import com.orbitz.consul.model.catalog.CatalogService;
import com.thomas.oo.consul.DTO.ServiceDTO;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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

    ServiceDTO localServiceNoTags = new ServiceDTO(testPort, testServiceName, testServiceId);
    ServiceDTO localServiceTags = new ServiceDTO(testPort, testServiceName, testServiceId, "tag1","tag2","tag3","tag4","tag5");
    ServiceDTO remoteServiceNoTags = new ServiceDTO("localhost", testPort, testServiceName, testServiceId);
    ServiceDTO remoteServiceTags = new ServiceDTO("localhost", testPort, testServiceName, testServiceId, "tag1","tag2","tag3","tag4","tag5");

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
            consulClient.deregisterService(service.getServiceId());
        }
    }

    @Test
    public void registerLocalServiceWithNoTagsTest() throws Exception {
        consulClient.registerLocalService(localServiceNoTags);
        consulClient.addNewTTLCheck(testServiceId, 10);
        consulClient.passTTLCheck(testServiceId);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));
    }

    @Test
    public void registerLocalServiceWithTagsTest() throws Exception {
        consulClient.registerLocalService(localServiceTags);
        consulClient.addNewTTLCheck(testServiceId, 10);
        consulClient.passTTLCheck(testServiceId);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));
    }

    @Test
    public void registerRemoteServiceWithNoTagsTest() throws Exception {
        consulClient.registerRemoteService(remoteServiceNoTags);
        consulClient.addNewTTLCheck(testServiceId, 10);
        consulClient.passTTLCheck(testServiceId);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));
    }

    @Test
    public void registerRemoteServiceWithTagsTest() throws Exception {
        consulClient.registerRemoteService(remoteServiceTags);
        consulClient.addNewTTLCheck(testServiceId, 10);
        consulClient.passTTLCheck(testServiceId);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));

    }

    @Test
    public void queryForAllServicesWithNoTagsTest() throws Exception {
        consulClient.registerLocalService(localServiceNoTags);
        assertTrue(consulClient.queryForAllServices().contains(localServiceNoTags.getServiceName()));
    }

    @Test
    public void queryForAllServicesWithTagsTest() throws Exception {
        consulClient.registerLocalService(localServiceNoTags);
        List<CatalogService> services = consulClient.queryForAllServices("tag1");
        assertTrue(services.stream().noneMatch(s -> s.getServiceId().equalsIgnoreCase(localServiceNoTags.getServiceId())));
        consulClient.registerLocalService(localServiceTags);
        services = consulClient.queryForAllServices("tag1");
        assertTrue(services.stream().anyMatch(s -> s.getServiceId().equalsIgnoreCase(localServiceNoTags.getServiceId())));
    }

    @Test
    public void queryForServiceWithNoTagsTest() throws Exception {
        consulClient.registerLocalService(localServiceNoTags);
        assertTrue(consulClient.queryForService(testServiceName, "active").size() == 0);
        List<CatalogService> catalogServices = consulClient.queryForService(testServiceName);
        assertTrue(catalogServices.size() == 1);
        assertTrue(catalogServices.get(0).getAddress().equalsIgnoreCase("127.0.0.1"));
        catalogServices = consulClient.queryForService(testServiceName);
        assertTrue(catalogServices.get(0).getServicePort()==testPort);
    }

    @Test
    public void queryForServiceWithTagsTest() throws Exception {
        consulClient.registerLocalService(new ServiceDTO(testPort, testServiceName, testServiceId, "active", "version1"));
        consulClient.registerLocalService(new ServiceDTO(testPort + 1, testServiceName, testServiceId +"1","active", "version2"));
        consulClient.registerLocalService(new ServiceDTO(testPort + 2, testServiceName, testServiceId +"2", "disabled", "version1"));

        assertTrue(consulClient.queryForService(testServiceName, "active").size() == 2);
        assertTrue(consulClient.queryForService(testServiceName, "version1").size() == 2);
        assertTrue(consulClient.queryForService(testServiceName, "version1", "active").size() == 1);
    }

    @Test
    public void addNewTCPCheckTest() throws Exception {
        consulClient.registerLocalService(new ServiceDTO(testPort, testServiceName, testServiceId, "active", "version1"));
        //connect to consul port, one that is already open.
        consulClient.addNewTCPCheck(testServiceId, "localhost:8500", 1);
        Thread.sleep(1000);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));
    }

    @Test
    public void removeANDTagsFromTest() throws Exception {
        String[] tags = {"active", "version1"};
        consulClient.registerLocalService(new ServiceDTO(testPort, testServiceName, testServiceId, tags));
        List<CatalogService> resultCatalogServices = consulClient.queryForService(testServiceName);
        assertTrue(resultCatalogServices.size()==1);
        List<String> resultTags = resultCatalogServices.get(0).getServiceTags();
        assertTrue(resultTags.containsAll(Arrays.asList(tags)));
        assertTrue(resultTags.size()==tags.length);
    }
}
