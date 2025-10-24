package org.example.chatservice.dbpersistence.scylla.session;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import org.example.chatservice.dbpersistence.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ScyllaCluster {
    private static final Logger log = LoggerFactory.getLogger(ScyllaCluster.class.getSimpleName());
    private static Cluster scyllaCluster;

    private ScyllaCluster(){
        // private constructor to prevent instantiation
    }

    private static void initializeCluster(){
        if(scyllaCluster == null){
            try{
                Integer[] ports = Config.getIntArrayProperty("scylla.ports");
                String host = Config.getProperty("scylla.host");
                List<InetAddress> contactPoints = new ArrayList<>();
                for (int i = 0; i < ports.length; i++) {
                    contactPoints.add(new InetSocketAddress(host, ports[i]).getAddress());
                }

                scyllaCluster = Cluster.builder()
                        .addContactPoints(contactPoints)
                        .withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(Config.getProperty("scylla.datacenter")).build())
                        .withoutJMXReporting()
                        .build();
                log.info("ScyllaCluster initialized successfully");

            } catch (Exception e){
                log.error("Error initializing ScyllaCluster: {}", e.getMessage());
                System.exit(-1);
            }
        }
    }

    public static Session getSession(){
        if(scyllaCluster == null){
            initializeCluster();
        }
        scyllaCluster.connect().execute("CREATE KEYSPACE IF NOT EXISTS " +
                Config.getProperty("scylla.keyspace") +
                " WITH replication = {'class':'NetworkTopologyStrategy', 'DC1':1, 'DC2':1, 'DC3':1};");
        return scyllaCluster.connect(Config.getProperty("scylla.keyspace"));
    }
}
