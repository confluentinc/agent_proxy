package io.confluent.pas.agent.proxy.frameworks.java.kafka;

import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.KafkaPropertiesFactory;
import io.confluent.pas.agent.common.utils.Lazy;
import io.confluent.pas.agent.proxy.frameworks.java.kafka.impl.TopicManagementImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TopicManagementTest {

    @Mock
    private KafkaConfiguration kafkaConfiguration;

    @Mock
    private KafkaAdminClient kafkaAdminClient;

    @Mock
    private DescribeClusterResult describeClusterResult;

    @Mock
    private KafkaFuture<Collection<Node>> nodes;

    private TopicManagement topicManagement;

    @BeforeEach
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        MockitoAnnotations.openMocks(this);

        when(kafkaConfiguration.applicationId()).thenReturn(RandomStringUtils.secure().nextAscii(10));
        when(kafkaConfiguration.schemaRegistryUrl()).thenReturn("mock://schema-registry");
        when(kafkaConfiguration.brokerServers()).thenReturn("mock://bootstrap-servers");
        when(kafkaConfiguration.schemaRegistryBasicAuthUserInfo()).thenReturn("user:pwd");
        when(kafkaConfiguration.saslJaasConfig()).thenReturn("sass_cfg");
        when(kafkaConfiguration.securityProtocol()).thenReturn(KafkaConfiguration.DEFAULT_SECURITY_PROTOCOL);
        when(kafkaConfiguration.registrationTopicName()).thenReturn(KafkaConfiguration.DEFAULT_REGISTRATION_TOPIC_NAME);
        when(kafkaConfiguration.topicConfiguration()).thenReturn(new KafkaConfiguration.DefaultTopicConfiguration());
        when(kafkaConfiguration.saslMechanism()).thenReturn(KafkaConfiguration.DEFAULT_SASL_MECHANISM);

        when(kafkaAdminClient.describeCluster()).thenReturn(describeClusterResult);
        when(describeClusterResult.nodes()).thenReturn(nodes);
        when(kafkaAdminClient.createTopics(any())).thenReturn(mock(CreateTopicsResult.class));
        when(kafkaAdminClient.createTopics(any()).all()).thenReturn(mock(KafkaFuture.class));
        when(kafkaAdminClient.createTopics(any()).all().get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(null);

        clearInvocations(kafkaAdminClient);

        topicManagement = new TopicManagementImpl(
                KafkaPropertiesFactory.getSchemRegistryClient(kafkaConfiguration),
                new KafkaConfiguration.DefaultTopicConfiguration(),
                new Lazy<>(() -> kafkaAdminClient));
    }

    @Test
    public void testCreateTopic() throws ExecutionException, InterruptedException, TimeoutException {
        when(describeClusterResult.nodes().get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(List.of(
                new Node(1, "host1", 9092),
                new Node(2, "host2", 9092),
                new Node(3, "host3", 9092),
                new Node(4, "host4", 9092)));

        topicManagement.createTopic("test-topic", String.class, String.class);

        verify(kafkaAdminClient, times(1)).describeCluster();
        verify(kafkaAdminClient, times(1)).createTopics(argThat((topics) -> {
            List<String> topicNames = topics.stream().map(NewTopic::name).toList();
            List<Integer> partitions = topics.stream().map(NewTopic::numPartitions).toList();
            List<Short> replicationFactors = topics.stream().map(NewTopic::replicationFactor).toList();

            return topicNames.size() == 1 && topicNames.contains("test-topic") &&
                    partitions.contains(6) && replicationFactors.contains((short) 3);
        }));
    }

    @Test
    public void testCreateTopicWithSchemas() throws ExecutionException, InterruptedException, TimeoutException {
        when(describeClusterResult.nodes().get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(List.of(
                new Node(1, "host1", 9092),
                new Node(2, "host2", 9092),
                new Node(3, "host3", 9092),
                new Node(4, "host4", 9092)));

        topicManagement.createTopic("test-topic", String.class, new JsonSchema("{\"type\":\"string\"}"));

        verify(kafkaAdminClient, times(1)).describeCluster();
        verify(kafkaAdminClient, times(1)).createTopics(argThat((topics) -> {
            List<String> topicNames = topics.stream().map(NewTopic::name).toList();
            List<Integer> partitions = topics.stream().map(NewTopic::numPartitions).toList();
            List<Short> replicationFactors = topics.stream().map(NewTopic::replicationFactor).toList();

            return topicNames.size() == 1 && topicNames.contains("test-topic") &&
                    partitions.contains(6) && replicationFactors.contains((short) 3);
        }));
    }

    @Test
    public void testCreateTopicNoBroker() throws ExecutionException, InterruptedException, TimeoutException {
        when(describeClusterResult.nodes().get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> topicManagement.createTopic("test-topic", String.class, String.class))
                .isInstanceOf(TopicManagementException.class)
                .hasMessage("No live Kafka brokers");
    }

}
