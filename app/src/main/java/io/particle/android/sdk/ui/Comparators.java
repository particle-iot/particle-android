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
package io.particle.android.sdk.ui;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


class Comparators {


    static class BooleanComparator implements Comparator<Boolean> {

        private static final BooleanComparator TRUE_FIRST = new BooleanComparator(true);

        /** <code>true</code> iff <code>true</code> values sort before <code>false</code> values. */
        private boolean trueFirst = false;

        static BooleanComparator getTrueFirstComparator() {
            return TRUE_FIRST;
        }

        BooleanComparator(final boolean trueFirst) {
            this.trueFirst = trueFirst;
        }

        @Override
        public int compare(final Boolean b1, final Boolean b2) {
            boolean v1 = b1.booleanValue();
            boolean v2 = b2.booleanValue();

            return (v1 ^ v2) ? ( (v1 ^ trueFirst) ? 1 : -1 ) : 0;
        }

        @Override
        public int hashCode() {
            final int hash = "BooleanComparator".hashCode();
            return trueFirst ? -1 * hash : hash;
        }

        @Override
        public boolean equals(final Object object) {
            return (this == object) ||
                    ((object instanceof BooleanComparator) &&
                            (this.trueFirst == ((BooleanComparator)object).trueFirst));
        }

    }


    @SuppressWarnings("rawtypes")
    private static final ComparableComparator COMP_COMP_INSTANCE = new ComparableComparator();


    static class NullComparator<E> implements Comparator<E> {

        // Specifies whether null is compared as higher than non-null.
        private final boolean nullsAreHigh;
        private final Comparator<? super E> nonNullComparator;

        public NullComparator(boolean nullsAreHigh) {
            this.nullsAreHigh = nullsAreHigh;
            //noinspection unchecked
            this.nonNullComparator = COMP_COMP_INSTANCE;
        }

        @Override
        public int compare(final E o1, final E o2) {
            if(o1 == o2) { return 0; }
            if(o1 == null) { return this.nullsAreHigh ? 1 : -1; }
            if(o2 == null) { return this.nullsAreHigh ? -1 : 1; }
            return this.nonNullComparator.compare(o1, o2);
        }

        @Override
        public int hashCode() {
            return (nullsAreHigh ? -1 : 1) * nonNullComparator.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if(obj == null) { return false; }
            if(obj == this) { return true; }
            if(!obj.getClass().equals(this.getClass())) { return false; }

            final NullComparator<?> other = (NullComparator<?>) obj;

            return this.nullsAreHigh == other.nullsAreHigh &&
                    this.nonNullComparator.equals(other.nonNullComparator);
        }

    }


    private static class ComparableComparator<T extends Comparable<? super T>> implements Comparator<T> {

        private ComparableComparator() {
            super();
        }

        @Override
        public int compare(T obj1, T obj2) {
            return obj1.compareTo(obj2);
        }

        @Override
        public int hashCode() {
            return "ComparableComparator".hashCode();
        }

        @Override
        public boolean equals(final Object object) {
            return this == object ||
                    null != object && object.getClass().equals(this.getClass());
        }
    }


    static class ComparatorChain<E> implements Comparator<E> {

        /** The list of comparators in the chain. */
        private final List<Comparator<E>> comparatorChain;
        /** Order - false (clear) = ascend; true (set) = descend. */
        private BitSet orderingBits = null;
        /** Whether the chain has been "locked". */
        private boolean isLocked = false;


        ComparatorChain(Comparator<E> comparator, boolean reverse) {
            comparatorChain = new ArrayList<>(1);
            comparatorChain.add(comparator);
            orderingBits = new BitSet(1);
            if (reverse == true) {
                orderingBits.set(0);
            }
        }

        void addComparator(Comparator<E> comparator) {
            addComparator(comparator, false);
        }

        void addComparator(Comparator<E> comparator, boolean reverse) {
            checkLocked();

            comparatorChain.add(comparator);
            if (reverse == true) {
                orderingBits.set(comparatorChain.size() - 1);
            }
        }

        int size() {
            return comparatorChain.size();
        }

        boolean isLocked() {
            return isLocked;
        }

        private void checkLocked() {
            if (isLocked == true) {
                throw new UnsupportedOperationException(
                        "Comparator ordering cannot be changed after the first comparison is performed");
            }
        }

        private void checkChainIntegrity() {
            if (comparatorChain.size() == 0) {
                throw new UnsupportedOperationException(
                        "ComparatorChains must contain at least one Comparator");
            }
        }

        @Override
        public int compare(E o1, E o2) throws UnsupportedOperationException {
            if (isLocked == false) {
                checkChainIntegrity();
                isLocked = true;
            }

            // iterate over all comparators in the chain
            final Iterator<Comparator<E>> comparators = comparatorChain.iterator();
            for (int comparatorIndex = 0; comparators.hasNext(); ++comparatorIndex) {

                final Comparator<? super E> comparator = comparators.next();
                int retval = comparator.compare(o1,o2);
                if (retval != 0) {
                    // invert the order if it is a reverse sort
                    if (orderingBits.get(comparatorIndex) == true) {
                        if (retval > 0) {
                            retval = -1;
                        } else {
                            retval = 1;
                        }
                    }
                    return retval;
                }
            }

            // if comparators are exhausted, return 0
            return 0;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (null != comparatorChain) {
                hash ^= comparatorChain.hashCode();
            }
            if (null != orderingBits) {
                hash ^= orderingBits.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (null == object) {
                return false;
            }
            if (object.getClass().equals(this.getClass())) {
                final ComparatorChain<?> chain = (ComparatorChain<?>) object;
                return (null == orderingBits ? null == chain.orderingBits : orderingBits.equals(chain.orderingBits)) &&
                        (null == comparatorChain ? null == chain.comparatorChain :
                                comparatorChain.equals(chain.comparatorChain));
            }
            return false;
        }

    }
}
