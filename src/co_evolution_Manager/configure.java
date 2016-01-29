package co_evolution_Manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	public static List<String> predicates_toResolve = new ArrayList<String>();
	public static List<String> predicates_notToResolve = new ArrayList<String>();
	
	public static String ontology;	
	private static OWLOntology OWLOntology;
	private static OWLOntologyManager manager;
	private static String filename = "";
	
	public void configureFiles (String sa, String sd, String ta, String td) throws IOException {
		reset_counters();
		createFiles ("SyncSrcAdd", "SyncSrcDel", "SyncTarAdd", "SyncTarDel");

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
	}

	
	configure (String synt, String p, String o, String it) throws IOException, OWLException{ 
		reset_counters();
		
		createFiles ("SyncSrcAdd", "SyncSrcDel", "SyncTarAdd", "SyncTarDel");
		configure.fileSyntax = synt;
		configure.ontology = o;						
		predicateList = getPredicates(p);
		if( !isEmpty (it))
			setinitialTarget(it);
		else
			setinitialTarget(null);	
		selectStrategy(); 
	}
	
	private static void selectStrategy() throws OWLException {		
		strategyforPredicate  = new HashMap<String, String>();
		filename = "strategy.xml";
		File file = new File(filename);

		if(!file.exists()) 	{
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
							strategyforPredicate.put(predicate, strategy);
							if (strategy.equals("syncsourceNkeeplocalWresolvedconflicts")) 
								predicates_toResolve.add(predicate);
							if (strategy.equals("syncsourceNkeeplocalBnotconflicts")) 
								predicates_notToResolve.add(predicate);	
							
					}
			create(strategyforPredicate);		
		}
		else 
			populate();	
		
		if (!predicates_toResolve.isEmpty() || !predicates_notToResolve.isEmpty()) {
			checkPredicateType (); 
			System.out.println("For manual resolution, press 0. For auto resolution, press 1.");	
			String r = main.scanner.nextLine();

			if (r.equals("0")) {
				Conflict_Resolver.manual_Selector.select(predicates_toResolve, predicates_notToResolve);
				resolver.manual_selector = true;
			} else if (r.equals("1")) {
			//Conflict_Resolver.function_Auto_Selector.select(predicates_toResolve, predicates_notToResolve);
				resolver.auto_selector = true;
			}
		}
	}
	public static void populate() {		
		try {
			File file = new File(filename);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();

			NodeList predList = doc.getElementsByTagName("Predicate");

			for (int temp = 0; temp < predList.getLength(); temp++) {
				Node pred = predList.item(temp);	    				

				if (pred.getNodeType() == Node.ELEMENT_NODE) {
					Element p = (Element) pred;
					String predicate = p.getAttribute("name");
					String strategy = p.getAttribute("strategy");
					strategyforPredicate.put(predicate, strategy);
					
					if (strategy.equals("syncsourceNkeeplocalWresolvedconflicts")) 
						predicates_toResolve.add(predicate);
					if (strategy.equals("syncsourceNkeeplocalBnotconflicts")) 
						predicates_notToResolve.add(predicate);	
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void create (Map<String, String> strategyforPredicate) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("Predicate_Strategy");
			doc.appendChild(rootElement);

			Iterator<String> keySetIterator = strategyforPredicate.keySet().iterator();
			while(keySetIterator.hasNext()) {	
				String key = keySetIterator.next();
				String value = strategyforPredicate.get(key);

				Element st = doc.createElement("Predicate");
				rootElement.appendChild(st);
				st.setAttribute("name", key);					
				st.setAttribute("strategy", value);		
			}		
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);

			StreamResult result = new StreamResult(new File(filename));
			transformer.transform(source, result);
		} catch (DOMException|ParserConfigurationException|TransformerException e) {
			e.printStackTrace();
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
		S1_Del_triplesize = 0;
		S2_Del_triplesize = 0;		
		Conflict_Finder.conflicts_Finder.S3_Del_triplesize = 0;
		
		S1_Add_triplesize = 0;
		S2_Add_triplesize = 0;		
		Conflict_Finder.conflicts_Finder.S3_Add_triplesize = 0;
		Conflict_Finder.conflicts_Finder.S3_Del_triplesize = 0;
		Conflict_Finder.conflicts_Finder.S4_Add_triplesize = 0;
		Conflict_Finder.conflicts_Finder.S4_Del_triplesize = 0;
		Conflict_Finder.source_Delta.number_Of_ConflictingTriples = 0;
		Conflict_Finder.source_Delta.number_Of_conflictingTriples_S3=0;
		Conflict_Finder.source_Delta.duplicate_Triples=0;
		configure.time_S1 = 0;
		configure.time_S2 = 0;		
		Conflict_Finder.conflicts_Finder.time_S3 = 0;
		Conflict_Finder.conflicts_Finder.time_S4 = 0;	
		Conflict_Finder.source_Delta.funobj_Triples = 0;
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
				statistics.predicateType.put(p, "OF"); 
		} else {

			OWLDataProperty dataprop = fac.getOWLDataProperty(i);
			Set<OWLDataRange> datarange = dataprop.getRanges(OWLOntology);

			if (!datarange.isEmpty()){
				if (dataprop.isFunctional(OWLOntology))
					statistics.predicateType.put(p, "DF"); 
			}
		}		
		reasoner.dispose();
	}
	

	private static void setinitialTarget(String s){
		initialTarget = s;
	}

	private static void setsourceAdditionsChangeset(String s){
		sourceAdditionsChangeset = s;
	}

	private static void setsourceDeletionsChangeset(String s){
		sourceDeletionsChangeset = s;
	}

	private static void settargetAdditionsChangeset(String s){
		targetAdditionsChangeset = s;
	}

	private static void settargetDeletionsChangeset(String s){
		targetDeletionsChangeset = s;
	}

	private static boolean isEmpty(String f) {
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
