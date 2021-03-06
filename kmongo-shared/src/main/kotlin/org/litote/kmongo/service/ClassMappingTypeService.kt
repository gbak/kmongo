/*
 * Copyright (C) 2017 Litote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.litote.kmongo.service

import org.bson.BsonDocument
import org.bson.codecs.configuration.CodecRegistry
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 *  Provides an object mapping utility using [java.util.ServiceLoader].
 */
interface ClassMappingTypeService {

    /**
     * Priority of this service. Greater is better.
     */
    fun priority(): Int

    fun filterIdToBson(obj: Any): BsonDocument

    fun toExtendedJson(obj: Any?): String

    fun filterIdToExtendedJson(obj: Any): String

    fun findIdProperty(type: KClass<*>): KProperty1<*, *>?

    fun <T,R> getIdValue(idProperty: KProperty1<T, R>, instance: T): R?

    fun codecRegistry() : CodecRegistry
}