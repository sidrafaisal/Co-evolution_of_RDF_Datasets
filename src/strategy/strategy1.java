package strategy;

import java.io.FileNotFoundException;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import co_evolution_Manager.configure;

public class strategy1 extends strategy{

	//Ti+1 = delta (Si) + Ti	

	public static void apply (Property property) throws FileNotFoundException {	

		StmtIterator iter ;
		long size = 0;
		if (configure.sourceDeletionsChangeset!=null) {
			iter = SrcDel_model.listStatements((Resource)null, property, (RDFNode)null);
			SyncTarDel_model.add(iter.toList());
			size += iter.toList().size();
			
			configure.S1_Del_triplesize = configure.S1_Del_triplesize + size;
		}

		if (configure.sourceAdditionsChangeset!=null) {
			size = 0;
			iter = SrcAdd_model.listStatements((Resource)null, property, (RDFNode)null);
			SyncTarAdd_model.add(iter.toList());
			size += iter.toList().size();

			configure.S1_Add_triplesize = configure.S1_Add_triplesize + size;
		}
	}
}
