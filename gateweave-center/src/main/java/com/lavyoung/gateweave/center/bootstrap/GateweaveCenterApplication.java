package com.lavyoung.gateweave.center.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the GateWeave control-plane process. */
@SpringBootApplication(scanBasePackages = "com.lavyoung.gateweave.center")
public class GateweaveCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(GateweaveCenterApplication.class, args);
    }
}
