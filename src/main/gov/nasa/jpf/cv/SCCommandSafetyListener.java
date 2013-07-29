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
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.choice.sc.SCEvent;
import gov.nasa.jpf.jvm.choice.sc.SCEventGenerator;
import gov.nasa.jpf.search.Search;

import java.io.IOException;
import java.util.Vector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;

public class SCCommandSafetyListener extends PropertyListenerAdapter {

	int _machineRef;

	MJIEnv _env;

	Vector<String> _path = new Vector<String>();

	Vector<String> _command = new Vector<String>();

	Vector<String> _assertion = new Vector<String>();

	Vector<String> _originalAssertion = new Vector<String>();

	boolean _result;

	int _failed_assertion_id;

	Vector<String> _trace;

	String _failedCommand;

	String _failedAssertion;

	int _failedAtStep;

	public SCCommandSafetyListener(Config conf) {

		String propertyFile = conf.getString("CommandSafetyListener.assertion");
		if (propertyFile == null)
			System.out.println("Warning - property file is needed  ");
		_result = true;
		_trace = new Vector<String>();
		_failedAtStep = 0;

		if (propertyFile != null) {
			getAssertions(propertyFile);
			System.out.println("#DEBUG-COMMAND-LISTENER# Paths are  "
					+ _path.toString());
			System.out.println("#DEBUG-COMMAND-LISTENER# Commands are  "
					+ _command.toString());
			System.out.println("#DEBUG-COMMAND-LISTENER# Assertions are  "
					+ _assertion.toString());
		}

	}

	public boolean check(Search search, JVM vm) {
		return _result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 gov.nasa.jpf.PropertyListenerAdapter#choiceGeneratorAdvanced(gov.nasa.jpf.jvm.JVM)
	 */

	public void choiceGeneratorAdvanced(JVM vm) {

		ThreadInfo ti = vm.getLastThreadInfo();
		_machineRef = ti.getThis();
		_env = ti.getEnv();

		ChoiceGenerator cg = vm.getChoiceGenerator();

		if (cg instanceof SCEventGenerator) {
			SCEventGenerator generator = (SCEventGenerator) cg;
			SCEvent scEve = generator.getNextChoice(); //this returns the next event to be executed
			String methodName = scEve.getMethodName(); // name of the event 

			System.out.println("#DEBUG-COMMAND-LISTENER# Command from script: "
					+ methodName);

			if (!methodName.equals("completion")) {
				_trace.add(methodName);
				_failedAtStep++;
			}

			// to be compared to the command name

			// if event is what we are interested in 
			// we need to check the state in the assertion to see if it is active

			int assertion_number = findAssertionNumber(methodName);

			if (assertion_number != -1) { // means that this command is of interest

				System.out
						.println("#DEBUG-COMMAND-LISTENER# Command from assertion file: "
								+ _command.elementAt(assertion_number));

				int NextActiveStateRef = _env.getReferenceField(_machineRef,
						"activeStates");
				
				while (NextActiveStateRef != MJIEnv.NULL) {
					int nameRef = _env.getReferenceField(NextActiveStateRef, "fieldName");
					String sName = _env.getStringObject(nameRef);
					// maps the state id to the string name for this state
					// machine?

					System.out
							.println("#DEBUG-COMMAND-LISTENER# State name from listener: "
									+ sName);

					if (sName != null) {
						System.out.println();

						System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& " + sName);
						System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& " + _assertion.elementAt(assertion_number));

						System.out.println();
						
						if (sName
								.equals(_assertion.elementAt(assertion_number))) {
							_result = false;
							System.out
							.println("#DEBUG-COMMAND-LISTENER# ERROR");
							_failedCommand = methodName;
							_failedAssertion = _originalAssertion
									.elementAt(assertion_number);
							_failed_assertion_id = assertion_number;
							// error; we don't want this state to be active
							System.out.println(this.getErrorMessage());
							break;
						}
					}

					NextActiveStateRef = _env.getReferenceField(
							NextActiveStateRef, "next");
				}
		
			}
		}
	}

	public String getErrorMessage() {
		// Print info about assertion violation
		return "ASSERTION VIOLATION: Command " + _command
				+ " issued while state " + _assertion + "is active";
	}

	public String getFailedCommand() {

		return _failedCommand;

	}

	public int getFailedStepSeqNo() {

		return _failedAtStep;

	}

	public String getFailedAssertion() {

		return _failedAssertion;

	}

	public Vector<String> getTrace() {

		return _trace;

	}

	public int findAssertionNumber(String command) {
		return (_command.indexOf(command));
	}

	public static String translatePath(String path) {
		String out = "";

		String[] smStates = path.split("/");
		for (int i = 1; i < smStates.length; i++) {

			String[] smState = smStates[i].split(":");
			if (smState[0].equals("PIssEps"))
				out += "";
			else
				out += ".__" + smState[0].toLowerCase();
			if (smState.length == 2) {

				if (smState[1].equals("Running"))
					if (smState[0].equals("PIssEps"))
						out += "__pisseps.__running1";
					else if (smState[0].equals("Component"))
						out += ".__running2";
					else if (smState[0].equals("TDdcuLA1B"))
						out += ".__running3";
					else if (smState[0].equals("TRpcmLA1BB"))
						out += ".__running4";
					else if (smState[0].equals("TRpcmLA1BE"))
						out += ".__running5";
					else
						out += ".__running";
			}
		}
		return out;
	}

	public void getAssertions(String fileName) {
		String line = "";
		String path = "";
		String lastPrefix = "";

		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));

			while ((line = in.readLine()) != null) {

				line = line.trim();
				if (line.startsWith("SM=")) {
					path = line.substring(3);
					_path.add(translatePath(path));
					lastPrefix = getPrefixFromPath(path);

				} else if (line.startsWith("CMD=")) {

					_command.add(lastPrefix + line.substring(4));

				} else if (line.startsWith("ASSERT=")) {

					_assertion.add(translatePath(line.substring(7)));
					_originalAssertion.add(line.substring(7));

				}
			}
			in.close();
		} catch (FileNotFoundException e) {

			System.err.println(e.getMessage());

		} catch (IOException e) {

			System.err.println(e.getMessage());

		}

	}

	public static String getPrefixFromPath(String path) {
		String out = "";

		String[] smStates = path.split("/");

		int i = smStates.length - 1;
		while (!(smStates[i].startsWith("TRpcmLA1B") || smStates[i]
				.startsWith("TRpcLA1B")))
			i--;

		//either an rpcm or an rpc
		out = smStates[i].split(":")[0].substring(1).toUpperCase() + "_" + out;
		String[] smState = smStates[i - 1].split(":");
		if (smState.length == 2 && smState[0].startsWith("TRpcmLA1B"))
			out = smState[0].substring(1).toUpperCase() + "_" + out;
		else if (smState[0].startsWith("TRpcLA1B")) { //an rpc; need both the rpc 
			out = smState[0].substring(1).toUpperCase() + "_" + out; //and its rpcm parent names
			String[] smParentState = smStates[i - 2].split(":");
			if (smParentState.length == 2)
				out = smParentState[0].substring(1).toUpperCase() + "_" + out;
		}

		return out;

	}

}
