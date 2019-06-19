package com.expogrow.hot.property.consule;

import org.springframework.core.env.PropertySource;

import com.netflix.config.DynamicPropertyFactory;

public class SpringArchaiusPropertySource extends PropertySource<Void> {

	public SpringArchaiusPropertySource(final String name) {
		super(name);
	}

	@Override
	public Object getProperty(final String name) {
		return DynamicPropertyFactory.getInstance().getStringProperty(name, null).get();
	}

}
