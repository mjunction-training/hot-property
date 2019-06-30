package com.expogrow.hot.property.consule;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.netflix.config.DynamicPropertyFactory;

@Component
public class DynamicPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements EnvironmentAware {

	private Environment environment;

	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	@Override
	protected String resolvePlaceholder(final String placeholder, final Properties springProps) {
		final String envValue = environment.getProperty(placeholder);
		final String dynamicValue = DynamicPropertyFactory.getInstance().getStringProperty(placeholder, null).get();
		return isNotEmpty(dynamicValue) ? dynamicValue.trim()
				: isNotEmpty(envValue) ? envValue.trim() : super.resolvePlaceholder(placeholder, springProps);
	}

}
