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







package metridoc.tool.gorm

import grails.persistence.Entity
import metridoc.iterators.Iterators
import metridoc.iterators.Record
import metridoc.service.gorm.GormService
import spock.lang.Specification

import static metridoc.writers.WrittenRecordStat.Status.*

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class GormIteratorWriterSpec extends Specification {
    def gService = new GormService(embeddedDataSource: true)

    void setup() {
        gService.init()
        gService.enableFor(GormHelper)
    }

    void "test basic entity writing workflow"() {
        when: "the data is written"
        def response = Iterators.fromMaps([foo: "asd"],
                [foo: "sdf"],
                [foo: "fgd"],
                [foo: "dfgh"]).toGormEntity(GormHelper)

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

        when: "data is written"
        def response = Iterators.fromMaps(
                [foo: "asd"],
                [foo: "sdf"],
                [foo: null],
                [foo: "dfgh"]
        ).toGormEntity(GormHelper)

        then: "three records are written and one is invalid"
        1 == response.invalidTotal
        3 == response.writtenTotal
        4 == response.getTotal()
    }

    def "test errors"() {
        when: "bad data is written"
        def response = Iterators.fromMaps(
                [foo: "asd"],
                [foo: "sdf"],
                [foo: "error"],
                [foo: "dfgh"]
        ).toGormEntity(GormHelper)

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