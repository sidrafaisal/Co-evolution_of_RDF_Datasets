package co_evolution_Manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import Conflict_Resolver.resolver;
import Conflict_Resolver.statistics;

public class configure {
	
	public static long time_S1 ;
	public static long time_S2 ;
	
	public static long S1_Del_triplesize;
	public static long S2_Del_triplesize;
	public static long S1_Add_triplesize;
	public static long S2_Add_triplesize;	
	
	public static String initialTarget;
	public static String sourceAdditionsChangeset;
	public static String sourceDeletionsChangeset; 
	public static String targetAdditionsChangeset;
	public static String targetDeletionsChangeset;
	
	public static String SyncSrcAdd = "SyncSrcAdd";
	public static String SyncSrcDel = "SyncSrcDel";
	public static String SyncTarAdd = "SyncTarAdd";
	public static String SyncTarDel = "SyncTarDel";
	
	public static String fileSyntax;
	public static List<String> predicateList;
	public static Map<String, String> strategyforPredicate;
	
	public static String ontology;	
	private static OWLOntology OWLOntology;
	private static OWLOntologyManager manager;

	public static void configureFiles (String sa, String sd, String ta, String td, String t) {
//System.out.println(sa+" "+sd+" " + ta+ " " + td + " " + t);
		if( !isEmpty (t))
			setinitialTarget(t);
		else
			setinitialTarget(null);	

		if( !isEmpty (sa))
			setsourceAdditionsChangeset(sa);
		else
			setsourceAdditionsChangeset(null);

		if( !isEmpty (sd))
			setsourceDeletionsChangeset(sd);
		else
			setsourceDeletionsChangeset(null);

		if( !isEmpty (ta))
			settargetAdditionsChangeset(ta);
		else
			settargetAdditionsChangeset(null);

		if( !isEmpty (td))
			settargetDeletionsChangeset(td);
		else
			settargetDeletionsChangeset(null);	
	
		//updateDatasetSize (); 
	}

	
	configure (String synt, String p, String o) throws IOException, OWLException{ 
		reset_counters();
		createFiles ("SyncSrcAdd", "SyncSrcDel", "SyncTarAdd", "SyncTarDel");
		configure.fileSyntax = synt;
		configure.ontology = o;						
		predicateList = getPredicates(p);
		selectStrategy(); //todo:autoload strategy

		//save(strat);	
	}
	
	private static void selectStrategy() throws OWLException {
		
		strategyforPredicate  = new HashMap<String, String>();

		List<String> predicates_toResolve = new ArrayList<String>();
		List<String> predicates_notToResolve = new ArrayList<String>();
		
		System.out.println("Select strategy for each predicate.\n Allowed strategies: 1-syncsourceNignorelocal, 2-nsyncsourceBkeeplocal, 3-syncsourceNkeeplocalBnotconflicts, " + 
		"4-syncsourceNkeeplocalWresolvedconflicts.");
		
		for (String predicate: predicateList) {
				System.out.println("\nEnter a strategy for property: "+ predicate);
				String strategy = main.scanner.nextLine();
				switch (strategy) {
				case "1":
					strategy= "syncsourceNignorelocal";
					break;
				case "2":
					strategy= "nsyncsourceBkeeplocal";
					break;
				case "3":
					strategy= "syncsourceNkeeplocalBnotconflict";
					break;
				case "4":
					strategy= "syncsourceNkeeplocalWresolvedconflicts";
					break;
				}
				configure.strategyforPredicate.put(predicate, strategy);
				if (strategy.equals("syncsourceNkeeplocalWresolvedconflicts")) 
					predicates_toResolve.add(predicate);
				if (strategy.equals("syncsourceNkeeplocalBnotconflicts")) 
					predicates_notToResolve.add(predicate);	
				
		}
		if (!predicates_toResolve.isEmpty() || !predicates_notToResolve.isEmpty()) {
			checkPredicateType (); 
			System.out.println("For manual resolution, press 0. For auto resolution, press 1.");	
			String r = main.scanner.nextLine();

			if (r.equals("0")) {
				Conflict_Resolver.manual_Selector.select(predicates_toResolve, predicates_notToResolve);
				resolver.manual_selector = true;
			}
			else if (r.equals("1")) {
			//	Conflict_Resolver.function_Auto_Selector.select(predicates_toResolve, predicates_notToResolve);
				resolver.auto_selector = true;
			}

			Conflict_Finder.source_Delta.setPredicateFunctionUseCounter ();	
		}
	}
	
