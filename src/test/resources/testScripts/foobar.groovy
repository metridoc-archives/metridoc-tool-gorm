import grails.persistence.Entity
import metridoc.core.MetridocScript
import metridoc.tool.gorm.GormTool

args = ["--mergeMetridocConfig=false", "--embeddedDataSource"] as String[]

/**
 * @author Tommy Barker
 */
use(MetridocScript) {
    def gorm = includeTool(GormTool)
    gorm.enableGormFor(Foo)
}
Foo.list()

@Entity
class Foo {
    String bar
}