package com.expogrow.hot.property.consule;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import com.ecwid.consul.v1.ConsulClient;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicWatchedConfiguration;
import com.netflix.config.WatchedUpdateResult;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Configuration
public class ConsulePropertyConfig
		implements InitializingBean, ApplicationContextInitializer<AbstractApplicationContext> {

	@Value("${consule.host:127.0.0.1}")
	private String consuleHost;

	@Value("${consule.port:8500}")
	private int consulePort;

	@Value("${consule.watchIntervalInSeconds:10}")
	private long watchIntervalInSeconds;

	@Value("${consule.rootPath:app/config/}")
	private String rootPath;

	@Value("${spring.application.name:${appName:spring-app}}")
	private String appName;

	private static final String _CONSULE_CONFIG = "consul-dynamic";

	@Autowired
	private AbstractApplicationContext applicationContext;

	@Autowired
	private ContextRefresher contextRefresher;

	@Override
	public void afterPropertiesSet() throws Exception {
		initialize(applicationContext);
	}

	@Override
	public void initialize(final AbstractApplicationContext applicationContext) {

		log.debug("Inside initialize ");

		final ConfigurableEnvironment environment = applicationContext.getEnvironment();

		final MutablePropertySources propertySources = environment.getPropertySources();

		propertySources.addFirst(propertySource());

		log.debug("Finished initialize ");

	}

	@Bean
	public ArchaiusPropertySource propertySource() {

		final ConsulClient client = new ConsulClient(consuleHost, consulePort);

		final ConsulePropertySource configSource = new ConsulePropertySource(rootPath + appName, client,
				watchIntervalInSeconds, SECONDS);

		configSource.startAsync();

		final ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();

		final AbstractConfiguration configuration = new DynamicWatchedConfiguration(configSource) {

			@Override
			public void updateConfiguration(final WatchedUpdateResult result) {

				super.updateConfiguration(result);

				if (result == null || !result.hasChanges()) {
					return;
				}

				final Set<String> propKeys = new HashSet<>();

				if (null != result.getComplete()) {
					propKeys.addAll(result.getComplete().keySet());
				}

				if (null != result.getAdded()) {
					propKeys.addAll(result.getAdded().keySet());
				}

				if (null != result.getChanged()) {
					propKeys.addAll(result.getChanged().keySet());
				}

				if (null != result.getDeleted()) {
					propKeys.addAll(result.getDeleted().keySet());
				}

				if (!propKeys.isEmpty()) {
					contextRefresher.refresh();
				}
			}

		};

		finalConfig.addConfiguration(configuration, _CONSULE_CONFIG);

		if (ConfigurationManager.isConfigurationInstalled()) {

			ConfigurationManager.loadPropertiesFromConfiguration(finalConfig);

		} else {

			ConfigurationManager.install(finalConfig);

		}

		return new ArchaiusPropertySource(_CONSULE_CONFIG, configuration);

	}

}
