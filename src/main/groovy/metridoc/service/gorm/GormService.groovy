/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.service.gorm

import grails.spring.BeanBuilder
import metridoc.core.services.DataSourceService
import metridoc.iterators.Iterators
import metridoc.tool.gorm.GormClassLoaderPostProcessor
import metridoc.tool.gorm.GormEnhancingBeanPostProcessor
import metridoc.tool.gorm.GormIteratorWriter
import metridoc.tool.gorm.HibernateDatastoreFactoryBean
import metridoc.utils.DataSourceConfigUtil
import org.apache.commons.dbcp.BasicDataSource
import org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext
import org.codehaus.groovy.grails.orm.hibernate.EventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.events.PatchedDefaultFlushEventListener
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.plugins.GrailsPluginManagerFactoryBean
import org.hibernate.SessionFactory
import org.springframework.context.ApplicationContext

/**
 * @author Tommy Barker
 */
class GormService extends DataSourceService {
    ApplicationContext applicationContext

    static {
        Iterators.WRITERS["gorm"] = GormIteratorWriter
    }

    /**
     * @deprecated
     * @param classes
     */
    void enableGormFor(Class... classes) {
        enableFor(classes)
    }

    @Override
    protected void doEnableFor(Class... classes) {
        def hibernateProperties = DataSourceConfigUtil.getHibernatePoperties(config)
        hibernateProperties.remove("hibernate.current_session_context_class")
        def dataSourceProperties = DataSourceConfigUtil.getDataSourceProperties(config)
        GormClassLoaderPostProcessor.gormClasses = classes as List
        applicationContext = loadBeanFactory(hibernateProperties, dataSourceProperties)
    }

    @Override
    SessionFactory getSessionFactory() {
        assert applicationContext : "[SessionFactory] cannot be retrieved until [enableFor] is called for one or more entities"
        applicationContext.getBean("sessionFactory", SessionFactory)
    }

    @Override
    void withTransaction(Closure closure) {
        throw new UnsupportedOperationException("[withTransaction] not supported, use [withTransaction] on loaded " +
                "entity instead")
    }

    ApplicationContext loadBeanFactory(Map hibernatePropertiesToSet, Map dataSourceProperties) {
        def beanBuilder = new BeanBuilder()

        def eventTriggeringInterceptor = new ClosureEventTriggeringInterceptor()

        def flushPatch = new PatchedDefaultFlushEventListener()

        beanBuilder.beans {
            xmlns gorm:"http://grails.org/schema/gorm"

            pluginManager(GrailsPluginManagerFactoryBean) {
                grailsDescriptor = "classpath:/grails.xml"
                application = ref("grailsApplication")
            }

            grailsConfigurator(GrailsRuntimeConfigurator, ref("grailsApplication")) {bean ->
                bean.initMethod = "configure"
                bean.dependsOn = "pluginManager"
                pluginManager = ref("pluginManager")
            }

            grailsResourceLoader(GrailsResourceLoaderFactoryBean)

            gorm.sessionFactory(
                    "base-package":"NA",
                    "data-source-ref":"dataSource",
                    "message-source-ref":"messageSource"
            ) {
                hibernateProperties = hibernatePropertiesToSet
                eventListeners = [
                        flush: flushPatch,
                        "pre-load": eventTriggeringInterceptor,
                        "post-load": eventTriggeringInterceptor,
                        save: eventTriggeringInterceptor,
                        "save-update": eventTriggeringInterceptor,
                        "pre-insert": eventTriggeringInterceptor,
                        "post-insert": eventTriggeringInterceptor,
                        "pre-update": eventTriggeringInterceptor,
                        "post-update": eventTriggeringInterceptor,
                        "pre-delete": eventTriggeringInterceptor,
                        "post-delete": eventTriggeringInterceptor
                ]
            }

            grailsDomainClassMappingContext(GrailsDomainClassMappingContext, ref("grailsApplication"))

            hibernateDatastore(HibernateDatastoreFactoryBean) {
                delegate.sessionFactory = ref("sessionFactory")
                grailsApplication = ref("grailsApplication")
                mappingContext = ref("grailsDomainClassMappingContext")
                interceptor = eventTriggeringInterceptor
            }

            gormEnhancingPostProcessor(GormEnhancingBeanPostProcessor) {
                delegate.sessionFactory = ref("sessionFactory")
                delegate.eventTriggeringInterceptor = eventTriggeringInterceptor
                application = ref("grailsApplication")
            }

            gormClassLoaderPostProcessor(GormClassLoaderPostProcessor)

            delegate.config(ConfigObject)

            eventInterceptor(EventTriggeringInterceptor, ref("hibernateDatastore"), ref("config"))

            dataSource(BasicDataSource) {
                def delegateToUse = delegate
                dataSourceProperties.each {key, value ->
                    delegateToUse."$key" = value
                }
            }

        }.createApplicationContext()
    }
}
