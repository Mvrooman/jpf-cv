package gov.nasa.jpf.cv;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Generates assumptions using Rivest and Schapire's improvement to
 * Angluin's L* Algorithm as described in Section 4.5 of "Inference of
 * Finite Automata Using Homing Sequences", Information and
 * Computation, 103, p. 299-347, 1993.
 *
 * Let n be the size of the machine being learned.  The worst-case 
 * bound on this are:
 *   |S| = O(n)
 *   |E| = O(n)
 *
 * The number of calls to the oracle is less than n.  The number of
 * queries is bounded by O(|A|n^2 + n log m) where m is the length of
 * the longest counter-example returned by the oracle.
 */

public class SETLearner {
  /**
   * The minimally adequate teacher
   */
  private MinimallyAdequateTeacher teacher_;

  /**
   * The set S
   */
  private TreeSet S_;

  /**
   * The set E.  Maps between elements of E and the Integer index of
   * the element.  This is used to index into the BitSets stored in T.
   */
  private TreeMap E_;

  /**
   * The alphabet
   */
  private Vector A_;

  /**
   * The table T.  This maps between sequences in (S union SA) stored
   * as Vectors and rows stored as BitSet.  The length of each row is
   * |E|.
   */
  private TreeMap T_;

  /**
   * A Mapping between the Rows in T and the Columns headings for the
   * row.  The keys are BitSets (stored in T) and a TreeSet of
   * elements in (S union SA).
   */
  private HashMap TRowsToColumnMap_ = new HashMap();

  /**
   * Compares two sequences so they can be stored in
   * TreeMaps/TreeSets.  This is lexicographic order, where elements
   * in the same position are compared to each other.
   */
  public static Comparator sequenceComparator = new Comparator() {
    public int compare(Object a, Object b) {
      Vector A = (Vector) a;
      Vector B = (Vector) b;

      for (int i = 0; i < A.size(); i++) {
        if (i >= B.size()) {
          return (1);
        }

        String elemA = (String) A.elementAt(i);
        String elemB = (String) B.elementAt(i);

        int compare = elemA.compareTo(elemB);
        if (compare != 0) {
          return (compare);
        }
      }

      // They are equal up to the point where A
      // runs out of elements
      if (A.size() < B.size()) {
        return (-1);
      } else {
        return (0);
      }
    }
  };

  public SETLearner(MinimallyAdequateTeacher teacher) throws SETException {
    teacher_ = teacher;
    teacher_.setSETLearner(this);

    S_ = new TreeSet(sequenceComparator);
    E_ = new TreeMap(sequenceComparator);
    T_ = new TreeMap(sequenceComparator);

    // Copy the alphabet.  Skip the tau event, which should be in the
    // first slot of composite's alphabet.
    A_ = new Vector();
    for (Iterator events = teacher_.getAlphabet(); events.hasNext();) {
      A_.addElement((String) events.next());
    }

    // Add lambda to path S_ and E_
    this.addToS(new Vector(0));
    this.addToE(new Vector(0));
  }

  //
  //
  // Methods for operating on S
  //
  // 

  /**
   * Gets the elements of S
   *
   * @return an Iterator over the elements of S
   */
  public Iterator getS() {
    return (S_.iterator());
  }

  /**
   * Adds a new element to S.  This fills in the table T.
   *
   * @param newS the element to add to S
   */
  private void addToS(Vector newS) throws SETException {
    teacher_.println("Adding to S: " + printSequence(newS));

    // First add the new S to the set
    S_.add(newS);

    // Loop over A and E adding newS+E and newS+A+E to the table
    Iterator EElems = this.getE();
    while (EElems.hasNext()) {
      Vector EElem = (Vector) EElems.next();
      this.putEntry(newS, null, EElem);

      Iterator AElems = this.getA();
      while (AElems.hasNext()) {
        String AElem = (String) AElems.next();
        this.putEntry(newS, AElem, EElem);
      }
    }
  }

  //
  //
  // Methods for operating on E
  //
  //

  /**
   * Gets the elements of E
   *
   * @return an Iterator over the elements of E
   */
  public Iterator getE() {
    return (E_.keySet().iterator());
  }

  /**
   * For an element in E, gets its index
   *
   * @param EElem an element in E
   * @return the index
   */
  private int getEIndex(Vector EElem) {
    Integer val = (Integer) E_.get(EElem);
    if (val == null) {
      // This shouldn't happen if the algorithm is working correctly
      teacher_.println("ERROR!  E: " + printSequence(EElem)
          + " does not have an index.");

      return (-1);
    } else {
      return (val.intValue());
    }
  }

