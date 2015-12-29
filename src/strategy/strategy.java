package strategy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;

import co_evolution_Manager.configure;

public class strategy {
	
	static String strategy;	

	public static void apply () throws FileNotFoundException {		
		for (String p: configure.predicateList)
		{
			String strategy = configure.strategyforPredicate.get(p);
			Property property = ResourceFactory.createProperty(p);

			if (strategy.equals("syncsourceNignorelocal")) {
				long startTime   = System.currentTimeMillis();
				strategy1.apply(property);
				long endTime   = System.currentTimeMillis();
				configure.time_S1 += (endTime - startTime);	
				
			} else if (strategy.equals("nsyncsourceBkeeplocal")) {
				long startTime   = System.currentTimeMillis();
				strategy2.apply(property);
				long endTime   = System.currentTimeMillis();
				configure.time_S2 += (endTime - startTime);	
			}
		}
	
		for (String p: configure.predicateList)
		{
			String strategy = configure.strategyforPredicate.get(p);

			if (strategy.equals("syncsourceNkeeplocalBnotconflicts") || strategy.equals("syncsourceNkeeplocalWresolvedconflicts")) {
				strategy34.apply();
			break;
			}	
		}
		
	//	deleteTriples (configure.initialTarget, configure.SyncTarDel);
	//	addTriples(configure.initialTarget, configure.SyncTarAdd);

	}

	// delete the triples for final output
	public static void deleteTriples (String initialtarget, String targetDeletionsChangeset) throws FileNotFoundException, org.apache.jena.riot.RiotException {

		if (initialtarget!=null) {	
				Model imodel = FileManager.get().loadModel(initialtarget, configure.fileSyntax);	

				if (targetDeletionsChangeset!=null) {
					Model tmodel = FileManager.get().loadModel(targetDeletionsChangeset, configure.fileSyntax);		

					StmtIterator iter = tmodel.listStatements();

					while (iter.hasNext()) {
						Statement stmt = iter.nextStatement();  // get next statement 
						imodel.getGraph().delete(stmt.asTriple());	// Delete the triples of target from initial		    					   
					}
					tmodel.close();
				} 
				imodel.write(new FileOutputStream(initialtarget), configure.fileSyntax);
				imodel.close();
		} 
	}

	// write in output file
	public static void addTriples(String inputfilename, String outputfilename) throws FileNotFoundException, org.apache.jena.riot.RiotException {
		if (inputfilename!=null)
		{
				Model model = FileManager.get().loadModel(inputfilename, configure.fileSyntax);			
				model.write(new FileOutputStream(outputfilename, true), configure.fileSyntax);
				model.close();
			}
	}
	public static void setStrategy(String s){
		strategy = s;
	}
	
	public String getStrategy(){
		return strategy;
	}
}
