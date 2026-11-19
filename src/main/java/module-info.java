// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: Copyright © 2026 ShadelessFox

/**
 * This module provides facilities for reading and writing process memory.
 */
module sh.adelessfox.procmem {
    requires static org.jetbrains.annotations; // Required for org.jetbrains.annotations.Debug.Renderer
    requires static java.compiler; // Required for sh.adelessfox.procmem.struct.GenerateStructBuilders

    exports sh.adelessfox.procmem.process;
    exports sh.adelessfox.procmem.struct;
    exports sh.adelessfox.procmem.util;
    exports sh.adelessfox.procmem;
}
