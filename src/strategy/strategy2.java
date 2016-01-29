package strategy;

import java.io.FileNotFoundException;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import co_evolution_Manager.configure;

public class strategy2 extends strategy{

	//Ti+1 = delta (Ti) + Ti

	public static void apply (Property property) throws FileNotFoundException {	

		StmtIterator iter ;		

		if (configure.targetAdditionsChangeset!=null) {			
			iter = TarAdd_model.listStatements((Resource)null, property, (RDFNode)null);
			SyncTarAdd_model.add(iter.toList());
			SyncSrcAdd_model.add(iter.toList());

			configure.S2_Add_triplesize = configure.S2_Add_triplesize + iter.toList().size();
		}

		if (configure.targetDeletionsChangeset!=null) {
			iter = TarDel_model.listStatements((Resource)null, property, (RDFNode)null);
			SyncTarDel_model.add(iter.toList());
			SyncSrcDel_model.add(iter.toList());

			configure.S2_Del_triplesize = configure.S2_Del_triplesize + iter.toList().size();
		}
	}
}
