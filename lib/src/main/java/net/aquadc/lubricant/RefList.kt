package net.aquadc.lubricant

import java.lang.ref.Reference

internal inline fun <T : Any> ArrayList<out Reference<T>>.forEachReferent(block: (T) -> Unit) {
    var i = 0
    while (i < size) {
        val referent = get(i).get()
        if (referent === null) {
            removeAt(i)
        } else {
            block(referent)
            i++
        }
    }
}

internal fun <T : Any> ArrayList<out Reference<T>>.removeReferent(value: T) {
    var i = 0
    while (i < size) {
        val referent = get(i).get()
        if (referent === value) {
            removeAt(i)
            break
        }
        if (referent === null) {
            removeAt(i)
        } else {
            i++
        }
    }
    while (i < size) {
        val referent = get(i).get()
        if (referent === null) {
            removeAt(i)
        } else {
            i++
        }
    }
}
