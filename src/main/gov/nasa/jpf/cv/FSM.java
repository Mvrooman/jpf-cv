package gov.nasa.jpf.cv;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FSM implements Visitor {
	Set<String> _alphabet = new HashSet<String>();

	Map<String, Integer> _stateNameToId = new HashMap<String, Integer>();

	Map<Integer, String> _stateIdToName = new HashMap<Integer, String>();

	Map<String, String> _sameStates = new HashMap<String, String>();

	Map<Integer, Map<String, Integer>> _transitions = new HashMap<Integer, Map<String, Integer>>();

	private int _nextId = 0;

	public Map<String, String> getSameStates() {
		return _sameStates;
	}

	public Map<Integer, Map<String, Integer>> getTransitions() {
		return _transitions;
	}

	public String idToName(int id) {
		return _stateIdToName.get(id);
	}

	public int nameToId(String name) {
		return _stateNameToId.get(name);
	}

	public synchronized int numberOfStates() {
		return _stateIdToName.size();
	}

	public Set<String> getAlphabet() {
		return _alphabet;
	}

	/**
	 * Declare that 's1' and 's2' are two names for the same state and assign them a state ID (an
	 * int). If one of them already has an ID, use that. If they both already have IDs that are
	 * different, throw an exception.
	 */
	public synchronized void sameState(String s1, String s2) {
		_sameStates.put(s1, s2);
		_sameStates.put(s2, s1);

		Integer idS1 = _stateNameToId.get(s1);
		Integer idS2 = _stateNameToId.get(s2);

		if (idS1 == null && idS2 == null) {
			// Both are new state names.  Assign same ID to both states,
			// but the ID --> Name mapping will return S1.
			Integer newId = _nextId++;
			_stateNameToId.put(s1, newId);
			_stateIdToName.put(newId, s1);
			_stateNameToId.put(s2, newId);
		} else if (idS2 == null) {
			_stateNameToId.put(s2, idS1);
		} else if (idS1 == null) {
			_stateNameToId.put(s1, idS2);
		} else if (!idS2.equals(idS1)) {
			throw new RuntimeException(
					String
							.format(
									"The states %s and %s are already mapped to different IDs.",
									s1, s2));
		} // else, s1 and s2 already have the same ID.
	}

	public synchronized void setTransition(String start, String action,
			String end) {
		Integer startId = _stateNameToId.get(start);
		Integer endId = _stateNameToId.get(end);
		if (startId == null) {
			startId = _nextId++;
			_stateNameToId.put(start, startId);
			_stateIdToName.put(startId, start);
		}
		if (endId == null) {
			endId = _nextId++;
			_stateNameToId.put(end, endId);
			_stateIdToName.put(endId, end);
		}
		Map<String, Integer> transition = _transitions.get(startId);
		if (transition == null) {
			transition = new HashMap<String, Integer>();
			_transitions.put(startId, transition);
		}
		transition.put(action, endId);
		_alphabet.add(action);
	}

	public synchronized int getTransition(int startId, String action) {
		Map<String, Integer> transitions = _transitions.get(startId);
		if (transitions == null) {
			return -1;
		} else {
			Integer transition = transitions.get(action);
			return (transition == null ? -1 : transition);
		}
	}

	public String toString() {
		return String.format("[%s  alphabet: %s   State Names->IDs: %s",
				_transitions, _alphabet, _stateNameToId);
	}
}