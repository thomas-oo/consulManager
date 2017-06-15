package com.thomas.oo.consul.consul;

import com.google.common.collect.Sets;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.Check;
import com.orbitz.consul.model.agent.ImmutableCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.CatalogService;
import com.orbitz.consul.model.catalog.ImmutableCatalogService;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;
import com.thomas.oo.consul.DTO.ServiceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A wrapper client to provide main use cases of Consul.
 * A few notes: Agent vs Catalog. An agent is the local consul agent which holds the local and global state, there can be multiple such agents in a cluster
 * A Catalog is the global state across these agents. A register to an agent will first update the local state, then the global state.
 * A query to an agent will be only of the local state. Thus register to agent, query with catalog
 */
@Service
public class ConsulClient {
    private static final String CRITICAL = "critical";
    private final Consul consul;

    @Autowired
    public ConsulClient(Consul consul){
        this.consul = consul;
    }

    /**
     * Registers a local service at the desired port with no healthcheck, local being the address of the consul agent
     * A query for this service will fail by default, add a new healthcheck and pass it for the query to pass.
     *
     * @param serviceDTO - Local service to register
     * */
    public void registerLocalService(ServiceDTO serviceDTO) {
        String[] newTags = addANDTagsFrom(serviceDTO.getTags());
        AgentClient agentClient = consul.agentClient();
        Registration registration = ImmutableRegistration.builder().port(serviceDTO.getPort()).name(serviceDTO.getServiceName()).id(serviceDTO.getServiceId()).addTags(newTags).build();
        agentClient.register(registration);
        return;
    }

    /**
     * Registers a remote service at the desired address and port with no healthcheck.
     * A query for this service will fail by default, ad a new healthcheck and pass it for the query to pass.
     * @param serviceDTO Remote service to register
     */
    public void registerRemoteService(ServiceDTO serviceDTO){
        String[] newTags = addANDTagsFrom(serviceDTO.getTags());
        AgentClient agentClient = consul.agentClient();
        Registration registration = ImmutableRegistration.builder().address(serviceDTO.getAddress()).port(serviceDTO.getPort()).name(serviceDTO.getServiceName()).id(serviceDTO.getServiceId()).addTags(newTags).build();
        agentClient.register(registration);
        return;
    }

    //Todo:TEST
    public List<CatalogService> queryForAllServices(String... tags){
        CatalogClient catalogClient = consul.catalogClient();
        Arrays.sort(tags);
        String totalTag="";
        if(tags.length>=1){
            totalTag=tags[0];
        }
        if(tags.length>1){
            for(int i = 1; i<tags.length; i++){
                totalTag+="AND"+tags[i];
            }
        }
        final String tag = totalTag;
        List<String> serviceNames = new ArrayList<>();
        Map<String, List<String>> servicesMap = catalogClient.getServices().getResponse();
        //filter
        if(!tag.isEmpty()){
            servicesMap.entrySet().removeIf(e -> !e.getValue().contains(tag));
        }
        serviceNames.addAll(servicesMap.keySet());
        List<CatalogService> services = new ArrayList<>();
        for(String serviceName : serviceNames){
            services.addAll(queryForService(serviceName, tag));
        }
        return services;
    }

    /**
     * Queries for services by service name and some tags to filter on.
     *
     * @param serviceName Name of the service
     * @param tags Tags you want to filter the service on. The request services will include all the tags, there's no option for OR currently
     * @return
     */
    public List<CatalogService> queryForService(String serviceName, String... tags) {
        CatalogClient catalogClient = consul.catalogClient();
        String totalTag = createTotalTag(tags);
        ImmutableQueryOptions.Builder queryOptionsBuilder = ImmutableQueryOptions.builder();
        if(!totalTag.isEmpty()){
            queryOptionsBuilder.addTag(totalTag).build();
        }
        QueryOptions queryOptions = queryOptionsBuilder.build();
        List<CatalogService> catalogServices = catalogClient.getService(serviceName, queryOptions).getResponse();
        List<CatalogService> resultCatalogServices = removeANDTagsFrom(catalogServices);
        return resultCatalogServices;
    }

    //Todo:TEST
    public Optional<CatalogService> queryForServiceByServiceId(String serviceName, String serviceId){
        List<CatalogService> catalogServices = queryForService(serviceName);
        Optional<CatalogService> service = catalogServices.stream().filter(s -> s.getServiceId().equalsIgnoreCase(serviceId)).findFirst();
        return service;
    }

    //Todo:TEST
    public Optional<CatalogService> queryForServiceByAddressAndPort(String serviceName, String address, int port){
        List<CatalogService> catalogServices = queryForService(serviceName);
        Optional<CatalogService> service = catalogServices.stream().filter(s -> s.getAddress().equalsIgnoreCase(address)&&s.getServicePort()==port).findFirst();
        return service;
    }

