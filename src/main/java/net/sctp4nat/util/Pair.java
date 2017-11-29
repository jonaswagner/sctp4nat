/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sctp4nat.util;

/**
 * This class is taken from net.tomp2p.utils. No changes were made.
 * 
 * @author Thomas Bocek
 *
 * @param <K>
 * @param <V>
 */
public class Pair<K, V> {

	private final K element0;
	private final V element1;

	public static <K, V> Pair<K, V> create(K element0, V element1) {
		return new Pair<K, V>(element0, element1);
	}

	public Pair(K element0, V element1) {
		this.element0 = element0;
		this.element1 = element1;
	}

	public K element0() {
		return element0;
	}

	public V element1() {
		return element1;
	}
	
	public Pair<K, V> element0(K element0) {
	    return new Pair<K, V>(element0, element1);
    }
	
	public Pair<K, V> element1(V element1) {
	    return new Pair<K, V>(element0, element1);
    }
	
	public boolean isEmpty() {
		return element0 == null && element1 == null;
	}
	
	public static <K,V> Pair<K, V> empty() {
		return new Pair<K, V>(null, null);
	}

	/**
	 * Checks the two objects for equality by delegating to their respective
	 * {@link Object#equals(Object)} methods.
	 * 
	 * @param o
	 *            the {@link Pair} to which this one is to be checked for
	 *            equality
	 * @return true if the underlying objects of the Pair are both considered
	 *         equal
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		Pair<?, ?> p = (Pair<?, ?>) o;
		return equals(p.element0, element0) && equals(p.element1, element1);
	}

	/**
	 * Compute a hash code using the hash codes of the underlying objects
	 * 
	 * @return a hashcode of the Pair
	 */
	@Override
	public int hashCode() {
		return (element0 == null ? 0 : element0.hashCode()) ^ (element1 == null ? 0 : element1.hashCode());
	}

	private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

}