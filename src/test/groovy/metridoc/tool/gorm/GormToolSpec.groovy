package metridoc.tool.gorm

import groovy.sql.Sql
import spock.lang.Specification

import javax.sql.DataSource

/**
 * Created with IntelliJ IDEA on 8/1/13
 * @author Tommy Barker
 */
class GormToolSpec extends Specification {

    def tool = new GormTool()

    void "enableGorm should fail on more than one call"() {
        when:
        tool.enableGorm(User)

        then:
        noExceptionThrown()

        when:
        tool.enableGorm(User)

        then:
        thrown(IllegalStateException)
    }

    void "test basic gorm operations"() {
        setup:
        tool.enableGorm(User)
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
}
