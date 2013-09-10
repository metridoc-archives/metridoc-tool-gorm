import foo.FooBar
import metridoc.core.MetridocScript
import metridoc.core.tools.ConfigTool
import metridoc.tool.gorm.GormTool

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
use(MetridocScript) {
    includeTool(mergeMetridocConfig: false, ConfigTool)
    def gorm = includeTool(embeddedDataSource: true, GormTool)
    gorm.enableGormFor(FooBar)
    FooBar.list()
}