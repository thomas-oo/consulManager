package com.thomas.oo.consul.consul;

import com.google.common.collect.Sets;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.StatusClient;
import com.orbitz.consul.model.agent.Check;
import com.orbitz.consul.model.agent.ImmutableCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.CatalogService;
import com.orbitz.consul.model.catalog.ImmutableCatalogService;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;
import com.thomas.oo.consul.DTO.CheckDTO;
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
    @org.springframework.beans.factory.annotation.Value("${consul.consulAddressAndPort}") private String consulAddressAndPort;

    @Autowired
    public ConsulClient(Consul consul,@org.springframework.beans.factory.annotation.Value("${consul.consulAddressAndPort}")String consulAddressAndPort){
        this.consul = consul;
        this.consulAddressAndPort = consulAddressAndPort;
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

    public List<CatalogService> queryForAllServices(String... tags){
        CatalogClient catalogClient = consul.catalogClient();
        String totalTag = createTotalTag(tags);
        List<String> serviceNames = new ArrayList<>();
        Map<String, List<String>> servicesMap = catalogClient.getServices().getResponse();
        //filter
        if(!totalTag.isEmpty()){
            servicesMap.entrySet().removeIf(e -> !e.getValue().contains(totalTag));
        }
        serviceNames.addAll(servicesMap.keySet());
        List<CatalogService> services = new ArrayList<>();
        for(String serviceName : serviceNames){
            services.addAll(queryForService(serviceName, totalTag));
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

    public Optional<CatalogService> queryForServiceByServiceId(String serviceName, String serviceId){
        List<CatalogService> catalogServices = queryForService(serviceName);
        Optional<CatalogService> service = catalogServices.stream().filter(s -> s.getServiceId().equalsIgnoreCase(serviceId)).findFirst();
        return service;
    }

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
     * @param httpCheckDTO
     */
    public void addNewHTTPCheck(String serviceId, CheckDTO.HTTPCheckDTO httpCheckDTO){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = httpCheckDTO.getInterval()+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(serviceId).http(httpCheckDTO.getUrl()).interval(stringInterval).name("HTTP check for '"+serviceId+"'").id(httpCheckDTO.getCheckId());
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     *
     * @param serviceId Id of the specific service (unique across service)
     * @param scriptCheckDTO
     */
    public void addNewScriptCheck(String serviceId, CheckDTO.ScriptCheckDTO scriptCheckDTO){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = scriptCheckDTO.getInterval()+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(serviceId).script(scriptCheckDTO.getScript()).interval(stringInterval).name("Script check for '"+serviceId+"'").id(scriptCheckDTO.getCheckId());
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     *
     * @param serviceId Id of the specific service (unique across service)
     * @param tcpCheckDTO
     */
    public void addNewTCPCheck(String serviceId, CheckDTO.TCPCheckDTO tcpCheckDTO){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = tcpCheckDTO.getInterval()+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(serviceId).tcp(tcpCheckDTO.getAddressAndPort()).interval(stringInterval).name("TCP check for '"+serviceId+"'").id(tcpCheckDTO.getCheckId());
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     * A kind of "dead man switch" check, it is up to the service to perform a PUT to the pass endpoint.
     * @param serviceId Id of the specific service (unique across service)
     * @param ttlCheckDTO
     */
    public void addNewTTLCheck(String serviceId, CheckDTO.TTLCheckDTO ttlCheckDTO){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = ttlCheckDTO.getInterval()+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(serviceId).ttl(stringInterval).name("TTL check for '"+serviceId+"'").id(ttlCheckDTO.getCheckId());
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    public void passTTLCheck(ServiceDTO serviceDTO) throws NotRegisteredException {

        AgentClient agentClient = consul.agentClient();
        //get checkId of service
        List<HealthCheck> healthChecks = getHealthChecks(serviceDTO);
        HealthCheck ttlCheck = healthChecks.stream().filter(h -> h.getName().contains("TTL")).findFirst().get();
        agentClient.passCheck(ttlCheck.getCheckId());
    }

    public void removeCheck(String checkId){
        AgentClient agentClient = consul.agentClient();
        agentClient.deregisterCheck(checkId);
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

    public String createTotalTag(String[] tags) {
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

    //get healthchecks for the specific service
    public List<HealthCheck> getHealthChecks(ServiceDTO serviceDTO){
        HealthClient healthClient = consul.healthClient();
        List<HealthCheck> healthChecks = healthClient.getServiceChecks(serviceDTO.getServiceName()).getResponse();
        healthChecks = healthChecks.stream().filter(h -> h.getServiceId().get().equalsIgnoreCase(serviceDTO.getServiceId())).collect(Collectors.toList());
        consul.destroy();
        return healthChecks;
    }

    public Optional<HealthCheck> getHealthCheck(ServiceDTO serviceDTO, String checkId){
        HealthClient healthClient = consul.healthClient();
        List<HealthCheck> healthChecks = healthClient.getServiceChecks(serviceDTO.getServiceName()).getResponse();
        Optional<HealthCheck> healthCheck = healthChecks.stream().filter(h -> h.getServiceId().get().equalsIgnoreCase(serviceDTO.getServiceId())&&h.getCheckId().equalsIgnoreCase(checkId)).findFirst();
        return healthCheck;
    }

    public String getValue(String key){
        KeyValueClient keyValueClient = consul.keyValueClient();
        com.google.common.base.Optional<Value> value = keyValueClient.getValue(key);
        if(!value.isPresent()){
            return "";
        }else{
            return value.get().getValueAsString().get();
        }
    }

    public boolean keyExists(String key){
        KeyValueClient keyValueClient = consul.keyValueClient();
        List<String> allKeys = keyValueClient.getKeys("");
        boolean keyExists = allKeys.contains(key);
        return keyExists;
    }

    public boolean folderExists(String folder){
        if(!folder.endsWith("/")){
            folder += "/";
        }
        KeyValueClient keyValueClient = consul.keyValueClient();
        List<String> allKeys = keyValueClient.getKeys("");
        boolean folderExists = allKeys.contains(folder);
        return folderExists;
    }

    /**
     * Checks if keys exist in consul
     * @param keys Set of names of keys. Must be the full path of the key
     * @return True if all keys exist in consul
     */
    public boolean allKeysExist(Set<String> keys){
        KeyValueClient keyValueClient = consul.keyValueClient();
        List<String> allKeys = keyValueClient.getKeys("");
        return allKeys.containsAll(keys);
    }

    /**
     * Checks if folders exist in consul
     * @param folders Set of names of folders to check for
     * @return True if all folders exist in consul
     */
    public boolean allFoldersExist(Set<String> folders){
        KeyValueClient keyValueClient = consul.keyValueClient();
        List<String> allKeys = keyValueClient.getKeys("");
        for(String folder : folders){
            boolean folderExists = allKeys.stream().anyMatch(f -> f.startsWith(folder));
            if(!folderExists){
                return false;
            }
        }
        return true;
    }

    public boolean putEntry(String key, String value){
        KeyValueClient keyValueClient = consul.keyValueClient();
        return keyValueClient.putValue(key, value);
    }

    public boolean putEntryInFolder(String folder, String key, String value){
        if(!folder.endsWith("/")){
            folder+="/";
        }
        return putEntry(folder+key, value);
    }

    //Creates a folder
    public boolean putFolder(String folder){
        if(!folder.endsWith("/")){
            folder += "/";
        }
        KeyValueClient keyValueClient = consul.keyValueClient();
        return keyValueClient.putValue(folder);
    }

    //Deletes the exact matching key. Not recursive
    public void deleteKey(String key){
        KeyValueClient keyValueClient = consul.keyValueClient();
        keyValueClient.deleteKey(key);
    }

    public boolean putEntries(Map<Object, Object> entries, String destFolder) {
        String path = destFolder;
        if(!path.endsWith("/")){ //path should be a folder, thus end with /
            path+="/";
        }
        boolean createdFolder = putFolder(destFolder);
        if(!createdFolder){
            return false;
        }
        for(Map.Entry<Object, Object> entry: entries.entrySet()){
            String key = (String) entry.getKey();
            String value = String.valueOf(entry.getValue());
            boolean success = putEntry(path+key, value);
            if(!success){
                return false;
            }
        }
        return true;
    }

    public String getConsulAddressAndPort() {
        return consulAddressAndPort;
    }

    //Todo:test
    public String getClusterLeader(){
        StatusClient statusClient = consul.statusClient();
        return statusClient.getLeader();
    }
}

