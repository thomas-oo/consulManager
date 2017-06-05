package consulClient;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.CatalogService;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;

import java.util.List;

public class ConsulClient {
    private static final String CRITICAL = "critical";
    private static Consul consul = Consul.builder().build();


    /**
     * Registers a local service at the desired port
     * @param port Local port the service exists at
     * @param healthCheckInterval Interval of tcp health check
     * @param serviceName The name of the service
     * @param id The id of the node
     * @param tags The tags associated with the node
     */
    public static void registerLocalService(int port, int healthCheckInterval, String serviceName, String id, String ... tags){
        AgentClient agentClient = consul.agentClient();
        agentClient.register(port, Registration.RegCheck.tcp("localhost:"+port, healthCheckInterval), serviceName, id, tags);
        return;
    }

    /**
     *
     * @param id
     * @return
     */
    public static boolean checkLocalService(String serviceName, String id){
        HealthClient healthClient = consul.healthClient();
        List<HealthCheck> healthChecks = healthClient.getServiceChecks(serviceName).getResponse();
        for (HealthCheck healthCheck : healthChecks){
            if(healthCheck.getServiceId().get().equalsIgnoreCase(id)){
                //found the specific service node with id
                return !(healthCheck.getStatus().equalsIgnoreCase(CRITICAL));
            }
        }
        //didn't find the specific service node with id
        return false;
    }

    public static void deregisterLocalService(String id){
        AgentClient agentClient = consul.agentClient();
        agentClient.deregister(id);
    }

    //Multiple tags is actually broken in consul, the first tag is the only one that the request gets filtered on. Investigate prepared queries instead
    public static List<CatalogService> queryForService(String service, String ... tags){
        CatalogClient catalogClient = consul.catalogClient();
        QueryOptions queryOptions = ImmutableQueryOptions.builder().addTag(tags).build();
        List<CatalogService> catalogServices = catalogClient.getService(service, queryOptions).getResponse();
        return catalogServices;
    }

}
