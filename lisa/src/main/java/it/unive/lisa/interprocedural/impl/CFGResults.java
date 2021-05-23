package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import java.util.Collection;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link FunctionalLattice} from {@link ContextSensitivityToken}s to
 * {@link CFGWithAnalysisResults}s. This class is meant to store fixpoint
 * results on each token generated during the interprocedural analysis.
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
public class CFGResults<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>>
		extends FunctionalLattice<CFGResults<A, H, V>, ContextSensitivityToken, CFGWithAnalysisResults<A, H, V>> {

	/**
	 * Builds a new result.
	 * 
	 * @param lattice a singleton instance used for retrieving top and bottom
	 *                    values
	 */
	public CFGResults(CFGWithAnalysisResults<A, H, V> lattice) {
		super(lattice);
	}

	/**
	 * Stores the result of a fixpoint computation on a cfg, if needed. This
	 * method returns a pair of a boolean and a {@link CFGWithAnalysisResults},
	 * where ({@code prev} is the {@link CFGWithAnalysisResults} already present
	 * for the given {@code token}):
	 * <ul>
	 * <li>if no {@code prev} was stored for {@code token}, than that token is
	 * mapped to {@code result} and this method returns
	 * {@code <false, result>}</li>
	 * <li>if {@code prev <= result}, then {@code token} is mapped to
	 * {@code result} and this method returns {@code <true, result>}</li>
	 * <li>if {@code result <= prev}, then the mapping is left untouched and
	 * this method returns {@code <false, prev>}</li>
	 * <li>otherwise, {@code token} is mapped to {@code lub = prev.lub(result)}
	 * and this method returns {@code <true, lub>}</li>
	 * </ul>
	 * The value returned by this method is intended to be a hint that a new
	 * fixpoint computation is needed to ensure that the results are stable.
	 * 
	 * @param token  the {@link ContextSensitivityToken} that identifying the
	 *                   result
	 * @param result the {@link CFGWithAnalysisResults} to store
	 * 
	 * @return {@code true} if the previous result has been updated, if any
	 * 
	 * @throws SemanticException if something goes wrong during the update
	 */
	public Pair<Boolean, CFGWithAnalysisResults<A, H, V>> putResult(ContextSensitivityToken token,
			CFGWithAnalysisResults<A, H, V> result)
			throws SemanticException {
		CFGWithAnalysisResults<A, H, V> previousResult = function.get(token);
		if (previousResult == null) {
			// no previous result
			function.put(token, result);
			return Pair.of(false, result);
		} else if (previousResult.lessOrEqual(result)) {
			// previous is smaller than result
			if (result.lessOrEqual(previousResult))
				// they are equal
				return Pair.of(false, previousResult);
			else {
				// result is bigger, store that instead
				function.put(token, result);
				return Pair.of(true, result);
			}
		} else if (result.lessOrEqual(previousResult)) {
			// result is smaller than previous
			return Pair.of(false, previousResult);
		} else {
			// result and previous are not comparable
			CFGWithAnalysisResults<A, H, V> lub = previousResult.lub(result);
			function.put(token, lub);
			return Pair.of(true, lub);
		}
	}

	/**
	 * Yields {@code true} if a result exists for the given {@code token}.
	 * 
	 * @param token the {@link ContextSensitivityToken} that identifying the
	 *                  result
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean contains(ContextSensitivityToken token) {
		return function != null && function.containsKey(token);
	}

	/**
	 * Yields all the results stored in this object, for any possible
	 * {@link ContextSensitivityToken} used.
	 * 
	 * @return the results
	 */
	public Collection<CFGWithAnalysisResults<A, H, V>> getAll() {
		return function.values();
	}

	@Override
	public CFGResults<A, H, V> top() {
		return new CFGResults<>(lattice.top());
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && (function == null || function.isEmpty());
	}

	@Override
	public CFGResults<A, H, V> bottom() {
		return new CFGResults<>(lattice.bottom());
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && (function == null || function.isEmpty());
	}
}
