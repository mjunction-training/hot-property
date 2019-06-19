package com.expogrow.hot.property.consule;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import com.ecwid.consul.v1.ConsulClient;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicWatchedConfiguration;

@Configuration
public class ConsulePropertyConfig {
	@Autowired
	private ConfigurableEnvironment env;

	@Autowired
	private ArchaiusPropertySource propertySource;

	@PostConstruct
	public void init() {
		env.getPropertySources().addFirst(propertySource);
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ArchaiusPropertySource propertySource(@Value("${consule.host:127.0.0.1}") final String consuleHost,
			@Value("${consule.port:8500}") final int consulePort,
			@Value("${consule.watchIntervalInSeconds:10}") final long watchIntervalInSeconds,
			@Value("${consule.rootPath:app/config/}") final String rootPath,
			@Value("${spring.application.name:spring-app}") final String appName) {
		final ConsulClient client = new ConsulClient(consuleHost, consulePort);

		final ConsulePropertySource configSource = new ConsulePropertySource(rootPath + appName, client,
				watchIntervalInSeconds, TimeUnit.SECONDS);

		configSource.startAsync();

		final ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();

		final AbstractConfiguration configuration = new DynamicWatchedConfiguration(configSource);

		finalConfig.addConfiguration(configuration, "consul-dynamic");

		ConfigurationManager.install(finalConfig);

		final ArchaiusPropertySource bridgeSource = new ArchaiusPropertySource("consul-dynamic", configuration);

		return bridgeSource;
	}

}
