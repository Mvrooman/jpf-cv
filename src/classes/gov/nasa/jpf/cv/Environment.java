/*
 * Created on Nov 17, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package gov.nasa.jpf.cv;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class MethodInvocation {
}

public class Environment implements Runnable {
// *** THIS IS A MODEL CLASS EXECUTED BY JPF ****
	
	Object target;
	
	public Environment(String[] args) {
		// TODO Auto-generated constructor stub
		
		if (args.length > 0) {
			String className = args[0];
			try {
				Class<?> cls = Class.forName(className);
				target = cls.newInstance(); // this works only if constructor
												// needs no arguments

			} catch (Throwable t ) {
				System.out.println("Exception " + t + " during Environment constructor");
			}
			
		}
	}
	

	public void run() {
		// TODO Auto-generated method stub
		// get the methods of target
		// and execute them - maybe according to some script 
		
		// needs to be updated by corina and dimitra
		   Method[] methods = target.getClass().getDeclaredMethods();
		    for (Method m : methods) {
		      if (m.getAnnotation(Alphabet.class) != null) {
		        try {
		          // <2do> no arguments yet
//		          logAction(target, m);
		          m.invoke(target); // equivalent to "target.m" 
		        } catch (IllegalAccessException iace) {
		          // runtime error
//		          runtimeError(iace);
		        } catch (IllegalArgumentException iare) {
		          // that's a runtime error, too
//		          runtimeError(iare);
		        } catch (InvocationTargetException ite) {
		          // this ultimately makes it into a property
//		          propertyViolation(ite);
		        }
		      }
		    }
	}
	
		/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Environment env = new Environment (args);		
		env.run();
		
	}
}
