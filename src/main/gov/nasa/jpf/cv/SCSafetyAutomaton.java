//
// Copyright (C) 2007 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.cv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.choice.sc.SCEvent;
import gov.nasa.jpf.jvm.choice.sc.SCEventGenerator;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// <2do> we should use polymorphism to differentiate between assumption and property,
// this is a classic case of a type flag - add SCPropertyAutomaton and SCAssumptionAutomaton
public class SCSafetyAutomaton {

	boolean isAssumption = false; // otherwise it is a property
	int currentState = 0;

	Set<String> alphabet_;

	boolean checkResult = true;
	String automatonName;
	String module;
	Candidate candidate;

  // <2do> this should go away, it is only used to retrieve events that are in the alphabet
  // for which we should use the SCEventGenerator CGs
	Path counterexample;

	private MemoizeTable memoized_;

	// An FSM derived from a Candidate.
	// Maps a transition name(String) to a state (Integer).
	private class FSMState extends HashMap<String, Integer> {
	};

	FSMState[] automaton;

	// a vector of states of the automaton
	// each element is a hashmap (FMState) that stores transitions
	// using as key the action name and value the destination state

	public SCSafetyAutomaton(boolean assumption, String fsmFile) {
		TransitionsParser tp = new TransitionsParser(fsmFile);
		FSM fsm = new FSM();
		tp.parse( fsm);

		isAssumption = assumption;
		automatonName = new File(fsmFile).getName();
		alphabet_ = fsm.getAlphabet();
		createAutomaton(fsm);
	}

	public SCSafetyAutomaton(boolean assumption, AbstractList<String> transitions,
			Vector<String> alpha, String nm, String module) {
		isAssumption = assumption;
		automatonName = nm;

		alphabet_ = new HashSet<String>();
    alphabet_.addAll(alpha);

		createAutomaton(transitions);
		System.out.println(this);
		this.module = module;
	}

	public SCSafetyAutomaton(boolean assumption, Candidate candidate,
			Vector<String> alpha, String nm, String module,  MemoizeTable memoized_) {
		isAssumption = assumption;
		automatonName = nm;

		alphabet_ = new HashSet<String>();
    alphabet_.addAll(alpha);

    this.candidate = candidate;
		this.module = module;
		this.memoized_= memoized_;
		createAutomaton();
		System.out.println(this);
	}
	
	public SCSafetyAutomaton(boolean assumption, Candidate candidate,
			Vector<String> alpha, String nm, String module) {
		isAssumption = assumption;
		automatonName = nm;

		alphabet_ = new HashSet<String>();
    alphabet_.addAll(alpha);

		this.candidate = candidate;
		this.module = module;
		createAutomaton();
		System.out.println(this);
	}
	
	public SCSafetyAutomaton(boolean assumption, FSM fsm, String nm) {
		isAssumption = assumption;
		automatonName = nm;
		alphabet_ = fsm.getAlphabet();
		createAutomaton(fsm);
		System.out.println(this);
	}

	private void createAutomaton() {
		int numStates = candidate.numberOfStates();
		automaton = new FSMState[numStates];

		for (int i = 0; i < numStates; i++) {
			FSMState state = new FSMState();
			automaton[i] = state;
			for (String action : alphabet_) {
				int end = candidate.getTransition(i, action);
				if (end != -1)
					state.put(action, end);
			}
		}
	}

	private void createAutomaton(FSM fsm) {
		int numStates = fsm.numberOfStates();
		automaton = new FSMState[numStates];

		for (int i = 0; i < numStates; i++) {
			FSMState state = new FSMState();
			automaton[i] = state;
			for (String action : alphabet_) {
				int end = fsm.getTransition(i, action);
				if (end != -1)
					state.put(action, end);
			}
		}
	}

	// this one creates automata for queries expressed as a
	// vector of transitions
	private void createAutomaton(AbstractList<String> transitions) {
		int numStates = transitions.size();
		automaton = new FSMState[numStates + 1];

		for (int i = 0; i < numStates; i++) {
			FSMState state = new FSMState();
			automaton[i] = state;
			state.put(transitions.get(i), i + 1);
		}
		automaton[numStates] = new FSMState();
	}

	public void reset() {
		currentState = 0;
	}

	public Path getCounterexample() {
		return counterexample;
	}

	public void advance(String name) {
		advance(name, null, null,false);
	}
	
