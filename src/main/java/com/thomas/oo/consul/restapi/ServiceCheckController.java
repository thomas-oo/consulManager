package com.thomas.oo.consul.restapi;

import com.google.gson.Gson;
import com.orbitz.consul.model.catalog.CatalogService;
import com.orbitz.consul.model.health.HealthCheck;
import com.thomas.oo.consul.DTO.CheckDTO;
import com.thomas.oo.consul.DTO.ServiceDTO;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ServiceCheckController {
    private ConsulClient consulClient;
    private final String locationHeader = "Location";
    @Autowired
    public ServiceCheckController(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    @RequestMapping(value = "/services/{serviceName}/{serviceId}/checks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getHealthChecks(@PathVariable("serviceName") String serviceName, @PathVariable("serviceId") String serviceId){
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setServiceName(serviceName); serviceDTO.setServiceId(serviceId);
        //check that service exists
        Optional<CatalogService> service = consulClient.queryForServiceByServiceId(serviceName, serviceId);
        if(!service.isPresent()){
            return new ResponseEntity(Collections.singletonMap("Error", "No service with serviceId "+serviceId),HttpStatus.NOT_FOUND);
        }
        List<HealthCheck> healthChecks = consulClient.getHealthChecks(serviceDTO);
        return new ResponseEntity(new Gson().toJson(healthChecks), HttpStatus.OK);
    }

    /**
     * Create a new health check to a specific service of service id. The supported checks are http, tcp, script, and ttl checks.
     * @param serviceName Name of the service
     * @param serviceId Service id
     * @param checkDTO The check to register.
     * @return The
     */
    @RequestMapping(value = "/services/{serviceName}/{serviceId}/checks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addHealthCheckToService(@PathVariable("serviceName") String serviceName, @PathVariable("serviceId") String serviceId, @RequestBody CheckDTO checkDTO){
        if(checkDTO.getInterval()==0){
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error", "interval field mandatory in body.")), HttpStatus.BAD_REQUEST);
        }
        checkDTO.setCheckId(UUID.randomUUID().toString());
        if(checkDTO instanceof CheckDTO.HTTPCheckDTO){
            CheckDTO.HTTPCheckDTO httpCheckDTO = (CheckDTO.HTTPCheckDTO) checkDTO;
            if(httpCheckDTO.getUrl().isEmpty()){
                return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error", "url field mandatory in body.")), HttpStatus.BAD_REQUEST);
            }
            consulClient.addNewHTTPCheck(serviceId, httpCheckDTO);
        }else if(checkDTO instanceof CheckDTO.ScriptCheckDTO){
            CheckDTO.ScriptCheckDTO scriptCheckDTO = (CheckDTO.ScriptCheckDTO) checkDTO;
            if(scriptCheckDTO.getScript().isEmpty()){
                return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error", "script field mandatory in body.")), HttpStatus.BAD_REQUEST);
            }
            consulClient.addNewScriptCheck(serviceId, scriptCheckDTO);
        }else if(checkDTO instanceof CheckDTO.TCPCheckDTO){
            CheckDTO.TCPCheckDTO tcpCheckDTO = (CheckDTO.TCPCheckDTO) checkDTO;
            if(tcpCheckDTO.getAddressAndPort().isEmpty()){
                return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error", "addressAndPort field mandatory in body.")), HttpStatus.BAD_REQUEST);
            }
            consulClient.addNewTCPCheck(serviceId, tcpCheckDTO);
        }else if(checkDTO instanceof CheckDTO.TTLCheckDTO){
            CheckDTO.TTLCheckDTO ttlCheckDTO = (CheckDTO.TTLCheckDTO) checkDTO;
            consulClient.addNewTTLCheck(serviceId, ttlCheckDTO);
        }else{
            return new ResponseEntity(new Gson().toJson(Collections.singletonMap("Error", "No health check of type found")), HttpStatus.BAD_REQUEST);
        }
        String location = String.format("/%s/%s/checks/%s",serviceName, serviceId, checkDTO.getCheckId());
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(locationHeader, location);
        return new ResponseEntity(Collections.singletonMap("Message", "Check added of type "+checkDTO.getClass().getSimpleName()), headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/services/{serviceName}/{serviceId}/checks/{checkId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getHealthCheck(@PathVariable("serviceName") String serviceName, @PathVariable("serviceId") String serviceId, @PathVariable("checkId") String checkId){
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setServiceName(serviceName); serviceDTO.setServiceId(serviceId);
        Optional<HealthCheck> healthCheck = consulClient.getHealthCheck(serviceDTO, checkId);
        if(!healthCheck.isPresent()){
            return new ResponseEntity(Collections.singletonMap("Error", "No check with checkId "+checkId + " found."), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(new Gson().toJson(healthCheck), HttpStatus.OK);
    }

    @RequestMapping(value = "/services/{serviceName}/{serviceId}/checks/{checkId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteCheck(@PathVariable("serviceName") String serviceName, @PathVariable("serviceId") String serviceId, @PathVariable("checkId") String checkId){
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setServiceName(serviceName);
        serviceDTO.setServiceId(serviceId);
        Optional<HealthCheck> healthCheck = consulClient.getHealthCheck(serviceDTO, checkId);
        if(healthCheck.isPresent()){
            consulClient.removeCheck(checkId);
            return new ResponseEntity(Collections.singletonMap("Message", "Removed check "+checkId),HttpStatus.OK);
        }else{
            return new ResponseEntity(Collections.singletonMap("Error", "No check with checkId "+checkId),HttpStatus.NOT_FOUND);
        }
    }
}
