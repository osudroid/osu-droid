@file:JvmName("LangUtil")

package com.reco1l.framework.extensions

import com.reco1l.framework.lang.async
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses

/**
 * [Class.getSimpleName]
 */
val Any?.className: String
    get() = this?.javaClass?.simpleName ?: "Null"

/**
 * Create a new instance with the given parameters.
 */
fun <T : Any> KClass<T>.createInstance(vararg parameters: Any?): T
{
    if (parameters.isEmpty())
    {
        return createInstance()
    }

    val constructor = constructors.first { it.parameters.size == parameters.size }
    val params = constructor.parameters

    val arguments = params.associateWith { parameters[params.indexOf(it)] }

    return constructor.callBy(arguments)
}

/**
 * Iterate over all class fields of a specific type.
 *
 * @param type The type of the fields to filter.
 * @param action The action to execute.
 */
inline fun <reified T : Any> Any.forEachFieldOf(type: KClass<T>, action: (T) -> Unit)
{
    this::class.memberProperties
            .filter { it.returnType.classifier == type }
            .forEach { action(it.getter.call(this) as T) }
}

/**
 * Improved comparator with ascending boolean property.
 */
inline fun <T> compareBy(ascending: Boolean, crossinline selector: (T) -> Comparable<*>?) = Comparator<T> { a, b ->

    if (ascending)
    {
        compareValuesBy(a, b, selector)
    }
    else compareValuesBy(b, a, selector)
}

/**
 * Special block to ignore exceptions
 */
inline fun ignoreException(block: () -> Unit)
{
    try
    {
        block()
    }
    catch (e: Exception)
    {
        if (BuildConfig.DEBUG)
            e.printStackTrace()
    }
}

/**
 * Prettier try-catch with result returning.
 */
inline fun <reified T> (() -> T).orCatch(onException: (e: Exception) -> T): T
{
    return try { this() } catch (e: Exception) { onException(e) }
}

/**
 * Prettier try-catch on async thread.
 */
fun (() -> Any).orAsyncCatch(onException: ((e: Exception) -> Unit)?)
{
    async { try { this() } catch (e: Exception) { onException?.invoke(e) } }
}

/**
 * Get all subclasses of a type from a package.
 */
inline fun <reified T> getSubclassesOf(packageName: String): List<Class<*>>
{
    val `package` = Class.forName(packageName)

    return `package`.declaredClasses.filter { it.kotlin.isSubclassOf(T::class) }
}

/**
 * Get all sub interfaces of a type from a package.
 */
inline fun <reified T> getSubInterfacesOf(packageName: String) = getSubclassesOf<T>(packageName).filter { it.isInterface }

/**
 * Get all superclasses of the desired type.
 */
fun KClass<*>.superClassesOfType(base: KClass<*>) = this.superclasses.filter { it.isSubclassOf(base) }
