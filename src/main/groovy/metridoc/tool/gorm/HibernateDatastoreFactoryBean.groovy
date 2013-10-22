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
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.grails.datastore.mapping.model.MappingContext
import org.hibernate.SessionFactory
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

public class HibernateDatastoreFactoryBean implements FactoryBean<HibernateDatastore>, ApplicationContextAware {

    SessionFactory sessionFactory

    MappingContext mappingContext

    GrailsApplication grailsApplication

    ClosureEventTriggeringInterceptor interceptor

    ApplicationContext applicationContext

    @Override
    public HibernateDatastore getObject() throws Exception {
        def hibernateDatastore = new HibernateDatastore(mappingContext, sessionFactory, grailsApplication.getConfig())
        hibernateDatastore.applicationContext = applicationContext
        interceptor.setDatastores([(sessionFactory): hibernateDatastore])
        hibernateDatastore
    }

    @Override
    public Class<?> getObjectType() {
        return HibernateDatastore.class
    }

    @Override
    public boolean isSingleton() {
        return true
    }
}