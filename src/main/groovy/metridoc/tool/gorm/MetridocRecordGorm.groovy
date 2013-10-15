package metridoc.tool.gorm

import com.sun.xml.internal.ws.util.StringUtils
import metridoc.core.RecordLoader
import metridoc.iterators.Record

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocRecordGorm implements RecordLoader {

    private entityInstance

    @Override
    void validate() {
        if (!entityInstance.validate()) {
            if (entityInstance.errors.fieldErrorCount) {
                def message = "error on field [${entityInstance.errors.fieldError.field}] with error code [${entityInstance.errors.fieldError.code}]"
                throw new AssertionError(message)
            }
            else {
                throw new RuntimeException("unknown error occurred \n $entityInstance.errors")
            }
        }
    }

    @Override
    boolean acceptRecord(Record record) {
        if (entityInstance.metaClass.respondsTo(entityInstance, "acceptRecord", [Record] as Object[])) {
            return entityInstance.acceptRecord(record)
        }

        return true
    }

    @Override
    boolean shouldSave() {
        if (entityInstance.metaClass.respondsTo(entityInstance, "shouldSave")) {
            return entityInstance.shouldSave()
        }

        return true
    }

    void save() {
        entityInstance.save(failOnError: true)
    }

    @Override
    void populate(Record record) {

        try {
            if(entityInstance.metaClass.respondsTo(entityInstance, "populate")) {
                entityInstance.populate(record)
            }
            else {
                def dataOfInterest = record.body.findAll {
                    String propertyName = StringUtils.capitalize(it.key)
                    boolean keep = entityInstance.metaClass.respondsTo(entityInstance, "set${propertyName}",
                            [it.value.getClass()] as Object[])
                    return keep
                }

                dataOfInterest.each {
                    entityInstance."$it.key" = it.value
                }
            }
            validate()
        }
        catch (ClassCastException e) {
            throw new AssertionError("Cast error setting values", e)
        }
    }
}
