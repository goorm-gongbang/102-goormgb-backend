package com.goormgb.be.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic paymentCompletedTopic() {
		return TopicBuilder.name(EventTopic.PAYMENT_COMPLETED)
				.partitions(3)
				.replicas(1)
				.build();
	}

	@Bean
	public NewTopic orderCancelledTopic() {
		return TopicBuilder.name(EventTopic.ORDER_CANCELLED)
				.partitions(3)
				.replicas(1)
				.build();
	}

	@Bean
	public NewTopic bankTransferExpiredTopic() {
		return TopicBuilder.name(EventTopic.BANK_TRANSFER_EXPIRED)
				.partitions(3)
				.replicas(1)
				.build();
	}
}
