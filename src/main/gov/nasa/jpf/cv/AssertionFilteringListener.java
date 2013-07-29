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
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.choice.sc.SCEvent;
import gov.nasa.jpf.jvm.choice.sc.SCEventGenerator;

public class AssertionFilteringListener extends PropertyListenerAdapter {

  String[] methodNames;

  public AssertionFilteringListener(Config conf) {
    methodNames = conf.getStringArray("assertionFilter.include");
  }

  public void executeInstruction(JVM vm) {

		Instruction insn = vm.getLastInstruction();

		if (insn instanceof ATHROW) {
			ThreadInfo ti = vm.getLastThreadInfo();

			int xobjref = ti.peek();
			ElementInfo ei = vm.getElementInfo(xobjref);
			ClassInfo ci = ei.getClassInfo();
			if (ci.getName().equals("java.lang.AssertionError")) {

				ChoiceGenerator cg = vm.getChoiceGenerator();
				while (!((cg == null) || (cg instanceof SCEventGenerator))) {
					cg = cg.getPreviousChoiceGenerator();
				}

				if (cg == null)
					return;

				SCEventGenerator scg = (SCEventGenerator) cg;
				SCEvent scEve = scg.getNextChoice();
				for (String m : methodNames) {
					if (scEve.getId().equals(m))
						return;
				}
				ti.skipInstruction(insn.getNext());
				// pcm - since we are going on, we have to update the operand
				// stack so that we don't run out of stack space
				ti.pop();
			}

		}
	}
}

