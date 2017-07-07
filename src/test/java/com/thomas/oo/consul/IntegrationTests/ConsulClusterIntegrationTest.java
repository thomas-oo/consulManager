package com.thomas.oo.consul.IntegrationTests;

import com.thomas.oo.consul.TestConfig;
import com.thomas.oo.consul.consul.ConsulClient;
import net.schmizz.sshj.Config;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

//Goal: test the integrity of consul when run in a 3 server cluster
//Should be able to serve requests for services, kv values even if a server goes down and even if the leader server goes down
//Should be able to retain services that are registered through an agent even if the agent fails
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class, loader = AnnotationConfigContextLoader.class)
@Order(value = Ordered.LOWEST_PRECEDENCE)
@TestExecutionListeners(listeners = {ConsulClusterIntegrationTest.class, DependencyInjectionTestExecutionListener.class})
@TestPropertySource("classpath:testConfig.properties")
public class ConsulClusterIntegrationTest extends AbstractTestExecutionListener{
    //Todo: find a way to start up 3 remote consul servers, for now they must be running and in a cluster together
    @Value("${cluster.node1}")String node1Address;
    @Value("${cluster.node1.confPath}")String node1ConfPath;
    @Value("${cluster.node1.dataDirPath}")String node1DataDirPath;
    @Value("${cluster.node2}")String node2Address;
    @Value("${cluster.node2.confPath}")String node2ConfPath;
    @Value("${cluster.node2.dataDirPath}")String node2DataDirPath;
    @Value("${cluster.node3}")String node3Address;
    @Value("${cluster.node3.confPath}")String node3ConfPath;
    @Value("${cluster.node3.dataDirPath}")String node3DataDirPath;
    @Value("${cluster.node4}")String node4Address;
    @Value("${cluster.node4.dataDirPath}")String node4DataDirPath;
    Config config = new DefaultConfig();
    SSHClient node2ssh;
    String startServerCommand = "consul agent -server -ui -config-dir=%s -data-dir=%s -bind=%s -client=0.0.0.0 &";
    String startClientCommand = "consul agent -ui -data-dir=%s -bind=%s -client=0.0.0.0";

    @Autowired
    ConsulClient consulClient;


    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        super.beforeTestClass(testContext);
        testContext.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);
        //start up 2 remote consul servers
        startConsulServer(node2Address,node2ConfPath,node2DataDirPath);
        startConsulServer(node3Address,node3ConfPath,node3DataDirPath);

//        SSHClient sshClient = new SSHClient();
//        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
//        sshClient.loadKnownHosts();
//        sshClient.connect(node2Address);
//        sshClient.authPassword("root","system");
//        Session session = sshClient.startSession();
//        Session.Command command = session.exec(String.format(startServerCommand, node2ConfPath, node2DataDirPath, node2Address));
//        session.close();
//        sshClient.disconnect();
    }

    private void startConsulServer(String nodeAddress, String nodeConfPath, String nodeDataDirPath) throws Exception{
        SSHClient sshClient = new SSHClient(config);
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.loadKnownHosts();
        sshClient.connect(nodeAddress);
        sshClient.authPassword("root","system");
        Session session = sshClient.startSession();
        Session.Command command = session.exec(String.format(startServerCommand, nodeConfPath, nodeDataDirPath, nodeAddress));
        session.close();
        sshClient.disconnect();
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        super.afterTestClass(testContext);
        stopConsulServer(node2Address);
        stopConsulServer(node3Address);
    }

    private void stopConsulServer(String nodeAddress) throws IOException {
        SSHClient sshClient = new SSHClient(config);
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.loadKnownHosts();
        sshClient.connect(nodeAddress);
        sshClient.authPassword("root","system");
        Session session = sshClient.startSession();
        Session.Command command = session.exec("pkill consul");
        command.close();
        session.close();
        sshClient.disconnect();
    }

    @Before
    public void setUp() throws Exception {
        node2ssh = new SSHClient(config);
        node2ssh.addHostKeyVerifier(new PromiscuousVerifier());
        node2ssh.loadKnownHosts();
        node2ssh.connect(node2Address);
        node2ssh.authPassword("root", "system");
    }

    @After
    public void tearDown() throws Exception {
        node2ssh.disconnect();
    }

    @Ignore
    @Test
    public void leaderFailsTest() throws Exception {
        String leaderAddressAndHost = consulClient.getClusterLeader();
        //Force leader to fail
    }

    @Test
    public void startRemoteServerTest() throws Exception {
        Session session = node2ssh.startSession();
        Session.Command command = session.exec("consul members");
        System.out.println(IOUtils.readFully(command.getInputStream()));
        command.join();
        int exitCode = command.getExitStatus();
        assertEquals(0,exitCode);
        session.close();
    }
}
