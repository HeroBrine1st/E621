package ru.herobrine1st.e621.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.lang.NullPointerException
import kotlin.reflect.KProperty

/**
 * Костыль для того, чтобы использовать state для хранения общей (между Screen и соответствующего actionBarActions) ViewModel/StateHolder
 * Без использования этого класса возможны гейзенбаги
 * P.s. на самом деле хватило бы обычного mutableStateOf, но мне нужен lateinit
 */
class LateinitMutableState<T> {
    private val internalState = mutableStateOf<T?>(null)
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        internalState.getValue(thisRef, property) ?: throw NullPointerException("Property ${property.name} is not initialized!")

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        internalState.setValue(thisRef, property, value)
}

fun <T> lateinitMutableState(): LateinitMutableState<T> {
    return LateinitMutableState()
}