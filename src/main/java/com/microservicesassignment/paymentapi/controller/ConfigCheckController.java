package com.microservicesassignment.paymentapi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ConfigCheckController {

    private final String applicationName;
    private final String configMessage;
    private final String configMode;
    private final String serverPort;
    private final Environment environment;

    public ConfigCheckController(@Value("${spring.application.name:payment-api}") String applicationName,
                                 @Value("${config.check.message:Payment API configuration active}") String configMessage,
                                 @Value("${config.check.mode:local}") String configMode,
                                 @Value("${server.port:8082}") String serverPort,
                                 Environment environment) {
        this.applicationName = applicationName;
        this.configMessage = configMessage;
        this.configMode = configMode;
        this.serverPort = serverPort;
        this.environment = environment;
    }

    @GetMapping("/configcheck")
    public Map<String, Object> configCheck() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", applicationName);
        response.put("message", configMessage);
        response.put("mode", configMode);
        response.put("serverPort", serverPort);
        response.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        return response;
    }
}
