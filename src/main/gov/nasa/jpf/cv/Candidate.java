package gov.nasa.jpf.cv;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/**
 * A candidate assumption.  Contains a mapping between states in the
 * assumption, and the column in S the state corresponds to.
 */
public class Candidate
{
  /**
   * The mapping between states and columns in S
   */
  private TreeMap candidateStateToS_;

  /**
   * The transitions for this state
   */
  private HashMap[] transitions_;

  /**
   * Creates a new candidate
   *
   * @param states the number of states
   * @param map the mapping
   */
  public Candidate(int states, TreeMap map)
  {
    candidateStateToS_ = map;

    transitions_ = new HashMap[states];
    for(int i = 0; i < states; i++)
      {
	transitions_[i] = new HashMap();
      }
  }

  /**
   * Gets the candidate assumption
   * 
   * @return the candidate assumption
   */
  public Map[] getTransitions()
  {
    return(transitions_);
  }


  /**
   * Gets the number of states
   * 
   * @return the number of states
   */
  public int numberOfStates()
  {
    return(transitions_.length);
  }

  /**
   * Gets the column in S for the given assumption state
   *
   * @param the index of the assumption state
   * @return the column in S
   */
  public Vector getS(int id)
  {
    return((Vector)candidateStateToS_.get(new Integer(id)));
  }

  /**
   * Sets a transition in the candidate
   *
   * @param state the starting state
   * @param action the action that is performed
   * @param end the ending state
   */
  public void setTransition(int start, String action, int end)
  {
    HashMap startTransitions = transitions_[start];
    startTransitions.put(action, new Integer(end));
  }

  /**
   * Gets a transition in the candidate
   *
   * @param state the starting state
   * @param action the action that is performed
   *
   * @return the ending state, or -1 if no transition has been set
   */
  public int getTransition(int start, String action)
  {
    HashMap startTransitions = transitions_[start];
    Integer end = (Integer)startTransitions.get(action);

    if(end == null)
      {
	return(-1);
      }
    else
      {
	return(end.intValue());
      }
  }
} // end class Candidate

