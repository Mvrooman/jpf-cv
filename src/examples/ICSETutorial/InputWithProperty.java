package ICSETutorial;

import gov.nasa.jpf.sc.State;
import gov.nasa.jpf.cv.CVState;

public class InputWithProperty extends CVState {
  
   
  class S0 extends State {
    
      public void input() {
          setNextState(s1);
      } 
      public void output() {
          assert(false);
      }       
//      public void acknowledge() {
//    	  setNextState(sink);
//      }
//      public void send() {
//    	  setNextState(sink);
//      }

  } S0 s0 = makeInitial(new S0());
  
  class S1 extends State {
	    
      public void send() {
          setNextState(s4);
      }
      public void output() {
          setNextState(s2);
      } 
//      public void acknowledge() {
//    	  setNextState(sink);
//      }
//      public void input() {
//    	  setNextState(sink);
//      }


  } S1 s1 = new S1();

  
  class S2 extends State {
      public void send() {
          setNextState(s3);
      }
      public void output() {
          assert(false);
      }
//      public void acknowledge() {
//    	  setNextState(sink);
//      }
//      public void input() {
//    	  setNextState(sink);
//      }


  } S2 s2 = new S2();
 
  class S3 extends State {
      public void acknowledge() {
          setNextState(s0);
      } 
      public void output() {
          assert(false);
      }
//      public void send() {
//    	  setNextState(sink);
//      }
//      public void input() {
//    	  setNextState(sink);
//      }

  } S3 s3 = new S3();
  
  class S4 extends State {
	    
      public void output() {
          setNextState(s3);
      } 

      public void acknowledge() {
          setNextState(s5);
      } 
//      public void send() {
//    	  setNextState(sink);
//      }
//      public void input() {
//    	  setNextState(sink);
//      }


  } S4 s4 = new S4();

  
  class S5 extends State {
      public void input() {
          assert(false);
      }  
      public void output() {
          setNextState(s0);
      }
//      public void send() {
//    	  setNextState(sink);
//      }
//      public void acknowledge() {
//    	  setNextState(sink);
//      }
      
  } S5 s5 = new S5();
  
//  class Sink extends State {
//      public void input() {
//          setNextState(sink);
//      }  
//      public void output() {
//          setNextState(sink);
//      }
//      public void send() {
//    	  setNextState(sink);
//      }
//      public void acknowledge() {
//    	  setNextState(sink);
//      }
//      
//  } Sink sink = new Sink();
  
  
}
 