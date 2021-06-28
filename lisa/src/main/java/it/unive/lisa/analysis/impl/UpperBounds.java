package it.unive.lisa.analysis.impl;
/*
    Coded by Eleonora Garbin 869831, Zonelli Mattia 870038.
*/


import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.symbolic.value.Identifier;

import java.util.HashSet;
import java.util.Set;



public class UpperBounds extends InverseSetLattice<UpperBounds, Identifier> {


    private final boolean isTop;
    private final boolean isBottom;

    /**
     * Builds the lattice, with a predefined set
     *
     */
    public UpperBounds(Set<Identifier> elements) {
        this(elements, false, false);
    }

    /**
     * Build the lattice in case we want to set the field @isTop or @isBottom.
     */
    public UpperBounds(Set<Identifier> elements, boolean isTop, boolean isBottom) {
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
        return new UpperBounds(elements, false, true);
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
    protected UpperBounds mk(Set<Identifier> set) {
        return new UpperBounds(set);
    }
}