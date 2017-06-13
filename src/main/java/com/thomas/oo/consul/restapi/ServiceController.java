package com.thomas.oo.consul.restapi;

import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import com.thomas.oo.consul.consul.ConsulTemplateService;
import com.thomas.oo.consul.loadBalancer.HAProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Represents the service endpoint. Will be used for CRUD of services in consul.
 */
@RestController
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
    @RequestMapping("/hello")
    public String helloWorld(){
        return "Hello world!";
    }

    //Create service
    public void createService(){

    }
}
