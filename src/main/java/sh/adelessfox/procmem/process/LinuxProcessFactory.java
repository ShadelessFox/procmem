// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

final class LinuxProcessFactory implements ProcessFactory {
    @Override
    public Process open(long pid) {
        return LinuxProcess.of(pid);
    }
}
