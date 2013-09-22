import grails.persistence.Entity
import metridoc.core.MetridocScript
import metridoc.service.gorm.GormService

args = ["--mergeMetridocConfig=false", "--embeddedDataSource"] as String[]

/**
 * @author Tommy Barker
 */
use(MetridocScript) {
    def gorm = includeService(GormService)
    gorm.enableGormFor(Foo)
}
Foo.list()

@Entity
class Foo {
    String bar
}