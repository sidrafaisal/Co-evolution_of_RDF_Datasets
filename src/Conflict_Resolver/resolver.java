package Conflict_Resolver;

public class resolver {
	
		public static String [] availableResolutionFunctions = {"sum", "average", "median", "variance", "stdDev", "max", "min", "any", "first", 
			"shortest", "longest", "concatenation", "bestSource", "globalVote", "latest", "threshold", "best", "topN", "chooseDepending", 
			"chooseCorresponding", "mostComplete"};	
		
		public static boolean manual_selector = false;
		public static boolean auto_selector = false;
		
		public static String apply (String function, String [] args, String type){	

			if(function.equals("sum") || function.equals("average") || function.equals("max") || function.equals("min") ||
					function.equals("stdDev") || function.equals("variance") || function.equals("median")) 
				return F_Math.Compute (function, args, type);
			
			else
				return F_Generic.Compute (function, args); 

/*else if (type.equals("anyURI") || type.equals("anySimpleType") || type.equals("ENTITIES") || type.equals("ENTITY") || 
					type.equals("token") || type.equals("string") || type.equals("normalizedString") || type.equals("NMTOKENS") || 
					type.equals("NMTOKEN") || type.equals("NCName") || type.equals("Name") || type.equals("IDREFS") || 
					type.equals("ID") || type.equals("IDREF") || type.equals("language")){
				   val = rv;
			} else if (type.equals("double")) {
				   val = rv;
			} */
			
		}
}
