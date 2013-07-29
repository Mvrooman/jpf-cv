package gov.nasa.jpf.cv;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;

/**
 * Parser for simple FSP-like properties.
 * 
 * <2do> users of TransitionParser have to be aware of that it might throw a
 * TransitionParser.Exception during construction and/or parse(). So far,
 * users didn't handle these exceptions gracefully, so we just let them
 * pass for now. This might change
 *
 * <2do> we might want to turn this into an Antlr grammar, to be more flexible
 *
 * @author bushnell
 */
public class TransitionsParser {

  public static class Exception extends RuntimeException {
    public Exception(String details){
      super(details);
    }
    public Exception(String details,Throwable cause){
      super(details,cause);
    }
  }

  protected Reader reader;

  public TransitionsParser (String fileName){
    try {
      reader = new FileReader(fileName);
    } catch (IOException ix){
      throw new Exception("unable to open input file: " + fileName);
    }
  }

  public TransitionsParser (Reader reader){
    this.reader = reader;
  }

	/**
	 * The top-level parser. Parses the FSM description contained in "reader"
	 * and at the appropriate points calls the visitor "v" to build the FSM.
	 * 
	 * @param reader
	 *            a Reader from which an FSM can be read.
	 * @param v
	 *            a Visitor that builds the FSM in memory.
	 */
	public void parse(Visitor v) {
		TransitionsParser.Transitions tr = new TransitionsParser.Transitions();
		StreamTokenizer st = new StreamTokenizer(reader);
		st.ordinaryChar('.');
		tr.parse(st, v);
	}

	/**
	 * Parser for the start of each local definition: "S = stuff ." or "S =
	 * stuff ,"
	 * 
	 * @author bushnell
	 * 
	 */
	private static class Transitions {
		public enum States {
			start, startDef, localDefs, end
		};

		States _state = States.start;

		String _stateName = "";

		LocalDef _localDefsParser = new LocalDef();

		/**
		 * Parses things like "S = stuff ." or "S = stuff ,". The parsing of
		 * "stuff" is delegated to a LocalDef parser.
		 * 
		 * @param st
		 *            The StreamTokenizer containing the text to be parsed.
		 * @param v
		 *            a Visitor that builds the FSM in memory.
		 */
		protected void parse(StreamTokenizer st, Visitor v) {
			try {
				int tokType;

				while (_state != States.end) {
					tokType = st.nextToken();

					if (tokType == StreamTokenizer.TT_EOF) {
						throw new Exception(
								"Transitions: premature EOF in TransitionsParser");
					}
					switch (_state) {
					case start:
						if (isStateId(st)) {
							_stateName = st.sval;
							_state = States.startDef;
						} else {
							throw new Exception(
									"Transitions: start of property def was not a state id (uppercase word): "
											+ st.sval);
						}
						break;

					case startDef:
						if (tokType == '=') {
							_localDefsParser.reset();
							_localDefsParser.parse(st, v, this);
							_state = States.localDefs;
						} else {
							throw new Exception(
									"Transitions: start of local def was not ',' or '.': "
											+ st.sval);
						}
						break;

					case localDefs:
						if (tokType == '.') {
							_state = States.end;
						} else if (tokType == ',') {
							_state = States.start;
						} else {
							throw new Exception(
									"Transitions: clause did not end with a '.': "
											+ st.sval);
						}
						break;

					default:
						throw new Exception(
								"Transitions: unhandled state: " + _state);
					}
				}
			} catch (IOException ex) {
				throw new Exception(
						"Transitions: IOException while getting next clause token",
						ex);
			}
		}
	}

	/**
	 * Parser for the body of a local definition: "Q0" or "( clause )".
	 * 
	 * @author bushnell
	 * 
	 */
	private static class LocalDef {
		public enum States {
			start, clauses, end
		};

		States _state = States.start;

		Clauses _clauseParser = new Clauses();

		public void reset() {
			_state = States.start;
			_clauseParser.reset();
		}

		/**
		 * Parse things like "Q0" or "( clause )". Delegates the parsing of
		 * "clause" to a "Clauses" parser.
		 * 
		 * @param st
		 *            The StreamTokenizer containing the text to be parsed.
		 * @param v
		 *            a Visitor that builds the FSM in memory.
		 * @param tr
		 *            the Transitions parser that knows the name of the state
		 *            being parsed.
		 */
		public void parse(StreamTokenizer st, Visitor v, Transitions tr) {
			try {
				int tokType;

				while (_state != States.end) {
					tokType = st.nextToken();

					if (tokType == StreamTokenizer.TT_EOF) {
						throw new Exception(
								"LocalDef: premature EOF in TransitionsParser");
					}
					switch (_state) {
					case start:
						if (isStateId(st)) {
							// S = A
							v.sameState(st.sval, tr._stateName);
							_state = States.end;
						} else if (tokType == '(') {
							// S = (a -> B ...)
							_clauseParser.reset();
							_clauseParser.parse(st, v, tr);
							_state = States.clauses;
						} else {
							throw new Exception(
									"LocalDef: start of local def was not a state id (uppercase word) or '(': "
											+ st.sval);
						}
						break;

					case clauses:
						if (tokType == ')') {
							_state = States.end;
						} else {
							throw new Exception(
									"LocalDef: clause did not end with ')': "
											+ st.sval);
						}
						break;

					default:
						throw new Exception(
								"LocalDef: unhandled state: " + _state);
					}
				}
			} catch (IOException ex) {
				throw new Exception(
						"LocalDef: IOException while getting next clause token",
						ex);
			}
		}
	}

