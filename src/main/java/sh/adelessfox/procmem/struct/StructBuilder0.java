// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder0 {
    private final String name;

    private final StructDefinition layout;

    StructBuilder0(String name, StructDefinition layout) {
        this.name = name;
        this.layout = layout;
    }

    public static StructBuilder0 create(String name) {
        return new StructBuilder0(name, StructDefinition.empty(name));
    }

    public <F0> StructBuilder1<F0> field(String name, Type.Sized<F0> type) {
        var placement = layout.append(name, type);
        return new StructBuilder1<F0>(this.name, placement.layout(), placement.field());
    }

    public StructBuilder0 padding(long bytes) {
        return new StructBuilder0(this.name, layout.padding(bytes));
    }
}
