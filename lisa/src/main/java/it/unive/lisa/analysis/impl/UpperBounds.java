package it.unive.lisa.analysis.impl;

import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.symbolic.value.Identifier;

import java.util.HashSet;
import java.util.Set;

public class UpperBounds extends InverseSetLattice<UpperBounds, String> {


    private final boolean isTop;
    private final boolean isBottom;

    /**
     * Builds the lattice.
     *

     */
    public UpperBounds(Set<String> elements) {
        this(elements, false, false);
    }

    private UpperBounds(Set<String> elements, boolean isTop, boolean isBottom) {
        super(elements);
        this.isTop = isTop;
        this.isBottom = isBottom;
    }

    @Override
    public UpperBounds top() {
        return new UpperBounds(new HashSet<>(), true, false);
    }

    @Override
    public UpperBounds bottom() {
        return new UpperBounds(new HashSet<>(), false, true);
    }

    @Override
    public boolean isTop() {
        return elements.isEmpty() && isTop;
    }

    @Override
    public boolean isBottom() {
        return isBottom;
    }

    @Override
    protected UpperBounds mk(Set<String> set) {
        return new UpperBounds(set);
    }
}