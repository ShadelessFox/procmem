// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.TypeLayout;

import java.util.List;

/**
 * Describes the layout of a structure for a particular target.
 */
public final class StructLayout {
    private final StructDefinition definition;
    private final TypeLayout layout;
    private final List<Member> members;

    StructLayout(StructDefinition definition, TypeLayout layout, List<Member> members) {
        this.definition = definition;
        this.layout = layout;
        this.members = List.copyOf(members);
    }

    public TypeLayout layout() {
        return layout;
    }

    public List<Member> members() {
        return members;
    }

    /**
     * Returns the field's byte offset, rejecting handles absent from this definition.
     */
    public long offset(StructMember.Field<?> field) {
        int index = field.index();
        if (index < 0 || index >= members.size() || members.get(index).member() != field) {
            throw new IllegalArgumentException("Field does not belong to structure " + definition.name());
        }
        return members.get(index).offset();
    }


    public record Member(StructMember member, long offset, TypeLayout layout) {
    }
}
