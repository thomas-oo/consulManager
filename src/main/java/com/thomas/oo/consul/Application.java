package com.thomas.oo.consul;

import com.orbitz.consul.Consul;
import com.thomas.oo.consul.consul.ConsulService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:config.properties")
public class Application {
    public static void main(String[] args){
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public Consul consul(ConsulService consulService){
        try {
            consulService.startProcess();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Consul.builder().build();
    }
}
