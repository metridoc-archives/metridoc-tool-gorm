import foo.FooBar
import metridoc.core.MetridocScript
import metridoc.core.tools.ConfigTool
import metridoc.service.gorm.GormService

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
use(MetridocScript) {
    includeService(mergeMetridocConfig: false, ConfigTool)
    def gorm = includeService(embeddedDataSource: true, GormService)
    gorm.enableGormFor(FooBar)
    FooBar.list()
}