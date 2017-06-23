package com.thomas.oo.consul.IntegrationTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import com.thomas.oo.consul.consul.ConsulTemplateService;
import com.thomas.oo.consul.util.PropertiesUtil;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
public class PropertiesUtilTest {
    @Autowired
    ConsulClient consulClient;
    @Autowired
    ConsulService consulService;
    @Autowired
    ConsulTemplateService consulTemplateService;
    @Autowired
    PropertiesUtil propertiesUtil;

    @After
    public void tearDown() throws Exception {
        propertiesUtil.cleanUpConfFile();
        propertiesUtil.deleteCreatedPropertyFiles();
    }

    @Test
    public void parsePropertiesTest() throws Exception {
        Map<Object,Object> configMap = propertiesUtil.parsePropertiesFile("config.properties");
        assertTrue(configMap.containsKey("consul.execPath"));
    }

    @Test
    public void parsePropertiesWithIncludeTest() throws Exception {
        Map<Object,Object> configMap = propertiesUtil.parsePropertiesFile("rest_api.properties");
        assertTrue(configMap.containsKey("include"));
    }

    @Test
    public void createPropertiesTest() throws Exception {
        Map<Object,Object> configMap = propertiesUtil.parsePropertiesFile("config.properties");
        consulClient.putEntries(configMap, "testProperties");
        propertiesUtil.createPropertiesFile("/git/consulPrototype/consulLoadBalancing/output/testProperties.properties", "testProperties");
        File outputProperties = new File("output/testProperties.properties");
        // wait a little bit for consul-template to write
        Thread.sleep(100);
        assertTrue(outputProperties.exists());
    }

    //TODO:More of an integration test than a unit test..

    @Test
    public void consulKVSyncTest() throws Exception {
        Map<Object,Object> configMap = propertiesUtil.parsePropertiesFile("config.properties");
        consulClient.putEntries(configMap, "testProperties");
        propertiesUtil.createPropertiesFile("/git/consulPrototype/consulLoadBalancing/output/testProperties.properties", "testProperties");
        File outputProperties = new File("output/testProperties.properties");
        Thread.sleep(100);
        assertTrue(outputProperties.exists());

        //put in a new KV entry
        //assert that it shows up in the properties file
        consulClient.putEntryInFolder("testProperties", "newKey", "newValue");
        Thread.sleep(100);
        Map<Object, Object> newConfigMap = propertiesUtil.parsePropertiesFile("output/testProperties.properties");
        assertTrue(newConfigMap.containsKey("newKey") && newConfigMap.get("newKey").equals("newValue"));
    }
}
