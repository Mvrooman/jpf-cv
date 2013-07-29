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

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.Path;
import gov.nasa.jpf.jvm.Step;
import gov.nasa.jpf.jvm.Transition;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.choice.sc.SCEvent;
import gov.nasa.jpf.jvm.choice.sc.SCEventGenerator;


import java.util.Vector;

public class SCConverter {

	/*
	 * Converts JPF path into a vector of "actions" from the alphabet
	 */
	public Vector getCex(JVM jvm, Vector alphabet) {
		Vector path = new Vector();

		ChoiceGenerator<?> cg = jvm.getChoiceGenerator();
		do {
			if (cg instanceof SCEventGenerator) {
				SCEvent scEve = ((SCEventGenerator) cg).getNextChoice();
				String action = getAction(scEve);
				if (alphabet.contains(action)) {
						path.add(0, action);
				}
			}
			cg = cg.getPreviousChoiceGenerator();
		} while (cg != null);

		return path;
	}

		public String getAction(SCEvent scEve) {
			String event = scEve.getMethodName();
			Object[] args = scEve.getArguments();
			int argNum = 0;
			if (args != null)
				argNum = args.length;

			//append any arguments
			if (argNum > 0) {
				event = event.concat("(");
				String param = null;
				for (int k = 0; k < argNum; k++) {
					if (args[k] instanceof String) {
						param = (String) args[k];
					} else if (args[k] instanceof Boolean) {
						Boolean foo = (Boolean) args[k];
						param = foo.toString();
					} else if (args[k] instanceof Integer) {
						Integer foo = (Integer) args[k];
						param = foo.toString();
					} else if (args[k] instanceof Double) {
						Double foo = ((Double) args[k])
								.doubleValue();
						param = foo.toString();
					}
					event = event.concat(param);
					if ((k + 1) < argNum)
						event = event.concat("#");
				}
				event = event.concat(")");
			}
			return (event);
		}

//	public Vector convertJPFPath(Path path_from_jpf, Vector alphabet) {
//		Vector path = new Vector();
//		int cex_length = path_from_jpf.size();
//		Transition t;
//		Step s;
//		for (int i = 0; i < cex_length; i++) {
//			t = path_from_jpf.get(i); // should check if t is null?
//			int transition_length = t.getStepCount();
//			//System.out.println("trans ");
//			for (int j = 0; j < transition_length; j++) {
//				s = t.getStep(j);
//				//System.out.println("step "+s.getInstruction());
//				Instruction instr = s.getInstruction();
//
//				if (instr instanceof InvokeInstruction && s.getNext()!=null) {
//					InvokeInstruction md = (InvokeInstruction) instr;
//					MethodInfo mi = md.getInvokedMethod();
//					String event = mi.getName();
//
//					ChoiceGenerator cg = t.getChoiceGenerator();
//					if (cg instanceof SCEventGenerator) {
//						SCEventGenerator cgEve = (SCEventGenerator) t
//								.getChoiceGenerator();
//						SCEvent scEve = cgEve.getNextChoice();
//						String scMethodName = scEve.getMethodName();
//
//						if (event.contains(scMethodName)) {
//
//	//						System.out.println("^^^^^^^^ The choice name here is " + scMethodName);
//	//						System.out.println("^^^^^^^^ The method name here is " + event + "\n");
//
//
//							Object[] args = scEve.getArguments();
//							int argNum = 0;
//							if (args != null)
//								argNum = args.length;
//
//							//append any arguments
//							if (argNum > 0) {
//								event = event.concat("(");
//								String param = null;
//								for (int k = 0; k < argNum; k++) {
//									if (args[k] instanceof String) {
//										param = (String) args[k];
//									} else if (args[k] instanceof Boolean) {
//										Boolean foo = (Boolean) args[k];
//										param = foo.toString();
//									} else if (args[k] instanceof Integer) {
//										Integer foo = (Integer) args[k];
//										param = foo.toString();
//									} else if (args[k] instanceof Double) {
//										Double foo = ((Double) args[k])
//												.doubleValue();
//										param = foo.toString();
//									}
//									event = event.concat(param);
//									if ((k + 1) < argNum)
//										event = event.concat("#");
//								}
//								event = event.concat(")");
//							}
//							for (int k = 0; k < alphabet.size(); k++) {
//								String action = (String) alphabet.elementAt(k);
//								if (event.compareTo(action) == 0) {
//									path.add(action);
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		System.out.println("Converted JPF CounterExample path:"
//				+ path.toString());
//		return path;
//	}
//
//

  // <2do> is this really what we want - adding argument values instead of
  // a type signature? Why use '#' instead of a ',' separator?
	public static String addParameters(String methodName, Object[] args) {
    if (args == null || args.length == 0){
      return methodName;

    } else {
      StringBuilder sb = new StringBuilder();

      sb.append(methodName);
      sb.append('(');
      for (int i=0; i<args.length; i++){
        if (i>0){
          sb.append('#');
        }
        sb.append(args[i]);
      }
      sb.append(')');
      return sb.toString();
    }
	}
}