  /**
   * Adds a new element to E.  This fills in T.
   *
   * @param newE the element to add
   */
  private void addToE(Vector newE) throws SETException {
    teacher_.println("Adding to E: " + printSequence(newE));

    int index = E_.size();
    E_.put(newE, new Integer(index));

    // Loop over S and A adding S+newE and S+A+newE to the table
    Iterator SElems = this.getS();
    while (SElems.hasNext()) {
      Vector SElem = (Vector) SElems.next();
      this.putEntry(SElem, null, newE);

      Iterator AElems = this.getA();
      while (AElems.hasNext()) {
        String AElem = (String) AElems.next();
        this.putEntry(SElem, AElem, newE);
      }
    }
  }

  //
  //
  // Methods for operating over A
  //
  //

  /**
   * Gets the elements of A
   *
   * @return an Iterator over the elements of A
   */
  public Iterator getA() {
    return (A_.iterator());
  }

  //
  //
  // Methods for accessing the table T
  //
  //

  /**
   * Adds a new entry to the table.  
   *
   * @param SElem the element in S
   * @param AElem the element in A, or null
   * @param EElem the element in E
   *
   * @return true if the sequence SAE is accepting, false otherwise
   */
  private boolean putEntry(Vector SElem, String AElem, Vector EElem)
      throws SETException {
    // First compute the column of the table we are in, as well as the
    // sequence we will be testing
    Vector column = new Vector();
    Vector sequence = new Vector();

    column.addAll(SElem);

    sequence.addAll(SElem);
    teacher_.print("S=" + printSequence(SElem));

    if (AElem != null) {
      column.add(AElem);
      sequence.add(AElem);
      teacher_.print("; A=" + AElem);
    }

    sequence.addAll(EElem);
    teacher_.print("; E=" + printSequence(EElem));

    // Determine if the sequence is accepting
    boolean accepts = teacher_.query(sequence);
    teacher_.println(":  " + accepts);

    // Get the row in the table for the column
    BitSet row = (BitSet) T_.get(column);
    if (row == null) {
      // Create the row if necessary
      row = new BitSet();
      T_.put(column, row);
    } else {
      // Remove the row from the reverse map
      removeRowFromMap(column, row);
    }

    // Get the index for E, modify the row, and add the row back into
    // the reverse map
    int EIndex = getEIndex(EElem);
    row.set(EIndex, accepts);
    addRowToMap(column, row);

    return (accepts);
  }

  /**
   * Gets an entry to the table.  
   *
   * @param SElem the element in S
   * @param AElem the element in A, or null
   * @param EElem the element in E
   *
   * @return true if the sequence SAE is accepting, false otherwise
   */
  private boolean getEntry(Vector SElem, String AElem, Vector EElem) {
    // First compute the column we are in
    Vector column = new Vector();
    column.addAll(SElem);

    if (AElem != null) {
      column.add(AElem);
    }

    // Get the row, and get the index of E
    BitSet row = (BitSet) T_.get(column);
    int EIndex = getEIndex(EElem);

    // Get the value from the row
    return (row.get(EIndex));
  }

  /**
   * Adds a column/row combination to the reverse map
   *
   * @param column the column (in S union SA)
   * @param row the row
   */
  private void addRowToMap(Vector column, BitSet row) {
    // Clone the row so it cannot be modified without our knowledge
    BitSet myRow = (BitSet) row.clone();

    // Get the column from the map, creating it if it doesn't already
    // exist
    TreeSet columns = (TreeSet) TRowsToColumnMap_.get(myRow);
    if (columns == null) {
      columns = new TreeSet(sequenceComparator);
      TRowsToColumnMap_.put(myRow, columns);
    }

    // Add the column to the set of columns for the row
    columns.add(column);
  }

  /**
   * Removes a column/row combination from the reverse map
   *
   * @param column the column (in S union SA)
   * @param row the row
   */
  private void removeRowFromMap(Vector column, BitSet row) {
    // Get the columns corresponding to the row
    TreeSet columns = (TreeSet) TRowsToColumnMap_.get(row);

    // Remove the requested row
    columns.remove(column);

    // If columns is empty, remove the entry for the row from the
    // HashMap
    if (columns.size() == 0) {
      TRowsToColumnMap_.remove(row);
    }
  }

