package gov.nasa.jpf.cv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import java.util.AbstractList;

public class SafetyAutomaton {

	boolean isAssumption = false; // otherwise it is a property

	int currentState = 0;

	Vector<String> alphabet_ = new Vector();  // <2do> turn this into an ArrayList

	boolean checkResult = true;

	String automatonName;

	Path counterexample;

	Candidate candidate;


	// An FSM derived from a Candidate.
	// Maps a transition name(String) to a state (Integer).
	private class FSMState extends HashMap<String, Integer> {
	};

	FSMState[] automaton;

	// a vector of states of the automaton
	// each element is a hashmap (FMState) that stores transitions
	// using as key the action name and value the destination state

	public SafetyAutomaton(boolean assumption, String fsmFile) {
		TransitionsParser tp = new TransitionsParser(fsmFile);
		FSM fsm = new FSM();
		tp.parse( fsm);
		isAssumption = assumption;
		automatonName = new File(fsmFile).getName();
		alphabet_ = new Vector<String>(fsm.getAlphabet());
		createAutomaton(fsm);
	}

	public SafetyAutomaton(boolean assumption, AbstractList<String> transitions,
			Vector<String> alpha, String nm) {
		isAssumption = assumption;
		automatonName = nm;
		alphabet_ = alpha;
		createAutomaton(transitions);
		System.out.println(this);
	}

	public SafetyAutomaton(boolean assumption, Candidate candidate,
			Vector<String> alpha, String nm) {
		isAssumption = assumption;
		automatonName = nm;
		alphabet_ = alpha;
		this.candidate = candidate;
		createAutomaton();
		System.out.println(this);
	}

	
	public SafetyAutomaton(boolean assumption, FSM fsm, String nm) {
		isAssumption = assumption;
		automatonName = nm;
		alphabet_ = new Vector<String>(fsm.getAlphabet());
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
		advance(name, null, null);
	}

	public boolean isNextStateError(String name, ThreadInfo ti, JVM jvm) {
		
		
		return( (automaton[currentState].get(name) == null) ? true : false);
	}
	
	public boolean isAssumption() {
		return isAssumption;
	}
	
	
	public void advance(String name, ThreadInfo ti, JVM jvm) {
		// remember that you may need to force the thread to yield
		// decide if we want to cut the thread info - currently not used
		
		int lastCurrentState = currentState;
//		System.out.println("++++++ Action is " + name);
		
		if (alphabet_.contains(name) && currentState != -1) {

			System.out.println("[AG REASONING] Action " + name + " is relevant for flight rule");
			
			Integer nextState = automaton[currentState].get(name);
			
			currentState = (nextState == null) ? -1 : nextState;
			// ti.reschedule(true);
		}

		
		if (currentState == -1) {
			if (isAssumption) { // is assumption
				currentState = lastCurrentState;
				if (jvm != null) 
					jvm.getSystemState().setIgnored(true);
				else
					System.out
							.println("JVM was null so could not apply assumption properly");
			} else {
				// is property
				checkResult = false;
        
        // <2DO> - see SCSafetyAutomaton.advance() for the reason why getClonedPath()
        // does not suffice !!
				//counterexample = jvm.getClonedPath();
        counterexample = jvm.getPath();
			} 			
		}
		
	}

	
	public int whatComesNext(String name, ThreadInfo ti, JVM jvm) {
		// remember that you may need to force the thread to yield
		// decide if we want to cut the thread info - currently not used

		if (alphabet_.contains(name) && currentState != -1) {
			Integer nextState = automaton[currentState].get(name);
			return ((nextState == null) ? -1 : nextState.intValue());
		} else
			throw new RuntimeException();
	}

	
	
	public void replay(Path path_from_jpf) {

		Vector<String> trace = convert(path_from_jpf);
		replay(trace);
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

	
	 // # TO DO #
	// move this method into some type of util package
	
	// converts jpf path into a vector of "actions" from alphabet
	private Vector convert(Path path_from_jpf) {// TBD
		Vector path = new Vector();
		int cex_length = path_from_jpf.size(); // size;
		Transition t;
		Step s;
        
        // [pcm] we don't have to start at transition 0, since we are not interested
        // in system initialization (this might be too restrictive in case this
        // automaton has to watch for class init calls)
		for (int i = 1; i < cex_length; i++) {
			t = path_from_jpf.get(i); // should check if t is null?
			int transition_length = t.getStepCount();
			for (int j = 0; j < transition_length; j++) {
				s = t.getStep(j);
				Instruction instr = s.getInstruction();
				if (instr instanceof InvokeInstruction) {
					InvokeInstruction md = (InvokeInstruction) instr;
					String word = md.getInvokedMethod().getName();
					//System.out.println("***** Word is " + word);
					// check if word corresponds to an action in the alphabet
					for (int k = 0; k < alphabet_.size(); k++) {
						String action = (String) alphabet_.elementAt(k);
						if (word.compareTo(action) == 0)
							path.add(action);
					}
				}
			}
		}
		return path;
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

}
