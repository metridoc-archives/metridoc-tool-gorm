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



package metridoc.tool.gorm

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormEnhancer
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormValidationApi
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport
import org.grails.datastore.mapping.validation.ValidationErrors
import org.hibernate.EntityMode
import org.hibernate.SessionFactory
import org.hibernate.metadata.ClassMetadata
import org.springframework.beans.BeanInstantiationException
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.validation.Errors

public class GormEnhancingBeanPostProcessor implements InitializingBean, ApplicationContextAware {

    SessionFactory sessionFactory

    GrailsApplication application

    ApplicationContext applicationContext

    @Override
    public void afterPropertiesSet() throws Exception {
        sessionFactory.allClassMetadata.each { className, ClassMetadata metadata ->
            Class mappedClass = metadata.getMappedClass(EntityMode.POJO)

            if (!application.getDomainClass(mappedClass.name)) {
                application.addDomainClass(mappedClass)
            }
        }

        try {
            //for whatever reason, when running within the context of MetridocScript,
            //initialiseFinders is not found.  Doing this hack to get everything to work
            HibernateGormEnhancer.metaClass.initialiseFinders = {
                delegate.finders = Collections.unmodifiableList(delegate.getAllDynamicFinders())
            }

            //similar problem here
            HibernateGormValidationApi.metaClass.resetErrors = { instance ->
                def errors = new ValidationErrors(instance)
                instance.errors = errors
                return errors
            }

            DomainClassGrailsPlugin.enhanceDomainClasses(application, applicationContext)
            HibernatePluginSupport.enhanceSessionFactory(sessionFactory, application, applicationContext)
        }
        catch (Throwable e) {
            throw new BeanInstantiationException(ConfigurableLocalSessionFactoryBean, "Error configuring GORM dynamic behavior: $e.message", e)
        }
    }
}
