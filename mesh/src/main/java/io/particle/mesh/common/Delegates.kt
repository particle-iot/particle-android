package io.particle.mesh.common

import mu.KLogger
import mu.KotlinLogging
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


fun <T> KLogger.logged(): ReadWriteProperty<Any?, T?> = NullableLogWhenSetVar(this)
fun <T> KLogger.logged(initialValue: T): ReadWriteProperty<Any?, T> {
    return NonNullLogWhenSetVar(initialValue, this)
}


private class NullableLogWhenSetVar<T>(logger: KLogger?) : ReadWriteProperty<Any?, T?> {

    private val log: KLogger = logger ?: KotlinLogging.logger {}

    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        log.info { "'${property.name}' value being updated to: $value" }
        this.value = value
    }
}


// this seems like I'm clearly doing something wrong by needing this whole second class...
private class NonNullLogWhenSetVar<T>(initialValue: T, logger: KLogger?) :
    ReadWriteProperty<Any?, T> {

    private val log: KLogger = logger ?: KotlinLogging.logger {}

    private var value: T = initialValue

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        log.info { "'${property.name}' value being updated to: $value" }
        this.value = value
    }
}
