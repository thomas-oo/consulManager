package com.thomas.oo.consul.IntegrationTests;

import com.orbitz.consul.model.catalog.CatalogService;
import com.thomas.oo.consul.DTO.CheckDTO;
import com.thomas.oo.consul.DTO.ServiceDTO;
import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
@TestPropertySource("classpath:testConfig.properties")
public class ConsulClientTest{

    @Autowired
    ConsulClient consulClient;

    @Autowired
    ConsulService consulService;

    String consulAddress;

    String testServiceName = "testService";
    String testServiceId = "test";
    int testPort = 7070;

    ServiceDTO localServiceNoTags = new ServiceDTO(testPort, testServiceName, testServiceId);
    ServiceDTO localServiceTags = new ServiceDTO(testPort, testServiceName, testServiceId, "tag1","tag2","tag3","tag4","tag5");
    ServiceDTO remoteServiceNoTags = new ServiceDTO("localhost", testPort, testServiceName, testServiceId);
    ServiceDTO remoteServiceTags = new ServiceDTO("localhost", testPort, testServiceName, testServiceId, "tag1","tag2","tag3","tag4","tag5");

    @Before
    public void setUp() throws Exception {
        String consulHostName = consulClient.getConsulAddressAndPort().split(":")[0];
        InetAddress address;
        if(consulHostName.equalsIgnoreCase("localhost")){
            address = InetAddress.getLocalHost();
        }else{
            address = InetAddress.getByName(consulHostName);
        }
        consulAddress = address.getHostAddress();
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
        CheckDTO.TTLCheckDTO ttlCheckDTO = new CheckDTO.TTLCheckDTO();
        ttlCheckDTO.setInterval(10);
        consulClient.addNewTTLCheck(testServiceId, ttlCheckDTO);
        consulClient.passTTLCheck(localServiceNoTags);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));
    }

    @Test
    public void registerLocalServiceWithTagsTest() throws Exception {
        consulClient.registerLocalService(localServiceTags);
        CheckDTO.TTLCheckDTO ttlCheckDTO = new CheckDTO.TTLCheckDTO();
        ttlCheckDTO.setInterval(10);
        consulClient.addNewTTLCheck(testServiceId, ttlCheckDTO);
        consulClient.passTTLCheck(localServiceTags);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));
    }

    @Test
    public void registerRemoteServiceWithNoTagsTest() throws Exception {
        consulClient.registerRemoteService(remoteServiceNoTags);
        CheckDTO.TTLCheckDTO ttlCheckDTO = new CheckDTO.TTLCheckDTO();
        ttlCheckDTO.setInterval(10);
        consulClient.addNewTTLCheck(testServiceId, ttlCheckDTO);
        consulClient.passTTLCheck(remoteServiceNoTags);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));
    }

    @Test
    public void registerRemoteServiceWithTagsTest() throws Exception {
        consulClient.registerRemoteService(remoteServiceTags);
        CheckDTO.TTLCheckDTO ttlCheckDTO = new CheckDTO.TTLCheckDTO();
        ttlCheckDTO.setInterval(10);
        consulClient.addNewTTLCheck(testServiceId, ttlCheckDTO);
        consulClient.passTTLCheck(remoteServiceTags);
        assertTrue(consulClient.checkService(testServiceName, testServiceId));
    }

    @Test
    public void queryForAllServicesWithNoTagsTest() throws Exception {
        consulClient.registerLocalService(localServiceNoTags);
        List<CatalogService> services = consulClient.queryForAllServices();
        assertTrue(services.stream().anyMatch(s -> s.getServiceId().equalsIgnoreCase(localServiceNoTags.getServiceId())));
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
        assertEquals(0,consulClient.queryForService(testServiceName, "active").size());
        List<CatalogService> catalogServices = consulClient.queryForService(testServiceName);
        assertEquals(1, catalogServices.size());
        assertEquals(consulAddress,catalogServices.get(0).getAddress());
        catalogServices = consulClient.queryForService(testServiceName);
        assertEquals(testPort, catalogServices.get(0).getServicePort());
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
        CheckDTO.TCPCheckDTO tcpCheckDTO = new CheckDTO.TCPCheckDTO();
        tcpCheckDTO.setAddressAndPort("localhost:8500");
        tcpCheckDTO.setInterval(1);
        consulClient.addNewTCPCheck(testServiceId, tcpCheckDTO);
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

    @Test
    public void keyValueTest() throws Exception {
        String key = "testKey";
        String value = "testValue";
        consulClient.putEntry(key, value);
        assertTrue(consulClient.getValue(key).equals(value));
        consulClient.deleteKey(key);
        assertTrue(consulClient.getValue(key).equals(""));
    }

    @Test
    public void removeCheckTest() throws Exception {
        CheckDTO.TTLCheckDTO ttlCheckDTO = new CheckDTO.TTLCheckDTO();
        ttlCheckDTO.setCheckId(UUID.randomUUID().toString());
        ttlCheckDTO.setInterval(10);
        consulClient.registerLocalService(localServiceNoTags);
        consulClient.addNewTTLCheck(localServiceNoTags.getServiceId(), ttlCheckDTO);
        assertTrue(consulClient.getHealthCheck(localServiceNoTags, ttlCheckDTO.getCheckId()).isPresent());
        consulClient.removeCheck(ttlCheckDTO.getCheckId());
        assertFalse(consulClient.getHealthCheck(localServiceNoTags, ttlCheckDTO.getCheckId()).isPresent());
    }
}
