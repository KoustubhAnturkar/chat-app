package org.example.chatservice.chatbackend.scylla.session;

import com.datastax.driver.core.Session;
import org.springframework.stereotype.Component;


@Component
public class ScyllaSession {
    private final Session session;

    public ScyllaSession(ScyllaCluster scyllaCluster){
        session = scyllaCluster.getSession();
    }

    public Session getSession(){
        return session;
    }
}
