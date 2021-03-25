/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.ports.metadata.table;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * Wrapper for a value set from a {@link DictionaryInfo} to use in
 * {@link com.rapidminer.operator.ports.metadata.AttributeMetaData}
 * when converting with the {@link FromTableMetaDataConverter}. Keeps a valueSet and copies it on the first write
 * operation. The wrapping prevents a copy of the set to ensure immutability of the {@link DictionaryInfo} and allows to
 * reuse the underlying set if converting back to a {@link DictionaryInfo} in the {@link
 * com.rapidminer.operator.ports.metadata.ToTableMetaDataConverter} and the valueSet has not been changed.
 *
 * This set implementation is not thread-safe. Existing synchronization only ensures that the original value set is not
 * changed.
 *
 * @author Kevin Majchrzak, Gisa Meier
 * @since 9.9.0
 */
class CopyOnWriteValueSet implements Set<String>, Serializable {

	private Set<String> valueSet;
	private boolean copied;

	CopyOnWriteValueSet(Set<String> valueSet) {
		this.valueSet = valueSet;
		copied = false;
	}

	@Override
	public int size() {
		return valueSet.size();
	}

	@Override
	public boolean isEmpty() {
		return valueSet.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return valueSet.contains(o);
	}

	@Override
	public Iterator<String> iterator() {
		if (copied) {
			return valueSet.iterator();
		}

		return new Iterator<String>() {

			private Iterator<String> baseIterator = valueSet.iterator();
			private int position = 0;
			private boolean wasCopied = false;

			@Override
			public boolean hasNext() {
				return baseIterator.hasNext();
			}

			@Override
			public String next() {
				position++;
				return baseIterator.next();
			}

			@Override
			public void remove() {
				if (!wasCopied) {
					copyIfNotCopied();
					wasCopied = true;
					baseIterator = valueSet.iterator();
					for (int i = 0; i < position; i++) {
						baseIterator.next();
					}
				}
				baseIterator.remove();
			}
		};
	}

	@Override
	public void forEach(Consumer<? super String> action) {
		valueSet.forEach(action);
	}

	@Override
	public Object[] toArray() {
		return valueSet.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return valueSet.toArray(a);
	}

	@Override
	public boolean add(String s) {
		copyIfNotCopied();
		return valueSet.add(s);
	}

	@Override
	public boolean remove(Object o) {
		copyIfNotCopied();
		return valueSet.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return valueSet.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends String> c) {
		copyIfNotCopied();
		return valueSet.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		copyIfNotCopied();
		return valueSet.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		copyIfNotCopied();
		return valueSet.removeAll(c);
	}

	@Override
	public boolean removeIf(Predicate<? super String> filter) {
		copyIfNotCopied();
		return valueSet.removeIf(filter);
	}

	@Override
	public void clear() {
		copyIfNotCopied();
		valueSet.clear();
	}

	@Override
	public Spliterator<String> spliterator() {
		return valueSet.spliterator();
	}

	@Override
	public Stream<String> stream() {
		return valueSet.stream();
	}

	@Override
	public Stream<String> parallelStream() {
		return valueSet.parallelStream();
	}

	/**
	 * @return the underlying value set.
	 */
	synchronized Set<String> getValueSet() {
		return valueSet;
	}

	/**
	 * @return whether the underlying value set has not been copied
	 */
	synchronized boolean isUnchanged() {
		return !copied;
	}

	/**
	 * Copies the value set if it has not been copied before.
	 */
	private synchronized void copyIfNotCopied() {
		if (!copied) {
			valueSet = new TreeSet<>(valueSet);
			copied = true;
		}
	}

	/**
	 * When serializing, no change takes place, so only write the value set and not the wrapper.
	 */
	private Object writeReplace() throws ObjectStreamException {
		return valueSet;
	}

	@Override
	public String toString() {
		return valueSet.toString();
	}
}
