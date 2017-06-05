import com.orbitz.consul.model.catalog.CatalogService;
import consulClient.ConsulClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceRegisterTest {

    String serviceName = "testService";
    String testId = "test";
    int testPort = 7070;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
        List<CatalogService> services = ConsulClient.queryForService(serviceName);
        for (CatalogService service : services){
            ConsulClient.deregisterLocalService(service.getServiceId());
        }
    }

    @Test
    public void registerLocalService() throws Exception {
        ConsulClient.registerLocalService(testPort, 10, serviceName, testId);
        //defaults to false because services default to critical on register
        assertFalse(ConsulClient.checkLocalService(serviceName, testId));
    }

    @Test
    public void checkWebService() throws Exception {
        String webService = "web";
        String id = "web1";
        assertTrue(ConsulClient.checkLocalService(webService, id));
    }

    @Test
    public void queryForTags() throws Exception {
        ConsulClient.registerLocalService(testPort, 10, serviceName, testId, "active", "version1");
        ConsulClient.registerLocalService(testPort + 1, 10, serviceName, testId+"1", "active", "version2");
        ConsulClient.registerLocalService(testPort + 2, 10, serviceName, testId+"2", "disabled", "version1");

        assertTrue(ConsulClient.queryForService(serviceName, "active").size() == 2);
        assertTrue(ConsulClient.queryForService(serviceName, "version1").size() == 2);
        assertTrue(ConsulClient.queryForService(serviceName, "version1", "active").size() == 2);

    }
}
