package com.thomas.oo.consul.restapi;

import com.google.gson.Gson;
import com.orbitz.consul.model.catalog.CatalogService;
import com.thomas.oo.consul.DTO.ServiceDTO;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import com.thomas.oo.consul.consul.ConsulTemplateService;
import com.thomas.oo.consul.loadBalancer.HAProxyService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the service endpoint. Will be used for CRUD of services in consul.
 */
//TODO:Write Postman collection to test this endpoint
@RestController
@RequestMapping("/services/")
public class ServiceController {
    //Client
    private ConsulClient consulClient;
    //Services
    private ConsulService consulService;
    private ConsulTemplateService consulTemplateService;
    private HAProxyService haProxyService;
    //HAProxy address&port
    //TODO:read these from config
    private String haProxyAddress = "http://localhost:8000";
    private final String serviceNameHeader = "X-service-name";
    private final String tagNameHeader = "X-tag-name";
    private final String forwardedHostHeader = "X-Forwarded-Host";
    private final String localAddress = "127.0.0.1";
    private final String localHost = "localhost";
    private final String locationHeader = "Location";

    @Autowired
    public ServiceController(ConsulClient consulClient, ConsulService consulService, ConsulTemplateService consulTemplateService, HAProxyService haProxyService){
        this.consulClient = consulClient;
        this.consulService = consulService;
        this.consulTemplateService = consulTemplateService;
        this.haProxyService = haProxyService;
        startServices();
    }

    //TODO:add shutdown hooks to stop these services gracefully
    private void startServices() {
        try {
            consulService.startProcess();
            consulTemplateService.startProcess();
            haProxyService.startProcess();
        } catch (Exception e) {
            System.out.println("Services not started.");
            e.printStackTrace();
        }
    }

    //Hello world
    @RequestMapping("hello")
    public String helloWorld(){
        return "Hello world!";
    }

    /**
     * Get all services
     * @param tags Tags to filter on
     * @return Services matching query
     */
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllServices(@RequestParam(value = "tags", defaultValue = "") String...tags){
        String json = new Gson().toJson(consulClient.queryForAllServices(tags));
        return new ResponseEntity(json, HttpStatus.OK);
    }

    /**
     * Get one service, load-balanced according to serviceName and tags
     * Service has to not only be passing on consul but also reachable as HAProxy attempts to connect to it
     * @param serviceName Service name
     * @param tags Tags to filter on
     * @return One service that is reachable. Service returned may change due to load balancing
     */
    @RequestMapping(value = "{serviceName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getServices(@PathVariable("serviceName") String serviceName, @RequestParam(value = "tags", defaultValue = "") String... tags){
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(haProxyAddress);
        request.addHeader(serviceNameHeader, serviceName);
        Optional<CatalogService> service = Optional.empty();
        try {
            HttpResponse httpResponse = httpClient.execute(request);
            //Can be 200,404, etc TODO:check if X-Forwarded-Host header still returned if backend unreachable
            if(httpResponse.getStatusLine().getStatusCode()>=500 && httpResponse.getStatusLine().getStatusCode()<600){
                //backend cannot be reached
                return new ResponseEntity<String>(new Gson().toJson(Collections.singletonMap("Error: ", "No service found")), HttpStatus.NOT_FOUND);
            }
            String routedServerAddressAndPort = httpResponse.getFirstHeader(forwardedHostHeader).getValue();
            EntityUtils.consume(httpResponse.getEntity());
            String[] split = routedServerAddressAndPort.split(":");
            String routedServerAddress = split[0];
            String routedServerPort = split[1];
            service = consulClient.queryForServiceByAddressAndPort(serviceName, routedServerAddress, Integer.parseInt(routedServerPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(service.isPresent()){
            return new ResponseEntity(new Gson().toJson(service.get()), HttpStatus.OK);
        }else{
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error: ", "No service found")), HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "{serviceName}/id/{serviceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getService(@PathVariable("serviceName") String serviceName, @PathVariable("serviceId") String serviceId){
        Optional<CatalogService> service = consulClient.queryForServiceByServiceId(serviceName, serviceId);
        if(!service.isPresent()){
            return new ResponseEntity("Error: Service not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(new Gson().toJson(service.get()), HttpStatus.OK);
    }

    @RequestMapping(value = "{serviceName}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createService(@PathVariable("serviceName") String serviceName, @RequestBody ServiceDTO serviceDTO){
        serviceDTO.setServiceName(serviceName);
        if(serviceDTO.getAddress().isEmpty()){
            //mandatory, must explicitly state address even if local
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error: ", "address field mandatory in body.")), HttpStatus.BAD_REQUEST);
        }
        if(serviceDTO.getPort()==0){
            //mandatory, must explicitly state port
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error: ", "port field mandatory in body.")), HttpStatus.BAD_REQUEST);
        }
        if(serviceDTO.getServiceId().isEmpty()){
            serviceDTO.setServiceId(UUID.randomUUID().toString());
        }

        if(serviceDTO.getAddress().equalsIgnoreCase(localAddress) || serviceDTO.getAddress().equalsIgnoreCase(localHost)){
            consulClient.registerLocalService(serviceDTO);
        }else{
            consulClient.registerRemoteService(serviceDTO);
        }
        Optional<CatalogService> service = consulClient.queryForServiceByServiceId(serviceDTO.getServiceName(), serviceDTO.getServiceId());
        if(!service.isPresent()){
            return new ResponseEntity("Error: Failed to create service", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        CatalogService createdService = service.get();
        String location = String.format("/%s/id/%s/",createdService.getServiceName(), createdService.getServiceId());
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(locationHeader, location);
        return new ResponseEntity(new Gson().toJson(createdService), headers, HttpStatus.CREATED);
    }
}
