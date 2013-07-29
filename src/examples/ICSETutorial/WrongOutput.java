package ICSETutorial;

import gov.nasa.jpf.sc.State;
import gov.nasa.jpf.cv.CVState;

public class WrongOutput extends CVState {
  
    
  class S0 extends State {
    
      public void send() {
          setNextState(s1);
      } 
  } S0 s0 = makeInitial(new S0());
  
  class S1 extends State {
	    
      public void acknowledge() {
          setNextState(s2);
      } 
      
      public void send() {
          setNextState(s1);
      } 
  } S1 s1 = new S1();

  
  class S2 extends State {
	    
      public void acknowledge() {
          setNextState(s0);
      } 
  } S2 s2 = new S2();
  
  class InputCompletion extends State {
	    
      public void input() {
          setNextState(ic);
      } 
  } InputCompletion ic = makeInitial(new InputCompletion());

}
  
 