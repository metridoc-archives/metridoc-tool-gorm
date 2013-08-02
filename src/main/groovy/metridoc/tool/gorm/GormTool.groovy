package metridoc.tool.gorm

import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Slf4j
import metridoc.core.tools.DefaultTool
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
    private final ran = new AtomicBoolean(false)

    /**
     * This method can only be called once, subsequent calls throw an
     * IllegalStateException
     *
     * adds entities and boots up gorm
     * @param entities entities to add
     */
    void enableGorm(Class... entities) {
        if (ran.get()) {
            throw new IllegalStateException("enableGorm cannot be called more than once")
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
        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(stream.newReader())


        def file = File.createTempFile("gormToolContext.xml", null)
        template.make([
                gormToolBasePackage: packagesAsString
        ]).writeTo(file.newWriter("utf-8"))
        file.deleteOnExit()
        applicationContext = new FileSystemXmlApplicationContext(file.toURI().toURL().toString())
    }
}
