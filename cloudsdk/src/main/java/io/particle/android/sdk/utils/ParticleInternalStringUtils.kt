
// Contents lifted from StringUtils.java in Apache commons-lang

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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
package io.particle.android.sdk.utils


fun join(iterable: Iterable<*>?, separator: Char): String? {
    return if (iterable == null) {
        null
    } else join(iterable.iterator(), separator)
}


fun join(iterator: Iterator<*>?, separator: Char): String? {

    // handle null, zero and one elements before building a buffer
    if (iterator == null) {
        return null
    }
    if (!iterator.hasNext()) {
        return ""
    }
    val first = iterator.next()
    if (!iterator.hasNext()) {
        return objToString(first)
    }

    // two or more elements
    val buf = StringBuilder(256) // Java default is 16, probably too small
    if (first != null) {
        buf.append(first)
    }

    while (iterator.hasNext()) {
        buf.append(separator)
        val obj = iterator.next()
        if (obj != null) {
            buf.append(obj)
        }
    }

    return buf.toString()
}

private fun objToString(obj: Any?): String {
    return obj?.toString() ?: ""
}
