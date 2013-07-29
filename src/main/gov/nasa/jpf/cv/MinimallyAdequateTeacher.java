package gov.nasa.jpf.cv;

import java.util.AbstractList;
import java.util.Vector;
import java.util.Iterator;

public interface MinimallyAdequateTeacher
{
  public boolean query(AbstractList<String> sequence) throws SETException;
  
  public Vector conjecture(Candidate candidate) throws SETException;

  public void println(String s);
  public void print(String s);

  public Iterator getAlphabet();

  public Object getAssumption(Candidate candidate);

  public void setSETLearner(SETLearner set);
}
