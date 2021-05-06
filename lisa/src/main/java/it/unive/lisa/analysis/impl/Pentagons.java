package it.unive.lisa.analysis.impl;

import com.ibm.icu.impl.Pair;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

import java.util.Collection;
import java.util.Map;

public class Pentagons implements ValueDomain<Pentagons> {

    Map<String, Interval> b;
    Map<String, StrictUpperBound> s;

    public Pentagons (){ this(null, null); }

    private Pentagons(Map<String, Interval> b, Map<String, StrictUpperBound> s) {
        this.b = b;
        this.s = s;
    }

    @Override
    public Pentagons top() {
        return null;
    }

    @Override
    public Pentagons bottom() {
        return null;
    }

    @Override
    public boolean isTop() {
        return false;
    }

    @Override
    public boolean isBottom() {
        return false;
    }

    @Override
    public Pentagons lub(Pentagons other) throws SemanticException {
        return null;
    }

    @Override
    public Pentagons widening(Pentagons other) throws SemanticException {
        return null;
    }

    @Override
    public boolean lessOrEqual(Pentagons other) throws SemanticException {
        return false;
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
        return null;
    }
}
