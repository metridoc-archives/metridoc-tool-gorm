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

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.*
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.util.ClassUtils
import org.springframework.util.StringUtils

/**
 * @author Tommy Barker
 */
class GormClassLoaderPostProcessor implements BeanFactoryPostProcessor {
    public static List gormClasses = []
    String gormBeans
    boolean ran = false

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if(gormClasses && beanFactory.beanDefinitionNames.contains("grailsApplication") && !ran) {
            ran = true
            def classes = []
            gormClasses.each {
                classes.add(it)
                def definition = new GenericBeanDefinition()
                definition.beanClass = it
                registerDomainBean(it, beanFactory, "messageSource")
            }
            GenericBeanDefinition beanDefinition = beanFactory.getBeanDefinition("grailsApplication")
            def constructorArgs = new ConstructorArgumentValues()
            constructorArgs.addGenericArgumentValue(classes.toArray(new Class[classes.size()]));
            constructorArgs.addGenericArgumentValue(beanFactory.beanClassLoader);
            beanDefinition.constructorArgumentValues = constructorArgs
        }
    }

    private void registerDomainBean(final Class<?> entityClass, BeanDefinitionRegistry targetRegistry, String messageSourceRef) {
        GenericBeanDefinition beanDef = new GenericBeanDefinition();
        beanDef.setBeanClass(entityClass);
        beanDef.setScope("prototype");

        RootBeanDefinition domainDef = new RootBeanDefinition(MethodInvokingFactoryBean.class);

        domainDef.getPropertyValues().addPropertyValue("targetObject", new RuntimeBeanReference(GrailsApplication.APPLICATION_ID));
        domainDef.getPropertyValues().addPropertyValue("targetMethod", "getArtefact");
        domainDef.getPropertyValues().addPropertyValue("arguments", Arrays.asList(
                DomainClassArtefactHandler.TYPE,
                entityClass.getName()));

        final String domainRef = entityClass.getName() + "Domain";
        if (StringUtils.hasText(messageSourceRef)) {
            GenericBeanDefinition validatorDef = new GenericBeanDefinition();
            validatorDef.setBeanClass(HibernateDomainClassValidator.class);
            validatorDef.getPropertyValues().addPropertyValue("messageSource", new RuntimeBeanReference(messageSourceRef));
            validatorDef.getPropertyValues().addPropertyValue("domainClass", new RuntimeBeanReference(domainRef));
            validatorDef.getPropertyValues().addPropertyValue("sessionFactory", new RuntimeBeanReference("sessionFactory"));
            targetRegistry.registerBeanDefinition(entityClass.getName() + "Validator", validatorDef);
        }

        targetRegistry.registerBeanDefinition(entityClass.getName(), beanDef);
        targetRegistry.registerBeanDefinition(domainRef, domainDef);
    }
}
