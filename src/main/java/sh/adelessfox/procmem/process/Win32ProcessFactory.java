// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

final class Win32ProcessFactory implements ProcessFactory {
    @Override
    public Process open(long pid) {
        return Win32Process.open(pid);
    }
}
