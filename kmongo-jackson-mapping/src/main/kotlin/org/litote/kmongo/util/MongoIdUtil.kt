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

package org.litote.kmongo.util

import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonObjectId
import org.bson.BsonString
import org.bson.BsonValue
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import org.litote.kmongo.MongoId
import org.litote.kmongo.util.MongoIdUtil.IdPropertyWrapper.Companion.NO_ID
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.ReflectProperties.lazySoft
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod

/**
 * Returns the Mongo Id property of the [KClass],
 * or null if no id property is found.
 */
val KClass<*>.idProperty: KProperty1<*, *>?
    get() = MongoIdUtil.findIdProperty(this)

/**
 * Returns the Mongo Id value (which can be null),
 * or null if no id property is found.
 */
val Any?.idValue: Any?
    get() = this?.javaClass?.kotlin?.idProperty?.let { (it)(this) }

internal object MongoIdUtil {

    private sealed class IdPropertyWrapper {

        companion object {
            val NO_ID = NoIdProperty()
        }

        val property: KProperty1<*, *>?
            get() = when (this) {
                is NoIdProperty -> null
                is IdProperty -> prop
            }

        class NoIdProperty : IdPropertyWrapper()
        class IdProperty(val prop: KProperty1<*, *>) : IdPropertyWrapper()
    }

    private val propertyIdCache: MutableMap<KClass<*>, IdPropertyWrapper>
            by lazySoft { ConcurrentHashMap<KClass<*>, IdPropertyWrapper>() }

    fun findIdProperty(type: KClass<*>): KProperty1<*, *>? =
        propertyIdCache.getOrPut(type) {
            (getAnnotatedMongoIdProperty(type)
                    ?: getIdProperty(type))
                ?.let { IdPropertyWrapper.IdProperty(it) }
                    ?: NO_ID

        }.property

    private fun getIdProperty(type: KClass<*>): KProperty1<*, *>? =
        try {
            type.memberProperties.find { "_id" == it.name }
        } catch (error: KotlinReflectionInternalError) {
            //ignore
            null
        }

    fun getAnnotatedMongoIdProperty(type: KClass<*>): KProperty1<*, *>? =
        try {
            val parameter = type.primaryConstructor?.parameters?.firstOrNull { it.findAnnotation<BsonId>() != null }
            if (parameter != null) {
                type.memberProperties.firstOrNull { it.name == parameter.name }
            } else {
                type.memberProperties.find { p ->
                    p.javaField?.isAnnotationPresent(BsonId::class.java) == true
                            || p.getter.javaMethod?.isAnnotationPresent(BsonId::class.java) == true
                            || p.findAnnotation<MongoId>() != null
                }
            }
        } catch (error: KotlinReflectionInternalError) {
            //ignore
            null
        }

    fun getIdValue(idProperty: KProperty1<*, *>, instance: Any): Any? {
        idProperty.isAccessible = true
        return (idProperty)(instance)
    }

    fun getIdBsonValue(idProperty: KProperty1<*, *>, instance: Any): BsonValue? {
        val idValue = (idProperty)(instance)
        return when (idValue) {
            null -> null
            is ObjectId -> BsonObjectId(idValue)
            is String -> BsonString(idValue)
            is Double -> BsonDouble(idValue)
            is Int -> BsonInt32(idValue)
            is Long -> BsonInt64(idValue)
        //TODO direct mapping
            else -> KMongoUtil.toBson(KMongoUtil.toExtendedJson(idValue))
        }
    }
}