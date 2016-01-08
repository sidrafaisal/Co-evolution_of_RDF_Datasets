package Conflict_Resolver;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

@SuppressWarnings("javadoc")
public class function_Auto_Selector {

	private static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	@Nonnull
	private static OWLReasonerFactory reasonerFactory;
	@Nonnull
	private static OWLOntology ontology;
	private final static PrintStream out = System.out;

	public static void select(List<String> predicates_toResolve, List<String> predicates_notToResolve) {
		try{
			Map<String, String> resolutionFunctionforPredicate  = new HashMap<String, String>();
		//	List<String> predicateList = co_evolution_Manager.configure.predicateList; 
			@Nonnull 
			File documentIRI = new File(co_evolution_Manager.configure.ontology);
			ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
			
			reasonerFactory = new Reasoner.ReasonerFactory();


			OWLClass clazz = manager.getOWLDataFactory().getOWLThing();
			for (String predicate : predicates_toResolve) {				
				if (predicate.contains("owl:sameAs") || predicate.contains("rdfs:label") || predicate.contains("rdf:type") ) {

					String prefferedfname = checkProperty(clazz, predicate);
					resolutionFunctionforPredicate.put(predicate, "0");
				} else {				
			String prefferedfname = checkProperty(clazz, predicate);
			resolutionFunctionforPredicate.put(predicate, prefferedfname); 
				}
			}
	//		manual_Selector.filename = "manual_FunctionSelector_"+ co_evolution_Manager.configure.initialTarget+".xml";
	//		manual_Selector.create (resolutionFunctionforPredicate);	

		} catch (OWLException e){
			e.printStackTrace();
		}
	}
	
	private static String checkProperty(@Nonnull OWLClass clazz, String p) {
		String value = null;
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		OWLDataFactory fac = manager.getOWLDataFactory();
		IRI i = IRI.create(p); 
		System.out.println("data type");


		OWLObjectProperty objprop = fac.getOWLObjectProperty(i);
		Set<OWLClassExpression> classexpr= objprop.getRanges(ontology);
		Iterator<OWLClassExpression> expr = classexpr.iterator();
		
		if (!classexpr.isEmpty()){
			if (objprop.isFunctional(ontology) || objprop.isInverseFunctional(ontology))
			{
		
				value = "0";
				
			//while (expr.hasNext())
				//out.println(expr.next()+"object property");
			}System.out.println(expr.next());
			} else {

			OWLDataProperty dataprop = fac.getOWLDataProperty(i);
			Set<OWLDataRange> datarange = dataprop.getRanges(ontology);
			Iterator<OWLDataRange> range = datarange.iterator();

			if (!datarange.isEmpty()){
				System.out.println(range.next());
				System.out.println(dataprop.getEntityType());
				if (dataprop.isFunctional(ontology)){
					value = "0";
			}
			}
			while (range.hasNext())
				out.println(range.next()+"data Property");
		}
		
		reasoner.dispose();
		return value;
	}
}
