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

import groovy.text.XmlTemplateEngine
import metridoc.core.services.DataSourceService
import metridoc.iterators.Iterators
import metridoc.tool.gorm.GormClassLoaderPostProcessor
import metridoc.tool.gorm.GormIteratorWriter
import metridoc.utils.DataSourceConfigUtil
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.SystemUtils
import org.hibernate.SessionFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.support.FileSystemXmlApplicationContext
import org.springframework.util.ClassUtils

import java.text.SimpleDateFormat

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
        String gormBeans = ""
        classes.each { Class entityClass ->
            String name = entityClass.name
            gormBeans += "$name,"
        }
        gormBeans = StringUtils.chop(gormBeans)

        def stream = ClassUtils.classLoader.getResourceAsStream("gormToolContext.xml")
        def engine = new XmlTemplateEngine()
        def template = engine.createTemplate(stream.newReader())


        def file
        try {
            file = File.createTempFile("gormToolContext.xml", null)
        }
        catch (IOException ignored) {
            //getting around some issues I had with windows
            def home = SystemUtils.USER_HOME
            def slash = SystemUtils.FILE_SEPARATOR
            def tmpFolder = new File("${home}${slash}.metridoc${slash}tmp")
            if (!tmpFolder.exists()) {
                assert tmpFolder.mkdirs() : "Could not create temp folder $tmpFolder"
            }
            def stamp = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
            file = new File(tmpFolder, "gormToolContext${stamp}.xml")
            assert file.createNewFile() : "Could not create file ${file}"
        }

        def hibernateProperties = DataSourceConfigUtil.getHibernatePoperties(config)
        hibernateProperties.remove("hibernate.current_session_context_class")
        def dataSourceProperties = DataSourceConfigUtil.getDataSourceProperties(config)
        GormClassLoaderPostProcessor.gormClasses = classes as List
        template.make([
                gormBeans: gormBeans,
                hibernateProperties: hibernateProperties,
                dataSourceProperties: dataSourceProperties
        ]).writeTo(file.newWriter("utf-8"))
        file.deleteOnExit()
        applicationContext = new FileSystemXmlApplicationContext(file.toURI().toURL().toString())
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
}
