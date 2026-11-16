package com.example.starter;

import org.metabit.platform.support.config.*;

public class Main {
    public static void main(String[] args) {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("myco", "myapp").build()) {
            Configuration cfg = factory.getConfig("network");
            System.out.println("Peer: " + cfg.getString("peer"));
            System.out.println("Port: " + cfg.getInteger("port"));
            System.out.println("Probability: " + cfg.getDouble("probability"));
            System.out.println("Available configs: " + factory.listAvailableConfigurations().size() + " found");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}