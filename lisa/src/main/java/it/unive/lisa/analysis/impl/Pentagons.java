package it.unive.lisa.analysis.impl;


import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

import java.util.Collection;

public class Pentagons implements ValueDomain<Pentagons>{

    private Interval intv;
    private StrictUpperBound sub;
    boolean isTOP;
    boolean isBOTTOM;

    public Pentagons(){ this(null, null); }

    protected Pentagons(Interval intv, StrictUpperBound sub) {
        this.intv = intv;
        this.sub = sub;
        this.isTOP = false;
        this.isBOTTOM = false;
    }

    private Pentagons(Interval intv, StrictUpperBound sub, boolean isTOP, boolean isBOTTOM) {
        this.intv = intv;
        this.sub = sub;
        this.isTOP = isTOP;
        this.isBOTTOM = isBOTTOM;
    }


    @Override
    public Pentagons top() {

        return new Pentagons(null,null, true, false);
    }

    @Override
    public Pentagons bottom() {
        return new Pentagons(null,null, false, true);
    }

    @Override
    public boolean isTop() {
        return sub.isTop() && intv.isTop();
    }

    @Override
    public boolean isBottom() {
        return sub.isBottom() && intv.isBottom();
    }

    @Override
    public Pentagons lub(Pentagons other) throws SemanticException {
        return new Pentagons(this.intv.lub(other.intv), this.sub.lub(other.sub));
    }

    @Override
    public Pentagons widening(Pentagons other) throws SemanticException {
        return new Pentagons(this.intv.widening(other.intv), this.sub.widening(other.sub));
    }

    @Override
    public boolean lessOrEqual(Pentagons other) throws SemanticException {
        return this.intv.lessOrEqual(other.intv) &&  this.sub.lessOrEqual(other.sub); // da cambiare
    }

    @Override
    public Pentagons assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return null;

    }

    @Override
    public Pentagons smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return null;
    }

    @Override
    public Pentagons assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return null;
        /*
         x ->( intv = [1,4] )
         y ->( inv=[2,3])
         if x < y
         x ->( intv = [1,4], sub={y} )
         y ->( inv=[2,3])
         */

    }

    @Override
    public Pentagons forgetIdentifier(Identifier id) throws SemanticException {
        return null;
    }

    @Override
    public Pentagons forgetIdentifiers(Collection<Identifier> ids) throws SemanticException {
        return null;
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return null;
    }

    @Override
    public String representation() {
        if (isTop())
            return Lattice.TOP_STRING;
        else if (isBottom())
            return Lattice.BOTTOM_STRING;
        return "" + intv.representation() + ", {" + sub.representation() +"} " ;
    }
}
