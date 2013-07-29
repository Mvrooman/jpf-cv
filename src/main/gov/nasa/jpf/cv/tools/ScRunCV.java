package gov.nasa.jpf.cv.tools;


import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFShell;
import gov.nasa.jpf.cv.Candidate;
import gov.nasa.jpf.cv.SCModularTeacher;
import gov.nasa.jpf.cv.SCSafetyAutomaton;
import gov.nasa.jpf.cv.SETException;
import gov.nasa.jpf.cv.SETLearner;
import gov.nasa.jpf.util.LogManager;

import java.util.Vector;

public class ScRunCV implements JPFShell {

  Config conf;

  public ScRunCV (Config conf) {
    this.conf = conf;
    LogManager.init(conf);
  }

  public void start(String[] args){

		SCSafetyAutomaton property = null;
		/* create an alphabet -- should be read from a file */
		Vector alpha = new Vector();
		
		/* create a teacher for that alphabet */
		SCModularTeacher teacher = new SCModularTeacher(conf);

    try {
      /* run the learning algorithm */
      SETLearner learnAssumption = new SETLearner(teacher);
      Candidate a = (Candidate) learnAssumption.getAssumption();

      String storeAssumption = conf.getProperty("assumption.outputFile");

      System.out.println("\n\n********************************************");
      if (a == null) {
        System.out.println("Assumption is null - no environment can help");
      } else {
        System.out.print("Assumption generation completed. ");
        teacher.printCandidateAssumption(a);
        teacher.dumpCandidateStateMachine(a, storeAssumption);
      }
      System.out.println("********************************************");

    } catch (SETException sx){
      sx.printStackTrace();
    }
	}
}
