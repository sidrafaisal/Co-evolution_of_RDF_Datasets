package Conflict_Resolver;

import java.util.Arrays;

public class F_Numbers extends F_Math {

	public static String Compute (String function, String [] args){
		
		switch (function) {
		case "sum": {
			Double d = sum(args);
			return (String.valueOf(d));
			}
		
		case "average":{
			Double d = average(args);
			return (String.valueOf(d));
			}
		
		case "median": {
			Double d = median(args);
			return (String.valueOf(d));
			}
		
		case "variance": {
			Double d = variance(args);
			return (String.valueOf(d));
			}
		
		case "stdDev": {
			Double d = stdDev(args);
			return (String.valueOf(d));
			}
		
		case "max": {
			Double d = max(args);
			return (String.valueOf(d));
			}
		
		case "min": {
			Double d = min(args);
			return (String.valueOf(d));
			}
		
		default:
			return "";
		}
	}
	
	public static double max(String [] args) {			
		double max = Double.MIN_VALUE;
		for (String input : args)
		{
			double value = Double.parseDouble(input); 
			if(value > max) 
		         max = value;
		}
		return max;
	}
	
		public static double min (String[] args) {
			double min = Double.MAX_VALUE;
			for (String input : args)
			{
				double value = Double.parseDouble(input); 
				if(value < min) 
			         min = value;
			}
			return min;
		}
		
		public static double sum (String[] args) {
			double sum = 0;
			for (String input : args)
				sum += Double.parseDouble(input);
			return sum;
		}

		public static double average (String[] args) {
			double average = 0;
			average = sum (args) / args.length;
			return average;
		}
		
		public static double median (String[] args) {	
			int size = args.length;
			double input [] = new double [size];
			double median = 0;
			
			for (int i = 0; i < size; i++)
				input [i] = Double.parseDouble(args[i]);		
			
			Arrays.sort(input);
			
			if (size % 2 == 0)
				median = (input[size/2] + input[size/2 - 1])/2;
			else
				median = input[size/2];
			return median;
		}
		
		public static double variance(String[] args) {
			int size = args.length;
			double input [] = new double [size];			
			for (int i = 0; i < size; i++)
				input [i] = Double.parseDouble(args[i]);	
			
		    double avg = average(args);
		    double variance = 0;
		    for(double value : input)
		    	variance += (avg-value)*(avg-value);
		    return variance/size;
		}

		public static double stdDev(String[] args) {
		    return Math.sqrt(variance(args));
		}
		
}
