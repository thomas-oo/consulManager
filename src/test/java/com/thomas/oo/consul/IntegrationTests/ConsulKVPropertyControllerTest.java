package com.thomas.oo.consul.IntegrationTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.controllers.ConsulKVPropertyController;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
@TestPropertySource("classpath:testConfig.properties")
public class ConsulKVPropertyControllerTest {
    @Autowired
    ConsulKVPropertyController consulKVPropertyController;
    @Autowired
    ConsulClient consulClient;

    public static final String TEST_DEFAULT_PROPERTIES = "test-default-properties/";
    public static final String OUTPUT_PROPERTY_FOLDER_NAME = "outputPropertyFiles";
    public static final String CONFIG_FILES_FOLDER_NAME = "/root/Documents/consulProto/propertyFiles/config";
    File outputPropertiesFolder;

    @Before
    public void setUp() throws Exception {
        //Populate consul KV with some default properties
        Map<String, Path> kvFolderToPath = new HashMap<>();
        kvFolderToPath.put(TEST_DEFAULT_PROPERTIES, new File("config.properties").toPath());
        consulKVPropertyController.uploadPropertyFiles(kvFolderToPath);

        //Make sure output property folder exists
        outputPropertiesFolder = new File(OUTPUT_PROPERTY_FOLDER_NAME);
        if(!outputPropertiesFolder.exists()){
            outputPropertiesFolder.mkdir();
        }
    }

    @After
    public void tearDown() throws Exception {
        consulKVPropertyController.stopWritingPropertyFiles();
        consulKVPropertyController.deleteCreatedPropertyFiles();
        Thread.sleep(100);

        assertTrue(outputPropertiesFolder.exists());
        assertEquals(0, outputPropertiesFolder.list().length);
    }

