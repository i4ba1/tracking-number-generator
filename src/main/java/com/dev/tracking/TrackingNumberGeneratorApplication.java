package com.dev.tracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

@SpringBootApplication
@EnableReactiveMongoAuditing
public class TrackingNumberGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackingNumberGeneratorApplication.class, args);
    }

}
