package it.unive.lisa.analysis.impl;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.MapRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;

import java.util.*;


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

    protected StrictUpperBound(UpperBounds lattice, Map<Identifier, UpperBounds> function) {
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
        return /*lattice.isTop() &&*/ (function == null || function.isEmpty());
    }
    
    @Override
    public boolean isBottom() {
        return lattice.isBottom();
    }

    /**
     * Handle the assign operation for the Strict Upper Bound domain.
     * If it is the first time we are encountering a variable and in the right hand-side of the statement there is
     * a simple expression with only numbers then we proceed to add it to the map with strict upper bound set to Top.
     * If in the right hand-side there is a variable (already in the strictUpperBound) minus a single number, then
     * we add the Sub of the right hand-side variable to the Sub of the left hand-side varible.
     * If we have a different situations of the previously described we just return a new Sub equals to the previous one.
     */
    @Override
    public StrictUpperBound assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
        if (pp.toString().contains("=")) {
            if (!function.containsKey(id)){
                //System.out.println("TEST assign "+ expression.toString());
                Map<Identifier, UpperBounds> result_map  = mkNewFunction(function);
                // caso: y = x-1;
                if (expression instanceof BinaryExpression && ((BinaryExpression) expression).getOperator() == BinaryOperator.NUMERIC_SUB){
                    BinaryExpression binaryExpression = (BinaryExpression) expression;
                    if (binaryExpression.getLeft() instanceof Identifier && binaryExpression.getRight() instanceof Constant){
                        HashSet<Identifier> hs = new HashSet<>();
                        if (!result_map.get(binaryExpression.getLeft()).toString().contains("#TOP#")) {

                            for (Identifier ub : result_map.get(binaryExpression.getLeft()).elements()) {
                                hs.add(ub);
                            }
                        }
                        hs.add((Identifier) binaryExpression.getLeft());
                        result_map.put(id, new UpperBounds(hs));
                        //System.out.println("ASSIGN: " + pp);
                    }
                }else {
                    result_map.put(id, lattice.top());
                }
                System.out.println("ASSIGN: " + pp + ", maps: " + function.entrySet() + ", lattice: " + lattice.toString());
                return new StrictUpperBound(lattice, result_map);
            }

        }
        //System.out.println("ASSIGN: " + pp + "; " + function.entrySet());
        //return this;
        return new StrictUpperBound(lattice, function);
    }

    /**
     * it seems that it never use this function
     *
     */
    @Override
    public StrictUpperBound smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        //System.out.println("SMALL STEP "+ expression);
        //return this;
        return new StrictUpperBound(lattice, function);
    }

    /**
     *
     *
     */

    @Override
    public StrictUpperBound assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {

        System.out.println("inASSUME: " + expression + ", maps: " + function.entrySet() + ", lattice: " + lattice.toString());


        // the the ccondition starts with a negation, remove it and "flip" the sign.
        if (expression instanceof UnaryExpression && ((UnaryExpression) expression).getExpression() instanceof BinaryExpression) {
            expression = expression.removeNegations();
        }

        // if
        if (expression instanceof BinaryExpression) {

            BinaryExpression binaryExpression = (BinaryExpression) expression;
            Map<Identifier, UpperBounds>  result_map = mkNewFunction(function);
            if (binaryExpression.getLeft() instanceof Identifier && binaryExpression.getRight() instanceof Identifier) {

                // case: == then sub of both identifier has to become the union of both.
                if (binaryExpression.getOperator() == BinaryOperator.COMPARISON_EQ) {
                    Identifier sx = (Identifier) binaryExpression.getLeft();
                    Identifier dx = (Identifier) binaryExpression.getRight();

                    result_map.replace(sx, result_map.get(sx).glb(result_map.get(dx)));
                    result_map.replace(dx, result_map.get(sx).glb(result_map.get(dx)));

                    System.out.println("outASSUME: " + expression + " " + result_map.entrySet() + ", " + lattice.toString());
                    return new StrictUpperBound(lattice, result_map);
                }
                // case:  !=, nothing change
                if (binaryExpression.getOperator() == BinaryOperator.COMPARISON_NE){
                    System.out.println("outASSUME: " + expression + " " + result_map.entrySet() + ", " + lattice.toString());
                    return new StrictUpperBound(lattice, result_map);
                }


                // cases < or > or <= or >=
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

                    // "x < x" same identifier in both left and right hand-side of statement.
                    if (sx.equals(dx)
                            && (binaryExpression.getOperator() == BinaryOperator.COMPARISON_GT ||
                            binaryExpression.getOperator() == BinaryOperator.COMPARISON_LT)) {
                        result_map.replace(sx, lattice.bottom());
                    }


                    // inizio le modifiche sulla function
                    HashSet<Identifier> hs = new HashSet<>();
                    //
                    if (!result_map.get(sx).toString().contains("#TOP#")) {
                        // se il Sub di x aveva già delle varibili
                        for (Identifier ub : result_map.get(sx).elements()) {
                            hs.add(ub);
                            //lattice.elements().add(ub);
                        }
                    }

                    // se ho x < y devo aggiungere anche y all sub di x
                    if (binaryExpression.getOperator() == BinaryOperator.COMPARISON_GT ||
                            binaryExpression.getOperator() == BinaryOperator.COMPARISON_LT) {
                        hs.add(dx);
                    }

                    // x = s(x) U s(y)
                    if (!result_map.get(dx).toString().contains("#TOP#")) {
                        // se il Sub di y aveva già delle varibili
                        for (Identifier ub : result_map.get(dx).elements()) {
                            hs.add(ub);
                        }
                    }

                    if (!hs.isEmpty()) result_map.replace(sx, new UpperBounds(hs));

                    // bottomizzazione
                    if (result_map.containsKey(dx)) {
                        if (result_map.get(sx).elements().contains(dx) && result_map.get(dx).elements().contains(sx)) {
                            result_map.replace(sx, lattice.bottom());
                            result_map.replace(dx, lattice.bottom());
                            //return new StrictUpperBound(lattice.isBottom(), result_map);

                        }
                    }
                    System.out.println("outASSUME: " + expression + " " + result_map.entrySet() + ", " + lattice.toString());
                    return new StrictUpperBound(lattice, result_map);
                    //return new StrictUpperBound(new UpperBounds(hs), function);
                }

            }

        }


        //return this;
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
     * it seems that it never use this function
     */
    @Override
    public StrictUpperBound forgetIdentifiers(Collection<Identifier> ids) throws SemanticException {
        //System.out.println("FORGET ALL");
        return new StrictUpperBound(lattice, function);
    }

    /**
     * it seems that it never use this function
     */
    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
        System.out.println("SATISFIES " + expression);
        return Satisfiability.UNKNOWN;
    }

    @Override
    public StrictUpperBound pushScope(ScopeToken token) throws SemanticException {
        return new StrictUpperBound(lattice, function);
    }

    @Override
    public StrictUpperBound popScope(ScopeToken token) throws SemanticException {
        return new StrictUpperBound(lattice, function);
    }

    @Override
    public DomainRepresentation representation() {

        // se c'è un bottom nella mappa, return stringa bottom.
        if (bottomization()) return new StringRepresentation(Lattice.BOTTOM_STRING);
        else return new StringRepresentation(function.toString());
    }


    public boolean bottomization(){
        if (function.toString().contains(Lattice.BOTTOM_STRING)){
            return true;
        }else
            return false;
    }
}
