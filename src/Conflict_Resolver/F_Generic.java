package Conflict_Resolver;

import java.util.Arrays;
import java.util.Random;

import Conflict_Finder.source_Delta;

public class F_Generic {
	
	public static String latestSource;
	
	public static String Compute (String function, String [] args){
		switch (function) {
			
			case "any":
				return any(args);
			
			case "first":
				return first(args); 
			
			case "shortest":
				return shortest(args);

			case "longest":
				return longest(args);
			
			case "concatenation":
				return concatenation(args);	
			
			case "bestSource":
				return bestSource(args);
			
			case "globalVote":
				return globalVote();
			
			case "latest":
				return latest(args);
			
			case "threshold":
				return threshold(args);
			
			case "best":
				return best(args);
			
			case "topN":
				return topN(args);
			
			case "chooseDepending":
				return chooseDepending(args);
			
			case "chooseCorresponding":
				return chooseCorresponding(args);
			
			case "mostComplete":
				return mostComplete(args);
			
			default:
				return " ";
		}
	}
	
	/*								Resolution Functions				*/	
	
	
	public static String threshold (String[] args) {

		return args[0];
	}	
	public static String best (String[] args) {

		return args[0];
	}	
	public static String topN (String[] args) {
	
		return args[0];
	}	
	public static String chooseDepending (String[] args) {

		return args[0];
	}	
	public static String chooseCorresponding (String[] args) {

		return args[0];
	}	
	

	
	
//requires data as well as additional info
	
	public static String latest (String[] args) {
		//todo: read date from both files
		if (latestSource.equals("source"))
			return args[0];
		else 
			return args[1];	
	}
	
	public static String globalVote () {	
		
		String p = source_Delta.current_Predicate;
		return Conflict_Resolver.statistics.mostFrequentValue.get(p);	
	
	}	
	
	public static String bestSource (String[] args) {
		
		String p = source_Delta.current_Predicate;
		String preferedsource = statistics.preferedSourceforPredicate.get(p);
		
		if (preferedsource.equals("source"))
			return args[0];
		else
			return args[1];

	}	
	
	public static String mostComplete (String[] args) {
		
		String p = source_Delta.current_Predicate;
		String preferedsource = statistics.preferedSourceforPredicate.get(p);
	
		if (preferedsource.equals("source"))
			return args[0];
		else
			return args[1];
		
	}	
	
//requires only data	
	public static String first (String[] args) {
		Arrays.sort(args);
		return args[0];
	}	

	public static String shortest (String[] args) {		
	    String shortest = any(args);

	    for (String value : args) {
	        if (value.length() < shortest.length()) 
	            shortest = value;
	    }
	    return shortest;    
	}
	
	public static String longest (String[] args) {		
	    String longest = any(args);

	    for (String value : args) {
	        if (value.length() > longest.length()) 
	            longest = value;
	    }
	    return longest;    
	}
	
	public static String any (String[] args) {
		int randomValue = new Random().nextInt(args.length);
	    return args[randomValue];		
	}

	public static String concatenation (String[] args) {
		String concatenate = args[0];
		for (int i = 1; i < args.length; i++)
		{
			concatenate += ", ";
			concatenate += args[1];			
		}
		return concatenate;
	}
}
