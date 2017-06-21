package com.thomas.oo.consul;

import com.orbitz.consul.Consul;
import com.thomas.oo.consul.consul.ConsulClient;
import com.thomas.oo.consul.consul.ConsulService;
import com.thomas.oo.consul.consul.ConsulTemplateService;
import com.thomas.oo.consul.util.PropertiesUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class PropertiesUtilTest {
    ConsulClient consulClient;

    static ConsulService consulService;
    static String consulPath = "/usr/local/bin/consul";
    static String consulConfPath = "/root/Documents/consulProto/web.json";

    static ConsulTemplateService consulTemplateService;
    static String consulTemplatePath = "/usr/local/bin/consul-template";
    static String confFilePath = "/root/Documents/consulProto/haproxy.json";
    static String consulAddressAndPort = "localhost:8500";


    PropertiesUtil propertiesUtil;

    @BeforeClass
    public static void setUpClass() throws Exception {
        consulService = new ConsulService(consulPath,consulConfPath);
        consulService.startProcess();
        consulTemplateService = new ConsulTemplateService(consulTemplatePath,confFilePath,consulAddressAndPort);
        consulTemplateService.startProcess();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        consulService.stopProcess();
        consulTemplateService.stopProcess();
    }

    @Before
    public void setUp() throws Exception {
        consulClient = new ConsulClient(Consul.builder().build());
        propertiesUtil = new PropertiesUtil(consulTemplateService);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void parsePropertiesTest() throws Exception {
        Map<Object,Object> configMap = propertiesUtil.parsePropertiesFile("config.properties");
        consulClient.putEntries(configMap, "config");
        assertTrue(consulClient.keyExists("config/consul.execPath"));
    }

    @Test
    public void createPropertiesTest() throws Exception {
        Map<Object,Object> configMap = propertiesUtil.parsePropertiesFile("config.properties");
        consulClient.putEntries(configMap, "testProperties");
        propertiesUtil.createPropertiesFile("/git/consulPrototype/consulLoadBalancing/output/testProperties.properties", "testProperties");
    }
}
