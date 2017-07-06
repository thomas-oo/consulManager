package com.thomas.oo.consul.util;

import com.thomas.oo.consul.consul.ConsulTemplateService;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@Service
public class PropertyFilesUtil {

    ConsulTemplateService consulTemplateService;
    final String config = "template = { source = \"%s\" destination = \"%s\"} \n";

    String targetConfFile;
    Set<String> addedLines = new HashSet<>();
    Set<String> createdPropertyFiles = new HashSet<>();

    @Autowired
    public PropertyFilesUtil(ConsulTemplateService consulTemplateService) {
        this.consulTemplateService = consulTemplateService;
        targetConfFile = consulTemplateService.getConfFilePath();
    }

    /**
     * Parses a .properties file into a map
     * include key will show up as a key in the returned map, thus no inclusion or interpolation done here
     * @param propertiesFile
     * @return A map representing the KV entries in the properties file
     * @throws ConfigurationException
     */
    public Map<Object, Object> parsePropertiesFile(String propertiesFile) throws ConfigurationException {
        PropertiesConfiguration.setInclude("IDONOTWANTINCLUDETOWORK");
        PropertiesConfiguration configuration = new PropertiesConfiguration(propertiesFile);
        Map<Object, Object> configMap = new ConfigurationMap(configuration);
        return configMap;
    }

    /**
     *
     * Creates a .property file at the desired location by reading a KV folder in consul
     * @param destinationFile Desired full property file location and name, must end with .properties
     * @param consulKVFolder The desired consul KV folder to load
     * @throws Exception
     */
    public void createPropertiesFile(String destinationFile, String consulKVFolder) throws IOException {
        if(!destinationFile.endsWith(".properties")){
            throw new IllegalArgumentException(String.format("Destination file %s should end with .properties.", destinationFile));
        }
        Path templatePath = createTemplate(consulKVFolder);
        Path confFilePath = Paths.get(targetConfFile);
        String finalConfig = String.format(config, templatePath, destinationFile);
        addedLines.add(finalConfig.replace("\n", ""));
        Files.write(confFilePath, finalConfig.getBytes(), StandardOpenOption.APPEND);
        consulTemplateService.reloadConfig();
        createdPropertyFiles.add(destinationFile);
    }

    /**
     * Create a new template to list key value pairs for the desired consul KV folder
     * @param consulKVFolder The desired consul KV folder to load
     * @return The path of the new template, a temp file that will delete on exit of the JVM.
     * @throws IOException
     */
    public Path createTemplate(String consulKVFolder) throws IOException {
        String file =  getClass().getClassLoader().getResource("propertiesTemplate.ctmpl").getFile();
        byte[] bytes = Files.readAllBytes(Paths.get(file));
        String template = new String(bytes, Charset.defaultCharset());
        String newTemplate = template.replace("FOLDERNAME", consulKVFolder);
        //now write newTemplate somewhere
        Path tempTemplatePath = Files.createTempFile(null,".ctmpl");
        //make sure the file deletes when the jvm exits
        tempTemplatePath.toFile().deleteOnExit();
        Files.write(tempTemplatePath, newTemplate.getBytes(), StandardOpenOption.CREATE);
        return tempTemplatePath;
    }

    /**
     * Cleans up the original conf file used by consul template service. Deletes the added additional template lines.
     * Should be called in a shutdown hook or test cleanup method
     * @throws IOException
     */
    @PreDestroy
    public void cleanUpConfFile() throws IOException {
        Path confFilePath = Paths.get(targetConfFile);
        List<String> lines = Files.readAllLines(confFilePath);
        List<String> updatedLines = lines.stream().filter(s -> !addedLines.contains(s)).collect(Collectors.toList());
        Files.write(confFilePath, updatedLines);
        addedLines.removeAll(addedLines);
        consulTemplateService.reloadConfig();
    }

    /**
     * Deletes created property files.
     */
    public void deleteCreatedPropertyFiles() {
        for(String createdPropertyFile : createdPropertyFiles){
            Path createdPropertyFilePath = Paths.get(createdPropertyFile);
            createdPropertyFilePath.toFile().delete();
        }
        createdPropertyFiles.removeAll(createdPropertyFiles);
    }
}
