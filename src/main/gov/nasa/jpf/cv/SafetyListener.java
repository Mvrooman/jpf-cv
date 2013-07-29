package gov.nasa.jpf.cv;

///import TestSafetyAutomaton;

import java.io.IOException;
import gov.nasa.jpf.Config;
import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Pattern;

import gov.nasa.jpf.*;
import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.search.Search;


public class SafetyListener extends PropertyListenerAdapter {

	SafetyAutomaton P = null;
	static int currentConfigListener = 1;

    private static <T> Vector<T> newVector(T... things) {
        return new Vector<T>(Arrays.asList(things));
}

	public SafetyListener(SafetyAutomaton automaton) {
		P = automaton;
		System.out.println("Constructor invoked with  " + P);		
	}
	
	public SafetyListener(Config conf) {

		boolean isAssumption = false;
		String propertyFile = conf.getString("safetyListener"+ currentConfigListener + ".assumption");
		if (propertyFile == null) 
			propertyFile = conf.getString("safetyListener"+ currentConfigListener + ".property");
		else isAssumption = true;	
		System.out.println("#Dimitra# Property file is  " + propertyFile);
		
    if (propertyFile != null) {
      currentConfigListener++;
      P = new SafetyAutomaton(isAssumption, propertyFile);
      System.out.println("#Dimitra# Automaton is  " + P);
    }
	}
	
	
	public Path getCounterexample() {
		if (P != null) return P.getCounterexample();
		else return null;
	}
	
	
	public void instructionExecuted(JVM vm) {

		if (!vm.getSystemState().isIgnored()) {
			Instruction instr = vm.getLastInstruction();
			ThreadInfo ti = vm.getLastThreadInfo();

			if (instr instanceof InvokeInstruction
					&& !ti.isInstructionSkipped()) {
				InvokeInstruction md = (InvokeInstruction) instr;
				P.advance(md.getInvokedMethod().getName(), ti, vm);
				// we will add this again when we know what is returned above

				// maybe here you also need to "reset" the property by
				// re-playing
				// search.getVM().getPath() in case of bfs
			}
		}
	}

	// ensure that this will work when search is different than DFS
	public void stateBacktracked(Search search) {
    P.replay(search.getVM().getPath()); // you need to backtrack the property
	}

	public boolean check(Search search, JVM vm) {
		return P.getCheckResult();
	}

	public String getErrorMessage() {
		// Print info about property violation
		return (P.getErrorMessage());
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
