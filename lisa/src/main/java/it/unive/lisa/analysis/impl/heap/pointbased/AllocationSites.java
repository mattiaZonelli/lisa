package it.unive.lisa.analysis.impl.heap.pointbased;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A heap domain tracking sets of {@link AllocationSite}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class AllocationSites extends SetLattice<AllocationSites, AllocationSite>
		implements NonRelationalHeapDomain<AllocationSites> {

	private static final AllocationSites TOP = new AllocationSites(new HashSet<>(), true);
	private static final AllocationSites BOTTOM = new AllocationSites(new HashSet<>(), false);

	private final boolean isTop;

	/**
	 * Builds an instance of HeapIdentiferSetLattice, corresponding to the top
	 * element.
	 */
	public AllocationSites() {
		this(new HashSet<>(), true);
	}

	private AllocationSites(Set<AllocationSite> set, boolean isTop) {
		super(set);
		this.isTop = isTop;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public boolean isBottom() {
		return !isTop && elements.isEmpty();
	}

	@Override
	public AllocationSites top() {
		return TOP;
	}

	@Override
	public AllocationSites bottom() {
		return BOTTOM;
	}

	@Override
	protected AllocationSites mk(Set<AllocationSite> set) {
		return new AllocationSites(set, false);
	}

	@Override
	public Iterator<AllocationSite> iterator() {
		return this.elements.iterator();
	}

	@Override
	public AllocationSites eval(SymbolicExpression expression,
			HeapEnvironment<AllocationSites> environment, ProgramPoint pp) {
		return new AllocationSites(Collections.singleton((AllocationSite) expression), false);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression,
			HeapEnvironment<AllocationSites> environment, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isTop ? 1231 : 1237);
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
		AllocationSites other = (AllocationSites) obj;
		if (isTop != other.isTop)
			return false;
		return true;
	}

	@Override
	public DomainRepresentation representation() {
		return new StringRepresentation(toString());
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return Collections.emptyList();
	}

	@Override
	protected AllocationSites lubAux(AllocationSites other) throws SemanticException {
		Map<String, AllocationSite> lub = new HashMap<>();

		// all weak identifiers are part of the lub
		elements.stream().filter(AllocationSite::isWeak).forEach(e -> lub.put(e.getName(), e));
		// common ones will be overwritten
		other.elements.stream().filter(AllocationSite::isWeak).forEach(e -> lub.put(e.getName(), e));

		// strong identifiers are only added if we did not consider a
		// weak identifier with the same name
		elements.stream().filter(Predicate.not(AllocationSite::isWeak))
				.filter(e -> !lub.containsKey(e.getName()))
				.forEach(e -> lub.put(e.getName(), e));

		other.elements.stream().filter(Predicate.not(AllocationSite::isWeak))
				.filter(e -> !lub.containsKey(e.getName()))
				.forEach(e -> lub.put(e.getName(), e));

		return new AllocationSites(new HashSet<>(lub.values()), false);
	}

	@Override
	public boolean tracksIdentifiers(Identifier id) {
		return id.getDynamicType().isPointerType() || id.getDynamicType().isUntyped();
	}

	@Override
	public boolean canProcess(SymbolicExpression expression) {
		return expression.getDynamicType().isPointerType() || expression.getDynamicType().isUntyped();
	}

	@Override
	public HeapEnvironment<AllocationSites> assume(HeapEnvironment<AllocationSites> environment,
			SymbolicExpression expression,
			ProgramPoint pp) throws SemanticException {
		return environment;
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression,
			HeapEnvironment<AllocationSites> environment, ProgramPoint pp) throws SemanticException {
		return new ExpressionSet<>();
	}
}
