package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * A {@link NativeCall} with a exactly three arguments.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class TernaryNativeCall extends NativeCall {

	/**
	 * Builds the untyped native call. The location where this call happens is
	 * unknown (i.e. no source file/line/column is available). The static type
	 * of this call is {@link Untyped}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param left          the first parameter of this call
	 * @param middle        the second parameter of this call
	 * @param right         the third parameter of this call
	 */
	protected TernaryNativeCall(CFG cfg, String constructName, Expression left, Expression middle, Expression right) {
		super(cfg, constructName, left, middle, right);
	}

	/**
	 * Builds the native call. The location where this call happens is unknown
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param staticType    the static type of this call
	 * @param left          the first parameter of this call
	 * @param middle        the second parameter of this call
	 * @param right         the second parameter of this call
	 */
	protected TernaryNativeCall(CFG cfg, String constructName, Type staticType, Expression left, Expression middle,
			Expression right) {
		super(cfg, constructName, staticType, left, middle, right);
	}

	/**
	 * Builds the untyped native call, happening at the given location in the
	 * program. The static type of this call is {@link Untyped}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the source file. If unknown, use {@code null}
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param left          the first parameter of this call
	 * @param middle        the second parameter of this call
	 * @param right         the second parameter of this call
	 */
	protected TernaryNativeCall(CFG cfg, CodeLocation location, String constructName,
			Expression left, Expression middle, Expression right) {
		super(cfg, location, constructName, left, middle, right);
	}

	/**
	 * Builds the native call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the source file. If unknown, use {@code null}
	 * @param constructName the name of the construct invoked by this native
	 *                          call
	 * @param staticType    the static type of this call
	 * @param left          the first parameter of this call
	 * @param middle        the second parameter of this call
	 * @param right         the second parameter of this call
	 */
	protected TernaryNativeCall(CFG cfg, CodeLocation location, String constructName, Type staticType,
			Expression left, Expression middle, Expression right) {
		super(cfg, location, constructName, staticType, left, middle, right);
	}

	@Override
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState,
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V>[] computedStates,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {
		AnalysisState<A, H, V> result = null;

		for (SymbolicExpression left : params[0])
			for (SymbolicExpression middle : params[1])
				for (SymbolicExpression right : params[2]) {
					AnalysisState<A, H,
							V> tmp = ternarySemantics(entryState, interprocedural, computedStates[0], left,
									computedStates[1],
									middle, computedStates[2], right);
					if (result == null)
						result = tmp;
					else
						result = result.lub(tmp);
				}

		return result;
	}

	/**
	 * Computes the semantics of the call, after the semantics of the parameters
	 * have been computed. Meta variables from the parameters will be forgotten
	 * after this call returns.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param entryState      the entry state of this binary call
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param leftState       the state obtained by evaluating {@code left} in
	 *                            {@code entryState}
	 * @param leftExp         the symbolic expression representing the computed
	 *                            value of the first parameter of this call
	 * @param middleState     the state obtained by evaluating {@code middle} in
	 *                            {@code leftState}
	 * @param middleExp       the symbolic expression representing the computed
	 *                            value of the second parameter of this call
	 * @param rightState      the state obtained by evaluating {@code right} in
	 *                            {@code middleState}
	 * @param rightExp        the symbolic expression representing the computed
	 *                            value of the third parameter of this call
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	protected abstract <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> ternarySemantics(
					AnalysisState<A, H, V> entryState,
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> leftState, SymbolicExpression leftExp,
					AnalysisState<A, H, V> middleState, SymbolicExpression middleExp,
					AnalysisState<A, H, V> rightState, SymbolicExpression rightExp)
					throws SemanticException;
}
