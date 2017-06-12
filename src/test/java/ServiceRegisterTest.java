import com.google.common.collect.Sets;
import com.orbitz.consul.model.catalog.CatalogService;
import consul.ConsulClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ServiceRegisterTest {

    String testServiceName = "testService";
    String testServiceId = "test";
    int testPort = 7070;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        List<CatalogService> services = ConsulClient.queryForService(testServiceName);
        for (CatalogService service : services){
            ConsulClient.deregisterLocalService(service.getServiceId());
        }
    }

    @Test
    public void registerLocalServiceWithNoTagsTest() throws Exception {
        ConsulClient.registerLocalService(testPort, testServiceName, testServiceId, Collections.<String>emptySet());
        ConsulClient.addNewTTLCheck(testServiceId, 10);
        ConsulClient.passTTLCheck(testServiceId);
        assertTrue(ConsulClient.checkLocalService(testServiceName, testServiceId));
    }

    @Test
    public void registerLocalServiceWithTagsTest() throws Exception {
        Set<String> tags = Sets.newHashSet("tag1","tag2","tag3","tag4","tag5");
        ConsulClient.registerLocalService(testPort, testServiceName, testServiceId, tags);
        ConsulClient.addNewTTLCheck(testServiceId, 10);
        ConsulClient.passTTLCheck(testServiceId);
        assertTrue(ConsulClient.checkLocalService(testServiceName, testServiceId));
    }

    @Test
    public void queryForServiceWithNoTagsTest() throws Exception {
        ConsulClient.registerLocalService(testPort, testServiceName, testServiceId, Collections.<String>emptySet());
        assertTrue(ConsulClient.queryForService(testServiceName, "active").size() == 0);
        List<CatalogService> catalogServices = ConsulClient.queryForService(testServiceName);
        assertTrue(catalogServices.size() == 1);
        assertTrue(catalogServices.get(0).getAddress().equalsIgnoreCase("127.0.0.1"));
        assertTrue(catalogServices.get(0).getServicePort()==testPort);
    }

    @Test
    public void queryForServiceWithTagsTest() throws Exception {
        ConsulClient.registerLocalService(testPort, testServiceName, testServiceId, new HashSet<String>(Arrays.asList("active", "version1")));
        ConsulClient.registerLocalService(testPort + 1, testServiceName, testServiceId +"1", new HashSet<String>(Arrays.asList("active", "version2")));
        ConsulClient.registerLocalService(testPort + 2, testServiceName, testServiceId +"2", new HashSet<String>(Arrays.asList("disabled", "version1")));

        assertTrue(ConsulClient.queryForService(testServiceName, "active").size() == 2);
        assertTrue(ConsulClient.queryForService(testServiceName, "version1").size() == 2);
        assertTrue(ConsulClient.queryForService(testServiceName, "version1", "active").size() == 1);
    }

    @Test
    public void addNewTCPCheckTest() throws Exception {
        ConsulClient.registerLocalService(testPort, testServiceName, testServiceId, new HashSet<String>(Arrays.asList("active", "version1")));
        //connect to a dummy port, one that is already open TODO: find how to open a port locally for testing purposes
        ConsulClient.addNewTCPCheck(testServiceId, "localhost:8080", 1);
        Thread.sleep(1000);
        assertTrue(ConsulClient.checkLocalService(testServiceName, testServiceId));
    }
}
