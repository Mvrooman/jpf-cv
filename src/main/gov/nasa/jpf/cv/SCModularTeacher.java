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
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.Path;
import gov.nasa.jpf.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

/*
 * This teacher helps the learner learn the assumption A for
 * component M to satisfy P (P is specified as an assertion
 * violation in M)
 */
public class SCModularTeacher implements MinimallyAdequateTeacher {
	private SETLearner set_;

	private MemoizeTable memoized_;

	private Vector alphabet_;

	private SCSafetyListener property = null;

	private String module1_, module2_;

	private Config JPFargs_;
	
	/*
	 * utility method to create a list of elements to add to the alphabet
	 * based on performing symbolic execution apriori
	 */
	private Vector<String> createAlphaList(Vector<Pair<String,Vector<String>>> allSummaries){
		
		Vector<String> newAlpha = new Vector<String>();
		Pair<String,Vector<String>> mi;
		Iterator it = allSummaries.iterator();
		while (it.hasNext()){
			mi = (Pair<String,Vector<String>>)it.next();
			String mainInfo = mi._1;
			Vector<String> pathConditions = mi._2;
			String methodName = mainInfo.substring(mainInfo.indexOf(":")+1, mainInfo.indexOf("("));
			if (pathConditions.size() > 0){
				  Iterator it2 = pathConditions.iterator();
				  while(it2.hasNext()){
					  String pc = (String)it2.next();
					  if (pc.contains("$"))
						  pc = pc.substring(0,pc.indexOf("$"));
					  String testCase = methodName + "(";
					  String symValues = mainInfo.substring(mainInfo.lastIndexOf("(")+1,mainInfo.length()-1);
					  String argValues = mainInfo.substring(mainInfo.indexOf(")")+1);
					  argValues = argValues.substring(argValues.indexOf("(")+1,argValues.indexOf(")"));
					  StringTokenizer st = new StringTokenizer(symValues, ",");
					  StringTokenizer st2 = new StringTokenizer(argValues, ",");
					  while(st.hasMoreTokens()){
						  String token = st.nextToken();
						  String actualValue = st2.nextToken();
						  if (pc.contains(token)){
							  String temp = pc.substring(pc.indexOf(token));
							  String val = temp.substring(temp.indexOf("[")+1,temp.indexOf("]"));
							  testCase = testCase + val + ",";
						  }else{
							  //should not have "don't cares" - use concrete value 
							  testCase = testCase + actualValue + ",";
						  }
					  }
					  if (testCase.endsWith(","))
						  testCase = testCase.substring(0,testCase.length()-1); 
					  testCase = testCase + ")";
					  //filter out duplicate test cases caused by multiple copies
					  //of a path condition
					  if (!newAlpha.contains(testCase))
						  newAlpha.add(testCase);
				  }
			}	
		}

		return newAlpha;
	}

