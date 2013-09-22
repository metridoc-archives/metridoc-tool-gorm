package metridoc.tool.gorm

import metridoc.iterators.WrappedIterator
import metridoc.writers.WriteResponse

/**
 * @author Tommy Barker
 */
class GormWrappedIteratorExtension {
    public static WriteResponse toGormEntity(WrappedIterator self, Class entity) {
        def writer = new GormIteratorWriter(gormClass: entity)
        return writer.write(self.getWrappedIterator())
    }
}
