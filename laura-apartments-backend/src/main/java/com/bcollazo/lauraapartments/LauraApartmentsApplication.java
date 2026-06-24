package com.bcollazo.lauraapartments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Sin esto el @Scheduled del echoTest no arranca nunca (Spring lo ignora calladito).
@SpringBootApplication
@EnableScheduling
public class LauraApartmentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LauraApartmentsApplication.class, args);
    }

}