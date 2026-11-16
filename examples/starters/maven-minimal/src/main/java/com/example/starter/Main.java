package com.example.starter;

import org.metabit.platform.support.config.*;

public class Main {
    public static void main(String[] args) {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("myco", "myapp").build()) // create and initialize
            {
            Configuration cfg = factory.getConfig("network"); // initialize access to a specific configuration (and its tree of sources)
            System.out.println("Peer: " + cfg.getString("peer")); // get and print a String
            System.out.println("Port: " + cfg.getInteger("port")); // get and print an Integer
            System.out.println("Probability: " + cfg.getDouble("probability")); // get and print a Double
            System.out.println("Available configs: " + factory.listAvailableConfigurations().size() + " found"); // list available configurations
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}