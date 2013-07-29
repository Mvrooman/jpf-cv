                                                                                                                                                                                                                                                                                                                            //
// Copyright (C) 2007 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.cv;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.Path;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.choice.sc.SCEvent;
import gov.nasa.jpf.jvm.choice.sc.SCEventGenerator;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.JPFLogger;


/**
 * <2do> add some comment describing what this thing does
 * <2do> what about a null automaton?  some methods check for it, others don't
 */
public class SCSafetyListener extends PropertyListenerAdapter {

  protected static JPFLogger log = JPF.getLogger("gov.nasa.jpf.cv");

	SCSafetyAutomaton P = null;
	static int currentConfigListener = 1;

	public SCSafetyListener(SCSafetyAutomaton automaton) {
		P = automaton;
	}
		
	public SCSafetyListener(Config conf) {
    P = createSCSafetyAutomaton(conf);
    currentConfigListener++;
	}

  SCSafetyAutomaton createSCSafetyAutomaton (Config conf){
    String key = "safetyListener" + currentConfigListener;

    // <2do> what if there are both assumption and property keys?
    // try assumptions first
    String propertyFile = conf.getString(key + ".assumption");
    if (propertyFile != null){
      // <2do> this should return an SCAssumptionAutomaton
      return new SCSafetyAutomaton(true, propertyFile);

    } else {
      // if that fails try a property
      propertyFile = conf.getString(key + ".property");
      if (propertyFile != null){
      // <2do> this should return an SCPropertyAutomaton
        return new SCSafetyAutomaton(false, propertyFile);

      } else {
        // <2do> out of luck - this seems like an error
        return null;
      }
    }
  }

	public Path getCounterexample() {
		if (P != null) return P.getCounterexample();
		else return null;
	}

		
	//--- our Listener interface

  boolean setPropertyState;

	public void choiceGeneratorAdvanced(JVM vm){
		
		ChoiceGenerator cg = vm.getChoiceGenerator();
		
		if (cg instanceof SCEventGenerator){
      setPropertyState = false;

			SCEventGenerator generator = (SCEventGenerator)cg;
			SCEvent scEvent = generator.getNextChoice(); //this returns the next event to be executed
			String eventId = SCConverter.addParameters(scEvent.getMethodName(), scEvent.getArguments());

      if (P.isInAlphabet(eventId)){
        if (P.isAssumption() && P.isNextStateError(eventId)) {
          // skip this event
          log.info("ignored event: ", eventId);

          // note this will advance the CG *before* entering the transition
          vm.getSystemState().setIgnored(true);

        } else {
          ThreadInfo ti = vm.getLastThreadInfo();
          P.advance(eventId, ti, vm, false);
          log.info("event ", eventId, " advances property to state: ", P.getCurrentState());

          // NOTE - we can't set the 'AutomatonState' field here because this notification
          // happens *before* the KernelState is pushed, which means the field would
          // not get properly restored if we set it here

          setPropertyState = true;
        }
      }
		}		
	}

  public void executeInstruction(JVM vm){
    if (setPropertyState){
      // now we are in a new transition, so we can change the KernelState
      ThreadInfo ti = vm.getLastThreadInfo();
      int propertyState = P.getCurrentState();
      ti.getEnv().setStaticIntField("gov.nasa.jpf.cv.CVState", "AutomatonState", propertyState);

      setPropertyState = false;
    }
  }

	public void stateBacktracked(Search search) {
    stateRestored(search);
	}

	public void stateRestored(Search search) {
    // restore the property state from the data in our masterstate
	  ThreadInfo ti = ThreadInfo.getCurrentThread();
    int propertyState = ti.getEnv().getStaticIntField("gov.nasa.jpf.cv.CVState", "AutomatonState");
		P.setCurrentState(propertyState);
    log.info("property state restored to: ", propertyState);
	}

  //--- our Property interface
	public boolean check(Search search, JVM vm) {
			return P.getCheckResult();
	}

	public String getErrorMessage() {
		// Print info about property violation
		return (P.getErrorMessage());
	}

}
