package com.lavyoung.gateweave.engine.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the GateWeave data-plane process. */
@SpringBootApplication(scanBasePackages = "com.lavyoung.gateweave.engine")
public class GateweaveEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(GateweaveEngineApplication.class, args);
    }
}
