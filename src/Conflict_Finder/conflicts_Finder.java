package Conflict_Finder;

import strategy.strategy;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shared.AddDeniedException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import co_evolution_Manager.configure;
public class conflicts_Finder {

	public static boolean resolve;
	public static long time_S3, time_S4;
	public static long S3_Add_triplesize;
	public static long S3_Del_triplesize;
	public static long S4_Add_triplesize;
	public static long S4_Del_triplesize;

	public static void identifyConflicts(Property property, boolean r) throws RiotException, FileNotFoundException, AddDeniedException, OWLOntologyCreationException{
		resolve = r;
		long starttriples = 0, 	startTime = 0;

		if (configure.sourceAdditionsChangeset != null) { 	
			starttriples = strategy.SyncTarAdd_model.size() ;

			startTime = System.currentTimeMillis();
			source_Delta.apply(property);					//Step 1 & 2
			long endTime   = System.currentTimeMillis();
			if (resolve)
				time_S4 += (endTime - startTime);		
			else
				time_S3 += (endTime - startTime);

			long endtriples = strategy.SyncTarAdd_model.size() ;
			increaseAddTriples(endtriples - starttriples);

		}
		startTime = System.currentTimeMillis();
		applyDelTarget(property);								//Step 3
		long endTime   = System.currentTimeMillis();
		if (resolve)
			time_S4 += (endTime - startTime);		
		else
			time_S3 += (endTime - startTime);

		starttriples = strategy.SyncTarAdd_model.size();
		startTime = System.currentTimeMillis();
		applyAddTarget(property);								//Step 4

		endTime   = System.currentTimeMillis();
		if (resolve)
			time_S4 += (endTime - startTime);		
		else
			time_S3 += (endTime - startTime);
		
		long endtriples = strategy.SyncTarAdd_model.size() ;
		increaseAddTriples(endtriples - starttriples);

	}	

	public static void increaseAddTriples (long value){
		if (resolve)
			S4_Add_triplesize += value;
		else
			S3_Add_triplesize += value;
	}
	public static void increaseDelTriples (long value){
		if (resolve)
			S4_Del_triplesize += value;
		else
			S3_Del_triplesize += value;
	}

	/*Apply rest of the changes directly*/

	public static void applyDelTarget(Property property) throws FileNotFoundException, org.apache.jena.riot.RiotException{
		if (configure.targetDeletionsChangeset!=null)	{
			StmtIterator iter = strategy.TarDel_model.listStatements((Resource)null, property, (RDFNode)null);
			strategy.SyncTarDel_model.add(iter.toList());
			strategy.SyncSrcDel_model.add(iter.toList());

			increaseDelTriples(iter.toList().size());
			strategy.SyncSrcDel_model.write(new FileOutputStream(configure.SyncSrcDel), configure.fileSyntax);		
			strategy.SyncTarDel_model.write(new FileOutputStream(configure.SyncTarDel), configure.fileSyntax);				
		}
	}

	public static void applyAddTarget(Property property) throws FileNotFoundException, org.apache.jena.riot.RiotException {
		if (configure.targetAdditionsChangeset!=null) {
			StmtIterator iter = strategy.TarAdd_model.listStatements((Resource)null, property, (RDFNode)null);
			strategy.SyncSrcAdd_model.add(iter.toList());
			strategy.SyncTarAdd_model.add(iter.toList());
			strategy.SyncTarAdd_model.write(new FileOutputStream(configure.SyncTarAdd), configure.fileSyntax);
			strategy.SyncSrcAdd_model.write(new FileOutputStream(configure.SyncSrcAdd), configure.fileSyntax);		

		}
	}
}
