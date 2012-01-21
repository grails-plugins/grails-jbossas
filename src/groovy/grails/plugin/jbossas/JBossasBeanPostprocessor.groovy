/* Copyright 2012 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.jbossas

import grails.util.GrailsUtil

import org.apache.log4j.Logger
import org.springframework.beans.PropertyValue
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor

/**
 * Sets properties in hibernate.properties to avoid NoSuchMethodException: org.hibernate.validator.ClassValidator.<init>
 *
 * @author Burt Beckwith
 */
class JBossasBeanPostprocessor implements BeanDefinitionRegistryPostProcessor {

	protected Logger log = Logger.getLogger(getClass())

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(
	 * 	org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		log.info 'postProcessBeanDefinitionRegistry'
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(
	 * 	org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {

		log.info 'postProcessBeanFactory start'

		def appConfig = beanFactory.parentBeanFactory.getBean('grailsApplication').config

		try {
			if (beanFactory.containsBean('hibernateProperties')) {
				updateHibernateProperties beanFactory, appConfig
			}
			else {
				log.debug 'No DataSource detected, not updating Memcached'
			}
		}
		catch (Throwable e) {
			handleError e, 'Problem updating DataSource'
		}
	}

	protected void updateHibernateProperties(ConfigurableListableBeanFactory beanFactory, appConfig) {

		def jbossConfig = appConfig.grails.plugin.jbossas

		boolean fix = getConfigBoolean(jbossConfig, 'fixHibernateValidator', true)
		if (!fix) {
			log.debug "Not updating Hibernate Properties"
			return
		}

		BeanDefinition beanDefinition = beanFactory.getBeanDefinition('hibernateProperties')
		PropertyValue propertyValue = beanDefinition.getPropertyValues().getPropertyValue('properties')
		Map properties = propertyValue.getValue()
		properties['hibernate.validator.apply_to_ddl'] = 'false'
		properties['hibernate.validator.autoregister_listeners'] = 'false'

		log.debug "Updated Hibernate Validator Properties"
	}

	protected Boolean getConfigBoolean(config, String name, Boolean defaultIfNotSet) {
		def value = config[name]
		return value instanceof Boolean ? value : defaultIfNotSet
	}

	protected void handleError(Throwable t, String prefix) {
		GrailsUtil.deepSanitize t
		log.error "$prefix: $t.message", t
	}
}
