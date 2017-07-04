package com.thomas.oo.consul.IntegrationTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.controllers.ConsulKVPropertyController;
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

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
public class ConsulKVPropertyControllerTest {

    public static final String TEST_DEFAULT_PROPERTIES = "test-default-properties/";
    @Autowired
    ConsulKVPropertyController consulKVPropertyController;

    @Before
    public void setUp() throws Exception {
        //Populate with some default properties
        Map<String, Path> kvFolderToPath = new HashMap<>();
        kvFolderToPath.put("test-default-properties/", new File("config.properties").toPath());
        consulKVPropertyController.uploadPropertyFiles(kvFolderToPath);
    }

    @After
    public void tearDown() throws Exception {
        consulKVPropertyController.deleteCreatedPropertyFiles();
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
        Thread.sleep(1000);
        assertTrue(consulKVPropertyController.checkIfAllFoldersExists(propertyFilesMap.keySet()));
    }

    @Test
    public void writePropertiesTest() throws Exception {
        File outputPropertyFilesFolder = new File("outputPropertyFiles");
        Map<String, Path> fileToPath = new HashMap<>();
        fileToPath.put(TEST_DEFAULT_PROPERTIES, new File("outputPropertyFiles/testOutput.properties").toPath());
        consulKVPropertyController.writePropertyFiles(fileToPath);
        Thread.sleep(100);
        assertTrue(outputPropertyFilesFolder.exists() && outputPropertyFilesFolder.list()[0].equals("testOutput.properties"));
    }
}
