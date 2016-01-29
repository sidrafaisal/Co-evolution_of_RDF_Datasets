package strategy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.FileManager;

import co_evolution_Manager.configure;

public class strategy {
	
	static String strategy;	

	public static Model SrcAdd_model, SrcDel_model, TarDel_model, TarAdd_model, Init_model,
	SyncTarAdd_model, SyncSrcAdd_model, SyncSrcDel_model, SyncTarDel_model;

	public static void apply () throws FileNotFoundException {		

		SyncTarAdd_model = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);		
		SyncTarDel_model = FileManager.get().loadModel(configure.SyncTarDel, configure.fileSyntax);		
		SyncSrcAdd_model = FileManager.get().loadModel(configure.SyncSrcAdd, configure.fileSyntax);
		SyncSrcDel_model = FileManager.get().loadModel(configure.SyncSrcDel, configure.fileSyntax);
		Init_model = FileManager.get().loadModel(configure.initialTarget, configure.fileSyntax);
		
		if (configure.sourceAdditionsChangeset!=null)
			SrcAdd_model = FileManager.get().loadModel(configure.sourceAdditionsChangeset, configure.fileSyntax);
		if (configure.sourceDeletionsChangeset!=null)
			SrcDel_model = FileManager.get().loadModel(configure.sourceDeletionsChangeset, configure.fileSyntax);
		if (configure.targetAdditionsChangeset!=null)
			TarAdd_model = FileManager.get().loadModel(configure.targetAdditionsChangeset, configure.fileSyntax);
		if (configure.targetDeletionsChangeset!=null)
			TarDel_model = FileManager.get().loadModel(configure.targetDeletionsChangeset, configure.fileSyntax);
		
		for (String p: configure.predicateList)
		{
			strategy = configure.strategyforPredicate.get(p);
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
			} else if (strategy.equals("syncsourceNkeeplocalBnotconflict")) {
				strategy34.apply(property, false);
			} else if (strategy.equals("syncsourceNkeeplocalWresolvedconflicts")) {
				strategy34.apply(property, true);
			}			
		}
		
		SyncTarAdd_model.write(new FileOutputStream(configure.SyncTarAdd), configure.fileSyntax);
		SyncTarAdd_model.close();	

		SyncTarDel_model.write(new FileOutputStream(configure.SyncTarDel), configure.fileSyntax);
		SyncTarDel_model.close();				

		SyncSrcAdd_model.write(new FileOutputStream(configure.SyncSrcAdd), configure.fileSyntax);
		SyncSrcAdd_model.close();	

		SyncSrcDel_model.write(new FileOutputStream(configure.SyncSrcDel), configure.fileSyntax);
		SyncSrcDel_model.close();			
		
		if (configure.sourceAdditionsChangeset!=null)
			SrcAdd_model.close();		
		if (configure.sourceDeletionsChangeset!=null)
			SrcDel_model.close();
		if (configure.targetAdditionsChangeset!=null) 
			TarAdd_model.close();
		if (configure.targetDeletionsChangeset!=null)
			TarDel_model.close();;
		Init_model.close();
	
}
}