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

public class strategy1 extends strategy{

	//Ti+1 = delta (Si) + Ti	

	public static void apply (Property property) throws FileNotFoundException {	

		StmtIterator iter ;
		long size = 0;
		if (configure.sourceDeletionsChangeset!=null) {
			
			Model SyncTarDel_model = FileManager.get().loadModel( configure.SyncTarDel, configure.fileSyntax);
			Model srcDel_model = FileManager.get().loadModel( configure.sourceDeletionsChangeset, configure.fileSyntax);	
			iter = srcDel_model.listStatements((Resource)null, property, (RDFNode)null);
			while (iter.hasNext()) {	
				Statement stmt = iter.nextStatement();
				SyncTarDel_model.add(stmt);
				size++;
			}
			srcDel_model.close();	
			SyncTarDel_model.write(new FileOutputStream(configure.SyncTarDel), configure.fileSyntax);
			SyncTarDel_model.close();	
			configure.S1_Del_triplesize = configure.S1_Del_triplesize + size;
		}

		if (configure.sourceAdditionsChangeset!=null) {
			size = 0;
			Model SyncTarAdd_model = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);
			Model srcAdd_model = FileManager.get().loadModel( configure.sourceAdditionsChangeset, configure.fileSyntax);
			iter = srcAdd_model.listStatements((Resource)null, property, (RDFNode)null);
			while (iter.hasNext()){		
				Statement stmt = iter.nextStatement(); 
				SyncTarAdd_model.add(stmt);
				size++;
			}
			SyncTarAdd_model.write(new FileOutputStream(configure.SyncTarAdd), configure.fileSyntax);
			SyncTarAdd_model.close();
			configure.S1_Add_triplesize = configure.S1_Add_triplesize + size;
		}
	}
}
