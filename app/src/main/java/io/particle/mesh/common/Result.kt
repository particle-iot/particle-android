package io.particle.mesh.common


sealed class Result<out V, out E> {

    abstract val value: V?
    abstract val error: E?

    data class Present<out V, out E>(
            override val value: V
    ) : Result<V, E>() {

        override val error: E? = null
    }

    data class Error<out V, out E>(
            override val error: E
    ) : Result<V, E>() {

        override val value: V? = null
    }

    data class Absent<out V, out E>(
            // data classes *require* that they have at least one unique param, so
            // this fulfills that contract
            private val _ignore_do_not_use_: Int = 0
    ) : Result<V, E>() {

        override val value: V? = null
        override val error: E? = null
    }

}
