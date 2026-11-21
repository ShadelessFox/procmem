// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import java.nio.file.Path;

/**
 * Represents a module loaded in a process.
 *
 * @param name the name of the module
 * @param path the path to the module on disk
 * @param base the base address of the module in memory, in bytes
 * @param size the size of the module in memory, in bytes
 * @see Process#modules()
 */
public record Module(String name, Path path, long base, long size) {
}
