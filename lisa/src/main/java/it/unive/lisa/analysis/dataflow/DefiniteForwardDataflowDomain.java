package it.unive.lisa.analysis.dataflow;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.SetRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * A {@link DataflowDomain} for <b>forward</b> and <b>definite</b> dataflow
 * analysis. Being definite means that this domain is an instance of
 * {@link InverseSetLattice}, i.e., is a set whose join operation is the set
 * intersection.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of {@link DataflowElement} contained in this domain
 */
public class DefiniteForwardDataflowDomain<E extends DataflowElement<DefiniteForwardDataflowDomain<E>, E>>
		extends InverseSetLattice<DefiniteForwardDataflowDomain<E>, E>
		implements DataflowDomain<DefiniteForwardDataflowDomain<E>, E> {

	private final boolean isTop;

	private final boolean isBottom;

	private final E domain;

	/**
	 * Builds an empty domain.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to perform <i>kill</i> and <i>gen</i> operations
	 */
	public DefiniteForwardDataflowDomain(E domain) {
		this(domain, new HashSet<>(), true, false);
	}

	private DefiniteForwardDataflowDomain(E domain, Set<E> elements, boolean isTop, boolean isBottom) {
		super(elements);
		this.domain = domain;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> assign(Identifier id, ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (isBottom())
			return this;

		// if id cannot be tracked by the underlying lattice,
		// or if the expression cannot be processed, return this
		if (!domain.tracksIdentifiers(id) || !domain.canProcess(expression))
			return this;
		DefiniteForwardDataflowDomain<E> killed = forgetIdentifiers(domain.kill(id, expression, pp, this));
		Set<E> updated = new HashSet<>(killed.elements);
		for (E generated : domain.gen(id, expression, pp, this))
			updated.add(generated);
		return new DefiniteForwardDataflowDomain<E>(domain, updated, false, false);
	}

	@Override
	public DefiniteForwardDataflowDomain<E> smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> assume(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		// TODO could be refined
		return this;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop())
			return this;

		Collection<E> toRemove = new LinkedList<>();
		for (E e : elements)
			if (e.getIdentifier().equals(id))
				toRemove.add(e);

		if (toRemove.isEmpty())
			return this;
		Set<E> updated = new HashSet<>(elements);
		updated.removeAll(toRemove);
		return new DefiniteForwardDataflowDomain<E>(domain, updated, false, false);
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		// TODO could be refined
		return Satisfiability.UNKNOWN;
	}

	@Override
	public DomainRepresentation representation() {
		return new SetRepresentation(elements, DataflowElement::representation);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isTop ? 1231 : 1237);
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefiniteForwardDataflowDomain<E> other = (DefiniteForwardDataflowDomain<E>) obj;
		if (isTop != other.isTop)
			return false;
		return true;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> top() {
		return new DefiniteForwardDataflowDomain<>(domain, new HashSet<>(), true, false);
	}

	@Override
	public boolean isTop() {
		return elements.isEmpty() && isTop;
	}

	@Override
	public DefiniteForwardDataflowDomain<E> bottom() {
		return new DefiniteForwardDataflowDomain<>(domain, new HashSet<>(), false, true);
	}

	@Override
	public boolean isBottom() {
		return elements.isEmpty() && isBottom;
	}

	@Override
	protected DefiniteForwardDataflowDomain<E> mk(Set<E> set) {
		return new DefiniteForwardDataflowDomain<>(domain, set, false, false);
	}

	@Override
	public Collection<E> getDataflowElements() {
		return elements;
	}
}
