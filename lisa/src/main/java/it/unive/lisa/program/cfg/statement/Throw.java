package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Skip;

/**
 * A statement that raises an error, stopping the execution of the current CFG
 * and propagating the error to among the call chain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Throw extends UnaryStatement {

	/**
	 * Builds the throw, raising {@code expression} as error, happening at the
	 * given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param location   the location where the expression is defined within the
	 *                       source file. If unknown, use {@code null}
	 * @param expression the expression to raise as error
	 */
	public Throw(CFG cfg, CodeLocation location, Expression expression) {
		super(cfg, location, expression);
	}

	@Override
	public final String toString() {
		return "throw " + getExpression();
	}

	@Override
	public boolean stopsExecution() {
		return true;
	}

	@Override
	public boolean throwsError() {
		return true;
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph, StatementStore<A, H, V> expressions)
					throws SemanticException {
		AnalysisState<A, H, V> result = getExpression().semantics(entryState, callGraph, expressions);
		expressions.put(getExpression(), result);
		if (!getExpression().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getExpression().getMetaVariables());
		return result.smallStepSemantics(new Skip(), this);
	}
}