	public boolean isInAlphabet(String name){
		if (alphabet_.contains(name))
			return true;
		else 
			return false;
	}
	
	public boolean isNextStateError(String name) {
		if (alphabet_.contains(name)) 	
				return( (automaton[currentState].get(name) == null) ? true : false);
		else return false;
	}
	
	public boolean isAssumption() {
		return isAssumption;
	}
	
	public void advance(String name, ThreadInfo ti, JVM jvm, boolean conformance) {
		// remember that you may need to force the thread to yield

		//Thread.dumpStack();
		int lastCurrentState = currentState;
		
		if (alphabet_.contains(name) && currentState != -1) {
			Integer nextState = automaton[currentState].get(name);
			currentState = (nextState == null) ? -1 : nextState;
		}

		if (currentState == -1) {
			if (isAssumption) { //reset to last state
				currentState = lastCurrentState;

			} else {
				//need to make sure the program really failed so
				//check memoized table here with this counterexample
				if (alphabet_.contains(name)){
					counterexample = jvm.getPath(); // <2do> this should go away

					if (conformance){
						//get sequence of alphabet events so that we can check for spurious CE

            AbstractList<String> sequence = getAlphabetSequence(jvm);
						Boolean recalled = memoized_.getResult(sequence);
						
						if (recalled == null) {
							checkResult = false;
							// we have to break the transition here so that we don't see
							// another potential instance of the breaking action
							ti.breakTransition();

						}else{ //this trace is in the teacher's memoized table

							if (recalled.booleanValue() == true){ // means result of query would be false
								counterexample = null;

                // backtrack, but don't ignore this transition - we need to keep it
                ti.breakTransition(true);

							}else{

								checkResult = false;
								// we have to break the transition here so that we don't see
								// another potential instance of the breaking action
								ti.breakTransition();
							}
						}				
					}else {
						checkResult = false;
						// we have to break the transition here so that we don't see
						// another potential instance of the breaking action
						ti.breakTransition();
					}
				}
			}
		}
	}

  AbstractList<String> getAlphabetSequence(JVM vm){
    ArrayList<String> list = new ArrayList<String>();

    for (SCEventGenerator cg : vm.getChoiceGeneratorsOfType( SCEventGenerator.class)){
      SCEvent e = cg.getNextChoice();
      String id = e.getEventName();

      if (alphabet_.contains(id)){
        list.add(id);
      }
    }

    return list;
  }

  /**
   * <2do> replace calls by getAlphabetSequence
   * @deprecated
   */
	public Vector<String> convert(Path path_from_jpf){
		Vector<String> path = new Vector<String>();
		int cex_length = path_from_jpf.size();
		Transition t = null;
        // [pcm] we don't have to start at transition 0, since we are not interested
        // in system initialization (this might be too restrictive in case this
        // automaton has to watch for class init calls)
		
		for (int i = 1; i < cex_length; i++) {
			t = path_from_jpf.get(i); // should check if t is null?
			int transition_length = t.getStepCount();

			for (Step s:t) {
				Instruction instr = s.getInstruction();
				if (instr instanceof InvokeInstruction) {
					InvokeInstruction md = (InvokeInstruction) instr;
					String word = filter(md.getInvokedMethodName());

          if (alphabet_.contains(word)){
            path.add(word);
          }
				}
			}
		}

		return path;
	}

	
	public static String filter(String original) {
		return(original.substring(0, original.indexOf('(')));
	}

	
	
	public void replay(Vector<String> trace) {
		reset();
		for (String step : trace) {
			if (currentState != -1) {
				Integer nextState = automaton[currentState].get(step);
				currentState = (nextState == null) ? -1 : nextState;
			}
		}
	}

	public boolean getCheckResult() {
		return checkResult;
	}

	public String getErrorMessage() {
		// Print info about property violation
		return ("Property " + automatonName + " violated");
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("[").append(getClass().getSimpleName()).append(" ")
				.append(currentState).append(" ");
		for (int i = 0; i < automaton.length; i++) {
			FSMState state = automaton[i];
			result.append("[").append(i).append(": ").append(state).append(
					"]; ");
		}
		// Remove the last "; "
		result.setLength(result.length() - 2);
		return result.append("]").toString();
	}
	
	public int getCurrentState() {
		return currentState;
	}
	public void setCurrentState(int state) {
		currentState = state;
	}
	
}
