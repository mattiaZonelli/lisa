package it.unive.lisa.analysis.dataflow.impl;

import it.unive.lisa.analysis.dataflow.DataflowElement;
import it.unive.lisa.analysis.dataflow.DefiniteForwardDataflowDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.PairRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of the constant propagation dataflow analysis, that focuses
 * only on integers.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ConstantPropagation
		implements DataflowElement<DefiniteForwardDataflowDomain<ConstantPropagation>, ConstantPropagation> {

	private final Identifier id;
	private final Integer v;

	/**
	 * Builds an empty constant propagation object.
	 */
	public ConstantPropagation() {
		this(null, null);
	}

	private ConstantPropagation(Identifier id, Integer v) {
		this.id = id;
		this.v = v;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public Identifier getIdentifier() {
		return id;
	}

	private Integer eval(SymbolicExpression e, DefiniteForwardDataflowDomain<ConstantPropagation> domain) {

		if (e instanceof Constant) {
			Constant c = (Constant) e;
			return c.getValue() instanceof Integer ? (Integer) c.getValue() : null;
		}

		if (e instanceof Identifier) {
			for (ConstantPropagation cp : domain.getDataflowElements())
				if (cp.getIdentifier().equals(e))
					return cp.v;

			return null;
		}

		if (e instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) e;
			Integer i = eval(unary.getExpression(), domain);

			if (i == null)
				return i;

			switch (unary.getOperator()) {
			case NUMERIC_NEG:
				return -i;
			default:
				break;
			}
		}

		if (e instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) e;
			Integer right = eval(binary.getRight(), domain);
			Integer left = eval(binary.getLeft(), domain);

			if (right == null || left == null)
				return null;

			switch (binary.getOperator()) {
			case NUMERIC_ADD:
				return left + right;
			case NUMERIC_DIV:
				return left == 0 ? null : (int) left / right;
			case NUMERIC_MOD:
				return right == 0 ? null : left % right;
			case NUMERIC_MUL:
				return left * right;
			case NUMERIC_SUB:
				return left - right;
			default:
				break;
			}

		}

		return null;
	}

	@Override
	public Collection<ConstantPropagation> gen(Identifier id, ValueExpression expression, ProgramPoint pp,
			DefiniteForwardDataflowDomain<ConstantPropagation> domain) {
		Set<ConstantPropagation> gen = new HashSet<>();

		Integer v = eval(expression, domain);
		if (v != null)
			gen.add(new ConstantPropagation(id, v));

		return gen;
	}

	@Override
	public Collection<Identifier> kill(Identifier id, ValueExpression expression, ProgramPoint pp,
			DefiniteForwardDataflowDomain<ConstantPropagation> domain) {
		Set<Identifier> set = new HashSet<>();
		set.add(id);
		return set;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
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
		ConstantPropagation other = (ConstantPropagation) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return !id.getDynamicType().isPointerType();
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		return !expression.getDynamicType().isPointerType();
	}

	@Override
	public DomainRepresentation representation() {
		return new PairRepresentation(new StringRepresentation(id), new StringRepresentation(v));
	}
}