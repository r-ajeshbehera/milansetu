package com.rajesh.milansetu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MilanSetuApplication {
    public static void main(String[] args) {
        SpringApplication.run(MilanSetuApplication.class, args);
    }
}
