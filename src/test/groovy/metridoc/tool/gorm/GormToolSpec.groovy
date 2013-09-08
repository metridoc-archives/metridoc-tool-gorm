package metridoc.tool.gorm

import groovy.sql.Sql
import metridoc.core.MetridocScript
import metridoc.core.tools.ConfigTool
import org.springframework.util.ClassUtils
import spock.lang.Specification

import javax.sql.DataSource

/**
 * Created with IntelliJ IDEA on 8/1/13
 * @author Tommy Barker
 */
class GormToolSpec extends Specification {

    def tool = new GormTool(mergeMetridocConfig: false, embeddedDataSource: true)

    void "enableGorm should fail on more than one call"() {
        when:
        tool.enableGormFor(User)

        then:
        noExceptionThrown()

        when:
        tool.enableGormFor(User)

        then:
        thrown(IllegalStateException)
    }

    void "test basic gorm operations"() {
        setup:
        tool.enableGormFor(User)
        def sql = new Sql(tool.applicationContext.getBean(DataSource))
        def user = new User(name: "joe", age: 7)

        when:
        User.withTransaction {
            user.save(flush: true)
        }
        def total = sql.firstRow("select count(*) as total from user").total

        then:
        noExceptionThrown()
        total == 1
    }

    void "test gorm with dataSource properties"() {
        setup:
        tool.includeTool(mergeMetridocConfig: false, ConfigTool)
        ConfigObject configObject = tool.binding.config
        configObject.dataSource.driverClassName = "org.h2.Driver"
        configObject.dataSource.username = "sa"
        configObject.dataSource.password = ""
        configObject.dataSource.url = "jdbc:h2:mem:devDbManual;MVCC=TRUE;LOCK_TIMEOUT=10000"
        tool.enableGormFor(User)
        def dataSource = tool.applicationContext.getBean(DataSource)
        def sql = new Sql(dataSource)
        def user = new User(name: "joe", age: 7)

        when:
        User.withTransaction {
            user.save(flush: true)
            user.errors.fieldErrorCount
        }
        def total = sql.firstRow("select count(*) as total from user").total

        then:
        dataSource.connection.metaData.getURL().startsWith("jdbc:h2:mem:devDb")
        noExceptionThrown()
        total == 1
    }

    void "test within the scope of MetridocScript"() {

        when:
        new GormToolScriptHelper().run()

        then:
        noExceptionThrown()
    }

    void "everything should work as a script"() {
        given:
        def scriptDir = new File("src/test/resources/testScripts")
        def scriptFile = new File("foobar.groovy", scriptDir)
        def shell = new GroovyShell()
        def thread = Thread.currentThread()
        def originalClassLoader = thread.contextClassLoader
        thread.contextClassLoader = shell.classLoader

        when:
        shell.evaluate(scriptFile)

        then:
        noExceptionThrown()

        cleanup:
        thread.contextClassLoader = originalClassLoader
    }
}

class GormToolScriptHelper extends Script {

    @Override
    def run() {
        use(MetridocScript) {
            def gorm = includeTool(embeddedDataSource: true, GormTool)
            gorm.enableGormFor(User)
        }
    }
}
