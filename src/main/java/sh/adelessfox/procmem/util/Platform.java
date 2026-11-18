// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.util;

/**
 * Represents the current platform.
 */
public enum Platform {
    WINDOWS,
    LINUX,
    MACOS,
    UNSUPPORTED;

    /**
     * Returns the current platform.
     *
     * @return the current platform, either {@code WINDOWS}, {@code LINUX}, {@code MACOS},
     * or {@code UNSUPPORTED} if the platform is not recognized
     */
    public static Platform current() {
        var name = System.getProperty("os.name").toLowerCase();
        if (name.startsWith("win")) {
            return WINDOWS;
        } else if (name.startsWith("linux")) {
            return LINUX;
        } else if (name.startsWith("mac")) {
            return MACOS;
        } else {
            return UNSUPPORTED;
        }
    }
}
