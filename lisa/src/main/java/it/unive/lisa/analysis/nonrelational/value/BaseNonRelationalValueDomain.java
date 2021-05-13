package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * Base implementation for {@link NonRelationalValueDomain}s. This class extends
 * {@link BaseLattice} and implements
 * {@link NonRelationalValueDomain#eval(it.unive.lisa.symbolic.SymbolicExpression, it.unive.lisa.analysis.lattices.FunctionalLattice, ProgramPoint)}
 * by taking care of the recursive computation of inner expressions evaluation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of this domain
 */
public abstract class BaseNonRelationalValueDomain<T extends BaseNonRelationalValueDomain<T>> extends BaseLattice<T>
		implements NonRelationalValueDomain<T> {

	@SuppressWarnings("unchecked")
	private class EvaluationVisitor implements ExpressionVisitor<T> {

		@Override
		public T visit(AccessChild expression, T receiver, T child, Object... params) throws SemanticException {
			throw new SemanticException("Cannot process a heap expression with a non-relational value domain");
		}

		@Override
		public T visit(HeapAllocation expression, Object... params) throws SemanticException {
			throw new SemanticException("Cannot process a heap expression with a non-relational value domain");
		}

		@Override
		public T visit(UnaryExpression expression, T arg, Object... params) throws SemanticException {
			if (arg.isBottom())
				return arg;

			return evalUnaryExpression(expression.getOperator(), arg, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(BinaryExpression expression, T left, T right, Object... params) throws SemanticException {
			if (left.isBottom())
				return left;
			if (right.isBottom())
				return right;

			if (expression.getOperator() == BinaryOperator.TYPE_CAST)
				return evalTypeCast(expression, left, right, (ProgramPoint) params[1]);

			if (expression.getOperator() == BinaryOperator.TYPE_CONV)
				return evalTypeConv(expression, left, right, (ProgramPoint) params[1]);

			return evalBinaryExpression(expression.getOperator(), left, right, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(TernaryExpression expression, T left, T middle, T right, Object... params)
				throws SemanticException {
			if (left.isBottom())
				return left;
			if (middle.isBottom())
				return middle;
			if (right.isBottom())
				return right;

			return evalTernaryExpression(expression.getOperator(), left, middle, right, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(Skip expression, Object... params) throws SemanticException {
			return bottom();
		}

		@Override
		public T visit(PushAny expression, Object... params) throws SemanticException {
			return evalPushAny(expression, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(Constant expression, Object... params) throws SemanticException {
			if (expression instanceof NullConstant)
				return evalNullConstant((ProgramPoint) params[1]);
			return evalNonNullConstant(expression, (ProgramPoint) params[1]);
		}

		@Override
		public T visit(Identifier expression, Object... params) throws SemanticException {
			return evalIdentifier(expression, (ValueEnvironment<T>) params[0], (ProgramPoint) params[1]);
		}

	}

	@Override
	public final Satisfiability satisfies(ValueExpression expression, ValueEnvironment<T> environment,
			ProgramPoint pp) throws SemanticException {
		if (expression instanceof Identifier)
			return satisfiesAbstractValue(environment.getState((Identifier) expression), pp);

		if (expression instanceof NullConstant)
			return satisfiesNullConstant(pp);

		if (expression instanceof Constant)
			return satisfiesNonNullConstant((Constant) expression, pp);

		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			if (unary.getOperator() == UnaryOperator.LOGICAL_NOT)
				return satisfies((ValueExpression) unary.getExpression(), environment, pp).negate();
			else {
				T arg = eval((ValueExpression) unary.getExpression(), environment, pp);
				if (arg.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesUnaryExpression(unary.getOperator(), arg, pp);
			}
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			if (binary.getOperator() == BinaryOperator.LOGICAL_AND)
				return satisfies((ValueExpression) binary.getLeft(), environment, pp)
						.and(satisfies((ValueExpression) binary.getRight(), environment, pp));
			else if (binary.getOperator() == BinaryOperator.LOGICAL_OR)
				return satisfies((ValueExpression) binary.getLeft(), environment, pp)
						.or(satisfies((ValueExpression) binary.getRight(), environment, pp));
			else {
				T left = eval((ValueExpression) binary.getLeft(), environment, pp);
				if (left.isBottom())
					return Satisfiability.BOTTOM;

				T right = eval((ValueExpression) binary.getRight(), environment, pp);
				if (right.isBottom())
					return Satisfiability.BOTTOM;

				return satisfiesBinaryExpression(binary.getOperator(), left, right, pp);
			}
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			T left = eval((ValueExpression) ternary.getLeft(), environment, pp);
			if (left.isBottom())
				return Satisfiability.BOTTOM;

			T middle = eval((ValueExpression) ternary.getMiddle(), environment, pp);
			if (middle.isBottom())
				return Satisfiability.BOTTOM;

			T right = eval((ValueExpression) ternary.getRight(), environment, pp);
			if (right.isBottom())
				return Satisfiability.BOTTOM;

			return satisfiesTernaryExpression(ternary.getOperator(), left, middle, right, pp);
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public final T eval(ValueExpression expression, ValueEnvironment<T> environment, ProgramPoint pp)
			throws SemanticException {
		return expression.accept(new EvaluationVisitor(), environment, pp);
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		// As default, base non relational values domains
		// tracks only non-pointer identifier
		return !id.getDynamicType().isPointerType();
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		return !expression.getDynamicType().isPointerType();
	}

	/**
	 * Yields the evaluation of an identifier in a given environment.
	 * 
	 * @param id          the identifier to be evaluated
	 * @param environment the environment where the identifier must be evaluated
	 * @param pp          the program point that where this operation is being
	 *                        evaluated
	 * 
	 * @return the evaluation of the identifier
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected T evalIdentifier(Identifier id, ValueEnvironment<T> environment, ProgramPoint pp)
			throws SemanticException {
		return environment.getState(id);
	}

	/**
	 * Yields the evaluation of a push-any expression.
	 * 
	 * @param pushAny the push-any expression to be evaluated
	 * @param pp      the program point that where this operation is being
	 *                    evaluated
	 * 
	 * @return the evaluation of the push-any expression
	 */
	protected T evalPushAny(PushAny pushAny, ProgramPoint pp) {
		return top();
	}

	/**
	 * Yields the evaluation of a type conversion expression.
	 * 
	 * @param conv  the type conversion expression
	 * @param left  the left expression, namely the expression to be converted
	 * @param right the right expression, namely the types to which left should
	 *                  be converted
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return the evaluation of the type conversion expression
	 */
	protected T evalTypeConv(BinaryExpression conv, T left, T right, ProgramPoint pp) {
		return conv.getTypes().isEmpty() ? bottom() : left;
	}

	/**
	 * Yields the evaluation of a type cast expression.
	 * 
	 * @param cast  the type casted expression
	 * @param left  the left expression, namely the expression to be casted
	 * @param right the right expression, namely the types to which left should
	 *                  be casted
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return the evaluation of the type cast expression
	 */
	protected T evalTypeCast(BinaryExpression cast, T left, T right, ProgramPoint pp) {
		return cast.getTypes().isEmpty() ? bottom() : left;
	}

	/**
	 * Yields the evaluation of the null constant {@link NullConstant}.
	 * 
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return the evaluation of the constant
	 */
	protected T evalNullConstant(ProgramPoint pp) {
		return top();
	}

	/**
	 * Yields the evaluation of the given non-null constant.
	 * 
	 * @param constant the constant to evaluate
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the constant
	 */
	protected T evalNonNullConstant(Constant constant, ProgramPoint pp) {
		return top();
	}

	/**
	 * Yields the evaluation of a {@link UnaryExpression} applying
	 * {@code operator} to an expression whose abstract value is {@code arg}. It
	 * is guaranteed that {@code arg} is not {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param arg      the instance of this domain representing the abstract
	 *                     value of the expresion's argument
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 */
	protected T evalUnaryExpression(UnaryOperator operator, T arg, ProgramPoint pp) {
		return top();
	}

	/**
	 * Yields the evaluation of a {@link BinaryExpression} applying
	 * {@code operator} to two expressions whose abstract value are {@code left}
	 * and {@code right}, respectively. It is guaranteed that both {@code left}
	 * and {@code right} are not {@link #bottom()} and that {@code operator} is
	 * neither {@link BinaryOperator#TYPE_CAST} nor
	 * {@link BinaryOperator#TYPE_CONV}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param left     the instance of this domain representing the abstract
	 *                     value of the left-hand side argument
	 * @param right    the instance of this domain representing the abstract
	 *                     value of the right-hand side argument
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 */
	protected T evalBinaryExpression(BinaryOperator operator, T left, T right, ProgramPoint pp) {
		return top();
	}

	/**
	 * Yields the evaluation of a {@link TernaryExpression} applying
	 * {@code operator} to two expressions whose abstract value are
	 * {@code left}, {@code middle} and {@code right}, respectively. It is
	 * guaranteed that both {@code left} and {@code right} are not
	 * {@link #bottom()}.
	 * 
	 * @param operator the operator applied by the expression
	 * @param left     the instance of this domain representing the abstract
	 *                     value of the left-hand side argument
	 * @param middle   the instance of this domain representing the abstract
	 *                     value of the middle argument
	 * @param right    the instance of this domain representing the abstract
	 *                     value of the right-hand side argument
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return the evaluation of the expression
	 */
	protected T evalTernaryExpression(TernaryOperator operator, T left, T middle, T right, ProgramPoint pp) {
		return top();
	}

	/**
	 * Yields the satisfiability of an abstract value of type {@code <T>}.
	 * 
	 * @param value the abstract value whose satisfiability is to be evaluated
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected Satisfiability satisfiesAbstractValue(T value, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of the null constant {@link NullConstant} on
	 * this abstract domain.
	 * 
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected Satisfiability satisfiesNullConstant(ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of the given non-null constant on this abstract
	 * domain.
	 * 
	 * @param constant the constant to satisfied
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} is the constant is satisfied by
	 *             this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of a {@link UnaryExpression} applying
	 * {@code operator} to an expression whose abstract value is {@code arg},
	 * returning an instance of {@link Satisfiability}. It is guaranteed that
	 * {@code operator} is not {@link UnaryOperator#LOGICAL_NOT} and {@code arg}
	 * is not {@link #bottom()}.
	 * 
	 * @param operator the unary operator applied by the expression
	 * @param arg      an instance of this abstract domain representing the
	 *                     argument of the unary expression
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, T arg, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of a {@link BinaryExpression} applying
	 * {@code operator} to two expressions whose abstract values are
	 * {@code left}, and {@code right}. This method returns an instance of
	 * {@link Satisfiability}. It is guaranteed that {@code operator} is neither
	 * {@link BinaryOperator#LOGICAL_AND} nor {@link BinaryOperator#LOGICAL_OR},
	 * and that both {@code left} and {@code right} are not {@link #bottom()}.
	 * 
	 * @param operator the binary operator applied by the expression
	 * @param left     an instance of this abstract domain representing the
	 *                     argument of the left-hand side of the binary
	 *                     expression
	 * @param right    an instance of this abstract domain representing the
	 *                     argument of the right-hand side of the binary
	 *                     expression
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, T left, T right,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the satisfiability of a {@link TernaryExpression} applying
	 * {@code operator} to three expressions whose abstract values are
	 * {@code left}, {@code middle} and {@code right}. This method returns an
	 * instance of {@link Satisfiability}. It is guaranteed that {@code left},
	 * {@code middle} and {@code right} are not {@link #bottom()}.
	 * 
	 * @param operator the ternary operator applied by the expression
	 * @param left     an instance of this abstract domain representing the
	 *                     argument of the left-most side of the ternary
	 *                     expression
	 * @param middle   an instance of this abstract domain representing the
	 *                     argument in the middle of the ternary expression
	 * @param right    an instance of this abstract domain representing the
	 *                     argument of the right-most side of the ternary
	 *                     expression
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} if the expression is satisfied
	 *             by this domain, {@link Satisfiability#NOT_SATISFIED} if it is
	 *             not satisfied, or {@link Satisfiability#UNKNOWN} if it is
	 *             either impossible to determine if it satisfied, or if it is
	 *             satisfied by some values and not by some others (this is
	 *             equivalent to a TOP boolean value)
	 */
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, T left, T middle, T right,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public final String toString() {
		return representation().toString();
	}

	@Override
	public final ValueEnvironment<T> assume(ValueEnvironment<T> environment, ValueExpression expression,
			ProgramPoint pp) throws SemanticException {
		if (expression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) expression;

			if (unary.getOperator() == UnaryOperator.LOGICAL_NOT) {
				ValueExpression rewritten = unary.removeNegations();
				// It is possible that the expression cannot be rewritten (e.g.,
				// !true) hence we recursively call assume iff something changed
				if (rewritten != unary)
					return assume(environment, rewritten, pp);
			}

			return assumeUnaryExpression(environment, unary.getOperator(), (ValueExpression) unary.getExpression(), pp);
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) expression;

			if (binary.getOperator() == BinaryOperator.LOGICAL_AND)
				return assume(environment, (ValueExpression) binary.getLeft(), pp)
						.glb(assume(environment, (ValueExpression) binary.getRight(), pp));
			else if (binary.getOperator() == BinaryOperator.LOGICAL_OR)
				return assume(environment, (ValueExpression) binary.getLeft(), pp)
						.lub(assume(environment, (ValueExpression) binary.getRight(), pp));
			else
				return assumeBinaryExpression(environment, binary.getOperator(), (ValueExpression) binary.getLeft(),
						(ValueExpression) binary.getRight(), pp);
		}

		if (expression instanceof TernaryExpression) {
			TernaryExpression ternary = (TernaryExpression) expression;

			return assumeTernaryExpression(environment, ternary.getOperator(), (ValueExpression) ternary.getLeft(),
					(ValueExpression) ternary.getMiddle(), (ValueExpression) ternary.getRight(), pp);
		}

		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a ternary
	 * expression with operator {@code operator}, left argument {@code left},
	 * middle argument {@code middle},and right argument {@code right} holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param operator    the operator of the ternary expression
	 * @param left        the left-hand side argument of the ternary expression
	 * @param middle      the middle-hand side argument of the ternary
	 *                        expression
	 * @param right       the right-hand side argument of the ternary expression
	 * @param pp          the program point where the ternary expression occurs
	 * 
	 * @return the environment {@code environment} assuming that a ternary
	 *             expression with operator {@code operator}, left argument
	 *             {@code left}, middle argument {@code middle},and right
	 *             argument {@code right} holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	protected ValueEnvironment<T> assumeTernaryExpression(ValueEnvironment<T> environment,
			TernaryOperator operator, ValueExpression left, ValueExpression middle, ValueExpression right,
			ProgramPoint pp) throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that a binary
	 * expression with operator {@code operator}, left argument {@code left},
	 * and right argument {@code right} holds. The binary expression with binary
	 * operator {@link BinaryOperator#LOGICAL_AND} and
	 * {@link BinaryOperator#LOGICAL_OR} are already handled by
	 * {@link BaseNonRelationalValueDomain#assume}.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param operator    the operator of the binary expression
	 * @param left        the left-hand side argument of the binary expression
	 * @param right       the right-hand side argument of the binary expression
	 * @param pp          the program point where the binary expression occurs
	 * 
	 * @return the environment {@code environment} assuming that a binary
	 *             expression with operator {@code operator}, left argument
	 *             {@code left}, and right argument {@code right} holds
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	protected ValueEnvironment<T> assumeBinaryExpression(ValueEnvironment<T> environment,
			BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint pp)
			throws SemanticException {
		return environment;
	}

	/**
	 * Yields the environment {@code environment} assuming that an unary
	 * expression with operator {@code operator} and argument {@code expression}
	 * holds.
	 * 
	 * @param environment the environment on which the expression must be
	 *                        assumed
	 * @param operator    the operator of the unary expression
	 * @param expression  the argument of the unary expression
	 * @param pp          the program point where the unary expression occurs
	 * 
	 * @return the environment {@code environment} assuming that an unary
	 *             expression with operator {@code operator} and argument
	 *             {@code expression} holds.
	 * 
	 * @throws SemanticException if something goes wrong during the assumption
	 */
	protected ValueEnvironment<T> assumeUnaryExpression(ValueEnvironment<T> environment,
			UnaryOperator operator, ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return environment;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T glb(T other) throws SemanticException {
		if (other == null || this.isBottom() || other.isTop() || this == other || this.equals(other)
				|| this.lessOrEqual(other))
			return (T) this;

		if (other.isBottom() || this.isTop() || other.lessOrEqual((T) this))
			return (T) other;

		return glbAux(other);
	}

	/**
	 * Performs the greatest lower bound operation between this domain element
	 * and {@code other}, assuming that base cases have already been handled. In
	 * particular, it is guaranteed that:
	 * <ul>
	 * <li>{@code other} is not {@code null}</li>
	 * <li>{@code other} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} is neither <i>top</i> nor <i>bottom</i></li>
	 * <li>{@code this} and {@code other} are not the same object (according
	 * both to {@code ==} and to {@link Object#equals(Object)})</li>
	 * <li>{@code this} and {@code other} are not comparable</li>
	 * </ul>
	 * 
	 * @param other the other domain element
	 * 
	 * @return the greatest lower bound between this domain element and other
	 */
	public T glbAux(T other) {
		return bottom();
	}
}
