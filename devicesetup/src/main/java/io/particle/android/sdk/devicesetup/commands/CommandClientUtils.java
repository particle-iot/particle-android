// This file is comprised entirely of code (C) the ASF, but modified
// to fit into a single file, for simplicity

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

package io.particle.android.sdk.devicesetup.commands;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by jensck from code lifted from Apache commons-lang, to avoid pulling in a 4k+ method
 * count dependency just for a few String methods
 */
class CommandClientUtils {

    public static String escapeJava(final String input) {
        return ESCAPE_JAVA.translate(input);
    }

    // Everything beneath this comment line is all just to feed the `escapeJava()` method above.


    private static String[][] JAVA_CTRL_CHARS_ESCAPE() { return JAVA_CTRL_CHARS_ESCAPE.clone(); }
    private static final String[][] JAVA_CTRL_CHARS_ESCAPE = {
            {"\b", "\\b"},
            {"\n", "\\n"},
            {"\t", "\\t"},
            {"\f", "\\f"},
            {"\r", "\\r"}
    };

    private static final CharSequenceTranslator ESCAPE_JAVA =
            new LookupTranslator(
                    new String[][] {
                            {"\"", "\\\""},
                            {"\\", "\\\\"},
                    }).with(
                    new LookupTranslator(JAVA_CTRL_CHARS_ESCAPE())
            ).with(
                    JavaUnicodeEscaper.outsideOf(32, 0x7f)
            );



    private abstract static class CharSequenceTranslator {

        static final char[] HEX_DIGITS = new char[] {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

        /**
         * Translate a set of codepoints, represented by an int index into a CharSequence,
         * into another set of codepoints. The number of codepoints consumed must be returned,
         * and the only IOExceptions thrown must be from interacting with the Writer so that
         * the top level API may reliably ignore StringWriter IOExceptions.
         *
         * @param input CharSequence that is being translated
         * @param index int representing the current point of translation
         * @param out Writer to translate the text to
         * @return int count of codepoints consumed
         * @throws IOException if and only if the Writer produces an IOException
         */
        abstract int translate(CharSequence input, int index, Writer out) throws IOException;

        /**
         * Helper for non-Writer usage.
         * @param input CharSequence to be translated
         * @return String output of translation
         */
        private String translate(final CharSequence input) {
            if (input == null) {
                return null;
            }
            try {
                final StringWriter writer = new StringWriter(input.length() * 2);
                translate(input, writer);
                return writer.toString();
            } catch (final IOException ioe) {
                // this should never ever happen while writing to a StringWriter
                throw new RuntimeException(ioe);
            }
        }

        /**
         * Translate an input onto a Writer. This is intentionally final as its algorithm is
         * tightly coupled with the abstract method of this class.
         *
         * @param input CharSequence that is being translated
         * @param out Writer to translate the text to
         * @throws IOException if and only if the Writer produces an IOException
         */
        private void translate(final CharSequence input, final Writer out) throws IOException {
            if (out == null) {
                throw new IllegalArgumentException("The Writer must not be null");
            }
            if (input == null) {
                return;
            }
            int pos = 0;
            final int len = input.length();
            while (pos < len) {
                final int consumed = translate(input, pos, out);
                if (consumed == 0) {
                    // inlined implementation of Character.toChars(Character.codePointAt(input, pos))
                    // avoids allocating temp char arrays and duplicate checks
                    char c1 = input.charAt(pos);
                    out.write(c1);
                    pos++;
                    if (Character.isHighSurrogate(c1) && pos < len) {
                        char c2 = input.charAt(pos);
                        if (Character.isLowSurrogate(c2)) {
                            out.write(c2);
                            pos++;
                        }
                    }
                    continue;
                }
                // contract with translators is that they have to understand codepoints
                // and they just took care of a surrogate pair
                for (int pt = 0; pt < consumed; pt++) {
                    pos += Character.charCount(Character.codePointAt(input, pos));
                }
            }
        }

        /**
         * Helper method to create a merger of this translator with another set of
         * translators. Useful in customizing the standard functionality.
         *
         * @param translators CharSequenceTranslator array of translators to merge with this one
         * @return CharSequenceTranslator merging this translator with the others
         */
        CharSequenceTranslator with(final CharSequenceTranslator... translators) {
            final CharSequenceTranslator[] newArray = new CharSequenceTranslator[translators.length + 1];
            newArray[0] = this;
            System.arraycopy(translators, 0, newArray, 1, translators.length);
            return new AggregateTranslator(newArray);
        }

        /**
         * <p>Returns an upper case hexadecimal <code>String</code> for the given
         * character.</p>
         *
         * @param codepoint The codepoint to convert.
         * @return An upper case hexadecimal <code>String</code>
         */
        static String hex(final int codepoint) {
            return Integer.toHexString(codepoint).toUpperCase(Locale.ENGLISH);
        }

    }


