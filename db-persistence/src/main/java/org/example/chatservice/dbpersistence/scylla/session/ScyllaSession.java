package org.example.chatservice.dbpersistence.scylla.session;

import com.datastax.driver.core.Session;


public class ScyllaSession {
    private static Session session;

    private ScyllaSession(){
        // private constructor to prevent instantiation
    }

    public static Session getSession(){
        if(session == null){;
            session = ScyllaCluster.getSession();
        }
        return session;
    }
}
