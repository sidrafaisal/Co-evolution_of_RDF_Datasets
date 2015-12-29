package Conflict_Finder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.util.FileManager;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import co_evolution_Manager.configure;
public class conflicts_Finder {

	public static boolean resolve;
	public static long identificationTime = 0;
	public static long resolutionTime = 0;

	public static long s3 = 0;

	public static void identifyConflicts(boolean r) throws RiotException, FileNotFoundException, AddDeniedException, OWLOntologyCreationException{
		resolve = r;
		long starttriples, startTime = 0;

		if (configure.sourceAdditionsChangeset != null) { 
			Model omodel = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);		
			starttriples = omodel.size() ;
			
			source_Delta.apply();					//Step 1 & 2

			long endtriples = omodel.size() ;
			omodel.close();
			s3 += (endtriples - starttriples);
		}
		
		applyDelTarget();								//Step 3

	//	if (configure.SyncTarAdd!=null) {
			Model omodel = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);		
			starttriples = omodel.size();
			
			applyAddTarget();								//Step 4
			
			long endtriples = omodel.size() ;
			omodel.close();
			s3 += (endtriples - starttriples);
	//	}
System.out.println(s3);
		long endTime   = System.currentTimeMillis();
		identificationTime += source_Delta.identificationTime + (endTime - startTime);		
		source_Delta.identificationTime = 0;
		
		resolutionTime += source_Delta.resolutionTime ;
		source_Delta.resolutionTime = 0;

	}	

	/*Apply rest of the changes directly*/

	public static void applyDelTarget() throws FileNotFoundException, org.apache.jena.riot.RiotException{
		if (configure.targetDeletionsChangeset!=null)	{
				Model sync_sd_model = FileManager.get().loadModel(configure.SyncSrcDel, configure.fileSyntax);
				Model SyncTarDel_model = FileManager.get().loadModel(configure.SyncTarDel, configure.fileSyntax);			

				Model TarDel_model = FileManager.get().loadModel(configure.targetDeletionsChangeset, configure.fileSyntax);
				StmtIterator iter = TarDel_model.listStatements();
				while (iter.hasNext()) {
					Statement stmt = iter.nextStatement();  // get next statement		 
					SyncTarDel_model.add(stmt);
					sync_sd_model.add(stmt);
				}
				TarDel_model.close();	

				sync_sd_model.write(new FileOutputStream(configure.SyncSrcDel), configure.fileSyntax);		
				sync_sd_model.close();
				
				SyncTarDel_model.write(new FileOutputStream(configure.SyncTarDel), configure.fileSyntax);		
				SyncTarDel_model.close();				
		}
	}

	/////////////////////////////////////////////////////Step4
	
	public static void applyAddTarget() throws FileNotFoundException, org.apache.jena.riot.RiotException {
		if (configure.targetAdditionsChangeset!=null) {
				Model sync_sa_model = FileManager.get().loadModel(configure.SyncSrcAdd, configure.fileSyntax);
				Model tar_Add_model = FileManager.get().loadModel(configure.targetAdditionsChangeset, configure.fileSyntax);
				Model SyncTarAdd_model = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);			

				StmtIterator iter = tar_Add_model.listStatements();
				while (iter.hasNext()) {
					Statement stmt = iter.nextStatement();  // get next statement		 
					SyncTarAdd_model.add(stmt);
					sync_sa_model.add(stmt);
				}
				tar_Add_model.close();	
				
				SyncTarAdd_model.write(new FileOutputStream(configure.SyncTarAdd), configure.fileSyntax);
				SyncTarAdd_model.close();
				
				sync_sa_model.write(new FileOutputStream(configure.SyncSrcAdd), configure.fileSyntax);		
				sync_sa_model.close();
		}
	}
}
