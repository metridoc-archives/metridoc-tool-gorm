package metridoc.tool.gorm

import grails.persistence.Entity
import metridoc.iterators.Iterators
import metridoc.iterators.Record
import spock.lang.Specification

import static metridoc.writers.WrittenRecordStat.Status.*

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class GormIteratorWriterSpec extends Specification {
    def gTool = new GormTool(mergeMetridocConfig: false, embeddedDataSource: true)
    def writer = new GormIteratorWriter(gormClass: GormHelper, gormTool: gTool)

    void "test basic entity writing workflow"() {
        given: "some valid data"
        def data = [
                [foo: "asd"],
                [foo: "sdf"],
                [foo: "fgd"],
                [foo: "dfgh"]
        ]
        def rowIterator = Iterators.toRowIterator(data)

        when: "the data is written"
        def response = writer.write(rowIterator)

        then: "appropriate data is returned"
        4 == response.aggregateStats[WRITTEN]
        0 == response.aggregateStats[IGNORED]
        0 == response.aggregateStats[ERROR]
        0 == response.aggregateStats[INVALID]
        GormHelper.withTransaction {
            4 == GormHelper.list().size()
        }
    }

    def "test validation errors"() {
        given: "some invalid data"
        def data = [
                [foo: "asd"],
                [foo: "sdf"],
                [foo: null],
                [foo: "dfgh"]
        ]
        def rowIterator = Iterators.toRowIterator(data)

        when: "data is written"
        def response = writer.write(rowIterator)

        then: "three records are written and one is invalid"
        1 == response.invalidTotal
        3 == response.writtenTotal
        4 == response.getTotal()
    }

    def "test errors"() {
        given: "bad data"
        def data = [
                [foo: "asd"],
                [foo: "sdf"],
                [foo: "error"],
                [foo: "dfgh"]
        ]
        def rowIterator = Iterators.toRowIterator(data)

        when: "data is written"
        def response = writer.write(rowIterator)
        def throwables = response.fatalErrors

        then: "one error is recorded into the response"
        1 == response.errorTotal
        1 == response.getTotal()
        1 == throwables.size()
        throwables[0] instanceof RuntimeException
    }
}

@Entity
class GormHelper {

    String foo

    boolean shouldSave() {
        true
    }

    boolean acceptRecord(Record record) {
        if (record.body.foo == "error") {
            throw new RuntimeException("error")
        }
        record.body.containsKey("foo")
    }

}