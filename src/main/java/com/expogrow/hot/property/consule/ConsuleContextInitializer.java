package com.expogrow.hot.property.consule;

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

public class ConsuleContextInitializer implements ApplicationContextInitializer<AbstractApplicationContext> {

	@Value("${consule.host:localhsot}")
	private String consuleHost;
	@Value("${consule.port:8500}")
	private int consulePort;
	@Value("${consule.rootPath:app/config}")
	private String rootPath;

	@Override
	public void initialize(final AbstractApplicationContext applicationContext) {

		final ConsulClient client = new ConsulClient(consuleHost, consulePort);

		final ConsulePropertySource configSource = new ConsulePropertySource(rootPath, client);

		configSource.startAsync();

		final ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();

		final AbstractConfiguration configuration = new DynamicWatchedConfiguration(configSource);

		finalConfig.addConfiguration(configuration, "consul-dynamic");

		ConfigurationManager.install(finalConfig);

		final ConfigurableEnvironment environment = applicationContext.getEnvironment();

		final MutablePropertySources propertySources = environment.getPropertySources();

		final ArchaiusPropertySource bridgeSource = new ArchaiusPropertySource("consul-dynamic", configuration);

		propertySources.addFirst(bridgeSource);

	}

}
