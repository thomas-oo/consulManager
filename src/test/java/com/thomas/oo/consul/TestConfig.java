package com.thomas.oo.consul;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.thomas.oo.consul.consul.ConsulService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@TestPropertySource("classpath:testConfig.properties")
public class TestConfig {

    @Value("${consul.consulAddressAndPort}")String consulAddressAndHost;

    @Bean
    public Consul consul(ConsulService consulService){
        try {
            consulService.startProcess();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Connects consul object to a consul agent that is running at consulAddressAndHost
        Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromString(consulAddressAndHost)).build();
        return consul;
    }
}
