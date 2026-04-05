package io.github.haburashi76.wordQuiz.util

import kotlin.reflect.KProperty

class LazyVal<T> internal constructor(){
    var value: T? = null
        set(value) {
            if (field != null) error("LazyVal cannot be redefined")
            field = value
        }
}

fun <T> lazyVal() = LazyVal<T>()

operator fun <T> LazyVal<T>.getValue(any: Any?, property: KProperty<*>): T? = value

operator fun <T> LazyVal<T>.setValue(any: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

