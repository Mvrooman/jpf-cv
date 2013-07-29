package gov.nasa.jpf.cv;

import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class WEnumeration implements Enumeration
{
  private Vector P_;
  private Vector W_;
  private Vector X_;

  private int   currentP_;
  private int   currentW_;
  private int[] currentX_;

  private boolean done_;

  /**
   * @param P a Vector of sequences
   * @param W a Vector of distinguishing sequences
   * @param X the Alphabet, a Vector a strings
   * @param size the exponent to apply to X
   */
  public WEnumeration(Vector P, Vector W, Vector X, int size)
  {
    if(size < 0)
      {
	throw(new RuntimeException("size < 0 in W Enumeration"));
      }

    done_ = false;
    P_ = P;
    W_ = W;
    X_ = X;

    currentP_ = 0;
    currentW_ = 0;

    currentX_ = new int[size];
    for(int i = 0; i < size; i++)
      {
	currentX_[i] = 0;
      }
  }


  public boolean hasMoreElements()
  {
    return(! done_);
  }

  public Object nextElement()
  {
    if(done_)
      {
	throw(new NoSuchElementException());
      }

    // Construct the current element
    Vector toReturn = new Vector();
    toReturn.addAll((Vector)P_.elementAt(currentP_));
    for(int i = 0; i < currentX_.length; i++)
      {
	toReturn.add(X_.elementAt(currentX_[i]));
      }
    toReturn.addAll((Vector)W_.elementAt(currentW_));

    // Now to the increment
    
    // First increment W
    currentW_++;
    if(currentW_ == W_.size())
      {
	// W has maxed out.  We need to reset it and increment P
	currentW_ = 0;
	currentP_++;

	if(currentP_ == P_.size())
	  {
	    // P has maxed out.  We need to reset it and increment X
	    currentP_ = 0;
	    
	    if(currentX_.length == 0)
	      {
		done_ = true;
	      }
	    else
	      {
		int i = currentX_.length - 1;
		boolean keepGoing = true;
		while(keepGoing)
		  {
		    currentX_[i]++;
		    if(currentX_[i] == X_.size())
		      {
			currentX_[i] = 0;
			i--;

			if(i == -1)
			  {
			    currentX_ = null;
			    done_ = true;
			    keepGoing = false;
			  }
		      }
		    else
		      {
			keepGoing = false;
		      }
		  }
	      }
	  }
      }

    return(toReturn);
  }
}
