package metridoc.tool.gorm

import groovy.text.XmlTemplateEngine
import groovy.util.logging.Slf4j
import metridoc.core.tools.ConfigTool
import metridoc.core.tools.DefaultTool
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
class GormTool extends DefaultTool {
    ApplicationContext applicationContext
    final ran = new AtomicBoolean(false)
    boolean embedded
    /**
     * This method can only be called once, subsequent calls throw an
     * IllegalStateException
     *
     * adds entities and boots up gorm
     * @param entities entities to add
     */
    void enableGormFor(Class... entities) {
        includeTool(ConfigTool)
        if (ran.get()) {
            throw new IllegalStateException("enableGormFor cannot be called more than once")
        }

        ran.getAndSet(true)

        def packages = [] as Set<String>
        entities.each { Class entityClass ->
            String name = entityClass.name
            int index = name.lastIndexOf(".")
            packages << name.substring(0, index)
        }

        def packagesAsString = new String()

        packages.each {
            packagesAsString += it + ","
        }

        packagesAsString = StringUtils.chop(packagesAsString) //remove last comma

        def stream = ClassUtils.classLoader.getResourceAsStream("gormToolContext.xml")
        def engine = new XmlTemplateEngine()
        def template = engine.createTemplate(stream.newReader())


        def file = File.createTempFile("gormToolContext.xml", null)
        def config = binding.config
        def hibernateProperties = DataSourceConfigUtil.getHibernatePoperties(config)
        hibernateProperties.remove("hibernate.current_session_context_class")
        def dataSourceProperties = DataSourceConfigUtil.getDataSourceProperties(config)
        template.make([
                gormToolBasePackage: packagesAsString,
                embedded: embedded,
                hibernateProperties: hibernateProperties,
                dataSourceProperties: dataSourceProperties
        ]).writeTo(file.newWriter("utf-8"))
        file.deleteOnExit()
        applicationContext = new FileSystemXmlApplicationContext(file.toURI().toURL().toString())
    }
}
