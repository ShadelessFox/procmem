// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

/**
 * Describes the layout of a type in memory.
 *
 * @param size      the size of the type in bytes
 * @param alignment the alignment of the type in bytes, must be a power of 2
 */
public record TypeLayout(long size, long alignment) {
    public TypeLayout {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        if (alignment <= 0) {
            throw new IllegalArgumentException("Alignment must be positive");
        }
        if ((alignment & alignment - 1) != 0) {
            throw new IllegalArgumentException("Alignment must be a power of 2");
        }
    }
}