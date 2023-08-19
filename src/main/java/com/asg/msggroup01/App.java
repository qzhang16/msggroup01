package com.asg.msggroup01;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

/**
 * Hello world!
 *
 */
public class App implements MessageListener {
    private static final CountDownLatch latch = new CountDownLatch(2);

    private final String name;
    private final Map<String, String> receivedMessages;

    public App(String name, Map<String, String> receivedMessages) {
        this.name = name;
        this.receivedMessages = receivedMessages;
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");

        try {
            InitialContext initContext = new InitialContext();
            Queue msggroupQ = (Queue) initContext.lookup("queue/msggroupQueue");

            Map<String, String> map = new ConcurrentHashMap<>();

            try (ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616", "admin",
                    "admin");
                    JMSContext jmsContext2 = cf.createContext();
                    JMSContext jmsContext = cf.createContext()) {

                JMSConsumer consumer1 = jmsContext2.createConsumer(msggroupQ);
                consumer1.setMessageListener(new App("consumer1", map));
                JMSConsumer consumer2 = jmsContext2.createConsumer(msggroupQ);
                consumer2.setMessageListener(new App("consumer2", map));


                

                JMSProducer producer = jmsContext.createProducer();
                Random rand01 = new Random(100);
                TextMessage msg = null;

                for (int i = 0; i < 10; i++) {
                    msg = jmsContext.createTextMessage("number " + rand01.nextInt(10));
                    msg.setStringProperty("JMSXGroupID", "group-" + rand01.nextInt(2));
                    msg.setIntProperty("myId", i);
                    producer.send(msggroupQ, msg);
                    System.out.println("Sending : " + msg.getText() + " with " + msg.getStringProperty("JMSXGroupID"));

                }

                msg = jmsContext.createTextMessage("number " + 11);
                    msg.setStringProperty("JMSXGroupID", "group-" + 0);
                    msg.setIntProperty("myId", 11);
                    producer.send(msggroupQ, msg);
                    System.out.println("Sending : " + msg.getText() + " with " + msg.getStringProperty("JMSXGroupID"));

                    msg = jmsContext.createTextMessage("number " + 12);
                    msg.setStringProperty("JMSXGroupID", "group-" + 1);
                    msg.setIntProperty("myId", 12);
                    producer.send(msggroupQ, msg);
                    System.out.println("Sending : " + msg.getText() + " with " + msg.getStringProperty("JMSXGroupID"));


                    latch.await();

                    map.forEach((k, v) -> System.out.println(k + " : " + v));
                
            } catch (JMSException | InterruptedException e) {
                e.printStackTrace();
            }

        } catch (NamingException e) {
            e.printStackTrace();

        }

    }

    @Override
    public void onMessage(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            System.out.println("Listener " + name + " received message : " + msg.getText());
            receivedMessages.put(msg.getText(), name);
            if ((msg.getIntProperty("myId") == 11 && msg.getStringProperty("JMSXGroupID").equals("group-0")) || 
                    (msg.getIntProperty("myId") == 12 && msg.getStringProperty("JMSXGroupID").equals("group-1"))) {
                latch.countDown();
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
        
    }
}
