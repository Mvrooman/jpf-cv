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
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.Path;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.jvm.choice.sc.SCEvent;
import gov.nasa.jpf.jvm.choice.sc.SCEventGenerator;
import gov.nasa.jpf.search.Search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

public class SCConformanceListener extends PropertyListenerAdapter {

	SCSafetyAutomaton P = null;
	static int currentConfigListener = 1;
    private static <T> Vector<T> newVector(T... things) {
        return new Vector<T>(Arrays.asList(things));
    }
    
	public SCConformanceListener(SCSafetyAutomaton automaton) {
		P = automaton;
		System.out.println("Constructor invoked with  " + P);		
	}
	
	public SCConformanceListener(SCSafetyAutomaton automaton, Boolean oracle2) {
		P = automaton;
		System.out.println("Constructor invoked with  " + P);		
	}
	
	public SCConformanceListener(Config conf) {

		boolean isAssumption = false;
		String propertyFile = conf.getString("safetyListener"+ currentConfigListener + ".assumption");
		if (propertyFile == null) 
			propertyFile = conf.getString("safetyListener"+ currentConfigListener + ".property");
		else isAssumption = true;	
		System.out.println("#AG reasoning# Property file is  " + propertyFile);
		
    if (propertyFile != null) {
      currentConfigListener++;
      P = new SCSafetyAutomaton(isAssumption, propertyFile);
      System.out.println("#AG reasoning# Automaton is  " + P);
    }
	}
	/*
	 * (non-Javadoc)
	 * if the instruction to be executed is an error in the module 
	 * or an error in P if p is separate then you
	 * backtrack because you look for pairs of "OK" in system and "error" 
	 * in the assumption
	 */
	 public void executeInstruction(JVM vm) {
		Instruction insn = vm.getLastInstruction();
		if (insn instanceof ATHROW) {
			ThreadInfo ti = vm.getLastThreadInfo();
			if (!ti.isInstructionSkipped()) { // make sure that other listeners did not set instruction to skipped
				int xobjref = ti.peek();
				ElementInfo ei = vm.getElementInfo(xobjref);
				ClassInfo ci = ei.getClassInfo();
				if (ci.getName().equals("java.lang.AssertionError")) {
					//int msgref = ei.getIntField("detailMessage");
					ti.skipInstruction(); // for efficiency - so as not to
					// process the exception
					vm.getSystemState().setIgnored(true); // backtracks
				}
			}
		}
	}

	 /*
	  * (non-Javadoc)
	  * After the instruction is executed, append the parameters (if appropriate)
	  * and advance the property automaton
	  */
	 
	public void instructionExecuted(JVM vm) {
		JVM jvm = vm;
		Instruction instr = jvm.getLastInstruction();
		ThreadInfo ti = jvm.getLastThreadInfo();
		if (instr instanceof ReturnInstruction) {
			ReturnInstruction md = (ReturnInstruction) instr;
			String methodName = md.getMethodInfo().getName();
			ChoiceGenerator cg = vm.getChoiceGenerator();
			if (cg instanceof SCEventGenerator){
				SCEventGenerator eventCG = (SCEventGenerator)cg;
				SCEvent scEve = eventCG.getNextChoice();
				String cgMethodName = scEve.getMethodName();	
//				System.out.println("ICSE: Method name is :" + methodName);
//				System.out.println("ICSE: CG method name is :" + cgMethodName);
				if (methodName.startsWith(cgMethodName)){
					Object[] args = scEve.getArguments();
					int argNum = 0;
					if (args != null)
						argNum = args.length;
					if (argNum > 0){
						SCConverter scConvert = new SCConverter();
						cgMethodName = scConvert.addParameters(cgMethodName, args);						
					}
				
					if (P.isInAlphabet(cgMethodName)) {
						P.advance(cgMethodName, ti, jvm, true);
						MJIEnv env = ti.getEnv();
						env.setStaticIntField("gov.nasa.jpf.cv.CVState",
								"AutomatonState", P.getCurrentState());
					}
				}
			}
		}
	}
	
	public Path getCounterexample() {
		if (P != null) return P.getCounterexample();
		else return null;
	}
	
	// ensure that this will work when search is different than DFS
	public void stateBacktracked(Search search) {
//		System.out.println(" BACKTRACKING ... ");
//		P.replay(search.getVM().getPath());
		JVM vm = search.getVM();
		ThreadInfo ti = vm.getLastThreadInfo();
		MJIEnv env = ti.getEnv();
		P.setCurrentState(env.getStaticIntField("gov.nasa.jpf.cv.CVState", "AutomatonState"));
	}

	public boolean check(Search search, JVM vm) {
		return P.getCheckResult();
	}

	public String getErrorMessage() {
		// Print info about property violation
		return (P.getErrorMessage());
	}
	
	public Vector<String> convert(Path path) {
		return (P.convert(path));		
	}
	
	
/**
	public static void main(String[] args) {

        Vector<String> alphaInOut = newVector("in", "out");
		SafetyListener assumption = new SafetyListener(new SafetyAutomaton(true, 
				TestSafetyAutomaton.newInOutCandidate(), alphaInOut,  "AssumptionIO"));
		SafetyListener property = new SafetyListener(new SafetyAutomaton(false, 
				TestSafetyAutomaton.newInOutCandidate(), alphaInOut, "PropertyIO"));
		Config conf = JPF.createConfig(args);
		// do own settings here

		JPF jpf = new JPF(conf);
	
		jpf.addSearchListener(assumption);
		jpf.addVMListener(assumption);
		jpf.addSearchProperty(assumption);
		 
		
		jpf.addSearchListener(property);
		jpf.addVMListener(property);
		jpf.addSearchProperty(property);

		jpf.run();

	}
**/
}
