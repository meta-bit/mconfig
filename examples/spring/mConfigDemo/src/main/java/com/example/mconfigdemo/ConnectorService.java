package com.example.mconfigdemo;

import org.springframework.stereotype.Service;

@Service
public class ConnectorService {

    private final NetworkConfigAdapter adapter;

    public ConnectorService(NetworkConfigAdapter adapter) {
    this.adapter = adapter;
    }

    public String currentConnectionString() {
    return String.format("%s:%d (tls=%b)",
            adapter.getHost(),
            adapter.getPort(),
            adapter.isTls());
    }
}