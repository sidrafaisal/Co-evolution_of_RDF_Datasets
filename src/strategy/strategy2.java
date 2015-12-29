package strategy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;

import co_evolution_Manager.configure;

public class strategy2 extends strategy{

	//Ti+1 = delta (Ti) + Ti

	public static void apply (Property property) throws FileNotFoundException {	

		StmtIterator iter ;		
		long size = 0;
		if (configure.targetAdditionsChangeset!=null) {			
			Model SyncTarAdd_model = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);
			Model SyncSrcAdd_model = FileManager.get().loadModel(configure.SyncSrcAdd, configure.fileSyntax);

			Model tarAdd_model = FileManager.get().loadModel(configure.targetAdditionsChangeset, configure.fileSyntax);
			iter = tarAdd_model.listStatements((Resource)null, property, (RDFNode)null);

			while (iter.hasNext()){
				Statement stmt = iter.nextStatement(); 
				SyncTarAdd_model.add(stmt);
				SyncSrcAdd_model.add(stmt);
				size++;
			}
			SyncTarAdd_model.write(new FileOutputStream(configure.SyncTarAdd), configure.fileSyntax);
			SyncTarAdd_model.close();	
			SyncSrcAdd_model.write(new FileOutputStream(configure.SyncSrcAdd), configure.fileSyntax);
			SyncSrcAdd_model.close();	
			tarAdd_model.close();

			configure.S2_Add_triplesize = configure.S2_Add_triplesize + size;
		}

		if (configure.targetDeletionsChangeset!=null) {
			size = 0;
			Model SyncTarDel_model = FileManager.get().loadModel(configure.SyncTarDel, configure.fileSyntax);
			Model SyncSrcDel_model = FileManager.get().loadModel(configure.SyncSrcDel, configure.fileSyntax);

			Model tarDel_model = FileManager.get().loadModel(configure.targetDeletionsChangeset, configure.fileSyntax);
			iter = tarDel_model.listStatements((Resource)null, property, (RDFNode)null);
			while (iter.hasNext()){
				Statement stmt = iter.nextStatement(); 
				SyncTarDel_model.add(stmt);
				SyncSrcDel_model.add(stmt);
				size++;
			}

			SyncSrcDel_model.write(new FileOutputStream(configure.SyncSrcDel), configure.fileSyntax);
			SyncSrcDel_model.close();	
			SyncTarDel_model.write(new FileOutputStream(configure.SyncTarDel), configure.fileSyntax);
			SyncTarDel_model.close();	
			tarDel_model.close();

			configure.S2_Del_triplesize = configure.S2_Del_triplesize + size;
		}
	}
}
