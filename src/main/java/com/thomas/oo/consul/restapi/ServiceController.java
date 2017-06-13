package com.thomas.oo.consul.restapi;

import com.google.gson.Gson;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import com.thomas.oo.consul.consul.ConsulTemplateService;
import com.thomas.oo.consul.loadBalancer.HAProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Represents the service endpoint. Will be used for CRUD of services in consul.
 */
//TODO:Write Postman collection to test this endpoint
@RestController
@RequestMapping("/catalog/services/")
public class ServiceController {
    //Client
    private ConsulClient consulClient;
    //Services
    private ConsulService consulService;
    private ConsulTemplateService consulTemplateService;
    private HAProxyService haProxyService;

    @Autowired
    public ServiceController(ConsulClient consulClient, ConsulService consulService, ConsulTemplateService consulTemplateService, HAProxyService haProxyService){
        this.consulClient = consulClient;
        this.consulService = consulService;
        this.consulTemplateService = consulTemplateService;
        this.haProxyService = haProxyService;
    }
    //Hello world
    @RequestMapping("hello")
    public String helloWorld(){
        return "Hello world!";
    }

    //Get all services according to tags
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAllServices(@RequestParam(value = "tags", defaultValue = "") String...tags){
        String json = new Gson().toJson(consulClient.queryForAllServices(tags));
        return json;
    }

    @RequestMapping(value = "{service}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getServices(@PathVariable("service") String serviceName, @RequestParam(value = "tags", defaultValue = "") String... tags){
        String json = new Gson().toJson(consulClient.queryForService(serviceName, tags));
        return json;
    }
}
