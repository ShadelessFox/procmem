// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import java.nio.ByteOrder;

/**
 * Describes layout properties for a view of memory.
 *
 * @param pointerSize the size of a pointer in bytes (4 or 8)
 * @param byteOrder   the byte order of multibyte values in memory
 */
public record MemoryLayout(int pointerSize, ByteOrder byteOrder) {
    /** Memory layout for 64-bit AMD64 architecture. */
    public static final MemoryLayout AMD64 = new MemoryLayout(8, ByteOrder.LITTLE_ENDIAN);

    public MemoryLayout {
        if (pointerSize != 4 && pointerSize != 8) {
            throw new IllegalArgumentException("Unsupported pointer size: " + pointerSize);
        }
    }
}