/* Copyright (C) 2014 TU Dortmund
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
package net.automatalib.util.automata.predicates;

import javax.annotation.Nonnull;

import net.automatalib.automata.fsa.FiniteStateAcceptor;

import com.google.common.base.Predicate;

final class AcceptanceStatePredicate<S> implements Predicate<S> {
	private final FiniteStateAcceptor<? super S, ?> fsa;
	private final boolean acceptance;
	
	public AcceptanceStatePredicate(FiniteStateAcceptor<? super S, ?> fsa, boolean acceptance) {
		this.fsa = fsa;
		this.acceptance = acceptance;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.google.common.base.Predicate#apply(java.lang.Object)
	 */
	@Override
	public boolean apply(@Nonnull S state) {
		return fsa.isAccepting(state) == acceptance;
	}
	
	
}
