package Conflict_Resolver;

public class resolver {
	
		public static String [] availableResolutionFunctions = {"sum", "average", "median", "variance", "stdDev", "max", "min", "any", "first", 
			"shortest", "longest", "concatenation", "bestSource", "globalVote", "mostComplete"};	
		//"latest", "threshold", "best", "topN", "chooseDepending", "chooseCorresponding",
		
		public static boolean manual_selector = false;
		public static boolean auto_selector = false;
		
		public static String apply (String function, String [] args, String type){	

			if(function.equals("sum") || function.equals("average") || function.equals("max") || function.equals("min") ||
					function.equals("stdDev") || function.equals("variance") || function.equals("median")) 
				return F_Math.Compute (function, args, type);
			
			else
				return F_Generic.Compute (function, args); 
			
		}
}
