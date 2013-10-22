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
import metridoc.core.MetridocScript
import metridoc.iterators.Record
import metridoc.service.gorm.GormService
import metridoc.writers.WriteResponse
import spock.lang.Specification

import static metridoc.iterators.Iterators.createWriter
import static metridoc.iterators.Iterators.toRowIterator

/**
 * Created with IntelliJ IDEA on 9/10/13
 * @author Tommy Barker
 */
class MetridocRecordGormSpec extends Specification {

    void "check that MetridocRecordGorm delegates to the entity classes populate method"() {
        setup:
        WriteResponse response
        def script = new Script() {

            def run() {
                includeTool(
                        embeddedDataSource: true,
                        GormService
                ).enableFor(FooBaz)

                def writer = createWriter(gormClass: FooBaz, "gorm")
                response = writer.write(
                        toRowIterator([
                                [bar: "foo", baz: "fam", populate: false],
                                [bar: "foo", baz: "fam", populate: false],
                                [bar: "foo", baz: "fam", populate: false]
                        ]))
                def errors = response.fatalErrors
                if (errors) throw errors[0]
            }
        }

        when:
        script.run()
        def allFoo = FooBaz.list()

        then:
        noExceptionThrown()
        3 == allFoo.findAll { it.populate == true }.size()
    }
}

@Entity
class FooBaz {
    String bar
    String baz
    boolean populate = false

    void populate(Record record) {
        record.body.each { key, value ->
            this."$key" = value
        }
        populate = true
    }
}
