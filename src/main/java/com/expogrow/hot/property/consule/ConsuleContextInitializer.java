package com.expogrow.hot.property.consule;

import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import com.ecwid.consul.v1.ConsulClient;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicWatchedConfiguration;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ConsuleContextInitializer implements ApplicationContextInitializer<AbstractApplicationContext> {

	@Value("${consule.host:127.0.0.1}")
	private String consuleHost;
	@Value("${consule.port:8500}")
	private int consulePort;
	@Value("${consule.watchIntervalInSeconds:10}")
	private long watchIntervalInSeconds;
	@Value("${consule.rootPath:app/config/}")
	private String rootPath;
	@Value("${spring.application.name:spring-app}")
	private String appName;

	@Override
	public void initialize(final AbstractApplicationContext applicationContext) {

		log.debug("Inside initialize ");

		final ConsulClient client = new ConsulClient(consuleHost, consulePort);

		final ConsulePropertySource configSource = new ConsulePropertySource(rootPath + appName, client,
				watchIntervalInSeconds, TimeUnit.SECONDS);

		configSource.startAsync();

		final ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();

		final AbstractConfiguration configuration = new DynamicWatchedConfiguration(configSource);

		finalConfig.addConfiguration(configuration, "consul-dynamic");

		ConfigurationManager.install(finalConfig);

		final ConfigurableEnvironment environment = applicationContext.getEnvironment();

		final MutablePropertySources propertySources = environment.getPropertySources();

		final ArchaiusPropertySource bridgeSource = new ArchaiusPropertySource("consul-dynamic", configuration);

		propertySources.addFirst(bridgeSource);

		log.debug("Inside addFirst ");

	}

}