  /**
   * For a row in T, gets the columns (in S union SA) that map to that row
   *
   * @param row the row in T
   * @return an Iterator of the columns
   */
  private Iterator getColumnsForRow(BitSet row) {
    return (((TreeSet) TRowsToColumnMap_.get(row)).iterator());
  }

  /**
   * For a sequence in SA, gets the sequence in S that has the same
   * row
   *
   * @param SElem the element in S
   * @param AElem the element in A (cannot be null)
   *
   * @return the row in S that has the same entries is SAElem, null if
   * no such row exists
   */
  public Vector getMatchingRow(Vector SElem, String AElem) {
    // Compute the column of the table we are in
    Vector column = new Vector();
    column.addAll(SElem);
    column.add(AElem);

    // Get the row for the element of SA
    BitSet row = (BitSet) T_.get(column);

    // Get the matching rows from the reverse map
    Iterator columns = getColumnsForRow(row);
    while (columns.hasNext()) {
      Vector candidateColumn = (Vector) columns.next();

      // If the candidate is in S_, return it
      if (S_.contains(candidateColumn)) {
        return (candidateColumn);
      }
    }

    return (null);
  }

  /**
   * Determines if a row in S corresponds to an accepting state
   *
   * @param SElem the element in S
   *
   * @return true if the row is accepting, false otherwise
   */
  public boolean isRowAccepting(Vector SElem) {
    return (this.getEntry(SElem, null, new Vector(0)));
  }

  //
  //
  // Other methods
  //
  //

  /**
   * Gets the assumption generated by the learner
   *
   * @return the assumption generated by the learner, or null if
   *         no assumption can help
   */
  public Object getAssumption() throws SETException {
    // Loop until we get don't get a counterexample
    while (true) {
      // Make the table closed
 //     System.out.println("Dimitra-debug making table closed ");	
      this.makeTClosed();

      // Now that the table is closed, construct the transitions
      // from it
      Candidate candidate = this.getCandidate();

      if (candidate == null) {
        teacher_.println("\n*** Assumption is null ***");
        return (null);
      }

      // Make the conjecture
      Vector counterExample = teacher_.conjecture(candidate);

      // If we don't have a counter-example, our assumption was
      // correct.  If we have a counter-example, update T and
      // re-loop
      if (counterExample == null) {
        teacher_.println("T is correct.  Finished");
        return (teacher_.getAssumption(candidate));
      } else {
        teacher_.println("T is incorrect.  Counterexample is:");
        for (Iterator actions = counterExample.iterator(); actions.hasNext();) {
          teacher_.println("\t" + (String) actions.next());
        }
        this.updateT(candidate, counterExample);
//        System.out.println("Dimitra-debug over updateT ");
      }
    }
  }

  private Candidate getCandidate() {
    if (!this.isRowAccepting(new Vector(0))) {
      return (null);
    }

    // First see how many states we need.  Any state of S that is not
    // accepting is going to be ignored
    TreeMap assumptionStateToS = new TreeMap();
    TreeMap stateToID = new TreeMap(sequenceComparator);
    int nextID = 1;
    for (Iterator SElems = this.getS(); SElems.hasNext();) {
      Vector SElem = (Vector) SElems.next();
      if (!isRowAccepting(SElem)) {
        // This is the non-accepting state.  There should be only
        // one, and it should have an ID of -1.
        if (assumptionStateToS.containsKey(new Integer(-1))) {
          throw (new RuntimeException("More than one" + " non-accepting state"));
        } else {
          stateToID.put(SElem, new Integer(-1));
          assumptionStateToS.put(new Integer(-1), SElem);
        }
      } else if (SElem.size() == 0) {
        // This is lambda, assign it ID 0
        stateToID.put(SElem, new Integer(0));
        assumptionStateToS.put(new Integer(0), SElem);
      } else {
        stateToID.put(SElem, new Integer(nextID));
        assumptionStateToS.put(new Integer(nextID), SElem);
        nextID++;
      }
    }

    Candidate candidate = new Candidate(nextID, assumptionStateToS);

    for (Iterator SElems = this.getS(); SElems.hasNext();) {
      Vector SElem = (Vector) SElems.next();
      int currentState = ((Integer) stateToID.get(SElem)).intValue();
      if (currentState < 0) {
        continue;
      }

      for (Iterator AElems = this.getA(); AElems.hasNext();) {
        String AElem = (String) AElems.next();

        Vector next = getMatchingRow(SElem, AElem);
        int nextState = ((Integer) stateToID.get(next)).intValue();
        if (nextState < 0) {
          continue;
        }

        candidate.setTransition(currentState, AElem, nextState);
      }
    }

    return (candidate);
  }

