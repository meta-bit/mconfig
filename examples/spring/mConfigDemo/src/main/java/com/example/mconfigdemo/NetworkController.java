package com.example.mconfigdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NetworkController {

    private final ConnectorService service;

    public NetworkController(ConnectorService service) {
    this.service = service;
    }

    @GetMapping("/conn")
    public String conn() {
    return service.currentConnectionString();
    }
}