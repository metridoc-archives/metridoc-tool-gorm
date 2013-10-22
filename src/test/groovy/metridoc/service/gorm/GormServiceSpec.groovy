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

import groovy.sql.Sql
import metridoc.core.MetridocScript
import metridoc.core.tools.ConfigTool
import metridoc.tool.gorm.User
import spock.lang.Specification

import javax.sql.DataSource

/**
 * Created with IntelliJ IDEA on 8/1/13
 * @author Tommy Barker
 */
class GormServiceSpec extends Specification {

    def service = new GormService(embeddedDataSource: true)

    void setup() {
        service.init()
    }

    void "enableGorm should fail on more than one call"() {
        when:
        service.enableFor(User)

        then:
        noExceptionThrown()

        when:
        service.enableFor(User)

        then:
        thrown(IllegalStateException)
    }

    void "test basic gorm operations"() {
        setup:
        service.enableFor(User)
        def sql = new Sql(service.applicationContext.getBean(DataSource))
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
        service.includeService(mergeMetridocConfig: false, ConfigTool)
        ConfigObject configObject = service.config
        configObject.dataSource.driverClassName = "org.h2.Driver"
        configObject.dataSource.username = "sa"
        configObject.dataSource.password = ""
        configObject.dataSource.url = "jdbc:h2:mem:devDbManual;MVCC=TRUE;LOCK_TIMEOUT=10000"
        service.enableFor(User)
        def dataSource = service.applicationContext.getBean(DataSource)
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
        new GormServiceScriptHelper().run()

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

    void "lets test invalid data"() {
        given: "empty user"
        def user = new User()
        service.enableGormFor(User)

        when:
        def valid
        User.withTransaction { valid = user.validate() }

        then:
        !valid
        "nullable" == user.errors.getFieldError("name").code
    }

    void "test retrieving the session factory"() {
        when:
        service.enableFor(User)
        def sessionFactory = service.sessionFactory

        then:
        noExceptionThrown()
        sessionFactory
    }

    void "test for error when getting sessionFactory and enableFor has not been called"() {
        when:
        service.sessionFactory

        then:
        def error = thrown(AssertionError)
        error.message.contains("[SessionFactory] cannot be retrieved until [enableFor] is called for one or more entities")
    }
}

class GormServiceScriptHelper extends Script {

    @Override
    def run() {
        def gorm = includeService(embeddedDataSource: true, GormService)
        gorm.enableFor(User)
    }
}