  /**
   * Given a counter-example that explains why the candidate
   * assumption is incorrect, updates the table by adding an element
   * to E to correct the problem.
   *
   * @param candidate the candidate assumption
   * @param counterExample the Trace explaining why our current
   * assumption is incorrect
   */
  private void updateT(Candidate candidate, Vector counterExample)
      throws SETException {
    // The counter-example results in a violation in the actual
    // system.  This means that our environment should "not accept"
    // this string.  We want to find the place in the counter example
    // where the candidate system and the actual system divide in
    // behavior.  

    // Use binary search to locate the place in the counter-example
    // where this occurs.  Our machine currently accepts the
    // counterExample.  Based on the Rivest and Schapire notation, We
    // want to find the smallest i such that alpha_i != alpha_(i+1).

    // Eliminate taus from the counter example
    Vector tauless = new Vector();
    for (Iterator actions = counterExample.iterator(); actions.hasNext();) {
      String action = (String) actions.next();

      if (!action.equals("tau")) {
        tauless.addElement(action);
//        System.out.println("Dimitra - debug: action added " + action);
      }
    }
    
//    System.out.println("Dimitra - debug: Size of cex is " + tauless.size());
    Boolean[] memoized = new Boolean[tauless.size() + 1];

    // Compute the low point for reference
    memoized[0] = new Boolean(getAlpha(candidate, tauless, 0));

    int i = binarySearch(candidate, tauless, memoized, 0, tauless.size());

    // We want to add to E the sequence r_(i+1) where 
    // counterExample = p_(i+1) r_(i+1) and |p_(i+1)| = i+1.
    Vector P = new Vector();
    Vector R = new Vector();
    for (Iterator actions = tauless.iterator(); actions.hasNext();) {
      String action = (String) actions.next();
      if (P.size() < (i + 1)) {
        P.addElement(action);
      } else {
        R.addElement(action);
      }
    }

    this.addToE(R);
  }

  /**
   * Runs binary search to determine where in the counter example,
   * alpha_i and alpha_(i+1) have different values.
   *
   * @param candidate the andidate machine
   * @param counterExample the counterExample being examined
   * @param memoized the array of already computed results
   * @param low the lower bound for binary search
   * @param high the upper bound for binary search
   * 
   * @return the i such that alpha_i and alpha_(i+1) have different values.
   */
  private int binarySearch(Candidate candidate, Vector counterExample,
      Boolean[] memoized, int low, int high) throws SETException {
    int testPoint1 = (low + high) / 2;
    int testPoint2 = testPoint1 + 1;

    boolean alpha1, alpha2;

    if (memoized[testPoint1] != null) {
      alpha1 = memoized[testPoint1].booleanValue();
    } else {
      alpha1 = getAlpha(candidate, counterExample, testPoint1);
      memoized[testPoint1] = new Boolean(alpha1);
    }

    if (memoized[testPoint2] != null) {
      alpha2 = memoized[testPoint2].booleanValue();
    } else {
      alpha2 = getAlpha(candidate, counterExample, testPoint2);
      memoized[testPoint2] = new Boolean(alpha2);
    }

    if (alpha1 != alpha2) {
      return (testPoint1);
    } else if (alpha1 == memoized[0].booleanValue()) {
      // testPoint1 == testPoint2 == the low point, we need to look
      // higher
      return (binarySearch(candidate, counterExample, memoized, testPoint1 + 1,
          high));
    } else {
      // testPoint1 == testPoint2 == the high point, we need to look
      // lower
      return (binarySearch(candidate, counterExample, memoized, low,
          testPoint1 - 1));
    }
  }

