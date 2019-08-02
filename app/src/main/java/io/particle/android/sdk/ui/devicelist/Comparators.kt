/*
 * This code modified from from original source which was licensed to the
 * Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.particle.android.sdk.ui.devicelist

import java.util.ArrayList
import java.util.BitSet
import java.util.Comparator


internal object Comparators {

    val trueFirstComparator = BooleanComparator(true)


    internal class BooleanComparator(
        /** `true` iff `true` values sort before `false` values. */
        private val trueFirst: Boolean
    ) : Comparator<Boolean> {

        override fun compare(b1: Boolean?, b2: Boolean?): Int {
            val v1 = b1!!
            val v2 = b2!!

            return if (v1 xor v2) if (v1 xor trueFirst) 1 else -1 else 0
        }

        override fun hashCode(): Int {
            val hash = "BooleanComparator".hashCode()
            return if (trueFirst) -1 * hash else hash
        }

        override fun equals(other: Any?): Boolean {
            return this === other || other is BooleanComparator && this.trueFirst == other.trueFirst
        }

    }


    internal class NullComparator<E : Comparable<E>>(
        private val nullsComeFirst: Boolean
    ) : Comparator<E> {

        private val nonNullComparator: Comparator<in E> = ComparableComparator()

        override fun compare(o1: E?, o2: E?): Int {
            if (o1 === o2) {
                return 0
            }
            if (o1 == null) {
                return if (this.nullsComeFirst) 1 else -1
            }
            return if (o2 == null) {
                if (this.nullsComeFirst) -1 else 1
            } else {
                this.nonNullComparator.compare(o1, o2)
            }
        }

        override fun hashCode(): Int {
            return (if (nullsComeFirst) -1 else 1) * nonNullComparator.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) {
                return false
            }
            if (other === this) {
                return true
            }
            if (other.javaClass != this.javaClass) {
                return false
            }

            val autre = other as NullComparator<*>?

            return this.nullsComeFirst == autre!!.nullsComeFirst && this.nonNullComparator == autre.nonNullComparator
        }

    }


    private class ComparableComparator<T : Comparable<T>> : Comparator<T> {

        override fun compare(obj1: T, obj2: T): Int {
            return obj1.compareTo(obj2)
        }

        override fun hashCode(): Int {
            return "ComparableComparator".hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return this === other || null != other && other.javaClass == this.javaClass
        }
    }


    internal class ComparatorChain<E>(comparator: Comparator<E>, reverse: Boolean) : Comparator<E> {

        /**
         * The list of comparators in the chain.
         */
        private val comparatorChain: MutableList<Comparator<E>>?
        /**
         * Order - false (clear) = ascend; true (set) = descend.
         */
        private val orderingBits: BitSet?
        /**
         * Whether the chain has been "locked".
         */
        var isLocked = false
            private set


        init {
            comparatorChain = ArrayList(1)
            comparatorChain.add(comparator)
            orderingBits = BitSet(1)
            if (reverse) {
                orderingBits.set(0)
            }
        }

        @JvmOverloads
        fun addComparator(comparator: Comparator<E>, reverse: Boolean = false) {
            checkLocked()

            comparatorChain!!.add(comparator)
            if (reverse) {
                orderingBits!!.set(comparatorChain.size - 1)
            }
        }

        fun size(): Int {
            return comparatorChain!!.size
        }

        private fun checkLocked() {
            if (isLocked) {
                throw UnsupportedOperationException(
                    "Comparator ordering cannot be changed after the first comparison is performed"
                )
            }
        }

        private fun checkChainIntegrity() {
            if (comparatorChain!!.size == 0) {
                throw UnsupportedOperationException(
                    "ComparatorChains must contain at least one Comparator"
                )
            }
        }

        @Throws(UnsupportedOperationException::class)
        override fun compare(o1: E, o2: E): Int {
            if (!isLocked) {
                checkChainIntegrity()
                isLocked = true
            }

            // iterate over all comparators in the chain
            val comparators = comparatorChain!!.iterator()
            var comparatorIndex = 0
            while (comparators.hasNext()) {

                val comparator = comparators.next()
                var retval = comparator.compare(o1, o2)
                if (retval != 0) {
                    // invert the order if it is a reverse sort
                    if (orderingBits!!.get(comparatorIndex)) {
                        if (retval > 0) {
                            retval = -1
                        } else {
                            retval = 1
                        }
                    }
                    return retval
                }
                ++comparatorIndex
            }

            // if comparators are exhausted, return 0
            return 0
        }

        override fun hashCode(): Int {
            var hash = 0
            if (null != comparatorChain) {
                hash = hash xor comparatorChain.hashCode()
            }
            if (null != orderingBits) {
                hash = hash xor orderingBits.hashCode()
            }
            return hash
        }

        override fun equals(`object`: Any?): Boolean {
            if (this === `object`) {
                return true
            }
            if (null == `object`) {
                return false
            }
            if (`object`.javaClass == this.javaClass) {
                val chain = `object` as ComparatorChain<*>?
                return (if (null == orderingBits) null == chain!!.orderingBits else orderingBits == chain!!.orderingBits) && if (null == comparatorChain)
                    null == chain.comparatorChain
                else
                    comparatorChain == chain.comparatorChain
            }
            return false
        }

    }
}
