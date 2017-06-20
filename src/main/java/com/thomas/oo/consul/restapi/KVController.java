package com.thomas.oo.consul.restapi;

import com.google.gson.Gson;
import com.thomas.oo.consul.consul.ConsulClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
public class KVController {
    private ConsulClient consulClient;
    private final String locationHeader = "Location";
    @Autowired
    public KVController(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    @RequestMapping(value = "/kv/{key}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getValue(@PathVariable("key") String key){
        String value = consulClient.getValue(key);
        if(value.isEmpty()){
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error", "No value found for key "+key)), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(new Gson().toJson(Collections.singletonMap(key, value)), HttpStatus.OK);
    }

    @RequestMapping(value = "/kv/{key}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> putValue(@PathVariable("key") String key, @RequestBody String value){
        boolean success = consulClient.putValue(key, value);
        if(success){
            String location = String.format("/kv/%s",key);
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(locationHeader, location);
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Message", "Value of key "+key +" is "+value)), headers, HttpStatus.CREATED);
        }else{
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error", "Failed to put key")), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/kv/{key}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteValue(@PathVariable("key") String key){
        String value = consulClient.getValue(key);
        if(value.isEmpty()){
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error", "No value for key "+key)), HttpStatus.NOT_FOUND);
        }else{
            consulClient.deleteKey(key);
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Message", "Deleted key "+key)), HttpStatus.OK);
        }
    }

}
