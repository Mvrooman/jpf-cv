package gov.nasa.jpf.cv;

import java.util.AbstractList;
import java.util.TreeMap;
import java.util.Vector;


/**
 * Stores results that have already been computed so they can be 
 * retrieved later
 */
public class MemoizeTable
{
  // The MemoizeTable stores the data in a tree format.  This way, if
  // a sequence has a prefix that is known to violate, we can easily
  // detect that and say that the sequence will violate without doing
  // simulating the sequence on the LTS system.

  /**
   * The children of this node in the table
   */
  private TreeMap children_;

  /**
   * The result for this node in the table, or null if it has not been
   * computed yet.
   */ 
  private Boolean isViolating_;

  /**
   * Creates a new MemoizeTable
   */
  public MemoizeTable()
  {
    children_    = new TreeMap();
    isViolating_ = null;
  }

  /**
   * Stores a result in the table
   * 
   * @param sequence the sequence
   * @param result the result
   */
  public void setResult(AbstractList<String> sequence, boolean result)
  {
    this.setResult(sequence, result, 0);
  }

  /**
   * Stores a result in the table
   *
   * @param sequence the sequence
   * @param result the result
   * @param position the depth in the tree this node is at
   */
  private void setResult(AbstractList<String> sequence, boolean result, int position)
    {
      if(sequence.size() == position)
	{
	  // We have reached the correct point
	  isViolating_ = new Boolean(result);
	}
      else
	{
	  // We need to go deeper
	  String action = sequence.get(position);
	  MemoizeTable child = (MemoizeTable)children_.get(action);
	  if(child == null)
	    {
	      child = new MemoizeTable();
	      children_.put(action, child);
	    }
	  
	  child.setResult(sequence, result, position + 1);
	}
    }

  /**
   * Retrieves a result from the table
   *
   * @param sequence the sequence
   *
   * @return the stored result, or null if the result has not been stored
   */ 
  public Boolean getResult(AbstractList<String> sequence)
  {
    return(this.getResult(sequence, 0));
  }


  /**
   * Retrieves a result from the table
   *
   * @param sequence the sequence
   * @param position the depth in the tree this node is at
   *
   * @return the stored result, or null if the result has not been stored
   */ 
  private Boolean getResult(AbstractList<String> sequence, int position) {
		if (sequence.size() == position) {
			// We have reached the maximum depth
			return (isViolating_);
		} else {
			// We _MAY_ need to go deeper.  If the sequence is violating,
			// we can stop here
			if ((isViolating_ != null) && (isViolating_.booleanValue())) {
				return (isViolating_);
			} else {
				// Go deeper if we can
				String action = (String) sequence.get(position);
				MemoizeTable child = (MemoizeTable) children_.get(action);
				if (child == null) {
					// We have no knowledge that can help
					return (null);
				} else {
					return (child.getResult(sequence, position + 1));
				}
			}
		}
	}
}
