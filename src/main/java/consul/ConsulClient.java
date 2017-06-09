package consul;

import com.google.common.collect.Sets;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.Check;
import com.orbitz.consul.model.agent.ImmutableCheck;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.CatalogService;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ConsulClient {
    private static final String CRITICAL = "critical";
    private static Consul consul = Consul.builder().build();


    /**
     * Registers a local service at the desired port with no healthcheck
     * A query for this service will fail by default, add a new healthcheck and pass it for the query to pass.
     *
     * @param port                Local port the service exists at
     * @param serviceName         The name of the service
     * @param id                  The id of the node
     * @param tags                The tags associated with the node. These are single tags (ie they do not contain tag1ANDtag2)
     */
    public static void registerLocalService(int port, String serviceName, String id, Set<String> tags) {
        Set<Set<String>> powerSet = Sets.powerSet(tags);
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
        AgentClient agentClient = consul.agentClient();
        agentClient.register(port, (Registration.RegCheck) null, serviceName, id, newTags);
        return;
    }

    /**
     * Checks the health of the local service that has been registered in Consul as per all healthchecks registered with the service
     *
     * @param serviceName Name of the service (may be shared across services)
     * @param id          Id of the specific service (unique across service)
     * @return True if the service is healthy or warning, false if the service is critical
     */
    public static boolean checkLocalService(String serviceName, String id) {
        HealthClient healthClient = consul.healthClient();
        List<HealthCheck> healthChecks = healthClient.getServiceChecks(serviceName).getResponse();
        for (HealthCheck healthCheck : healthChecks) {
            if (healthCheck.getServiceId().get().equalsIgnoreCase(id)) {
                //found the specific service node with id
                return !(healthCheck.getStatus().equalsIgnoreCase(CRITICAL));
            }
        }
        //didn't find the specific service node with id
        return false;
    }

    /**
     * Deregisters a local service with an id
     *
     * @param id Id of the specific service (unique across service)
     */
    public static void deregisterLocalService(String id) {
        AgentClient agentClient = consul.agentClient();
        agentClient.deregister(id);
    }

    /**
     * Queries for services by service name and some tags to filter on.
     *
     * @param serviceName Name of the service
     * @param tags Tags you want to filter the service on. The request services will include all the tags, there's no option for OR currently
     * @return
     */
    public static List<CatalogService> queryForService(String serviceName, String... tags) {
        //Multiple tags is actually broken in consul
        //Because of this, tags are designed internally to be AND'ed together and passed as a single tag
        //As such, registering tags also must be AND'ed together in every combination (power set)
        CatalogClient catalogClient = consul.catalogClient();
        if(tags.length==0){
            List<CatalogService> catalogServices = catalogClient.getService(serviceName).getResponse();
            return catalogServices;
        }

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
        QueryOptions queryOptions = ImmutableQueryOptions.builder().addTag(totalTag).build();
        List<CatalogService> catalogServices = catalogClient.getService(serviceName, queryOptions).getResponse();
        return catalogServices;
    }

    /**
     *
     * @param id Id of the specific service (unique across service)
     * @param url URL to perform GET requests
     * @param interval Interval to perform check in seconds
     */
    public static void addNewHTTPCheck(String id, String url, int interval){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = interval+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(id).http(url).interval(stringInterval).name("HTTP check for '"+id+"'").id("service:"+id);
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     *
     * @param id Id of the specific service (unique across service)
     * @param script Local script to execute, can point to a script file
     * @param interval Interval to perform check in seconds
     */
    public static void addNewScriptCheck(String id, String script, int interval){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = interval+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(id).script(script).interval(stringInterval).name("Script check for '"+id+"'").id("service:"+id);
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     *
     * @param id Id of the specific service (unique across service)
     * @param addressAndPort Address and port of connection to TCP
     * @param interval Interval to perform check in seconds
     */
    public static void addNewTCPCheck(String id, String addressAndPort, int interval){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = interval+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(id).tcp(addressAndPort).interval(stringInterval).name("TCP check for '"+id+"'").id("service:"+id);
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    /**
     * A kind of "dead man switch" check, it is up to the service to perform a PUT to the pass endpoint.
     * @param id Id of the specific service (unique across service)
     * @param interval Interval to perform check in seconds
     */
    public static void addNewTTLCheck(String id, int interval){
        AgentClient agentClient = consul.agentClient();
        String stringInterval = interval+"s";
        ImmutableCheck.Builder immutableCheckBuilder= ImmutableCheck.builder().serviceId(id).ttl(stringInterval).name("TTL check for '"+id+"'").id("service:"+id);
        Check check = immutableCheckBuilder.build();
        agentClient.registerCheck(check);
    }

    public static void passTTLCheck(String id) throws NotRegisteredException {
        AgentClient agentClient = consul.agentClient();
        agentClient.pass(id);
    }
}

