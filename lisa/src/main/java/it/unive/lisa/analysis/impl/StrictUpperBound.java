package it.unive.lisa.analysis.impl;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.analysis.impl.UpperBounds;


import java.util.*;

import static java.util.Collections.copy;


public class StrictUpperBound
        extends FunctionalLattice<StrictUpperBound, Identifier, UpperBounds>
        implements ValueDomain<StrictUpperBound> {


    public StrictUpperBound() {
        this(new UpperBounds(new HashSet<>()));
    }

    protected StrictUpperBound(UpperBounds lattice) {
        super(lattice);
        this.function = mkNewFunction(null);
    }

    private StrictUpperBound(UpperBounds lattice, Map<Identifier, UpperBounds> function) {
        super(lattice);
        this.function = mkNewFunction(function);
    }


    @Override
    public StrictUpperBound top() {
        return new StrictUpperBound(lattice.top());
    }

    @Override
    public StrictUpperBound bottom() {
        return new StrictUpperBound(lattice.bottom());
    }

    public boolean isTop() {
        return lattice.isTop() && (function == null || function.isEmpty());
    }

    @Override
    public boolean isBottom() {

        return lattice.isBottom();
    }


    @Override
    public StrictUpperBound assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
        if (pp.toString().contains("=")) {
            if (!function.containsKey(id)) function.put(id, lattice.top());
            //System.out.println("TEST assign "+ expression.toString());
        }
        System.out.println("ASSIGN: " + pp + "; " + function.entrySet());
        //return this;
        return new StrictUpperBound(lattice, function);
    }

    @Override
    public StrictUpperBound smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        //System.out.println("SMALL STEP "+ expression);
        //return this;
        return new StrictUpperBound(lattice, function);
    }

    private static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }



    @Override
    public StrictUpperBound assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {

        if (expression instanceof BinaryExpression) {

                BinaryExpression binaryExpression = (BinaryExpression) expression;
                if (binaryExpression.getLeft() instanceof Identifier && binaryExpression.getRight() instanceof Identifier) {
                    // caso < or > or <= or >=
                    if (binaryExpression.getOperator() == BinaryOperator.COMPARISON_GT ||
                            binaryExpression.getOperator() == BinaryOperator.COMPARISON_LT ||
                            binaryExpression.getOperator() == BinaryOperator.COMPARISON_GE ||
                            binaryExpression.getOperator() == BinaryOperator.COMPARISON_LE) {

                        Identifier sx, dx;
                        if (binaryExpression.getOperator() == BinaryOperator.COMPARISON_LT ||
                                binaryExpression.getOperator() == BinaryOperator.COMPARISON_LE) {
                            // "x < y" or "x <= y"
                            sx = (Identifier) binaryExpression.getLeft();
                            dx = (Identifier) binaryExpression.getRight();
                        } else {
                            // "x > y" or "x >= y"
                            sx = (Identifier) binaryExpression.getRight();
                            dx = (Identifier) binaryExpression.getLeft();
                        }

                        if (sx.equals(dx)) {
                            forgetIdentifier(sx);
                            function.put(sx, lattice.bottom());

                            System.out.println("ASSUME: " + expression + " " + function.entrySet());
                            return new StrictUpperBound(lattice, function);
                        }

                        // procedo con le modifiche della function
                        HashSet<Identifier> hs = new HashSet<>();
                        // se il SUB di x non è vuoto e quindi TOP, mi devo creare un nuovo Set per gli upperBounds di x
                        if (!function.get(sx).toString().contains("#TOP#")) {
                            // se il Sub di x aveva già delle varibili
                            for (Identifier ub : function.get(sx).elements()) {
                                hs.add(ub);
                            }
                        }

                        // x -> s(x) U {y},
                        // se ho x < y devo aggiungere anche y all sub di x
                        if (binaryExpression.getOperator() == BinaryOperator.COMPARISON_GT ||
                                binaryExpression.getOperator() == BinaryOperator.COMPARISON_LT) {
                            hs.add(dx);
                        }

                        // x -> s(x) U s(y)
                        if (!function.get(dx).toString().contains("#TOP#")) {
                            // se il Sub di y aveva già delle varibili
                            for (Identifier ub : function.get(dx).elements()) {
                                hs.add(ub);
                            }
                        }

                        //se ho qualcosa da aggiungere al sub di sx allora lo modifico, sennò no
                        if (!hs.isEmpty()) function.replace(sx, new UpperBounds(hs));

                        // bottomizzazione ---> da qualche problema anche senza la distinzione tra false e true branches
                        //if (function.containsKey(dx)) {
                            if (function.get(sx).contains(dx) && function.get(dx).contains(sx)) {
                                function.replace(sx, lattice.bottom());
                                function.replace(dx, lattice.bottom());
                            }
                        //}

                        System.out.println("ASSUME: " + expression + " " + function.entrySet());
                        return new StrictUpperBound(lattice, function);
                    }
                }

            }


        if (expression instanceof UnaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression)((UnaryExpression) expression).getExpression();
            function.replace((Identifier) binaryExpression.getLeft(), lattice.bottom());
            System.out.println("UNARY: " + expression + " " + function.entrySet());
        }

        return new StrictUpperBound(lattice, function);
    }


    @Override
    public StrictUpperBound forgetIdentifier(Identifier id) throws SemanticException {
        //System.out.println("FORGET : " + function.keySet() + ", id:  "+ id);
        if (isTop() || isBottom()) {
            //System.out.println("\nFORGET isTop or isBottom");
            //return this;
            return new StrictUpperBound(lattice, function);
        }

        // devo creare un nuovo Sub senza nessun pair con key = id
        Map<Identifier, UpperBounds> mappa = new HashMap<>();
        for (Identifier key : function.keySet()) {
            if (!(key.equals(id))) mappa.put(key, function.get(key));
        }

        return new StrictUpperBound(lattice, mappa);
        //return this;

    }

    /**
     * NON VIENE MAI ESEGUITA, PERCHE'?
     */
    @Override
    public StrictUpperBound forgetIdentifiers(Collection<Identifier> ids) throws SemanticException {
        System.out.println("FORGET ALL");
        return this; // se ci entra è da cambiare
    }

    /**
     * NON VIENE MAI ESEGUITA, PERCHE'?
     */
    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        System.out.println("SATISFIES " + expression);
        return Satisfiability.UNKNOWN;
    }

    @Override
    public String representation() {
        String res = "";
        for (Identifier i : function.keySet())
            res = res + i + " -> " + function.get(i) + "; ";
        System.out.println(res);
        return res;
    }
}


/*
    test2() {
        def x = 0;
        for (def i = 1; i < x; i = i + 1)
            if (i < 0)
                x = x + 5;
            else
                x = x - 100;
        return x;
    }
 */