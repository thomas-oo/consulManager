package com.thomas.oo.consul.IntegrationTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import com.thomas.oo.consul.consul.ConsulTemplateService;
import com.thomas.oo.consul.util.PropertyFilesUtil;
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
public class PropertyFilesUtilTest {
    @Autowired
    ConsulClient consulClient;
    @Autowired
    ConsulService consulService;
    @Autowired
    ConsulTemplateService consulTemplateService;
    @Autowired
    PropertyFilesUtil propertyFilesUtil;

    @After
    public void tearDown() throws Exception {
        propertyFilesUtil.cleanUpConfFile();
        propertyFilesUtil.deleteCreatedPropertyFiles();
    }

    @Test
    public void parsePropertiesTest() throws Exception {
        Map<Object,Object> configMap = propertyFilesUtil.parsePropertiesFile("config.properties");
        assertTrue(configMap.containsKey("consul.execPath"));
    }

    @Test
    public void parsePropertiesWithIncludeTest() throws Exception {
        Map<Object,Object> configMap = propertyFilesUtil.parsePropertiesFile("rest_api.properties");
        assertTrue(configMap.containsKey("include"));
    }

    @Test
    public void createPropertiesTest() throws Exception {
        Map<Object,Object> configMap = propertyFilesUtil.parsePropertiesFile("config.properties");
        consulClient.putEntries(configMap, "testProperties");
        propertyFilesUtil.createPropertiesFile("/git/consulPrototype/consulLoadBalancing/output/testProperties.properties", "testProperties");
        File outputProperties = new File("output/testProperties.properties");
        // wait a little bit for consul-template to write
        Thread.sleep(1000);
        assertTrue(outputProperties.exists());
    }

    //TODO:More of an integration test than a unit test..
    @Test
    public void consulKVSyncTest() throws Exception {
        Map<Object,Object> configMap = propertyFilesUtil.parsePropertiesFile("config.properties");
        consulClient.putEntries(configMap, "testProperties");
        propertyFilesUtil.createPropertiesFile("/git/consulPrototype/consulLoadBalancing/output/testProperties.properties", "testProperties");
        File outputProperties = new File("output/testProperties.properties");
        Thread.sleep(1000);
        assertTrue(outputProperties.exists());

        //put in a new KV entry
        //assert that it shows up in the properties file
        consulClient.putEntryInFolder("testProperties", "newKey", "newValue");
        Thread.sleep(100);
        Map<Object, Object> newConfigMap = propertyFilesUtil.parsePropertiesFile("output/testProperties.properties");
        assertTrue(newConfigMap.containsKey("newKey") && newConfigMap.get("newKey").equals("newValue"));
    }
}
