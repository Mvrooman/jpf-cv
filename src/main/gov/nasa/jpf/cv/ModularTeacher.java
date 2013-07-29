package gov.nasa.jpf.cv;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.*;

import gov.nasa.jpf.*;
import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.util.*;
import gov.nasa.jpf.jvm.bytecode.*;

/* learn the assumption A for component M to satisfy P */
/* uses JPF */

public class ModularTeacher implements MinimallyAdequateTeacher {
  private SETLearner set_;

  private MemoizeTable memoized_;

  private Vector alphabet_;

  private SafetyListener property = null;

  private String module1_, module2_;

  public ModularTeacher(Collection alpha, SafetyAutomaton prop, String module1,
      String module2) {
    alphabet_ = new Vector();
    alphabet_.addAll(alpha);
    memoized_ = new MemoizeTable();
    module1_ = module1;
    module2_ = module2;

    if (prop != null)
      property = new SafetyListener(prop);
  }

  public boolean query(AbstractList<String> sequence) throws SETException {

    Boolean recalled = memoized_.getResult(sequence);
    if (recalled != null) {
      return (!recalled.booleanValue());
    } else {
      // play the query as an assumption
      System.out.println("\n New query: " + sequence);
      SafetyListener assumption = new SafetyListener(new SafetyAutomaton(true,
          sequence, alphabet_, "Query"));

      JPF jpf = createJPFInstance(assumption, property, module1_); // driver for M1
      jpf.run();
      boolean violating = jpf.foundErrors();
      memoized_.setResult(sequence, violating);
      return (!violating);
    }
  }

  public Vector conjecture(Candidate candidate) throws SETException {

    printCandidateAssumption(candidate);
    Vector toReturn = this.checkOracle1(candidate);
    if (toReturn != null || module2_ == null) {
      return (toReturn);
    } else {
      return (this.checkOracle2(candidate));
    }
  }

  // M1 || A |= P
  private Vector checkOracle1(Candidate assume) {
    System.out.println("Oracle 1 is executing\n\nAssumption is: ");
    printCandidateAssumption(assume);

    Vector counterexample = null;
    // set assumption listener with assumption
    // set property listener with property

    SafetyListener assumption = new SafetyListener(new SafetyAutomaton(true,
        assume, alphabet_, "Assumption"));
    JPF jpf = createJPFInstance(assumption, property, module1_);

    jpf.run();
    boolean violating = jpf.foundErrors();
    if (violating) {
      Path cex;
      if (property != null && ((cex = property.getCounterexample()) != null)) {
        // nothing to do - cex was set to path from property listener violation
      } else {
        cex = jpf.getVM().getPath();
      }

      counterexample = convert(cex);
    }
    return counterexample;
  }

  // check M2 |= A and analyze the counterexample
  // We can use for Oracle 2: M1 || P |= A
  // + P |= A
  // + other checks (!A || P is empty)
  // - not the weakest assumption: misses !M || !P -- not true???
  private Vector checkOracle2(Candidate assume) throws SETException {
    System.out.println("Oracle 2 is executing");

    Vector counterexample = null;
    // set property listener with assumption

    SafetyListener assumptionProperty = new SafetyListener(new SafetyAutomaton(
        false, assume, alphabet_, "AssumptionProperty"));
    JPF jpf = createJPFInstance(null, assumptionProperty, module2_);

    jpf.run();
    boolean violating = jpf.foundErrors();

    if (violating) {
      Path cex;
      if (property != null && ((cex = property.getCounterexample()) != null)) {
        // nothing to do - cex was set to path from property listener
        // violation
      } else {
        cex = jpf.getVM().getPath();
      }

      Vector counterexampleTrace = convert(cex);

      if (query(counterexampleTrace))
        counterexample = counterexampleTrace;
      else
        /* real error */
        println("***** ERROR: System and environment are incompatible!!!");
    }
    return counterexample;
  }

  public JPF createJPFInstance(PropertyListenerAdapter assumption,
      SafetyListener property, String targetClass) {

    String[] args = new String[2];

    Class[] argTypes = { String[].class };

    try {
      Method m = Class.forName(targetClass).getDeclaredMethod("main", argTypes);
      args[0] = targetClass;
    } catch (ClassNotFoundException cnf) {
      // dont know what to do here
      // decide later
    } catch (NoSuchMethodException nsm) {
      System.out.println(" >>>>  entered here");
      args[0] = "gov.nasa.jpf.cv.Environment";
      args[1] = targetClass; // you want to run an environment for targetClass
    }

    Config conf = JPF.createConfig(args);
    JPF jpf = new JPF(conf);

    if (assumption != null) {
      jpf.addPropertyListener(assumption);
    }

    if (property != null) {
      jpf.addPropertyListener(property);
    }

    return jpf;
  }

  // # TO DO #
  // move this method into some type of util package

  // converts jpf path into a vector of "actions" from alphabet
  private Vector convert(Path path_from_jpf) {//TBD
    Vector path = new Vector();
    int cex_length = path_from_jpf.size(); //size;
    Transition t;
    Step s;
    for (int i = 0; i < cex_length; i++) {
      t = path_from_jpf.get(i); // should check if t is null?
      int transition_length = t.getStepCount();
      for (int j = 0; j < transition_length; j++) {
        s = t.getStep(j);
        Instruction instr = s.getInstruction();
        if (instr instanceof InvokeInstruction) {
          InvokeInstruction md = (InvokeInstruction) instr;
          String word = md.getInvokedMethod().getName();
          //                System.out.println("Next action in path is : " + word);
          // check if word corresponds to an action in the alphabet   
          for (int k = 0; k < alphabet_.size(); k++) {
            String action = (String) alphabet_.elementAt(k);
            if (word.compareTo(action) == 0) {
              path.add(action);
              //                        System.out.println("Next action in path is : " + action);
            }
          }
        }
      }
    }
    return path;
  }

  public void printCandidateAssumption(Candidate candidate) {
    System.out.println("Candidate assumption:");
    int nStates = candidate.numberOfStates();
    for (int start = 0; start < nStates; start++)
      for (int action_index = 0; action_index < alphabet_.size(); action_index++) {
        String action = (String) alphabet_.elementAt(action_index);
        int end = candidate.getTransition(start, action);
        if (end != -1)
          System.out.println("Transition: " + start + " " + action + " " + end);
      }
  }

  public void dumpCandidateStateMachine(Candidate candidate, String file) {
    PrintWriter pw = null;
    try {
      pw = new PrintWriter(file);
      int nStates = candidate.numberOfStates();
      for (int start = 0; start < nStates; start++) {
        boolean firstAction = true;

        for (int action_index = 0; action_index < alphabet_.size(); action_index++) {
          String action = (String) alphabet_.elementAt(action_index);
          int end = candidate.getTransition(start, action);
          if (end != -1) {
            if (firstAction) {
              firstAction = false;
              pw.format("S%s = ( %s -> S%s\n", start, action, end);
            } else {
              pw.format("     | %s -> S%s\n", action, end);
            }
            if (action_index == alphabet_.size() - 1) {
              if (start == nStates - 1) {
                pw.println("    ).");
              } else {
                pw.println("    ),");
              }
            }
          }
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (pw != null) {
        pw.close();
      }
    }
  }

  public Iterator getAlphabet() {
    return (alphabet_.iterator());
  }

  public void println(String s) {
    System.out.println(s);
  }

  public void print(String s) {
    System.out.print(s);
  }

  public Object getAssumption(Candidate c) {
    return c;
  }

  public void setSETLearner(SETLearner set) {
    set_ = set;
  }
}