	public SCModularTeacher(Config conf) {
		//indicates to perform a preprocessing step that updates the alphabet
		//based on an initial run of JPF where one or more methods is exectued
		//symbolically
		boolean refineAlpha = conf.getBoolean("assumption.alphabet.refine");
		
		memoized_ = new MemoizeTable();
		String[] targetArgs = conf.getTargetArgs();
		if (targetArgs.length < 1)
			throw new RuntimeException("No target arguments configured");
		
		module1_ = targetArgs[0]; // this is the statechart master state
		module2_ = null;
		JPFargs_ = conf;	
		String[] alpha = conf.getStringArray("assumption.alphabet");
		alphabet_ = new Vector();
		
		if (refineAlpha){
			//process the alphabet to determine which member(s) of the alphabet
			//are to be updated
//			System.out.print(">>>>Original alphabet: ");
			for (int i=0; i<alpha.length; i++){
				System.out.print(alpha[i]);
				if ((i+1) < alpha.length)
					System.out.print(", ");
			}
			System.out.println();
			//to automate: call jpf here to get results of a symbolic run
			//expand alphabet by adding more specific instance(s) of the
			//method/event specification to create a more precise assumption
			
			//use the results of the symb exec run to update the alphabet
			//the results from the symbolic execution for the basis for the new
			//alphabet; then anything specified in assumption.alphabet
			//if symb exec results in a "don't care" for a value, use the concrete
			//value to avoid the user having to update the alphabet generated
			Vector<Pair<String,Vector<String>>> allSummaries = new Vector<Pair<String,Vector<String>>>();
			
			try{
				BufferedReader in = new BufferedReader(new FileReader("outFile.txt"));
				String line = in.readLine();
				boolean firstMethod = true;
				Vector<String> pathConditions = null;
				Pair<String,Vector<String>> mi;
				String methodInfo = "";
				while(line != null){
					if ((line.startsWith("METHOD"))){
						if (firstMethod){
							firstMethod = false;
						}else{
							mi = new Pair<String,Vector<String>>(methodInfo,pathConditions);
							allSummaries.add(mi);
						}
						pathConditions = new Vector<String>();
						methodInfo = line;
					}else {
						if (!line.startsWith("# =")) //omit extraneous line
							pathConditions.add(line);
					}
					line = in.readLine();
				}
				mi = new Pair<String,Vector<String>>(methodInfo,pathConditions);
				allSummaries.add(mi);
				
				Vector<String> alphaList = createAlphaList(allSummaries);
				Iterator it = alphaList.iterator();
				while (it.hasNext()){
					String element = (String)it.next();
					alphabet_.add(element);
				}
				//the following is not quite right; it doesn't match as planned
				//todo: fix (no real harm in current implementation)
				for (int i=0; i<alpha.length; i++){
					if (!alphabet_.contains(alpha[i])){
						alphabet_.add(alpha[i]);
					}
				}
		    } catch (Exception e) {
		    }
		}else{
			for (String a : alpha) {
				alphabet_.add(a);
			}
		}
	}

