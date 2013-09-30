import grails.persistence.Entity
import metridoc.core.services.ConfigService
import metridoc.service.gorm.GormService

args = ["--mergeMetridocConfig=false", "--embeddedDataSource"] as String[]

/**
 * @author Tommy Barker
 */

includeService(ConfigService)
def gorm = includeService(GormService)
gorm.enableFor(Foo)

Foo.list()

@Entity
class Foo {
    String bar
}