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

import metridoc.iterators.Record
import metridoc.iterators.RecordIterator
import metridoc.service.gorm.GormService
import metridoc.writers.DefaultIteratorWriter
import metridoc.writers.WriteResponse

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class GormIteratorWriter extends DefaultIteratorWriter {

    Class gormClass

    @Override
    WriteResponse write(RecordIterator recordIterator) {
        assert gormClass: "gormClass must not be null"

        def response

        //try catch forces a failure in transaction to trigger rollback
        try {
            gormClass.withTransaction {
                response = super.write(recordIterator)
                if (response.errorTotal) {
                    throw response.fatalErrors[0]
                }
            }
        }
        catch (Throwable throwable) {
            response = new WriteResponse()
            response.addError(throwable)
        }

        return response
    }

    @Override
    boolean doWrite(int lineNumber, Record record) {
        def instance = new MetridocRecordGorm(entityInstance: gormClass.newInstance())
        if (instance.acceptRecord(record)) {
            instance.populate(record)

            if (instance.shouldSave()) {
                instance.save()
                return true
            }
        }

        return false
    }
}
