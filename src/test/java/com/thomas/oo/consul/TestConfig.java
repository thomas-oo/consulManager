package com.thomas.oo.consul;

import com.orbitz.consul.Consul;
import com.thomas.oo.consul.consul.ConsulService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@PropertySource("classpath:config.properties")
public class TestConfig {

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
