package metridoc.tool.gorm

import groovy.text.XmlTemplateEngine
import groovy.util.logging.Slf4j
import metridoc.core.tools.ConfigTool
import metridoc.core.tools.DataSourceTool
import metridoc.utils.DataSourceConfigUtil
import org.apache.commons.lang.StringUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.support.FileSystemXmlApplicationContext
import org.springframework.util.ClassUtils

import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Tommy Barker
 */
@Slf4j
class GormTool extends DataSourceTool {
    ApplicationContext applicationContext
    final ran = new AtomicBoolean(false)

    /**
     * This method can only be called once, subsequent calls throw an
     * IllegalStateException
     *
     * adds entities and boots up gorm
     * @param entities entities to add
     */
    void enableGormFor(Class... entities) {

        if (!binding.hasVariable("configTool")) {
            includeTool(mergeMetridocConfig: mergeMetridocConfig, ConfigTool)
            init()
        }

        if (ran.get()) {
            throw new IllegalStateException("enableGormFor cannot be called more than once")
        }

        ran.getAndSet(true)

        String gormBeans = ""
        entities.each { Class entityClass ->
            String name = entityClass.name
            gormBeans += "$name,"
        }
        gormBeans = StringUtils.chop(gormBeans)

        def stream = ClassUtils.classLoader.getResourceAsStream("gormToolContext.xml")
        def engine = new XmlTemplateEngine()
        def template = engine.createTemplate(stream.newReader())


        def file = File.createTempFile("gormToolContext.xml", null)
        def config = binding.config
        def hibernateProperties = DataSourceConfigUtil.getHibernatePoperties(config)
        hibernateProperties.remove("hibernate.current_session_context_class")
        def dataSourceProperties = DataSourceConfigUtil.getDataSourceProperties(config)
        GormClassLoaderPostProcessor.gormClasses = entities as List
        template.make([
                gormBeans: gormBeans,
                hibernateProperties: hibernateProperties,
                dataSourceProperties: dataSourceProperties
        ]).writeTo(file.newWriter("utf-8"))
        file.deleteOnExit()
        applicationContext = new FileSystemXmlApplicationContext(file.toURI().toURL().toString())
    }
}
