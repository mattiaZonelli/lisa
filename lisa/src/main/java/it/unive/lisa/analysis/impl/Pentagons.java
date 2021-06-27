package it.unive.lisa.analysis.impl;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.symbolic.value.ValueExpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Pentagons implements ValueDomain<Pentagons> {

    private final ValueEnvironment<Interval> intv;
    private final StrictUpperBound sub;
    private final boolean isTOP;
    private final boolean isBOTTOM;

    public Pentagons() {
        this(new ValueEnvironment<>(new Interval(), new HashMap<>()), new StrictUpperBound());
    }

    protected Pentagons(ValueEnvironment<Interval> intv, StrictUpperBound sub) {
        super();
        this.intv = intv;
        this.sub = sub;
        this.isTOP = false;
        this.isBOTTOM = false;
    }


    @Override
    public Pentagons top() {
        return new Pentagons(intv.top(), sub.top());
    }

    @Override
    public Pentagons bottom() {
        return new Pentagons(intv.bottom(), sub.bottom());
    }

    @Override
    public boolean isTop() {
        return sub.isTop() && intv.isTop();
    }

    @Override
    //public boolean isBottom() { return sub.isBottom() || intv.isBottom(); }
    public boolean isBottom() { return sub.bottomization() || intv.isBottom(); }

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
        return this.intv.lessOrEqual(other.intv) && this.sub.lessOrEqual(other.sub); // da cambiare
    }

    // function that refin intvs looking at subs
    private Pentagons refine(){
        Map<Identifier, Interval> result = new HashMap<>();
        // for each identifier in the map (sub environment)
        for (Identifier sx : sub.getKeys()){
            if (sub.getState(sx).isBottom() || sub.getState(sx).isTop()){
                result.put(sx, intv.getState(sx));
            }else{
                // for each sub of identifier sx in the map
                for (Identifier dx : sub.getState(sx)){
                    // if neither intv of sx and dx are top or bottom
                    if (!intv.getState(sx).isBottom() && !intv.getState(sx).isTop() && !intv.getState(dx).isBottom() &&
                        !intv.getState(dx).isTop()){
                        // we get the bounds of intv of both identifiers sx and dx
                        Integer s_low = intv.getState(sx).getLow();
                        Integer s_high = intv.getState(sx).getHigh();
                        Integer d_low = intv.getState(dx).getLow();
                        Integer d_high = intv.getState(dx).getHigh();

                        // sx < dx, sx->[s_low, s_high], dx->[d_low, d_high]
                        if (s_low >= d_low){
                            result.put(sx, new Interval());
                        }else if (/*s_low < d_low &&*/ s_high >= d_low){
                            result.put(sx, new Interval(s_low, d_low-1));
                        }else {
                            result.put(sx, intv.getState(sx));
                        }
                    }else{
                        result.put(sx, intv.getState(sx));
                    }
                }
            }
        }
        //System.out.println("TEST: " + result.toString() + " " + sub.toString());
        return new Pentagons(new ValueEnvironment<>(new Interval(), result), sub);
    }

    @Override
    public Pentagons assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
        //System.out.println("ASSIGN: " + pp );
        Pentagons pntg = new Pentagons(intv.assign(id, expression, pp), sub.assign(id, expression, pp));
        return pntg.refine();
    }

    @Override
    public Pentagons smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return new Pentagons(intv, sub);
    }

    @Override
    public Pentagons assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        //System.out.println("ASSUME: " + expression );
        Pentagons pntg  = new Pentagons(intv, sub.assume(expression, pp));
        return pntg.refine();


    }

    @Override
    public Pentagons forgetIdentifier(Identifier id) throws SemanticException {
        return new Pentagons(intv.forgetIdentifier(id), sub.forgetIdentifier(id));
    }

    @Override
    public Pentagons forgetIdentifiers(Collection<Identifier> ids) throws SemanticException {
        return new Pentagons(intv.forgetIdentifiers(ids), sub.forgetIdentifiers(ids));
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        return Satisfiability.UNKNOWN;
    }

    @Override
    public Pentagons pushScope(ScopeToken token) throws SemanticException {
        return new Pentagons(intv.pushScope(token), sub.pushScope(token));
    }

    @Override
    public Pentagons popScope(ScopeToken token) throws SemanticException {
        return new Pentagons(intv.popScope(token), sub.popScope(token));
    }

    @Override
    public DomainRepresentation representation() {
        /*
         x ->( intv = [1,4], sub={y} )
         y ->( inv=[2,3])
         */
        if (isTop())
            return new StringRepresentation(Lattice.TOP_STRING);
        else if (isBottom()){

            return new StringRepresentation(Lattice.BOTTOM_STRING);
        }
        else
            return new StringRepresentation("" + intv.representation() + ", {" + sub.representation() + "} ");
    }
}