	/**
	 * Parser for a clause in a local definition: "a -> B" or "a -> B | c -> D"
	 * 
	 * @author bushnell
	 * 
	 */
	private static class Clauses {
		public enum States {
			start, head, tail, clause, choice, end
		};

		States _state = States.start;

		String _lastAction;

		public void reset() {
			_state = States.start;
		}

		/**
		 * Parse things like "a -> B" or "a -> B | c -> D".
		 * 
		 * @param st
		 *            The StreamTokenizer containing the text to be parsed.
		 */
		public void parse(StreamTokenizer st, Visitor v, Transitions tr) {
			try {
				int tokType;

				while (_state != States.end) {
					tokType = st.nextToken();

					if (tokType == StreamTokenizer.TT_EOF) {
						throw new Exception(
								"Clauses: premature EOF in TransitionsParser");
					}
					switch (_state) {
					case start:
						if (!isActionId(st)) {
							throw new Exception(
									"Clauses: head of clause was not an action id (lowercase word): "
											+ st.sval);
						}
						// For example, the "a" in "a -> B"
						_lastAction = st.sval;
						_state = States.head;
						break;

					case head:
						if ('-' != tokType) {
							throw new Exception(
									"Clauses: arrow did not follow head of clause: "
											+ st.sval);
						} else {
							tokType = st.nextToken();
							if ('>' != tokType) {
								throw new Exception(
										"Clauses: arrow did not follow head of clause: -"
												+ st.sval);
							}
						}
						// The "->" in "a -> B"
						_state = States.tail;
						break;

					case tail:
						if (!isStateId(st)) {
							throw new Exception(
									"Clauses: head of clause was not a state identifier (uppercase word): "
											+ st.sval);
						}
						// The "B" in "a -> B"
						_state = States.clause;
						v.setTransition(tr._stateName, _lastAction, st.sval);
						break;

					case clause:
						if ('|' == tokType) {
							_state = States.start;
						} else if (')' == tokType) {
							st.pushBack();
							_state = States.end;
						} else {
							throw new Exception(
									"Clauses: did not find '|' or end of clause: "
											+ st.sval);
						}
						break;

					default:
						throw new Exception("Clauses: unhandled state: "
								+ _state);
					}
				}
			} catch (IOException ex) {
				throw new Exception(
						"Clauses: IOException while getting next clause token",
						ex);
			}
		}
	}

	private static boolean isStateId(StreamTokenizer st) {
		return (st.ttype == StreamTokenizer.TT_WORD && Character
				.isUpperCase(st.sval.charAt(0)));
	}

	private static boolean isActionId(StreamTokenizer st) {
		return (st.ttype == StreamTokenizer.TT_WORD && Character
				.isLowerCase(st.sval.charAt(0)));
	}

	/**
	 * Helper method: convert a SteamTokenizer token type (an int) to its name
	 * (a String).
	 * 
	 * @param tokType
	 *            A token type from StreamTokenizer.
	 * @return The name of "tokType"
	 */
	private static String tokTypeToString(int tokType) {
		String type = "";
		switch (tokType) {
		case StreamTokenizer.TT_EOF:
			type = "EOF";
			break;
		case StreamTokenizer.TT_EOL:
			type = "EOL";
			break;
		case StreamTokenizer.TT_NUMBER:
			type = "NUMBER";
			break;
		case StreamTokenizer.TT_WORD:
			type = "WORD";
			break;
		default:
			if (0 <= tokType && tokType <= Character.MAX_VALUE)
				type = Character.toString((char) tokType);
			else
				type = "??? " + tokType + " ???";
		}
		return type;
	}

	/**
	 * An example of how to use TransitionsParser.
	 * 
	 * @param args
	 *            args[0] is the name of the file to be parsed. Only the first
	 *            definition in the file is parsed.
	 * 
	 * 
	 * @throws FileNotFoundException
	 *             when args[0] is not the name of an existing file.
	 */

  // <2do> turn this into a unit test
	public static void main(String[] args) throws FileNotFoundException {
		String automaton = "Q0	= (d -> Q1|a -> Q4),Q1	= (e -> Q2),Q2	= (f -> STOP),Q4	= (b -> Q5),Q5	= (c -> Q0).";
		TransitionsParser tp = new TransitionsParser(new StringReader(automaton));
		FSM c = new FSM();
		tp.parse(c);
		System.out.println("Fsm: " + c._transitions);
		System.out.println("  same states: " + c._sameStates);
		System.out.println("  id->name: " + c._stateIdToName);
		System.out.println("  name->id: " + c._stateNameToId);
		System.out.println("  numberOfStates: " + c.numberOfStates());
		System.out.println("  alphabet: " + c.getAlphabet());
		for (int start=0; start<c.numberOfStates(); start++) {
			for (String action : c.getAlphabet()) {
				int end = c.getTransition(start, action);
				if (end != -1) {
					System.out.format("Transition: %s --%s--> %s\n", start, action, end);
				}
			}
		}
		
		SafetyAutomaton sa = new SafetyAutomaton(false, c, "Fred");
		System.out.println(sa);
	}
}
