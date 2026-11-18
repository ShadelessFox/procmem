// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.util.Platform;

sealed interface ProcessFactory
    permits Win32ProcessFactory, LinuxProcessFactory {

    static ProcessFactory get() {
        return switch (Platform.current()) {
            case WINDOWS -> new Win32ProcessFactory();
            case LINUX -> new LinuxProcessFactory();
            default -> throw new UnsupportedOperationException("Unsupported platform");
        };
    }

    Process open(long pid);
}
