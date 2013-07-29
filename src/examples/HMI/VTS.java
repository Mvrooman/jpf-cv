package HMI;

import gov.nasa.jpf.sc.State;
import gov.nasa.jpf.cv.CVState;

public class VTS extends CVState
{
	class LOW1 extends State
	{
		public void pushUp()
		{
			setNextState (medium1);
		}
		
		public void upp()
		{
			setNextState (low2);
		}
		
//		public void down(){assert (false);}
		public void pullDown(){assert (false);}
	} final LOW1 low1 = makeInitial (new LOW1());
	
	class LOW2 extends State
	{
		public void down()
		{
			setNextState (low1);
		}
		
		public void pushUp()
		{
			setNextState (medium1);
		}
		
		public void upp()
		{
			setNextState (low3);
		}
		
		public void pullDown(){assert (false);}
	} final LOW2 low2 = new LOW2();
	
	class LOW3 extends State
	{
		public void down()
		{
			setNextState (low2);
		}
		
		public void pushUp()
		{
			setNextState (high1);
		}
		
//		public void up(){assert (false);}
		public void pullDown(){assert (false);}
	} final LOW3 low3 = new LOW3();
	
	class MEDIUM1 extends State
	{
		public void pushUp()
		{
			setNextState (high1);
		}
		
		public void upp()
		{
			setNextState (medium2);
		}
		
		public void pullDown()
		{
			setNextState (low3);
		}
		
//		public void down(){assert (false);}
	} final MEDIUM1 medium1 = new MEDIUM1();
	
	class MEDIUM2 extends State
	{
		public void down()
		{
			setNextState (medium1);
		}
		
		public void pushUp()
		{
			setNextState (high2);
		}
		
		public void pullDown()
		{
			setNextState (low3);
		}
		
//		public void up(){assert (false);}
	} final MEDIUM2 medium2 = new MEDIUM2();
	
	class HIGH1 extends State
	{
		public void upp()
		{
			setNextState (high2);
		}
		
		public void pullDown()
		{
			setNextState (medium2);
		}
		
//		public void down(){assert (false);}
		public void pushUp(){assert (false);}
	} final HIGH1 high1 = new HIGH1();
	
	class HIGH2 extends State
	{
		public void down()
		{
			setNextState (high1);
		}
		
		public void pullDown()
		{
			setNextState (medium2);
		}
		
		public void upp()
		{
			setNextState (high3);
		}
		
		public void pushUp(){assert (false);}
	} final HIGH2 high2 = new HIGH2();
	
	class HIGH3 extends State
	{
		public void down()
		{
			setNextState (high2);
		}
		
		public void pullDown()
		{
			setNextState (medium2);
		}
		
//		public void up(){assert (false);}
		public void pushUp(){assert (false);}
	} final HIGH3 high3 = new HIGH3();
}