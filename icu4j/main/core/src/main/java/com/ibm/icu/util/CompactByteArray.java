// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;
import com.ibm.icu.impl.Utility;

/**
 * class CompactATypeArray : use only on primitive data types
 * Provides a compact way to store information that is indexed by Unicode
 * values, such as character properties, types, keyboard values, etc.This
 * is very useful when you have a block of Unicode data that contains
 * significant values while the rest of the Unicode data is unused in the
 * application or when you have a lot of redundance, such as where all 21,000
 * Han ideographs have the same value.  However, lookup is much faster than a
 * hash table.
 * A compact array of any primitive data type serves two purposes:
 * <UL>
 *     <LI>Fast access of the indexed values.
 *     <LI>Smaller memory footprint.
 * </UL>
 * A compact array is composed of a index array and value array.  The index
 * array contains the indices of Unicode characters to the value array.
 *
 * @see                CompactCharArray
 * @author             Helena Shih
 * @internal
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public final class CompactByteArray implements Cloneable {

    /**
     * The total number of Unicode characters.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static  final int UNICODECOUNT =65536;

    /**
     * Default constructor for CompactByteArray, the default value of the
     * compact array is 0.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public CompactByteArray()
    {
        this((byte)0);
    }

    /**
     * Constructor for CompactByteArray.
     * @param defaultValue the default value of the compact array.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public CompactByteArray(byte defaultValue)
    {
        int i;
        values = new byte[UNICODECOUNT];
        indices = new char[INDEXCOUNT];
        hashes = new int[INDEXCOUNT];
        for (i = 0; i < UNICODECOUNT; ++i) {
            values[i] = defaultValue;
        }
        for (i = 0; i < INDEXCOUNT; ++i) {
            indices[i] = (char)(i<<BLOCKSHIFT);
            hashes[i] = 0;
        }
        isCompact = false;

        this.defaultValue = defaultValue;
    }

    /**
     * Constructor for CompactByteArray.
     * @param indexArray the indices of the compact array.
     * @param newValues the values of the compact array.
     * @exception IllegalArgumentException If the index is out of range.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public CompactByteArray(char indexArray[],
                            byte newValues[])
    {
        int i;
        if (indexArray.length != INDEXCOUNT)
            throw new IllegalArgumentException("Index out of bounds.");
        for (i = 0; i < INDEXCOUNT; ++i) {
            char index = indexArray[i];
            if (index >= newValues.length+BLOCKCOUNT)
                throw new IllegalArgumentException("Index out of bounds.");
        }
        indices = indexArray;
        values = newValues;
        isCompact = true;
    }

    /**
     * Constructor for CompactByteArray.
     *
     * @param indexArray the RLE-encoded indices of the compact array.
     * @param valueArray the RLE-encoded values of the compact array.
     *
     * @throws IllegalArgumentException if the index or value array is
     *          the wrong size.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public CompactByteArray(String indexArray,
                            String valueArray)
    {
        this( Utility.RLEStringToCharArray(indexArray),
              Utility.RLEStringToByteArray(valueArray));
    }

    /**
     * Get the mapped value of a Unicode character.
     * @param index the character to get the mapped value with
     * @return the mapped value of the given character
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public byte elementAt(char index)
    {
        return (values[(indices[index >> BLOCKSHIFT] & 0xFFFF)
                       + (index & BLOCKMASK)]);
    }

    /**
     * Set a new value for a Unicode character.
     * Set automatically expands the array if it is compacted.
     * @param index the character to set the mapped value with
     * @param value the new mapped value
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void setElementAt(char index, byte value)
    {
        if (isCompact)
            expand();
        values[index] = value;
        touchBlock(index >> BLOCKSHIFT, value);
    }

    /**
     * Set new values for a range of Unicode character.
     *
     * @param start the starting offset of the range
     * @param end the ending offset of the range
     * @param value the new mapped value
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void setElementAt(char start, char end, byte value)
    {
        int i;
        if (isCompact) {
            expand();
        }
        for (i = start; i <= end; ++i) {
            values[i] = value;
            touchBlock(i >> BLOCKSHIFT, value);
        }
    }
    /**
     * Compact the array.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void compact() {
        compact(false);
    }

    /**
     * Compact the array.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void compact(boolean exhaustive)
    {
        if (!isCompact) {
            int limitCompacted = 0;
            int iBlockStart = 0;
            char iUntouched = 0xFFFF;

            for (int i = 0; i < indices.length; ++i, iBlockStart += BLOCKCOUNT) {
                indices[i] = 0xFFFF;
                boolean touched = blockTouched(i);
                if (!touched && iUntouched != 0xFFFF) {
                    // If no values in this block were set, we can just set its
                    // index to be the same as some other block with no values
                    // set, assuming we've seen one yet.
                    indices[i] = iUntouched;
                } else {
                    int jBlockStart = 0;
                    int j = 0;
                    for (j = 0; j < limitCompacted;
                            ++j, jBlockStart += BLOCKCOUNT) {
                        if (hashes[i] == hashes[j] &&
                                arrayRegionMatches(values, iBlockStart,
                                values, jBlockStart, BLOCKCOUNT)) {
                            indices[i] = (char)jBlockStart;
                            break;
                        }
                    }
                    if (indices[i] == 0xFFFF) {
                        // we didn't match, so copy & update
                        System.arraycopy(values, iBlockStart,
                            values, jBlockStart, BLOCKCOUNT);
                        indices[i] = (char)jBlockStart;
                        hashes[j] = hashes[i];
                        ++limitCompacted;

                        if (!touched) {
                            // If this is the first untouched block we've seen,
                            // remember its index.
                            iUntouched = (char)jBlockStart;
                        }
                    }
                }
            }
            // we are done compacting, so now make the array shorter
            int newSize = limitCompacted*BLOCKCOUNT;
            byte[] result = new byte[newSize];
            System.arraycopy(values, 0, result, 0, newSize);
            values = result;
            isCompact = true;
            hashes = null;
        }
    }

    /**
     * Convenience utility to compare two arrays of doubles.
     * @param len the length to compare.
     * The start indices and start+len must be valid.
     */
    final static boolean arrayRegionMatches(byte[] source, int sourceStart,
                                            byte[] target, int targetStart,
                                            int len)
    {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (source[i] != target[i + delta])
            return false;
        }
        return true;
    }

    /**
     * Remember that a specified block was "touched", i.e. had a value set.
     * Untouched blocks can be skipped when compacting the array
     */
    private final void touchBlock(int i, int value) {
        hashes[i] = (hashes[i] + (value<<1)) | 1;
    }

    /**
     * Query whether a specified block was "touched", i.e. had a value set.
     * Untouched blocks can be skipped when compacting the array
     */
    private final boolean blockTouched(int i) {
        return hashes[i] != 0;
    }

    /**
     * For internal use only.  Do not modify the result, the behavior of
     * modified results are undefined.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public char[] getIndexArray()
    {
        return indices;
    }

    /**
     * For internal use only.  Do not modify the result, the behavior of
     * modified results are undefined.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public byte[] getValueArray()
    {
        return values;
    }

    /**
     * Overrides Cloneable
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public CompactByteArray clone()
    {
        try {
            CompactByteArray other = (CompactByteArray) super.clone();
            other.values = values.clone();
            other.indices = indices.clone();
            if (hashes != null) other.hashes = hashes.clone();
            return other;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    /**
     * Compares the equality of two compact array objects.
     * @param obj the compact array object to be compared with this.
     * @return true if the current compact array object is the same
     * as the compact array object obj; false otherwise.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj)                      // quick check
            return true;
        if (getClass() != obj.getClass())         // same class?
            return false;
        CompactByteArray other = (CompactByteArray) obj;
        for (int i = 0; i < UNICODECOUNT; i++) {
            // could be sped up later
            if (elementAt((char)i) != other.elementAt((char)i))
                return false;
        }
        return true; // we made it through the guantlet.
    }

    /**
     * Generates the hash code for the compact array object
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public int hashCode() {
        int result = 0;
        int increment = Math.min(3, values.length/16);
        for (int i = 0; i < values.length; i+= increment) {
            result = result * 37 + values[i];
        }
        return result;
    }

    // --------------------------------------------------------------
    // private
    // --------------------------------------------------------------

    /**
     * Expanding takes the array back to a 65536 element array.
     */
    private void expand()
    {
        int i;
        if (isCompact) {
            byte[]  tempArray;
            hashes = new int[INDEXCOUNT];
            tempArray = new byte[UNICODECOUNT];
            for (i = 0; i < UNICODECOUNT; ++i) {
                byte value = elementAt((char)i);
                tempArray[i] = value;
                touchBlock(i >> BLOCKSHIFT, value);
            }
            for (i = 0; i < INDEXCOUNT; ++i) {
                indices[i] = (char)(i<<BLOCKSHIFT);
            }
            values = null;
            values = tempArray;
            isCompact = false;
        }
    }

    private static  final int BLOCKSHIFT =7;
    private static  final int BLOCKCOUNT =(1<<BLOCKSHIFT);
    private static  final int INDEXSHIFT =(16-BLOCKSHIFT);
    private static  final int INDEXCOUNT =(1<<INDEXSHIFT);
    private static  final int BLOCKMASK = BLOCKCOUNT - 1;

    private byte[] values;
    private char indices[];
    private int[] hashes;
    private boolean isCompact;
    byte defaultValue;
}
