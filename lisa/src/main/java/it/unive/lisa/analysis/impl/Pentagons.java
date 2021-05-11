package it.unive.lisa.analysis.impl;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

import java.util.Collection;

public class Pentagons implements ValueDomain<Pentagons> {
    /*
        Or fare una variabile Map<String, Interval>
     */
    private ValueEnvironment<Interval> intv;
    private StrictUpperBound sub;
    boolean isTOP;
    boolean isBOTTOM;

    public Pentagons(){ this(null, null); }

    protected Pentagons(ValueEnvironment<Interval> intv, StrictUpperBound sub) {
        this.intv = intv;
        this.sub = sub;
        this.isTOP = false;
        this.isBOTTOM = false;
    }

    private Pentagons(ValueEnvironment<Interval> intv, StrictUpperBound sub, boolean isTOP, boolean isBOTTOM) {
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
        // per ogni variabile mappata fare la lub tra this.intv and other.intv , idem per sub
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
        System.out.println("TEST: "  + ", " + pp);
        return this;
        /*
          x = 4
          map x-> [intv = [4,4] , sub = {T}]
         */

    }
    // metodo refine per modificare gli intv in base ai sub, con le glb

    @Override
    public Pentagons smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return this;
    }

    @Override
    public Pentagons assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return this;
        /*
         x ->( intv = [1,4] )
         y ->( inv=[2,3])
         if x < y
         x ->( intv = [1,2], sub={y} )
         y ->( inv=[2,3])
         */

        /*


            x -> [intv= [2,4], sub ={T}]
            y -> [intv = [3,4], sub={T}]
            if (x < y-1)
            x -> [intv= [2,2], sub ={y}] ???
            y -> [intv = [3,4], sub={T}]
         */

    }

    @Override
    public Pentagons forgetIdentifier(Identifier id) throws SemanticException {
        return this;
    }

    @Override
    public Pentagons forgetIdentifiers(Collection<Identifier> ids) throws SemanticException {
        return this;
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return Satisfiability.UNKNOWN;
    }

    @Override
    public String representation() {
        /*
         x ->( intv = [1,4], sub={y} )
         y ->( inv=[2,3])
         */
        if (isTop())
            return Lattice.TOP_STRING;
        else if (isBottom())
            return Lattice.BOTTOM_STRING;
        return "" + intv.representation() + ", {" + sub.representation() +"} " ;
    }
}

/*
test2() {
        def x = 0;
        def i = 1;
        while ( i < x ){
            if (i < 0)
                x = x + 5;
            else
                x = x - 100;

            i = i + 1;
        }
        return x;
    }
 */