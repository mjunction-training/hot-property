
package com.expogrow.hot.property.consule;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.springframework.core.env.EnumerablePropertySource;

import com.netflix.config.util.ConfigurationUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ArchaiusPropertySource extends EnumerablePropertySource<Configuration> {

	public ArchaiusPropertySource(final String name, final Configuration source) {
		super(name, source);
	}

	@Override
	public String[] getPropertyNames() {

		log.debug("Inside getPropertyNames ");

		final Configuration config = super.getSource();

		final Properties properties = ConfigurationUtils.getProperties(config);

		final Set<String> keySet = properties.stringPropertyNames();

		return keySet.toArray(new String[keySet.size()]);

	}

	@Override
	public Object getProperty(final String name) {
		log.debug("Inside getProperty ");
		return getSource().getProperty(name);
	}

}
