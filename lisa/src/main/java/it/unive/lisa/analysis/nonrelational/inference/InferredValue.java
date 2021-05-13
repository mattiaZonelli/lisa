package it.unive.lisa.analysis.nonrelational.inference;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.analysis.nonrelational.NonRelationalElement;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.PairRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A {@link NonRelationalElement} that can be inferred by
 * {@link InferenceSystem}s. The main difference between a
 * {@link NonRelationalDomain} and an {@link InferredValue} is that
 * {@link #eval(ValueExpression, InferenceSystem, ProgramPoint)} returns
 * instances of {@link InferredPair}, to model the fact that every semantic
 * evaluation also modifies the execution state.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of inferred value
 */
public interface InferredValue<T extends InferredValue<T>>
		extends NonRelationalElement<T, ValueExpression, InferenceSystem<T>> {

	/**
	 * Evaluates a {@link ValueExpression}, assuming that the values of program
	 * variables are the ones stored in {@code environment}.
	 * 
	 * @param expression  the expression to evaluate
	 * @param environment the environment containing the values of program
	 *                        variables for the evaluation
	 * @param pp          the program point that where this operation is being
	 *                        evaluated
	 * 
	 * @return an new instance of this domain, representing the abstract result
	 *             of {@code expression} when evaluated on {@code environment}.
	 *             The returned value is a pair that express both the result of
	 *             the evaluation and the updated execution state
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public InferredPair<T> eval(ValueExpression expression, InferenceSystem<T> environment, ProgramPoint pp)
			throws SemanticException;

	/**
	 * A pair of instances of {@link InferredValue}, representing the result of
	 * an evaluation in the form of
	 * {@code <inferred value, new execution state>}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <T> the type of {@link InferredValue}
	 */
	public static class InferredPair<T extends InferredValue<T>> extends BaseLattice<InferredPair<T>> {

		private final T domain;

		private final T inferred;

		private final T state;

		/**
		 * Builds the pair.
		 * 
		 * @param domain   a singleton instance to be used during semantic
		 *                     operations to retrieve top and bottom values
		 * @param inferred the inferred value
		 * @param state    the execution state
		 */
		public InferredPair(T domain, T inferred, T state) {
			this.domain = domain;
			this.inferred = inferred;
			this.state = state;
		}

		/**
		 * Yields the instance of {@link InferredValue} representing the
		 * inferred value.
		 * 
		 * @return the inferred value
		 */
		public T getInferred() {
			return inferred;
		}

		/**
		 * Yields the instance of {@link InferredValue} representing the
		 * execution state.
		 * 
		 * @return the execution state
		 */
		public T getState() {
			return state;
		}

		@Override
		public InferredPair<T> top() {
			return new InferredPair<>(domain, domain.top(), domain.top());
		}

		@Override
		public boolean isTop() {
			return inferred.isTop() && state.isTop();
		}

		@Override
		public InferredPair<T> bottom() {
			return new InferredPair<>(domain, domain.bottom(), domain.bottom());
		}

		@Override
		public boolean isBottom() {
			return inferred.isBottom() && state.isBottom();
		}

		@Override
		protected InferredPair<T> lubAux(InferredPair<T> other) throws SemanticException {
			return new InferredPair<>(domain, inferred.lub(other.inferred), state.lub(other.state));
		}

		@Override
		protected InferredPair<T> wideningAux(InferredPair<T> other) throws SemanticException {
			return new InferredPair<>(domain, inferred.widening(other.inferred), state.widening(other.state));
		}

		@Override
		protected boolean lessOrEqualAux(InferredPair<T> other) throws SemanticException {
			return inferred.lessOrEqual(other.inferred) && state.lessOrEqual(other.state);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((inferred == null) ? 0 : inferred.hashCode());
			result = prime * result + ((state == null) ? 0 : state.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InferredPair<?> other = (InferredPair<?>) obj;
			if (inferred == null) {
				if (other.inferred != null)
					return false;
			} else if (!inferred.equals(other.inferred))
				return false;
			if (state == null) {
				if (other.state != null)
					return false;
			} else if (!state.equals(other.state))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		/**
		 * Yields a {@link DomainRepresentation} of the information contained in
		 * this pair.
		 * 
		 * @return the representation
		 */
		public DomainRepresentation representation() {
			return new InferredPairRepresentation(this);
		}
	}

	/**
	 * A {@link PairRepresentation} for an {@link InferredPair}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	static class InferredPairRepresentation extends PairRepresentation {

		/**
		 * Builds a new representation.
		 * 
		 * @param pair the inferred pair to represent
		 */
		public InferredPairRepresentation(InferredPair<?> pair) {
			super(pair.inferred.representation(), pair.state.representation());
		}

		@Override
		public String toString() {
			return "inferred: " + left + ", state: " + right;
		}
	}
}
