package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An identifier of a synthetic program variable that represents a resolved
 * memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapLocation extends Identifier {

	/**
	 * Builds the heap location.
	 * 
	 * @param types the runtime types of this expression
	 * @param name  the name of the location
	 * @param weak  whether or not this identifier is weak, meaning that it
	 *                  should only receive weak assignments
	 */
	public HeapLocation(ExternalSet<Type> types, String name, boolean weak) {
		super(types, name, weak);
	}

	@Override
	public String toString() {
		return "heap[" + (isWeak() ? "w" : "s") + "]:" + getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isWeak() ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeapLocation other = (HeapLocation) obj;
		if (isWeak() != other.isWeak())
			return false;
		return true;
	}

	@Override
	public Identifier lub(Identifier other) throws SemanticException {
		if (!getName().equals(other.getName()))
			throw new SemanticException("Cannot perform the least upper bound between different identifiers: '" + this
					+ "' and '" + other + "'");
		return isWeak() ? this : other;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
