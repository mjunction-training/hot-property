package com.expogrow.hot.property.consule;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecwid.consul.v1.ConsulClient;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicWatchedConfiguration;

@Configuration
public class ConsulePropertyConfig {

	@Bean
	public AbstractConfiguration addApplicationPropertiesSource(
			@Value("${consule.host:localhsot}") final String consuleHost,
			@Value("${consule.port:8500}") final int consulePort,
			@Value("${consule.rootPath:app/config/}") final String rootPath,
			@Value("${spring.application.name:spring-app}") final String appName) {

		final ConsulClient client = new ConsulClient(consuleHost, consulePort);

		final ConsulePropertySource configSource = new ConsulePropertySource(rootPath + appName, client);

		configSource.startAsync();

		final ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();

		final AbstractConfiguration configuration = new DynamicWatchedConfiguration(configSource);

		finalConfig.addConfiguration(configuration, "consul-dynamic");

		ConfigurationManager.install(finalConfig);

		return configuration;
	}

}
