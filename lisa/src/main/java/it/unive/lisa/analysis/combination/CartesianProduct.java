package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.PairRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A generic Cartesian product abstract domain between two non-communicating
 * {@link SemanticDomain}s (i.e., no exchange of information between the
 * abstract domains), assigning the same {@link Identifier}s and handling
 * instances of the same {@link SymbolicExpression}s.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 *
 * @param <C>  the concrete type of the cartesian product
 * @param <T1> the concrete instance of the left-hand side abstract domain of
 *                 the Cartesian product
 * @param <T2> the concrete instance of the right-hand side abstract domain of
 *                 the Cartesian product
 * @param <E>  the type of {@link SymbolicExpression} that {@code <T1>} and
 *                 {@code <T2>}, and in turn this domain, can process
 * @param <I>  the type of {@link Identifier} that {@code <T1>} and
 *                 {@code <T2>}, and in turn this domain, handle
 */
public abstract class CartesianProduct<C extends CartesianProduct<C, T1, T2, E, I>,
		T1 extends SemanticDomain<T1, E, I> & Lattice<T1>,
		T2 extends SemanticDomain<T2, E, I> & Lattice<T2>,
		E extends SymbolicExpression,
		I extends Identifier> implements SemanticDomain<C, E, I>, Lattice<C> {

	/**
	 * The left-hand side abstract domain.
	 */
	protected T1 left;

	/**
	 * The right-hand side abstract domain.
	 */
	protected T2 right;

	/**
	 * Builds the Cartesian product abstract domain.
	 * 
	 * @param left  the left-hand side of the Cartesian product
	 * @param right the right-hand side of the Cartesian product
	 */
	protected CartesianProduct(T1 left, T2 right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
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
		CartesianProduct<?, ?, ?, ?, ?> other = (CartesianProduct<?, ?, ?, ?, ?>) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		if (left instanceof Environment && right instanceof Environment) {
			Environment<?, ?, ?, ?> leftEnv = (Environment<?, ?, ?, ?>) left;
			Environment<?, ?, ?, ?> rightEnv = (Environment<?, ?, ?, ?>) right;
			String result = "";
			if (!leftEnv.isTop() && !leftEnv.isBottom()) {
				for (Identifier x : leftEnv.getKeys())
					result += x + ": (" + leftEnv.getState(x).representation() + ", "
							+ rightEnv.getState(x).representation() + ")\n";
				return result;
			} else if (!rightEnv.isTop() && !rightEnv.isBottom()) {
				for (Identifier x : rightEnv.getKeys())
					result += x + ": (" + leftEnv.getState(x).representation() + ", "
							+ rightEnv.getState(x).representation() + ")\n";
				return result;
			}
		}

		return "(" + left.representation() + ", " + right.representation() + ")";
	}

	/**
	 * Builds a new instance of cartesian product.
	 * 
	 * @param left  the first domain
	 * @param right the second domain
	 * 
	 * @return the new instance of product
	 */
	protected abstract C mk(T1 left, T2 right);

	@Override
	public final DomainRepresentation representation() {
		return new PairRepresentation(left.representation(), right.representation());
	}

	@Override
	public final C assign(I id, E expression, ProgramPoint pp) throws SemanticException {
		T1 newLeft = left.assign(id, expression, pp);
		T2 newRight = right.assign(id, expression, pp);
		return mk(newLeft, newRight);
	}

	@Override
	public final C smallStepSemantics(E expression, ProgramPoint pp) throws SemanticException {
		T1 newLeft = left.smallStepSemantics(expression, pp);
		T2 newRight = right.smallStepSemantics(expression, pp);
		return mk(newLeft, newRight);
	}

	@Override
	public final C assume(E expression, ProgramPoint pp) throws SemanticException {
		T1 newLeft = left.assume(expression, pp);
		T2 newRight = right.assume(expression, pp);
		return mk(newLeft, newRight);
	}

	@Override
	public final C forgetIdentifier(Identifier id) throws SemanticException {
		T1 newLeft = left.forgetIdentifier(id);
		T2 newRight = right.forgetIdentifier(id);
		return mk(newLeft, newRight);
	}

	@Override
	public final Satisfiability satisfies(E expression, ProgramPoint pp) throws SemanticException {
		return left.satisfies(expression, pp).and(right.satisfies(expression, pp));
	}

	@Override
	public final C lub(C other) throws SemanticException {
		return mk(left.lub(other.left), right.lub(other.right));
	}

	@Override
	public final C widening(C other) throws SemanticException {
		return mk(left.widening(other.left), right.widening(other.right));
	}

	@Override
	public final boolean lessOrEqual(C other) throws SemanticException {
		return left.lessOrEqual(other.left) && right.lessOrEqual(other.right);
	}

	@Override
	public final C top() {
		return mk(left.top(), right.top());
	}

	@Override
	public boolean isTop() {
		return left.isTop() && right.isTop();
	}

	@Override
	public final C bottom() {
		return mk(left.bottom(), right.bottom());
	}

	@Override
	public boolean isBottom() {
		return left.isBottom() && right.isBottom();
	}
}