	public static List<String> getPredicates (String filename) throws IOException {
		
		List <String> predicateList = new ArrayList<String>();
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(filename));
			while ((line = br.readLine()) != null) 
				predicateList.add(line);		
			br.close();
		return predicateList;

	}
	
	public static void reset_counters() {
		for (int i = 0; i < 8; i++)
			Conflict_Finder.source_Delta.number_Of_caseTriples [i] = 0;
		configure.time_S1 = 0;
		configure.time_S2 = 0;
		
		S1_Del_triplesize = 0;
		S2_Del_triplesize = 0;
		
		S1_Add_triplesize = 0;
		S2_Add_triplesize = 0;
		
	}
	private static void createFiles (String SyncSrcAdd, String SyncSrcDel, String SyncTarAdd, String SyncTarDel) throws IOException { 

	File nt = new File(SyncSrcAdd);
	if(!nt.exists())
		nt.createNewFile();
	configure.SyncSrcAdd = SyncSrcAdd;
	
	nt = new File(SyncSrcDel);
	if(!nt.exists())
		nt.createNewFile();
	configure.SyncSrcDel = SyncSrcDel;
	
	nt = new File(SyncTarAdd);
	if(!nt.exists())
		nt.createNewFile();
	configure.SyncTarAdd = SyncTarAdd;
	
	nt = new File(SyncTarDel);
	if(!nt.exists())
		nt.createNewFile();
	configure.SyncTarDel = SyncTarDel;
}
	private static void checkPredicateType () throws OWLException{
		@Nonnull 
			File f = new File(configure.ontology);
			manager = OWLManager.createOWLOntologyManager();
			OWLOntology = manager.loadOntologyFromOntologyDocument(f);

			for (String predicate : predicateList) 									
				checkProperty(predicate);
	}
	
	private static void checkProperty(String p) {
		OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(OWLOntology);
		OWLDataFactory fac = manager.getOWLDataFactory();
		IRI i = IRI.create(p); 

		OWLObjectProperty objprop = fac.getOWLObjectProperty(i);
		Set<OWLClassExpression> classexpr= objprop.getRanges(OWLOntology);

		if (!classexpr.isEmpty()){
			if (objprop.isFunctional(OWLOntology) ) //|| objprop.isInverseFunctional(OWLOntology)) 	
				statistics.predicateType.put(p, "F"); 
		} else {

			OWLDataProperty dataprop = fac.getOWLDataProperty(i);
			Set<OWLDataRange> datarange = dataprop.getRanges(OWLOntology);

			if (!datarange.isEmpty()){
				if (dataprop.isFunctional(OWLOntology))
					statistics.predicateType.put(p, "F"); 
			}
		}		
		reasoner.dispose();
	}
	
	public static void setfileSyntax(String s){
		fileSyntax = s;
	}

	public static void setinitialTarget(String s){
		initialTarget = s;
	}
	public static void setSyncSrcAdd(String s){
		SyncSrcAdd = s;
	}
	public static void setSyncSrcDel(String s){
		SyncSrcDel = s;
	}
	public static void setsourceAdditionsChangeset(String s){
		sourceAdditionsChangeset = s;
	}

	public static void setsourceDeletionsChangeset(String s){
		sourceDeletionsChangeset = s;
	}

	public static void settargetAdditionsChangeset(String s){
		targetAdditionsChangeset = s;
	}

	public static void settargetDeletionsChangeset(String s){
		targetDeletionsChangeset = s;
	}

	public static void setOntology(String o) {
		ontology = o;
	}
	public static boolean isEmpty(String f) {
		if (f!=null){
			File file = new File(f);
			if(file.length()<=0)
				return true;
			else
				return false;
		} else
			return false;
	}
	public static long getDatasetSize (String filename) {
		long number_Of_Triples  = 0;
		if (filename != null) {
			Model model = FileManager.get().loadModel(filename, configure.fileSyntax);
			number_Of_Triples = model.size();
			model.close();
		}	
		return number_Of_Triples;
	}
}