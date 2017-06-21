package com.thomas.oo.consul.util;

import com.thomas.oo.consul.consul.ConsulTemplateService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Service
public class PropertiesUtil {

    ConsulTemplateService consulTemplateService;

    String config = "template = { source = \"%s\" destination = \"%s\"} \n";

    @Autowired
    public PropertiesUtil(ConsulTemplateService consulTemplateService) {
        this.consulTemplateService = consulTemplateService;
    }

    //Parses a .properties file into a map
    public Map<Object, Object> parsePropertiesFile(String propertiesFilePath) throws ConfigurationException {
        Configuration configuration = new PropertiesConfiguration(propertiesFilePath);
        Map<Object, Object> configMap = new ConfigurationMap(configuration);
        return configMap;
    }

    public void createPropertiesFile(String destFilePath, String propertiesName) throws Exception {
        Path templatePath = createTemplate(propertiesName);
        Path confFilePath = Paths.get(consulTemplateService.getConfFilePath());
        Files.write(confFilePath, String.format(config, templatePath, destFilePath).getBytes(), StandardOpenOption.APPEND);
        consulTemplateService.reloadConfig();
    }

    public Path createTemplate(String name) throws IOException {
        String file =  getClass().getClassLoader().getResource("propertiesTemplate.ctmpl").getFile();
        byte[] bytes = Files.readAllBytes(Paths.get(file));
        String template = new String(bytes, Charset.defaultCharset());
        String newTemplate = template.replace("FOLDERNAME", name);
        //now write newTemplate somewhere
        Path tempTemplatePath = Files.createTempFile(null,".ctmpl");
        Files.write(tempTemplatePath, newTemplate.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        return tempTemplatePath;
    }

}
