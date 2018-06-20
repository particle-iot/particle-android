package io.particle.particlemesh.common

import kotlinx.coroutines.experimental.CancellationException
import java.lang.ref.WeakReference
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn


/**
 * Extension function for creating a weak reference which cancels corutines
 */
fun <T : Any> T.asReference() = Ref(this)


class Ref<out T : Any> internal constructor(obj: T) {

    private val weakRef = WeakReference(obj)

    suspend operator fun invoke(): T {
        return suspendCoroutineOrReturn {
            val ref = weakRef.get() ?: throw CancellationException()
            ref
        }
    }
}
