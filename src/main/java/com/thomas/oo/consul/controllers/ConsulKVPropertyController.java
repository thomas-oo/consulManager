package com.thomas.oo.consul.controllers;

import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.util.PropertyFilesUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
public class ConsulKVPropertyController {
    @Autowired
    ConsulClient consulClient;
    @Autowired
    PropertyFilesUtil propertyFilesUtil;

    Set<Path> createdPropertyFiles = new HashSet<>();

    public ConsulKVPropertyController(ConsulClient consulClient, PropertyFilesUtil propertyFilesUtil) {
        this.consulClient = consulClient;
        this.propertyFilesUtil = propertyFilesUtil;
    }

    /**
     * Bulk upload of prop files
     * @param kvFolderToPath Key - KV folder name to write to. Value - Properties path to read from
     */
    public boolean uploadPropertyFiles(Map<String, Path> kvFolderToPath) throws ConfigurationException {
        //Validate kvFolderToPath
        //make sure Paths all point to prop files
        if(!validatePropertyFileMap(kvFolderToPath)){
            return false;
        }
        for(Map.Entry<String, Path> entry : kvFolderToPath.entrySet()){
            Map<Object, Object> propertiesMap = propertyFilesUtil.parsePropertiesFile(entry.getValue().toString());
            boolean success = consulClient.putEntries(propertiesMap, entry.getKey());
            if(!success){
                return false;
            }
        }
        return true;
    }

    private boolean validatePropertyFileMap(Map<String, Path> kvFolderToPath) {
        for(Map.Entry<String, Path> entry : kvFolderToPath.entrySet()){
            if(!entry.getValue().toFile().getName().toLowerCase().endsWith(".properties")){
                return false;
            }
            if(!entry.getKey().endsWith("/")){
                return false;
            }
        }
        return true;
    }

    /**
     * Writes a .properties to the desired paths using KV values from a desired KV folder
     * @param kvFolderToPath Key - KV folder name to read from. Value - Properties path to write to
     * @return success
     */
    public boolean writePropertyFiles(Map<String, Path> kvFolderToPath) {
        if(!validatePropertyFileMap(kvFolderToPath)){
            return false;
        }
        for(Map.Entry<String, Path> entry : kvFolderToPath.entrySet()){
            try {
                propertyFilesUtil.createPropertiesFile(entry.getValue().toString(), entry.getKey());
                createdPropertyFiles.add(entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    //Todo: perhaps stop the property files from being written by consul-template before hand
    public void deleteCreatedPropertyFiles() {
        for(Path createdPropertyFile : createdPropertyFiles){
            createdPropertyFile.toFile().delete();
        }
        createdPropertyFiles.removeAll(createdPropertyFiles);
    }

    public void stopWritingPropertyFiles(){
        try {
            propertyFilesUtil.cleanUpConfFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkIfAllKeysExists(Set<String> keys) {
        return consulClient.allKeysExist(keys);
    }

    public boolean checkIfAllFoldersExists(Set<String> folders) {
        return consulClient.allFoldersExist(folders);
    }
}
