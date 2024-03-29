package com.expogrow.hot.property.consule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ConsulePropertyConfig.class)
@ComponentScan(basePackageClasses = DynamicPlaceholderConfigurer.class)
public @interface EnableDynamicProperties {

}
