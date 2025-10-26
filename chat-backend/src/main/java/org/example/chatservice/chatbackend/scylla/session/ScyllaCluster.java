package org.example.chatservice.chatbackend.scylla.session;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "scylla")
public class ScyllaCluster {
    private final Logger log = LoggerFactory.getLogger(ScyllaCluster.class.getSimpleName());
    private Cluster scyllaCluster;
    private String host;
    private String ports;
    private String datacenter;
    private String keyspace;
    private String availableDatacenters;

    public void setHost(String host) {
        this.host = host;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public void setAvailableDatacenters(String availableDatacenters) {
        this.availableDatacenters = availableDatacenters;
    }

    private void initializeCluster(){
        if(scyllaCluster == null){
            try{
                String[] portStrings = ports.split(",");
                List<InetAddress> contactPoints = new ArrayList<>();
                for (int i = 0; i < portStrings.length; i++) {
                    contactPoints.add(new InetSocketAddress(host, Integer.parseInt( portStrings[i])).getAddress());
                }

                scyllaCluster = Cluster.builder()
                        .addContactPoints(contactPoints)
                        .withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(datacenter).build())
                        .withoutJMXReporting()
                        .build();
                log.info("ScyllaCluster initialized successfully");

            } catch (Exception e){
                log.error("Error initializing ScyllaCluster: {}", e.getMessage());
                System.exit(-1);
            }
        }
    }

    public Session getSession(){
        if(scyllaCluster == null){
            initializeCluster();
        }

        String[] availableDCs = availableDatacenters.split(",");
        Map<String, Integer> dcReplication = new java.util.HashMap<>();
        for (String dc : availableDCs) {
            dcReplication.put(dc, 1);
        }
        String keySpaceQuery = SchemaBuilder.createKeyspace(keyspace)
                        .ifNotExists()
                        .withNetworkTopologyStrategy(dcReplication)
                        .withDurableWrites(true)
                        .build()
                        .getQuery();
        log.info("Ensuring keyspace exists with query: {}", keySpaceQuery);
        scyllaCluster.connect().execute(keySpaceQuery);
        return scyllaCluster.connect(keyspace);
    }
}
