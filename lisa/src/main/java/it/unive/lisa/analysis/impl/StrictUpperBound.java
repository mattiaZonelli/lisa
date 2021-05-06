package it.unive.lisa.analysis.impl;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.analysis.impl.UpperBounds;
import jdk.swing.interop.SwingInterOpUtils;

import java.util.*;

import static java.util.Collections.copy;


public class StrictUpperBound extends FunctionalLattice<StrictUpperBound, String, UpperBounds>
        implements ValueDomain<StrictUpperBound> {


    public StrictUpperBound() {
        this(new UpperBounds(new HashSet<>()));
    }

    protected StrictUpperBound(UpperBounds lattice) {
        super(lattice);
        this.function = mkNewFunction(null);
    }

    private StrictUpperBound(UpperBounds lattice, Map<String, UpperBounds> function) {
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
            if (!function.containsKey(id.toString())) function.put(id.toString(), lattice.top());
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
        if (expression.toString().contains("<") || expression.toString().contains(">")) {
            String tmp = expression.toString().replaceAll("vid", "");
            if (expression.toString().contains("!"))
                tmp = tmp.replace("!", "");
            String[] expr = tmp.split(" ");

            String sx, dx;
            if (expression.toString().contains("<")){
                // "x < y"
                sx = expr[0];
                dx = expr[2];
            }else{
                // "x > y"
                sx = expr[2];
                dx = expr[0];
            }
            // controllo che x e y siano variabili
            if (!isNumeric(sx) && !isNumeric(dx)){
                HashSet<String> hs = new HashSet<>();
                if (expression.toString().contains("!")) {
                    if (function.get(sx).contains(dx)){
                        // se il Sub è solo la variabile dx allora empty set e quindi TOP
                        if (function.get(sx).elements().toArray().length == 1) function.replace(sx, lattice.top());
                        else {
                            // se ho altre variabili oltre a dx allora le copio tutte tranne quella
                            for (String ub : function.get(sx).elements()){
                                if (!ub.equals(dx)) hs.add(ub);
                            }
                            function.replace(sx, new UpperBounds(hs));
                        }
                    }
                    //function.replace(expr[0], lattice.top()); // da controllare
                }else{
                    // se ho una relazione del tipo: x < y
                    if (!function.get(sx).toString().contains("#TOP#")){

                        // se il Sub di x aveva già delle varibili
                        for (String ub : function.get(sx).elements()){
                            hs.add(ub);
                        }
                    }
                    //lattice.elements().add(dx);
                    hs.add(dx);
                    function.replace(sx, new UpperBounds(hs));
                    //function.replace(expr[2], lattice.top()); // forse da togliere
                }

                // bottomizzazione
                if (function.containsKey(dx)) {
                    if (function.get(sx).elements().contains(dx) && function.get(dx).elements().contains(sx)) {
                        function.replace(sx, lattice.bottom());
                        function.replace(dx, lattice.bottom());
                    }
                }
                System.out.println("ASSUME: " + expression + function.entrySet());
                return new StrictUpperBound(lattice, function);

            }



        }
        //return this;
        return new StrictUpperBound(lattice, function);
    }


    @Override
    public StrictUpperBound forgetIdentifier(Identifier id) throws SemanticException {

        if (isTop() || isBottom()) {
            //System.out.println("\nFORGET isTop or isBottom");
            //return this;
            return new StrictUpperBound(lattice, function);
        }
        //System.out.println("FORGET : " + this.function.entrySet());
        // devo creare un nuovo Sub senza nessun pair con key = id
        Map<String, UpperBounds> mappa = new HashMap<>();
        for (String key : function.keySet()){
            if( !(key.equals(id.toString())) ) mappa.put(key, function.get(key));
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
        for (String i : function.keySet())
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