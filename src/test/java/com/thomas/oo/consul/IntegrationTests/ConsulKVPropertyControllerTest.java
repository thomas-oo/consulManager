package com.thomas.oo.consul.IntegrationTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.controllers.ConsulKVPropertyController;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
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

//TODO: refactor
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
public class ConsulKVPropertyControllerTest {

    public static final String TEST_DEFAULT_PROPERTIES = "test-default-properties/";
    @Autowired
    ConsulKVPropertyController consulKVPropertyController;
    @Autowired
    ConsulClient consulClient;

    @Before
    public void setUp() throws Exception {
        //Populate consul KV with some default properties
        Map<String, Path> kvFolderToPath = new HashMap<>();
        kvFolderToPath.put(TEST_DEFAULT_PROPERTIES, new File("config.properties").toPath());
        consulKVPropertyController.uploadPropertyFiles(kvFolderToPath);
    }

    @After
    public void tearDown() throws Exception {
        consulKVPropertyController.stopWritingPropertyFiles();
        consulKVPropertyController.deleteCreatedPropertyFiles();
        Thread.sleep(100);
        File outputFolder = new File("outputPropertyFiles");
        assertTrue(outputFolder.exists());
        assertEquals(0, outputFolder.list().length);
    }

    @Test
    public void uploadPropertiesTest() throws Exception {
        Map<String, Path> propertyFilesMap = new HashMap<>();
        //TODO: don't hardcode
        Path propertyFilesFolder = Paths.get("/root/Documents/consulProto/propertyFiles/config");
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
        File outputPropertyFilesFolder = new File("outputPropertyFiles");
        Map<String, Path> fileToPath = new HashMap<>();
        fileToPath.put(TEST_DEFAULT_PROPERTIES, new File("outputPropertyFiles/testOutput.properties").toPath());
        assertTrue(consulKVPropertyController.writePropertyFiles(fileToPath));
        Thread.sleep(100);
        assertTrue(outputPropertyFilesFolder.exists());
        assertEquals(1,  outputPropertyFilesFolder.list().length);
        assertEquals("testOutput.properties", outputPropertyFilesFolder.list()[0]);
    }

    @Test
    public void readPropertiesTest() throws Exception {
        //Write properties
        File outputPropertyFilesFolder = new File("outputPropertyFiles");
        Map<String, Path> fileToPath = new HashMap<>();
        fileToPath.put(TEST_DEFAULT_PROPERTIES, new File("outputPropertyFiles/testOutput.properties").toPath());
        assertTrue(consulKVPropertyController.writePropertyFiles(fileToPath));
        Thread.sleep(100);
        assertTrue(outputPropertyFilesFolder.exists());
        assertEquals(1,  outputPropertyFilesFolder.list().length);
        assertEquals("testOutput.properties", outputPropertyFilesFolder.list()[0]);

        //Try to read these properties
        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
        // do not expand include, use composite configuration to interpolate
        PropertiesConfiguration.setInclude("IDONOTWANTINCLUDETOWORK");
        propertiesConfiguration.setFileName("outputPropertyFiles/testOutput.properties");
        propertiesConfiguration.load();
        String consulExecPath = propertiesConfiguration.getString("consul.execPath");
        assertEquals(consulExecPath, "/usr/local/bin/consul");
    }

    //FIXME: Auto reloading is reliant on refresh delay. May be time sensitive and thus, a bit flaky
    @Test
    public void updatePropertiesTest() throws Exception {
        File outputPropertyFilesFolder = new File("outputPropertyFiles");
        Map<String, Path> fileToPath = new HashMap<>();
        fileToPath.put(TEST_DEFAULT_PROPERTIES, new File("outputPropertyFiles/testOutput.properties").toPath());
        assertTrue(consulKVPropertyController.writePropertyFiles(fileToPath));
        Thread.sleep(100);
        assertTrue(outputPropertyFilesFolder.exists());
        assertEquals(1,  outputPropertyFilesFolder.list().length);
        assertEquals("testOutput.properties", outputPropertyFilesFolder.list()[0]);
        long firstMod = new File("/git/consulPrototype/consulLoadBalancing/outputPropertyFiles/testOutput.properties").lastModified();

        //Now change a key
        String newValue = "newValue";
        consulClient.putEntry(TEST_DEFAULT_PROPERTIES+"consul.execPath", newValue);
        Thread.sleep(500);

        //Read properties
        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration("/git/consulPrototype/consulLoadBalancing/outputPropertyFiles/testOutput.properties");
        PropertiesConfiguration.setInclude("IDONOTWANTINCLUDETOWORK");
        FileChangedReloadingStrategy reloadingStrategy = new FileChangedReloadingStrategy();
        reloadingStrategy.setRefreshDelay(500);
        propertiesConfiguration.setReloadingStrategy(reloadingStrategy);
        propertiesConfiguration.load();
        String consulExecPath = propertiesConfiguration.getString("consul.execPath");
        assertEquals(newValue, consulExecPath);

        //Now change the key again
        consulClient.putEntry(TEST_DEFAULT_PROPERTIES+"consul.execPath", newValue+"1");
        Thread.sleep(500);
        //should automatically change property file
        long lastMod = new File("/git/consulPrototype/consulLoadBalancing/outputPropertyFiles/testOutput.properties").lastModified();
        assertNotEquals(firstMod, lastMod);
        //and propertiesConfiguration should automatically reload
        consulExecPath = propertiesConfiguration.getString("consul.execPath");
        assertEquals( newValue+"1", consulExecPath);
    }

    @Test
    public void interpolatePropertiesTest() throws Exception {
        //Goal: be able to utilize the include key to allow access to those properties

        //Upload rest_api and mdn-common properties to consul
        Map<String, Path> uploadMap = new HashMap<>();
        uploadMap.put("rest_api/", new File("rest_api.properties").toPath());
        uploadMap.put("mdn-common/", new File("mdn-common.properties").toPath());
        consulKVPropertyController.uploadPropertyFiles(uploadMap);

        Thread.sleep(100);

        //Write these properties out
        Map<String, Path> downloadMap = new HashMap<>();
        File outputRestApiProps = new File("outputPropertyFiles/rest_api.properties");
        downloadMap.put("rest_api/", outputRestApiProps.toPath());
        File outputCommonProps = new File("outputPropertyFiles/mdn-common.properties");
        downloadMap.put("mdn-common/", outputCommonProps.toPath());
        assertTrue(consulKVPropertyController.writePropertyFiles(downloadMap));

        Thread.sleep(100);

        //Build seperate configs for both
        PropertiesConfiguration.setInclude("IDONOTWANTINCLUDETOWORK");
        FileChangedReloadingStrategy restApiReloadingStrategy = new FileChangedReloadingStrategy();
        restApiReloadingStrategy.setRefreshDelay(1L);
        PropertiesConfiguration restApiProps = new PropertiesConfiguration(outputRestApiProps.getAbsolutePath());
        restApiProps.setReloadingStrategy(restApiReloadingStrategy);
        restApiProps.load();

        FileChangedReloadingStrategy commonReloadingStrategy = new FileChangedReloadingStrategy();
        commonReloadingStrategy.setRefreshDelay(1L);
        PropertiesConfiguration commonProps = new PropertiesConfiguration(outputCommonProps.getAbsolutePath());
        commonProps.setReloadingStrategy(commonReloadingStrategy);
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
}
