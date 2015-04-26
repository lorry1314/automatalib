/* Copyright (C) 2013 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
 * 
 * AutomataLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * AutomataLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with AutomataLib; if not, see
 * http://www.gnu.de/documents/lgpl.en.html.
 */
package net.automatalib.incremental.dfa.dag;

import java.util.ArrayDeque;
import java.util.Deque;

import net.automatalib.incremental.ConflictException;
import net.automatalib.incremental.dfa.Acceptance;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

/**
 * Incrementally builds an (acyclic) DFA, from a set of positive and negative words.
 * Using {@link #insert(Word, boolean)}, either the set of words definitely in the target language
 * or definitely <i>not</i> in the target language is augmented. The {@link #lookup(Word)} method
 * then returns, for a given word, whether this word is in the set of definitely accepted
 * words ({@link Acceptance#TRUE}), definitely rejected words ({@link Acceptance#FALSE}), or
 * neither ({@link Acceptance#DONT_KNOW}).
 * 
 * @author Malte Isberner 
 *
 * @param <I> input symbol class
 */
public class IncrementalDFADAGBuilder<I> extends AbstractIncrementalDFADAGBuilder<I> {
	
	/**
	 * Constructor. Initializes the incremental builder.
	 * @param inputAlphabet the input alphabet to use
	 */
	public IncrementalDFADAGBuilder(Alphabet<I> inputAlphabet) {
		super(inputAlphabet);
	}
	
	
	/**
	 * Checks the ternary acceptance status for a given word.
	 * @param word the word to check
	 * @return the acceptance status for the given word
	 */
	@Override
	public Acceptance lookup(Word<? extends I> word) {
		State s = getState(word);
		if(s == null)
			return Acceptance.DONT_KNOW;
		return s.getAcceptance();
	}
	
	/**
	 * Inserts a word into either the set of accepted or rejected words.
	 * @param word the word to insert
	 * @param accepting whether to insert this word into the set of accepted or rejected words.
	 */
	@Override
	public void insert(Word<? extends I> word, boolean accepting) {
		int len = word.length();
		Acceptance acc = Acceptance.fromBoolean(accepting);
		
		State curr = init;
		State conf = null;
		
		
		Deque<PathElem> path = new ArrayDeque<>();
		
		for(I sym : word) {
			if(conf == null && curr.isConfluence()) {
				conf = curr;
			}
			
			int idx = inputAlphabet.getSymbolIndex(sym);
			State succ = curr.getSuccessor(idx);
			if(succ == null)
				break;
			path.push(new PathElem(curr, idx));
			curr = succ;
		}
		
		int prefixLen = path.size();
		
		State last = curr;
		
		if(prefixLen == len) {
			// structural skeleton for this word already present
			Acceptance currAcc = curr.getAcceptance();
			if(currAcc == acc) {
				// consistent with our existing knowledge, nothing changes
				return;
			}
			
			if(currAcc != Acceptance.DONT_KNOW) {
				// conflict
				throw new ConflictException("Incompatible acceptances: " + currAcc + " vs " + acc);
			}
			if(conf != null || last.isConfluence()) {
				// there is a confluence (maybe in the last node), so duplicate
				// the last node (this will have to be propagated)
				last = clone(last, acc);
			}
			else if(last == init) {
				// we inserted the empty word, so update the acceptance of the initial
				// state (nothing else changes)
				updateInitSignature(acc);
				return;
			}
			else {
				// no confluence, so just update the signature
				last = updateSignature(last, acc);
			}
		}
		else {
			// we had to abort after processing a prefix
			if(conf != null) {
				if(conf == last) {
					// if the first confluence is the last node, this confluence gets resolved
					// directly by cloning (so act as if there was no confluence)
					conf = null;
				}
				// confluence always requires cloning, to separate this path from other paths
				last = hiddenClone(last);
			}
			else if(last != init) {
				hide(last);
			}
			
			Word<? extends I> suffix = word.subWord(prefixLen);
			I sym = suffix.firstSymbol();
			int suffTransIdx = inputAlphabet.getSymbolIndex(sym);
			State suffixState = createSuffix(suffix.subWord(1), acc);
			
			if(last != init) {
				last = unhide(last, suffTransIdx, suffixState);
			}
			else {
				updateInitSignature(suffTransIdx, suffixState);
			}
		}
		
		if(path.isEmpty())
			return;
		
		if(conf != null) {
			PathElem next;
			do {
				next = path.pop();
				State state = next.state;
				int idx = next.transIdx;
				state = clone(state, idx, last);
				last = state;
			} while(next.state != conf);
		}
		

		while(path.size() > 1) {
			PathElem next = path.pop();
			State state = next.state;
			int idx = next.transIdx;
			State updated = updateSignature(state, idx, last);
			if(state == updated)
				return;
			last = updated;
		}
		
		int finalIdx = path.pop().transIdx;
		
		updateInitSignature(finalIdx, last);
	}
	
	
	
	/**
	 * Retrieves the state reached by a given word.
	 * @param word the word
	 * @return the state reached by the given word, or <tt>null</tt> if no state is reachable
	 * by that word
	 */
	@Override
	protected State getState(Word<? extends I> word) {
		State s = init;
		
		for(I sym : word) {
			int idx = inputAlphabet.getSymbolIndex(sym);
			s = s.getSuccessor(idx);
			if(s == null)
				return null;
		}
		return s;
	}

	
	
	
	/**
	 * Creates a suffix state sequence, i.e., a linear sequence of states connected by transitions
	 * labeled by the letters of the given suffix word.
	 * @param suffix the suffix word
	 * @param acc the acceptance status of the final state
	 * @return the first state in the sequence
	 */
	private State createSuffix(Word<? extends I> suffix, Acceptance acc) {
		StateSignature sig = new StateSignature(alphabetSize, acc);
		sig.updateHashCode();
		State last = replaceOrRegister(sig);
		
		int len = suffix.length();
		for(int i = len - 1; i >= 0; i--) {
			sig = new StateSignature(alphabetSize, Acceptance.DONT_KNOW);
			I sym = suffix.getSymbol(i);
			int idx = inputAlphabet.getSymbolIndex(sym);
			sig.successors[idx] = last;
			sig.updateHashCode();
			last = replaceOrRegister(sig);
		}
		
		return last;
	}
	

}