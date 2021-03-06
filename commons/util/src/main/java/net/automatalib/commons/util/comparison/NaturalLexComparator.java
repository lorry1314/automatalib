/* Copyright (C) 2013 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
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
package net.automatalib.commons.util.comparison;

import java.util.Comparator;

/**
 * Lexicographical comparator using the natural ordering.
 * 
 * @author Malte Isberner 
 *
 * @param <T> iterable class
 * @param <U> element class
 */
final class NaturalLexComparator<T extends Iterable<U>, U extends Comparable<U>>
		implements Comparator<T> {
	
	@SuppressWarnings("rawtypes")
	private static final NaturalLexComparator<?,?> INSTANCE
		= new NaturalLexComparator();
	
	@SuppressWarnings("unchecked")
	public static <T extends Iterable<U>, U extends Comparable<U>>
	NaturalLexComparator<T,U> getInstance() {
		return (NaturalLexComparator<T,U>)INSTANCE;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(T o1, T o2) {
		return CmpUtil.lexCompare(o1, o2);
	}

}