	public boolean query(AbstractList<String> sequence) throws SETException {

		Boolean recalled = memoized_.getResult(sequence);
		if (recalled != null) {
			return (!recalled.booleanValue());
		} else {
			// play the query as an assumption
			System.out.println("\n New query: " + sequence);
			
			SCSafetyListener assumption = new SCSafetyListener(
					new SCSafetyAutomaton(true, sequence, alphabet_, "Query", module1_));

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
		if (toReturn != null) {
			return (toReturn);
		} else {
			return (this.scCheckOracle2(candidate));
		}
	}

	// M || A |= P
	private Vector checkOracle1(Candidate assume) {
		System.out.println("\n***************Oracle 1 is executing\n\nAssumption is: ");
		printCandidateAssumption(assume);

		Vector counterexample = null;
		// set assumption listener with assumption
		// set property listener with property

		SCSafetyListener assumption = new SCSafetyListener(
				new SCSafetyAutomaton(true, assume, alphabet_, "Assumption",module1_));
		JPF jpf = createJPFInstance(assumption, property, module1_);

		jpf.run();
		boolean violating = jpf.foundErrors();
		if (violating) {
			ChoiceGenerator cg = null;
			assert (property==null) : "We should not be getting here";
				// we do not cover the case where properties are specified separately
			SCConverter conv = new SCConverter();
			counterexample = conv.getCex(jpf.getVM(), alphabet_); 
		}
		return counterexample;
	}

	// Aerr |= M  - we assume that the property is an assertion within M
	private Vector scCheckOracle2(Candidate assume)throws SETException {
		System.out.println("Oracle 2 for statecharts is executing\n\nAssumption is: ");
		printCandidateAssumption(assume);
		Vector counterexample = null;
		boolean done = false;
		while (!done){
//			System.out.println("Dimitra - debug - oracle2: new loop iteration");
		    counterexample = null;
			// assumption is used as a property here but in a special way
		    String CompleteModule = module1_ + "Complete";
		    if (ClassInfo.tryGetResolvedClassInfo(CompleteModule) == null){
		    	CompleteModule = module1_;
		    }
		    
			SCConformanceListener assumption = new SCConformanceListener(
					new SCSafetyAutomaton(false, assume, alphabet_, "Assumption", CompleteModule , memoized_));
			JPF jpf = createJPFInstance(assumption, property, CompleteModule);	
//			System.out.println("Dimitra - debug - about to invoke JPF");
			jpf.run();
//			System.out.println("Dimitra - debug - JPF completed");
			
			Path jpfPath = assumption.getCounterexample();
			if (jpfPath != null){
				//nonerror in M & error in Aerr - this is what we are looking for
//				SCConverter scConvert = new SCConverter();
//				counterexample = scConvert.getCex(jpf.getVM(), alphabet_);
				counterexample = assumption.convert(jpfPath);
				if( query(counterexample)){ //if not a violating counter example we are done
					done = true; // a real counterexample to be returned to learner
				} // otherwise you need to continue with your loop
			}else
				done = true; // interface is permissive
		}
		return counterexample;
	}

	public JPF createJPFInstance(PropertyListenerAdapter assumption,
			PropertyListenerAdapter property, String targetClass) {


		JPFargs_.setTargetArgs(targetClass); // keeps all other arguments as specified in .jpf file
		
		
		JPF jpf = new JPF(JPFargs_);

		if (assumption != null) {
			jpf.addPropertyListener(assumption);
		}

		if (property != null) {
			jpf.addPropertyListener(property);
		}
		
		//	useful for debugging JPF
		//	jpf.addListener(new gov.nasa.jpf.tools.ExecTracker(conf));
		
		return jpf;
	}
	
	public void printCandidateAssumption(Candidate candidate) {
		System.out.println("Candidate assumption:");
		walkCandidateStateMachine(candidate, new ICandidateVisitor() {
			public void visit(boolean isFirstAction, int start, String action,
					int end) {
				System.out.println("Transition: " + start + " " + action + " "
						+ end);
			}

			public void stateIsDone(boolean stateHadActions,
					boolean isLastState, int state) {
			}
		});
	}

	public void dumpCandidateStateMachine(Candidate candidate, String file) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			walkCandidateStateMachine(candidate,
					new CandidateToStateMachine(pw));
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

	public void walkCandidateStateMachine(Candidate candidate,
			ICandidateVisitor visitor) {
		int nStates = candidate.numberOfStates();
		for (int start = 0; start < nStates; start++) {
			boolean isFirstAction = true;
			for (int action_index = 0; action_index < alphabet_.size(); action_index++) {
				String action = (String) alphabet_.elementAt(action_index);
				int end = candidate.getTransition(start, action);
				if (end != -1) {
					visitor.visit(isFirstAction, start, action, end);
					isFirstAction = false;
				}
			}
			visitor.stateIsDone(!isFirstAction, (start == nStates - 1), start);
		}
	}

	public static interface ICandidateVisitor {
		public void visit(boolean isFirstAction, int start, String action,
				int end);

		public void stateIsDone(boolean stateHadActions, boolean isLastState,
				int state);
	}

	private static class CandidateToStateMachine implements ICandidateVisitor {
		private PrintWriter _pw;

		public CandidateToStateMachine(PrintWriter pw) {
			_pw = pw;
		}

		public void visit(boolean isFirstAction, int start, String action,
				int end) {
			if (isFirstAction) {
				_pw.format("S%s = ( %s -> S%s\n", start, action, end);
			} else {
				_pw.format("     | %s -> S%s\n", action, end);
			}
		}

		public void stateIsDone(boolean stateHadActions, boolean isLastState,
				int state) {
			if (stateHadActions) {
				if (isLastState) {
					_pw.println("    ).");
				} else {
					_pw.println("    ),");
				}
			}
		}
	}
}
