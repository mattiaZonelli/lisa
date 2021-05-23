package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.outputs.DotCFG;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * A control flow graph, that has {@link Statement}s as nodes and {@link Edge}s
 * as edges. It also maps each statement (and its inner expressions) to the
 * result of a fixpoint computation, in the form of an {@link AnalysisState}
 * instance.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 */
public class CFGWithAnalysisResults<A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>>
		extends CFG implements Lattice<CFGWithAnalysisResults<A, H, V>> {

	/**
	 * The map storing the analysis results
	 */
	private final StatementStore<A, H, V> results;

	/**
	 * The map storing the entry state of each entry point
	 */
	private final StatementStore<A, H, V> entryStates;

	/**
	 * An optional string meant to identify this specific result, based on how
	 * it has been produced
	 */
	private String id;

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg       the original control flow graph
	 * @param singleton an instance of the {@link AnalysisState} containing the
	 *                      abstract state of the analysis that was executed,
	 *                      used to retrieve top and bottom values
	 */
	public CFGWithAnalysisResults(CFG cfg, AnalysisState<A, H, V> singleton) {
		this(cfg, singleton, Collections.emptyMap(), Collections.emptyMap());
	}

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg         the original control flow graph
	 * @param singleton   an instance of the {@link AnalysisState} containing
	 *                        the abstract state of the analysis that was
	 *                        executed, used to retrieve top and bottom values
	 * @param entryStates the entry state for each entry point of the cfg
	 * @param results     the results of the fixpoint computation
	 */
	public CFGWithAnalysisResults(CFG cfg, AnalysisState<A, H, V> singleton,
			Map<Statement, AnalysisState<A, H, V>> entryStates,
			Map<Statement, AnalysisState<A, H, V>> results) {
		super(cfg);
		this.results = new StatementStore<>(singleton);
		results.forEach(this.results::put);
		this.entryStates = new StatementStore<>(singleton);
		entryStates.forEach(this.entryStates::put);
	}

	private CFGWithAnalysisResults(CFG cfg, StatementStore<A, H, V> entryStates, StatementStore<A, H, V> results) {
		super(cfg);
		this.results = results;
		this.entryStates = entryStates;
	}

	/**
	 * Yields a string meant to identify this specific result, based on how it
	 * has been produced. This method might return {@code null}.
	 * 
	 * @return the identifier of this result
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the string meant to identify this specific result, based on how it
	 * has been produced.
	 * 
	 * @param id the identifier of this result (might be {@code null})
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Yields the computed result before a given statement (entry state).
	 *
	 * @param st the statement
	 *
	 * @return the result computed before the given statement
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public final AnalysisState<A, H, V> getAnalysisStateBefore(Statement st) throws SemanticException {
		if (getEntrypoints().contains(st))
			return entryStates.getState(st);
		return lub(predecessorsOf(st), false);
	}

	/**
	 * Yields the computed result at a given statement (exit state).
	 *
	 * @param st the statement
	 *
	 * @return the result computed at the given statement
	 */
	public final AnalysisState<A, H, V> getAnalysisStateAfter(Statement st) {
		return results.getState(st);
	}

	/**
	 * Yields the entry state.
	 * 
	 * @return the entry state of the CFG
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public final AnalysisState<A, H, V> getEntryState() throws SemanticException {
		return lub(this.getEntrypoints(), true);
	}

	/**
	 * Yields the exit state.
	 * 
	 * @return the entry state of the CFG
	 * 
	 * @throws SemanticException if the lub operator fails
	 */
	public final AnalysisState<A, H, V> getExitState() throws SemanticException {
		return lub(this.getNormalExitpoints(), false);
	}

	private AnalysisState<A, H, V> lub(Collection<Statement> statements, boolean entry) throws SemanticException {
		AnalysisState<A, H, V> result = null;
		for (Statement st : statements)
			if (result == null)
				result = entry ? getAnalysisStateBefore(st) : getAnalysisStateAfter(st);
			else
				result = result.lub(entry ? getAnalysisStateBefore(st) : getAnalysisStateAfter(st));
		return result;

	}

	/**
	 * Joins two {@link CFGWithAnalysisResults} together. The difference between
	 * this method and {@link #lub(CFGWithAnalysisResults)} is that this method
	 * does not set the ID of the resulting cfg.
	 * 
	 * @param other the other cfg
	 * 
	 * @return the least upper bound of the two cfgs without its id set
	 * 
	 * @throws SemanticException if something goes wrong during the join
	 */
	public CFGWithAnalysisResults<A, H, V> join(CFGWithAnalysisResults<A, H, V> other) throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()))
			throw new SemanticException("Cannot perform the least upper bound of two graphs with different descriptor");

		return new CFGWithAnalysisResults<>(this, entryStates.lub(other.entryStates),
				results.lub(other.results));
	}

	@Override
	public CFGWithAnalysisResults<A, H, V> lub(CFGWithAnalysisResults<A, H, V> other) throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()))
			throw new SemanticException("Cannot perform the least upper bound of two graphs with different descriptor");

		CFGWithAnalysisResults<A, H, V> lub = new CFGWithAnalysisResults<>(this, entryStates.lub(other.entryStates),
				results.lub(other.results));
		lub.setId(joinIDs(other));
		return lub;
	}

	@Override
	public CFGWithAnalysisResults<A, H, V> widening(CFGWithAnalysisResults<A, H, V> other) throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()))
			throw new SemanticException("Cannot perform the least upper bound of two graphs with different descriptor");

		CFGWithAnalysisResults<A, H,
				V> widen = new CFGWithAnalysisResults<>(this, entryStates.widening(other.entryStates),
						results.widening(other.results));
		widen.setId(joinIDs(other));
		return widen;
	}

	private String joinIDs(CFGWithAnalysisResults<A, H, V> other) throws SemanticException {
		// we accept merging only if the ids are the same
		if (id == null) {
			if (other.id == null)
				return null;

			throw new SemanticException("Cannot join graphs with different IDs: '" + String.valueOf(id) + "' and '"
					+ String.valueOf(other.id) + "'");
		}

		if (other.id == null)
			throw new SemanticException("Cannot join graphs with different IDs: '" + String.valueOf(id) + "' and '"
					+ String.valueOf(other.id) + "'");

		if (!id.equals(other.id))
			throw new SemanticException("Cannot join graphs with different IDs: '" + String.valueOf(id) + "' and '"
					+ String.valueOf(other.id) + "'");

		return id;
	}

	@Override
	public boolean lessOrEqual(CFGWithAnalysisResults<A, H, V> other) throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()))
			throw new SemanticException("Cannot perform the least upper bound of two graphs with different descriptor");

		return entryStates.lessOrEqual(other.entryStates) && results.lessOrEqual(other.results);
	}

	@Override
	public CFGWithAnalysisResults<A, H, V> top() {
		return new CFGWithAnalysisResults<>(this, entryStates.top(), results.top());
	}

	@Override
	public boolean isTop() {
		return entryStates.isTop() && results.isTop();
	}

	@Override
	public CFGWithAnalysisResults<A, H, V> bottom() {
		return new CFGWithAnalysisResults<>(this, entryStates.bottom(), results.bottom());
	}

	@Override
	public boolean isBottom() {
		return entryStates.isBottom() && results.isBottom();
	}

	@Override
	protected DotCFG toDot(Function<Statement, String> labelGenerator) {
		return DotCFG.fromCFG(this, id, labelGenerator);
	}
}