    private static class AggregateTranslator extends CharSequenceTranslator {

        private CharSequenceTranslator[] translators;

        /**
         * Specify the translators to be used at creation time.
         *
         * @param translators CharSequenceTranslator array to aggregate
         */
        AggregateTranslator(final CharSequenceTranslator... translators) {
            this.translators = translators.clone();
        }

        /**
         * The first translator to consume codepoints from the input is the 'winner'.
         * Execution stops with the number of consumed codepoints being returned.
         * {@inheritDoc}
         */
        @Override
        int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            for (final CharSequenceTranslator translator : translators) {
                final int consumed = translator.translate(input, index, out);
                if(consumed != 0) {
                    return consumed;
                }
            }
            return 0;
        }

    }

    static abstract class CodePointTranslator extends CharSequenceTranslator {

        /**
         * Implementation of translate that maps onto the abstract translate(int, Writer) method.
         * {@inheritDoc}
         */
        @Override
        int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            final int codepoint = Character.codePointAt(input, index);
            final boolean consumed = translate(codepoint, out);
            return consumed ? 1 : 0;
        }

        /**
         * Translate the specified codepoint into another.
         *
         * @param codepoint int character input to translate
         * @param out Writer to optionally push the translated output to
         * @return boolean as to whether translation occurred or not
         * @throws IOException if and only if the Writer produces an IOException
         */
        abstract boolean translate(int codepoint, Writer out) throws IOException;

    }


    static class UnicodeEscaper extends CodePointTranslator {

        private int below;
        private int above;
        private boolean between;

        /**
         * <p>Constructs a <code>UnicodeEscaper</code> for all characters. </p>
         */
        private UnicodeEscaper(){
            this(0, Integer.MAX_VALUE, true);
        }

        /**
         * <p>Constructs a <code>UnicodeEscaper</code> for the specified range. This is
         * the underlying method for the other constructors/builders. The <code>below</code>
         * and <code>above</code> boundaries are inclusive when <code>between</code> is
         * <code>true</code> and exclusive when it is <code>false</code>. </p>
         *
         * @param below int value representing the lowest codepoint boundary
         * @param above int value representing the highest codepoint boundary
         * @param between whether to escape between the boundaries or outside them
         */
        UnicodeEscaper(final int below, final int above, final boolean between) {
            this.below = below;
            this.above = above;
            this.between = between;
        }

        /**
         * <p>Constructs a <code>UnicodeEscaper</code> below the specified value (exclusive). </p>
         *
         * @param codepoint below which to escape
         * @return the newly created {@code UnicodeEscaper} instance
         */
        private static UnicodeEscaper below(final int codepoint) {
            return outsideOf(codepoint, Integer.MAX_VALUE);
        }

        /**
         * <p>Constructs a <code>UnicodeEscaper</code> above the specified value (exclusive). </p>
         *
         * @param codepoint above which to escape
         * @return the newly created {@code UnicodeEscaper} instance
         */
        private static UnicodeEscaper above(final int codepoint) {
            return outsideOf(0, codepoint);
        }

        /**
         * <p>Constructs a <code>UnicodeEscaper</code> outside of the specified values (exclusive). </p>
         *
         * @param codepointLow below which to escape
         * @param codepointHigh above which to escape
         * @return the newly created {@code UnicodeEscaper} instance
         */
        private static UnicodeEscaper outsideOf(final int codepointLow, final int codepointHigh) {
            return new UnicodeEscaper(codepointLow, codepointHigh, false);
        }

        /**
         * <p>Constructs a <code>UnicodeEscaper</code> between the specified values (inclusive). </p>
         *
         * @param codepointLow above which to escape
         * @param codepointHigh below which to escape
         * @return the newly created {@code UnicodeEscaper} instance
         */
        private static UnicodeEscaper between(final int codepointLow, final int codepointHigh) {
            return new UnicodeEscaper(codepointLow, codepointHigh, true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean translate(final int codepoint, final Writer out) throws IOException {
            if (between) {
                if (codepoint < below || codepoint > above) {
                    return false;
                }
            } else {
                if (codepoint >= below && codepoint <= above) {
                    return false;
                }
            }

            // TODO: Handle potential + sign per various Unicode escape implementations
            if (codepoint > 0xffff) {
                out.write(toUtf16Escape(codepoint));
            } else {
                out.write("\\u");
                out.write(HEX_DIGITS[(codepoint >> 12) & 15]);
                out.write(HEX_DIGITS[(codepoint >> 8) & 15]);
                out.write(HEX_DIGITS[(codepoint >> 4) & 15]);
                out.write(HEX_DIGITS[(codepoint) & 15]);
            }
            return true;
        }

        /**
         * Converts the given codepoint to a hex string of the form {@code "\\uXXXX"}
         *
         * @param codepoint
         *            a Unicode code point
         * @return the hex string for the given codepoint
         *
         * @since 3.2
         */
        protected String toUtf16Escape(final int codepoint) {
            return "\\u" + hex(codepoint);
        }
    }


    private static class JavaUnicodeEscaper extends UnicodeEscaper {

        /**
         * <p>
         * Constructs a <code>JavaUnicodeEscaper</code> above the specified value (exclusive).
         * </p>
         *
         * @param codepoint
         *            above which to escape
         * @return the newly created {@code UnicodeEscaper} instance
         */
        private static JavaUnicodeEscaper above(final int codepoint) {
            return outsideOf(0, codepoint);
        }

        /**
         * <p>
         * Constructs a <code>JavaUnicodeEscaper</code> below the specified value (exclusive).
         * </p>
         *
         * @param codepoint
         *            below which to escape
         * @return the newly created {@code UnicodeEscaper} instance
         */
        private static JavaUnicodeEscaper below(final int codepoint) {
            return outsideOf(codepoint, Integer.MAX_VALUE);
        }

        /**
         * <p>
         * Constructs a <code>JavaUnicodeEscaper</code> between the specified values (inclusive).
         * </p>
         *
         * @param codepointLow
         *            above which to escape
         * @param codepointHigh
         *            below which to escape
         * @return the newly created {@code UnicodeEscaper} instance
         */
        private static JavaUnicodeEscaper between(final int codepointLow, final int codepointHigh) {
            return new JavaUnicodeEscaper(codepointLow, codepointHigh, true);
        }

        /**
         * <p>
         * Constructs a <code>JavaUnicodeEscaper</code> outside of the specified values (exclusive).
         * </p>
         *
         * @param codepointLow
         *            below which to escape
         * @param codepointHigh
         *            above which to escape
         * @return the newly created {@code UnicodeEscaper} instance
         */
        private static JavaUnicodeEscaper outsideOf(final int codepointLow, final int codepointHigh) {
            return new JavaUnicodeEscaper(codepointLow, codepointHigh, false);
        }

        /**
         * <p>
         * Constructs a <code>JavaUnicodeEscaper</code> for the specified range. This is the underlying method for the
         * other constructors/builders. The <code>below</code> and <code>above</code> boundaries are inclusive when
         * <code>between</code> is <code>true</code> and exclusive when it is <code>false</code>.
         * </p>
         *
         * @param below
         *            int value representing the lowest codepoint boundary
         * @param above
         *            int value representing the highest codepoint boundary
         * @param between
         *            whether to escape between the boundaries or outside them
         */
        private JavaUnicodeEscaper(final int below, final int above, final boolean between) {
            super(below, above, between);
        }

        /**
         * Converts the given codepoint to a hex string of the form {@code "\\uXXXX\\uXXXX"}
         *
         * @param codepoint
         *            a Unicode code point
         * @return the hex string for the given codepoint
         */
        @Override
        protected String toUtf16Escape(final int codepoint) {
            final char[] surrogatePair = Character.toChars(codepoint);
            return "\\u" + hex(surrogatePair[0]) + "\\u" + hex(surrogatePair[1]);
        }

    }


    static class LookupTranslator extends CharSequenceTranslator {

        private HashMap<String, String> lookupMap;
        private HashSet<Character> prefixSet;
        private int shortest;
        private int longest;

        /**
         * Define the lookup table to be used in translation
         *
         * Note that, as of Lang 3.1, the key to the lookup table is converted to a
         * java.lang.String. This is because we need the key to support hashCode and
         * equals(Object), allowing it to be the key for a HashMap. See LANG-882.
         *
         * @param lookup CharSequence[][] table of size [*][2]
         */
        private LookupTranslator(final CharSequence[]... lookup) {
            lookupMap = new HashMap<>();
            prefixSet = new HashSet<>();
            int _shortest = Integer.MAX_VALUE;
            int _longest = 0;
            if (lookup != null) {
                for (final CharSequence[] seq : lookup) {
                    this.lookupMap.put(seq[0].toString(), seq[1].toString());
                    this.prefixSet.add(seq[0].charAt(0));
                    final int sz = seq[0].length();
                    if (sz < _shortest) {
                        _shortest = sz;
                    }
                    if (sz > _longest) {
                        _longest = sz;
                    }
                }
            }
            shortest = _shortest;
            longest = _longest;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            // check if translation exists for the input at position index
            if (prefixSet.contains(input.charAt(index))) {
                int max = longest;
                if (index + longest > input.length()) {
                    max = input.length() - index;
                }
                // implement greedy algorithm by trying maximum match first
                for (int i = max; i >= shortest; i--) {
                    final CharSequence subSeq = input.subSequence(index, index + i);
                    final String result = lookupMap.get(subSeq.toString());

                    if (result != null) {
                        out.write(result);
                        return i;
                    }
                }
            }
            return 0;
        }
    }

}