    /**
     * Checks the health of the service that has been registered in Consul as per all healthchecks registered with the service
     *
     * @param serviceName Name of the service (may be shared across services)
     * @param serviceId          Id of the specific service (unique across service)
     * @return True if the service is healthy or warning, false if the service is critical
     */
    public boolean checkService(String serviceName, String serviceId) {
        HealthClient healthClient = consul.healthClient();
        List<HealthCheck> healthChecks = healthClient.getServiceChecks(serviceName).getResponse();
        //need to first check that healtcheck for serviceid exists
        boolean exists = healthChecks.stream().anyMatch(h -> h.getServiceId().get().equalsIgnoreCase(serviceId));
        if(!exists){return false;}
        HealthCheck healthCheckForServiceId = healthChecks.stream().filter(h -> h.getServiceId().get().equalsIgnoreCase(serviceId)).findFirst().get();
        return !healthCheckForServiceId.getStatus().equalsIgnoreCase(CRITICAL);
    }

    /**
     * Deregisters a local service with an id
     *
     * @param serviceId Id of the specific service (unique across service)
     */
    public void deregisterService(String serviceId) {
        AgentClient agentClient = consul.agentClient();
        agentClient.deregister(serviceId);
    }

    /**
     *
     * @param serviceId Id of the specific service (unique across service)
     * @param url URL to perform GET requests
     * @param interval Interval to perform check in seconds
     */
    public void addNewHTTPCheck(String serviceId, String url, int interval){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = interval+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(serviceId).http(url).interval(stringInterval).name("HTTP check for '"+serviceId+"'").id("service:"+serviceId);
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     *
     * @param serviceId Id of the specific service (unique across service)
     * @param script Local script to execute, can point to a script file
     * @param interval Interval to perform check in seconds
     */
    public void addNewScriptCheck(String serviceId, String script, int interval){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = interval+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(serviceId).script(script).interval(stringInterval).name("Script check for '"+serviceId+"'").id("service:"+serviceId);
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     *
     * @param serviceId Id of the specific service (unique across service)
     * @param addressAndPort Address and port of connection to TCP
     * @param interval Interval to perform check in seconds
     */
    public void addNewTCPCheck(String serviceId, String addressAndPort, int interval){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = interval+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(serviceId).tcp(addressAndPort).interval(stringInterval).name("TCP check for '"+serviceId+"'").id("service:"+serviceId);
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     * A kind of "dead man switch" check, it is up to the service to perform a PUT to the pass endpoint.
     * @param serviceId Id of the specific service (unique across service)
     * @param interval Interval to perform check in seconds
     */
    public void addNewTTLCheck(String serviceId, int interval){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = interval+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(serviceId).ttl(stringInterval).name("TTL check for '"+serviceId+"'").id("service:"+serviceId);
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    public void passTTLCheck(String serviceId) throws NotRegisteredException {
        AgentClient agentClient = consul.agentClient();
        agentClient.pass(serviceId);
    }

    public List<CatalogService> removeANDTagsFrom(List<CatalogService> services){
        List<CatalogService> resultServices = new ArrayList<>();
        Iterator<CatalogService> iterator = services.iterator();
        while(iterator.hasNext()){
            CatalogService catalogService = iterator.next();
            List<String> tags = catalogService.getServiceTags();
            List<String> resultTags = tags.stream().filter(tag -> !tag.contains("AND")).collect(Collectors.toList());
            CatalogService resultCatalogService = ImmutableCatalogService.copyOf(catalogService).withServiceTags(resultTags);
            resultServices.add(resultCatalogService);
        }
        return resultServices;
    }

    public String[] addANDTagsFrom(String... tags){
        Set<String> tagsSet = new HashSet<String>(Arrays.asList(tags));
        Set<Set<String>> powerSet = Sets.powerSet(tagsSet);
        //powerset is immutable, make it mutable
        Set<TreeSet<String>> newPowerSet = new HashSet<TreeSet<String>>();
        for(Set<String> set : powerSet){
            TreeSet<String> sortedSet = new TreeSet<String>(set);
            newPowerSet.add(sortedSet);
        }
        newPowerSet.remove(Collections.emptySet());
        String[] newTags = new String[newPowerSet.size()];
        int i = 0;
        for(TreeSet<String> tagsToCombine : newPowerSet){
            String newTag = tagsToCombine.first();
            tagsToCombine.remove(tagsToCombine.first());
            Iterator iterator = tagsToCombine.iterator();
            while(iterator.hasNext()){
                newTag+="AND"+iterator.next();
            }
            newTags[i] = newTag;
            i++;
        }
        return newTags;
    }

    private String createTotalTag(String[] tags) {
        Arrays.sort(tags);
        String totalTag="";
        if(tags.length>=1){
            totalTag=tags[0];
        }
        if(tags.length>1){
            for(int i = 1; i<tags.length; i++){
                totalTag+="AND"+tags[i];
            }
        }
        return totalTag;
    }

}

