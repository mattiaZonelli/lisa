package it.unive.lisa.analysis.representation;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link DomainRepresentation} in the form of a set of values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SetRepresentation extends DomainRepresentation {

	private final SortedSet<DomainRepresentation> elements;

	/**
	 * Builds a new representation starting from the given set. {@code mapper}
	 * is used for transforming each element in the set to its individual
	 * representation.
	 * 
	 * @param <E>      the type of elements in the set
	 * @param elements the set to represent
	 * @param mapper   the function that knows how to convert elements to their
	 *                     representation
	 */
	public <E> SetRepresentation(Set<E> elements, Function<E, DomainRepresentation> mapper) {
		this(mapAndSort(elements, mapper));
	}

	private static <E> SortedSet<DomainRepresentation> mapAndSort(Iterable<E> elements,
			Function<E, DomainRepresentation> mapper) {
		SortedSet<DomainRepresentation> result = new TreeSet<>();
		for (E e : elements)
			result.add(mapper.apply(e));
		return result;
	}

	/**
	 * Builds a new representation containing the given set.
	 * 
	 * @param elements the set
	 */
	public SetRepresentation(Set<DomainRepresentation> elements) {
		if (elements instanceof SortedSet)
			this.elements = (SortedSet<DomainRepresentation>) elements;
		else
			this.elements = new TreeSet<>(elements);
	}

	@Override
	public String toString() {
		return "[" + StringUtils.join(elements, ", ") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
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
		SetRepresentation other = (SetRepresentation) obj;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		return true;
	}
}
