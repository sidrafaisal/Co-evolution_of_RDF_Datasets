package Conflict_Resolver;

import java.math.BigInteger;
import java.util.Arrays;

public class F_BigIntegers extends F_Math {	

	public static String Compute (String function, String [] args){
		
		if (function.equals("sum"))	
			return sum (function, args).toString();			
		
		else if (function.equals("average"))			
			return average (function, args).toString();
		
		else if (function.equals("max"))
			return max (function, args).toString();
		
		else if (function.equals("min"))
			return min (function, args).toString();
		
		else if (function.equals("median")) 
			return median (function, args).toString();

		else if (function.equals("variance")) 
			return variance (function, args).toString();		
		
		else if (function.equals("stdDev")) 
			return stdDev (function, args).toString();	
		
		else 
			return " ";
	}
	
	public static BigInteger sum (String function, String [] args){
		BigInteger o = new BigInteger(args[0]);
		for (int i = 1; i< args.length; i++)
		{
			BigInteger o1 = new BigInteger(args[i]);
			o = o.add(o1);
		}
		return o;
	}

	public static BigInteger average (String function, String [] args){
		BigInteger o = new BigInteger(args[0]);
		BigInteger size = new BigInteger(args.length+"");
		
		for (int i = 1; i < args.length; i++)
		{
			BigInteger o1 = new BigInteger(args[i]);
			o = o.add(o1);
		}
		return o.divide(size);
	}
	
	public static BigInteger max (String function, String [] args){
		BigInteger o = new BigInteger(args[0]);
		for (int i = 1; i < args.length; i++)
		{
			BigInteger o1 = new BigInteger(args[i]);
			o = o.max(o1);
		}
		return o;
	}

	public static BigInteger min (String function, String [] args){
		BigInteger o = new BigInteger(args[0]);
		for (int i = 1; i < args.length; i++)
		{
			BigInteger o1 = new BigInteger(args[i]);
			o = o.min(o1);
		}
		return o;
	}

	public static BigInteger median (String function, String [] args){
		int size = args.length;
		BigInteger  input [] = new BigInteger [size];
		BigInteger median = new BigInteger("0");
		BigInteger divider = new BigInteger("2");
		
		for (int i = 0; i < size; i++)
			input [i] = new BigInteger(args[i]);		
		
		Arrays.sort(input);
		
		if (size % 2 == 0)
			median = (input[size/2].add(input[size/2 - 1])).divide(divider);
		else
			median = input[size/2];						
	
		return median;
	}

	public static BigInteger variance (String function, String [] args){
		BigInteger o = new BigInteger(args[0]);
		BigInteger size = new BigInteger(args.length+"");
		for (int i = 1; i < args.length; i++)	{
			BigInteger o1 = new BigInteger(args[i]);
			o = o.add(o1);
		}
		BigInteger average = o.divide(size);
		BigInteger variance = new BigInteger("0");
		
		BigInteger  input [] = new BigInteger [args.length];
		
		for (int i = 0; i < args.length; i++)
			input [i] = new BigInteger(args[i]);

	    for(BigInteger value : input)
	    	variance = variance.add((average.subtract(value)).multiply(average.subtract(value)));

		return variance.divide(size);
	}

	public static BigInteger stdDev (String function, String [] args){
		BigInteger o = new BigInteger(args[0]);
		BigInteger size = new BigInteger(args.length+"");
		for (int i = 1; i < args.length; i++)
		{
			BigInteger o1 = new BigInteger(args[i]);
			o = o.add(o1);
		}
		BigInteger average = o.divide(size);
		BigInteger variance = new BigInteger("0");
		
		BigInteger  input [] = new BigInteger [args.length];
		
		for (int i = 0; i < args.length; i++)
			input [i] = new BigInteger(args[i]);

	    for(BigInteger value : input)
	    	variance = variance.add((average.subtract(value)).multiply(average.subtract(value)));

		return sqrt(variance.divide(size));
	}
	
	public static BigInteger sqrt(BigInteger n) {
		  BigInteger a = BigInteger.ONE;
		  BigInteger b = new BigInteger(n.shiftRight(5).add(new BigInteger("8")).toString());
		  while(b.compareTo(a) >= 0) {
		    BigInteger mid = new BigInteger(a.add(b).shiftRight(1).toString());
		    if(mid.multiply(mid).compareTo(n) > 0) b = mid.subtract(BigInteger.ONE);
		    else a = mid.add(BigInteger.ONE);
		  }
		  return a.subtract(BigInteger.ONE);
		}

}