  /**
   * For a given counterexample and a point in the counterexmaple i,
   * computes alpha_i.
   *
   * @param candidate the candidate machine
   * @param counterExample the sequence being examined
   * @param point where to make the split
   *
   * @return the value alpha_i
   */
  private boolean getAlpha(Candidate candidate, Vector counterExample, int point)
      throws SETException {
    // To compute alpha_i: 
    //
    // 1) Let the counter-example be = p_i r_i where |p_i| = i.  Run
    // the candidate machine on p_i.
    //
    // 2) Look at the state in candidate machine you end up.  This
    // state corresponds to an row s in (S U SA) in the table T.  
    //
    // 3) s is the homing sequence to bring the actual machine and the
    // candidate machine into the same state.  Run the actual machine
    // on s r_i.  The result is alpha_i.

    // First compute p_i and r_i
    Vector P = new Vector();
    Vector R = new Vector();
    Iterator actions = counterExample.iterator();
    while (actions.hasNext()) {
      String action = (String) actions.next();
      if (P.size() < point) {
        P.addElement(action);
      } else {
        R.addElement(action);
      }
    }

    // Now run the candidate machine on P.  This is known to be a
    // deterministic finite state automata.
    int currentState = 0;
    Iterator PActions = P.iterator();
    while (PActions.hasNext()) {
      String actionName = (String) PActions.next();
      currentState = candidate.getTransition(currentState, actionName);

      if (currentState == -1) {
        // We have gone into the error state.
        break;
      }
    }

    // currentState is the result of running the candidate machine on
    // P.  Look into assumptionStateToS_ to get the homing sequence
    // for this state.  Construct the sequence homing + R and run the
    // actual machine on this sequence.
    Vector sequence = new Vector();
    sequence.addAll(candidate.getS(currentState));
    sequence.addAll(R);

    return (teacher_.query(sequence));
  }

  /**
   * Makes T is closed. T is closed if forall s in SA, there is some
   * s' in S such that forall e in E, T(se) = T(s'e).
   */
  private void makeTClosed() throws SETException {
    teacher_.println("Making T closed");

    Vector offending = isTClosed();
    while (offending != null) {
      this.addToS(offending);
      offending = isTClosed();
    }
  }

  /**
   * Determines if T, the table, is closed.  T is closed if forall 
   * s in SA, there is some s' in S such that forall e in E, 
   * T(se) = T(s'e). 
   *
   * @return null if T is closed, otherwise, a sequence that needs to
   * be added to S to make T closed
   */
  private Vector isTClosed() {
    Iterator SElems = this.getS();
    while (SElems.hasNext()) {
      Vector SElem = (Vector) SElems.next();

      Iterator AElems = this.getA();
      while (AElems.hasNext()) {
        String AElem = (String) AElems.next();

        Vector sa = new Vector();
        sa.addAll(SElem);
        sa.add(AElem);

        if (getMatchingRow(SElem, AElem) == null) {
          return (sa);
        }
      }
    }

    return (null);
  }

  /**
   * Returns a string representation of a sequence
   *
   * @return a string representation of a sequence
   */
  private static String printSequence(Vector sequence) {
    StringBuilder toReturn = new StringBuilder();

    // An empty sequence is treated as a lambda
    if ((sequence == null) || (sequence.size() == 0)) {
      toReturn.append("lambda");
    } else {
      for (int i = 0; i < sequence.size(); i++) {
        if (i > 0) {
          toReturn.append(", ");
        }

        toReturn.append((String) sequence.elementAt(i));
      }
    }

    return (toReturn.toString());
  }

  public String printTable() {
    StringBuilder toReturn = new StringBuilder("SET Table is:\n");

    // First print the E's
    toReturn.append("\n\nE:\n");
    Vector[] sortedEElems = new Vector[E_.size()];
    Iterator EElems = this.getE();
    while (EElems.hasNext()) {
      Vector EElem = (Vector) EElems.next();
      int EIndex = this.getEIndex(EElem);
      sortedEElems[EIndex] = EElem;
    }
    for (int i = 0; i < sortedEElems.length; i++) {
      toReturn.append(i).append(":  ").append(printSequence(sortedEElems[i])).append('\n');
    }
    toReturn.append("\n\n");

    Iterator SElems = this.getS();
    while (SElems.hasNext()) {
      Vector SElem = (Vector) SElems.next();
      toReturn.append("S=").append(printSequence(SElem)).append("  ").append(T_.get(SElem)).append('\n');

      Iterator AElems = this.getA();
      while (AElems.hasNext()) {
        String AElem = (String) AElems.next();

        Vector SAElem = new Vector();
        SAElem.addAll(SElem);
        SAElem.add(AElem);

        toReturn.append("S=").append(printSequence(SElem)).append(" A=").append(AElem).append("  ").append(T_.get(SAElem)).append('\n');
      }
    }

    return (toReturn.toString());
  }
} // end class SET

