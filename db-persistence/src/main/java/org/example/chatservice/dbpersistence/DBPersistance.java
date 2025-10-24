package org.example.chatservice.dbpersistence;

import org.example.chatservice.dbpersistence.kafka.MessageWorker;

import java.util.concurrent.CountDownLatch;

public class DBPersistance {
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1);

        MessageWorker messageWorker = new MessageWorker(0, 15);
        new Thread(messageWorker).start();

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                messageWorker.shutdown(latch);
            }
        });
    }
}