    @Test
    public void uploadPropertiesTest() throws Exception {
        Map<String, Path> propertyFilesMap = new HashMap<>();
        Path propertyFilesFolder = Paths.get(CONFIG_FILES_FOLDER_NAME);
        File[] propertyFiles = propertyFilesFolder.toFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith("properties");
            }
        });
        for(File propertyFile : propertyFiles){
            String name = propertyFile.getName() + "/";
            propertyFilesMap.put(name, propertyFile.toPath());
        }
        consulKVPropertyController.uploadPropertyFiles(propertyFilesMap);
        Thread.sleep(100);
        assertTrue(consulKVPropertyController.checkIfAllFoldersExists(propertyFilesMap.keySet()));
    }

    @Test
    public void writePropertiesTest() throws Exception {
        Map<String, Path> fileToPath = new HashMap<>();
        fileToPath.put(TEST_DEFAULT_PROPERTIES, new File("outputPropertyFiles/testOutput.properties").toPath());
        assertTrue(consulKVPropertyController.writePropertyFiles(fileToPath));
        Thread.sleep(100);
        assertTrue(outputPropertiesFolder.exists());
        assertEquals(1,  outputPropertiesFolder.list().length);
        assertEquals("testOutput.properties", outputPropertiesFolder.list()[0]);
    }

    @Test
    public void readPropertiesTest() throws Exception {
        //Write properties
        Map<String, Path> fileToPath = new HashMap<>();
        fileToPath.put(TEST_DEFAULT_PROPERTIES, new File("outputPropertyFiles/testOutput.properties").toPath());
        assertTrue(consulKVPropertyController.writePropertyFiles(fileToPath));
        Thread.sleep(100);
        assertTrue(outputPropertiesFolder.exists());
        assertEquals(1,  outputPropertiesFolder.list().length);
        assertEquals("testOutput.properties", outputPropertiesFolder.list()[0]);

        //Try to read these properties
        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
        // do not expand include, use composite configuration to interpolate
        PropertiesConfiguration.setInclude("IDONOTWANTINCLUDETOWORK");
        propertiesConfiguration.setFileName("outputPropertyFiles/testOutput.properties");
        propertiesConfiguration.load();
        String consulExecPath = propertiesConfiguration.getString("consul.execPath");
        assertEquals(consulExecPath, "/usr/local/bin/consul");
    }

    @Test
    public void updatePropertiesTest() throws Exception {
        PropertiesConfiguration.setInclude("IDONOTWANTINCLUDETOWORK");

        Map<String, Path> fileToPath = new HashMap<>();
        File testOutputProperties = new File("outputPropertyFiles/testOutput.properties");
        fileToPath.put(TEST_DEFAULT_PROPERTIES, testOutputProperties.toPath());
        assertTrue(consulKVPropertyController.writePropertyFiles(fileToPath));
        Thread.sleep(100);
        assertTrue(outputPropertiesFolder.exists());
        assertEquals(1,  outputPropertiesFolder.list().length);
        assertEquals("testOutput.properties", outputPropertiesFolder.list()[0]);
        long firstMod = testOutputProperties.lastModified();

        //Now change a key
        String newValue = "newValue";
        consulClient.putEntry(TEST_DEFAULT_PROPERTIES+"consul.execPath", newValue);
        Thread.sleep(1000);

        //Read properties
        PropertiesConfiguration propertiesConfiguration = getPropertiesConfiguration(testOutputProperties);
        propertiesConfiguration.load();
        String consulExecPath = propertiesConfiguration.getString("consul.execPath");
        assertEquals(newValue, consulExecPath);

        //Now change the key again
        consulClient.putEntry(TEST_DEFAULT_PROPERTIES+"consul.execPath", newValue+"1");
        Thread.sleep(1000);
        //should automatically change property file
        long lastMod = testOutputProperties.lastModified();
        assertNotEquals(firstMod, lastMod);
        //and propertiesConfiguration should automatically reload
        consulExecPath = propertiesConfiguration.getString("consul.execPath");
        assertEquals( newValue+"1", consulExecPath);
    }

    @Test
    public void interpolatePropertiesTest() throws Exception {
        //Goal: be able to utilize the include key to allow access to those properties
        PropertiesConfiguration.setInclude("IDONOTWANTINCLUDETOWORK");

        //Upload rest_api and mdn-common properties to consul
        Map<String, Path> uploadMap = new HashMap<>();
        uploadMap.put("rest_api/", new File("rest_api.properties").toPath());
        uploadMap.put("mdn-common/", new File("mdn-common.properties").toPath());
        consulKVPropertyController.uploadPropertyFiles(uploadMap);

        Thread.sleep(100);

        //Write these properties out
        Map<String, Path> downloadMap = new HashMap<>();
        File outputRestApiProps = new File(OUTPUT_PROPERTY_FOLDER_NAME+"/rest_api.properties");
        downloadMap.put("rest_api/", outputRestApiProps.toPath());
        File outputCommonProps = new File(OUTPUT_PROPERTY_FOLDER_NAME+"/mdn-common.properties");
        downloadMap.put("mdn-common/", outputCommonProps.toPath());
        assertTrue(consulKVPropertyController.writePropertyFiles(downloadMap));

        Thread.sleep(100);
        assertTrue(outputRestApiProps.exists());
        assertTrue(outputCommonProps.exists());

        //Build seperate configs for both
        PropertiesConfiguration restApiProps = getPropertiesConfiguration(outputRestApiProps);
        restApiProps.load();

        PropertiesConfiguration commonProps = getPropertiesConfiguration(outputCommonProps);
        commonProps.load();

        //Join them together using composite config
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(restApiProps);
        compositeConfiguration.addConfiguration(commonProps);
        String result = compositeConfiguration.getString("rest.server.http.ip");
        //expected string is an interpolated result.
        String expected = "mdn-restapi-manager-rest.oam.mdn.dns.tmp";
        assertEquals(result,expected);
    }

    private PropertiesConfiguration getPropertiesConfiguration(File outputCommonProps) throws ConfigurationException {
        FileChangedReloadingStrategy commonReloadingStrategy = new FileChangedReloadingStrategy();
        commonReloadingStrategy.setRefreshDelay(1L);
        PropertiesConfiguration commonProps = new PropertiesConfiguration(outputCommonProps.getAbsolutePath());
        commonProps.setReloadingStrategy(commonReloadingStrategy);
        return commonProps;
    }
}
