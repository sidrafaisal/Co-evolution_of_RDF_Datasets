package Conflict_Resolver;

public class F_Math {

	public static String Compute (String function, String [] args, String type) {	
		
		String val = "";
		
		if (type.equals("unsignedLong") || type.equals("positiveInteger") || type.equals("nonPositiveInteger") || 
				type.equals("nonNegativeInteger") || type.equals("negativeInteger") || type.equals("integer")) 
			val = F_BigIntegers.Compute (function, args);
		
		else {
			
			val = F_Numbers.Compute (function, args); 
			Double d = Double.parseDouble(val);

			if (type.equals("int") || type.equals("unsignedShort")) 
				val = d.intValue() + "";	

			else if (type.equals("float")) 
				val = d.floatValue() + "";

			else if (type.equals("unsignedByte") || type.equals("short"))
				val = d.shortValue() + "";

			else if (type.equals("unsignedInt") || type.equals("long")) 
				val = d.longValue() + "";

		}
		return val;
	}

}
