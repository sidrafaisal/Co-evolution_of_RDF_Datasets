package Conflict_Finder;

import Conflict_Resolver.resolver;
import Conflict_Resolver.statistics;
import co_evolution_Manager.configure;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class source_Delta {

	public static String current_Predicate = "";
	public static String current_Predicate_Type = "";	
	public static long number_Of_ConflictingTriples = 0;
	public static long number_Of_ResolvedTriples = 0;

	public static Map<String, Integer> usedFunctions = new HashMap<String, Integer>();
	public static Map <String, Integer> predicateFunctionUseCounter = new HashMap <String, Integer> ();
	public static long [] number_Of_caseTriples = new long [8];
	static Model SrcAdd_model, SyncTarAdd_model, SyncSrcAdd_model, SyncSrcDel_model, TarAdd_model, SyncTarDel_model;

	public static void apply () throws RiotException, FileNotFoundException, AddDeniedException, OWLOntologyCreationException {

		SyncTarAdd_model = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);		
		SyncTarDel_model = FileManager.get().loadModel(configure.SyncTarDel, configure.fileSyntax);		
		SyncSrcAdd_model = FileManager.get().loadModel(configure.SyncSrcAdd, configure.fileSyntax);
		SyncSrcDel_model = FileManager.get().loadModel(configure.SyncSrcDel, configure.fileSyntax);
		if (configure.targetAdditionsChangeset!=null)
			TarAdd_model = FileManager.get().loadModel(configure.targetAdditionsChangeset, configure.fileSyntax);

			additions_changeset();
			deletions_changeset();	
			
			SyncTarAdd_model.write(new FileOutputStream(configure.SyncTarAdd), configure.fileSyntax);
			SyncTarAdd_model.close();	

			SyncTarDel_model.write(new FileOutputStream(configure.SyncTarDel), configure.fileSyntax);
			SyncTarDel_model.close();				

			SyncSrcAdd_model.write(new FileOutputStream(configure.SyncSrcAdd), configure.fileSyntax);
			SyncSrcAdd_model.close();	

			SyncSrcDel_model.write(new FileOutputStream(configure.SyncSrcDel), configure.fileSyntax);
			SyncSrcDel_model.close();				
			
			if (configure.targetAdditionsChangeset!=null) {
				TarAdd_model.write(new FileOutputStream(configure.targetAdditionsChangeset), configure.fileSyntax); //resolved values are of source
			TarAdd_model.close();
			}
	}

	/*Find conflicts for source additions changeset: Pick each triple s1,p1,o1 from source additions changeset and
	  check for s1,p1,o2 in target changesets and inital target*/

	public static void additions_changeset () throws FileNotFoundException, org.apache.jena.riot.RiotException, AddDeniedException, OWLOntologyCreationException{		
		if (configure.sourceAdditionsChangeset != null) {
				
			SrcAdd_model = FileManager.get().loadModel(configure.sourceAdditionsChangeset, configure.fileSyntax);
			StmtIterator iter = SrcAdd_model.listStatements();

			while (iter.hasNext()) {

				Statement stmt      = iter.nextStatement();  // get next statement
				Resource  subject   = stmt.getSubject();     // get the subject
				Property  predicate = stmt.getPredicate();   // get the predicate

				current_Predicate = predicate.toString();
				if(statistics.predicateType.get(current_Predicate)!=null)
					current_Predicate_Type = statistics.predicateType.get(current_Predicate);
				
				if (resolver.auto_selector) {
					if (stmt.getObject().isLiteral() &&
							!statistics.resolutionFunctionforPredicate.containsKey(current_Predicate)) {
					if (stmt.getObject().asLiteral().getDatatypeURI().contains("String")) 
						statistics.resolutionFunctionforPredicate.put(current_Predicate, "longest");	
					else 
						statistics.resolutionFunctionforPredicate.put(current_Predicate, "max");	
				} else if (stmt.getObject().isURIResource() &&
						!statistics.resolutionFunctionforPredicate.containsKey(current_Predicate))
					statistics.resolutionFunctionforPredicate.put(current_Predicate, "first");	
				}
				List<Triple> conflictingTriplesDeletionSource = null, 
						conflictingTriplesTarget = null, 
						conflictingTriplesAdditionTarget = null, 
						conflictingTriplesDeletionTarget = null;

				boolean flag_DS = false, flag_T = false, flag_AT = false, flag_DT = false;
				
				conflictingTriplesDeletionSource = findSimilarTriples(configure.sourceDeletionsChangeset, subject, predicate, Node.ANY, false) ;
				conflictingTriplesTarget = findSimilarTriples(configure.initialTarget, subject, predicate, Node.ANY, false) ;
				conflictingTriplesAdditionTarget = findSimilarTriples(configure.targetAdditionsChangeset, subject, predicate, Node.ANY, false) ;
				conflictingTriplesDeletionTarget = findSimilarTriples(configure.targetDeletionsChangeset, subject, predicate, Node.ANY, false) ;

				if(conflictingTriplesDeletionSource.size() > 0)
					flag_DS = true;
				if(conflictingTriplesTarget.size() > 0)
					flag_T = true;
				if(conflictingTriplesAdditionTarget.size() > 0)
					flag_AT = true;
				if(conflictingTriplesDeletionTarget.size() > 0)
					flag_DT = true;
				
				if ( (!flag_DT && !flag_AT) )	{	//added/modified by source
					if (!conflictingTriplesTarget.contains(stmt) && !conflictingTriplesDeletionSource.contains(stmt.asTriple())) { //may be modified multiple times
						SyncTarAdd_model.add(stmt);
						number_Of_caseTriples [0] += 1;
					}
				} else if (!flag_DS && flag_T && flag_DT && !flag_AT) {	//added by source and deleted by target
					if (conflictingTriplesDeletionTarget.contains(stmt.asTriple())) {
						SyncSrcDel_model.add(stmt);						
					} else {
						SyncTarAdd_model.add(stmt);			
						number_Of_caseTriples [0] += 1;
					}
				} else if (flag_DS && flag_T && flag_DT && !flag_AT) {	//modified by source and deleted by target
					if (conflictingTriplesDeletionTarget.contains(stmt.asTriple())) {
						SyncSrcDel_model.add(stmt);						
					} else {
						SyncTarAdd_model.add(stmt);			
						number_Of_caseTriples [2] += 1;
					}
				} else if (!flag_DS && !flag_T && !flag_DT && flag_AT)  {	// added by source and target
					resolve(stmt, conflictingTriplesAdditionTarget);	
					number_Of_caseTriples [4] += 1;
				} else if( (flag_DS && flag_T && flag_DT && flag_AT) || (flag_DS && !flag_T && flag_DT && flag_AT)) {	// modified by source and modified by target	
					resolve(stmt, conflictingTriplesAdditionTarget);
					number_Of_caseTriples [5] += 1;
				} else if (!flag_DS && flag_T && flag_DT && flag_AT) { 	//added by source and modified by target				
					if (conflictingTriplesDeletionTarget.contains(stmt.asTriple())) {//check same triple?
						/*for (int i=0;i<conflictingTriplesAdditionTarget.size();i++){
							SyncTarAdd_model.getGraph().add(conflictingTriplesAdditionTarget.get(i));
							SyncSrcAdd_model.getGraph().add(conflictingTriplesAdditionTarget.get(i));
						} */	// will be add in step5
						SyncSrcDel_model.add(stmt);
						number_Of_caseTriples [6] += 1;
					} else {		
						resolve(stmt, conflictingTriplesAdditionTarget);
						number_Of_caseTriples [6] += 1;
					}
				} else if (flag_DS && !flag_DT && flag_AT && !flag_T)  {	// modified by source and added by target
					resolve(stmt, conflictingTriplesAdditionTarget);	
					number_Of_caseTriples [7] += 1;
				} 			
			}	
			SrcAdd_model.close();		
		} 	
	}
	
	public static void resolve (Statement stmt, List<Triple> conflictingTriplesAdditionTarget) { 

		String functionforPredicate = statistics.resolutionFunctionforPredicate.get(current_Predicate);
		int conflictingTriplesAdditionsize = conflictingTriplesAdditionTarget.size();

		for (int i = 0; i < conflictingTriplesAdditionsize; i++) {
			Triple t = conflictingTriplesAdditionTarget.get(i);
			try {
			if(stmt.asTriple().equals(t)) {		//same values	
				if (!SyncTarAdd_model.contains(stmt))
					SyncTarAdd_model.add(stmt);		
				TarAdd_model.remove(stmt);		// avoid duplicate insertion
				Model TarDel_model = FileManager.get().loadModel(configure.targetDeletionsChangeset, configure.fileSyntax); 		// optional
				TarDel_model.remove(stmt);
				TarDel_model.close();		
			} else if (current_Predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
				resolveLabels(stmt, conflictingTriplesAdditionTarget, functionforPredicate);
				break;								
			} else if (current_Predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") ) {
					resolveType (stmt, conflictingTriplesAdditionTarget,functionforPredicate);
				break;
			} else if (current_Predicate.equals("http://www.w3.org/2002/07/owl#sameAs")) {
				resolveSameAs (stmt, conflictingTriplesAdditionTarget);
				break;
			} else if (configure.predicateList.contains("http://www.w3.org/2002/07/owl#sameAs")	
					&& stmt.getObject().isURIResource()) { // if we have sameAs triples, check if both objects are in sameas binding
					resolveURI(stmt, conflictingTriplesAdditionTarget);
				break;
			} // prev case of sameAs works only for object URIs, whereas we can also have literals
			else if (Conflict_Finder.conflicts_Finder.resolve) {
				resolveGenerally (stmt, conflictingTriplesAdditionTarget, functionforPredicate);	
				break;
			}
			} catch (RiotException | FileNotFoundException | AddDeniedException | OWLOntologyCreationException e) {
				e.printStackTrace();
			}
		}
	}

	public static void resolveType (Statement stmt, List<Triple> conflictingTriplesAdditionTarget, String functionforPredicate) throws AddDeniedException, OWLOntologyCreationException { 
		int conflictingTriplesAdditionsize = conflictingTriplesAdditionTarget.size();

		for (int i = 0; i < conflictingTriplesAdditionsize; i++) {
			Triple t = conflictingTriplesAdditionTarget.get(i);
			if (!isDisjoint(t.getObject().getURI(), stmt.asTriple().getObject().getURI())) {
				if (!SyncTarAdd_model.contains(stmt))
					SyncTarAdd_model.add(stmt);

				SyncTarAdd_model.getGraph().add(t);
				Statement src_s = ResourceFactory.createStatement( 
						ResourceFactory.createResource(t.getSubject().toString()),
						ResourceFactory.createProperty(t.getPredicate().toString()),
						ResourceFactory.createTypedLiteral(t.getObject())
						);
				if (!SyncSrcAdd_model.contains(src_s))
					SyncSrcAdd_model.getGraph().add(t);

			} else {
				String rv = Conflict_Resolver.resolver.apply(functionforPredicate, getURIstoResolve (stmt.getObject(), t), "String"); 
				if (t.getObject().toString().equals(rv)) {
					SyncTarAdd_model.getGraph().add(t);
					Statement src_s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createTypedLiteral(t.getObject())
							);
					if (!SyncSrcAdd_model.contains(src_s))
						SyncSrcAdd_model.getGraph().add(t);
					if (!SyncSrcDel_model.contains(stmt))
						SyncSrcDel_model.add(stmt);
				} else if (stmt.getObject().toString().equals(rv)) {
					if (!SyncTarAdd_model.contains(stmt))
						SyncTarAdd_model.add(stmt);
					Statement s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createPlainLiteral(t.getObject().toString())
							);
					TarAdd_model.remove(s);		

					if (SyncSrcDel_model.contains(stmt))		
						SyncSrcDel_model.remove(stmt);
					
				} else {
					Triple triple = Triple.create(stmt.getSubject().asNode(), stmt.getPredicate().asNode(), SyncTarAdd_model.createLiteral(rv).asNode());	
					SyncTarAdd_model.getGraph().add(triple);
					SyncSrcAdd_model.getGraph().add(triple);
					SyncSrcDel_model.add(stmt);
					Statement s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createPlainLiteral(t.getObject().toString())
							);
					TarAdd_model.remove(s);					
				}

				int counter = predicateFunctionUseCounter.get(current_Predicate) + 1;
				predicateFunctionUseCounter.put(current_Predicate, counter);
				number_Of_ConflictingTriples ++;
			}
		}
	}

	public static void resolveSameAs (Statement stmt, List<Triple> conflictingTriplesAdditionTarget) {
		int conflictingTriplesAdditionsize = conflictingTriplesAdditionTarget.size();
		for (int i = 0; i < conflictingTriplesAdditionsize; i++) {
			Triple t = conflictingTriplesAdditionTarget.get(i);
			if (!SyncTarAdd_model.contains(stmt))
				SyncTarAdd_model.add(stmt);
			SyncTarAdd_model.getGraph().add(t);
			Statement src_s = ResourceFactory.createStatement( 
					ResourceFactory.createResource(t.getSubject().toString()),
					ResourceFactory.createProperty(t.getPredicate().toString()),
					ResourceFactory.createTypedLiteral(t.getObject())
					);
			if (!SyncSrcAdd_model.contains(src_s))
				SyncSrcAdd_model.getGraph().add(t);
		}
	}
	public static void resolveURI(Statement stmt, List<Triple> conflictingTriplesAdditionTarget) throws RiotException, FileNotFoundException{ 

		String functionforPredicate = statistics.resolutionFunctionforPredicate.get(current_Predicate);
		int conflictingTriplesAdditionsize = conflictingTriplesAdditionTarget.size();
		for (int i = 0; i < conflictingTriplesAdditionsize; i++) {

			Triple t = conflictingTriplesAdditionTarget.get(i);
			Property p = ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#sameAs");

			List<Triple> sameAsInSourceAdd = findSimilarTriples(configure.sourceAdditionsChangeset, stmt.getSubject().asResource(), p, t.getObject(), false) ;
			List<Triple> sameAsInTargetAdd = findSimilarTriples(configure.targetAdditionsChangeset, stmt.getSubject().asResource(), p, t.getObject(), false) ;
			List<Triple> sameAsInTargetDel = findSimilarTriples(configure.targetDeletionsChangeset, stmt.getSubject().asResource(), p, t.getObject(), false) ;	
			List<Triple> sameAsInSourceDel = findSimilarTriples(configure.sourceDeletionsChangeset, stmt.getSubject().asResource(), p, t.getObject(), false) ;	
			List<Triple> sameAsInTarget = findSimilarTriples(configure.initialTarget, stmt.getSubject().asResource(), p, t.getObject(), false) ;	

			if (!sameAsInSourceAdd.isEmpty() || !sameAsInTargetAdd.isEmpty() || !sameAsInTargetDel.isEmpty() || !sameAsInSourceDel.isEmpty() || !sameAsInTarget.isEmpty()) {									
				if (!SyncTarAdd_model.contains(stmt))
					SyncTarAdd_model.add(stmt);

				SyncTarAdd_model.getGraph().add(t);									
				Statement src_s = ResourceFactory.createStatement( 
						ResourceFactory.createResource(t.getSubject().toString()),
						ResourceFactory.createProperty(t.getPredicate().toString()),
						ResourceFactory.createTypedLiteral(t.getObject())
						);
				if (!SyncSrcAdd_model.contains(src_s))
					SyncSrcAdd_model.getGraph().add(t);

			} else	{
				Resource r = ResourceFactory.createResource(t.getObject().toString());
				sameAsInSourceAdd = findSimilarTriples(configure.sourceAdditionsChangeset, r, p, stmt.getObject().asNode(), false) ;		
				sameAsInTargetAdd = findSimilarTriples(configure.targetAdditionsChangeset, r, p, stmt.getObject().asNode(), false) ;
				sameAsInTarget = findSimilarTriples(configure.initialTarget, r, p, stmt.getObject().asNode(), false) ;
				sameAsInTargetDel = findSimilarTriples(configure.targetDeletionsChangeset, r, p, stmt.getObject().asNode(), false) ; 
				sameAsInSourceDel = findSimilarTriples(configure.sourceDeletionsChangeset, r, p, stmt.getObject().asNode(), false) ;	

				if (!sameAsInSourceAdd.isEmpty() || !sameAsInTargetAdd.isEmpty() || !sameAsInTargetDel.isEmpty() || !sameAsInSourceDel.isEmpty() || !sameAsInTarget.isEmpty()) {

					if (!SyncTarAdd_model.contains(stmt))
						SyncTarAdd_model.add(stmt);

					SyncTarAdd_model.getGraph().add(t);	
					Statement src_s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createTypedLiteral(t.getObject())
							);
					if (!SyncSrcAdd_model.contains(src_s))
						SyncSrcAdd_model.getGraph().add(t);

				} else if (Conflict_Finder.conflicts_Finder.resolve) {
					String [] args = {"",""};
					args [0] = stmt.getObject().asResource().getURI().toString();
					args [1] = t.getObject().getURI().toString() ;
					String rv = Conflict_Resolver.resolver.apply(functionforPredicate, args, "String"); 

					if (args[0].equals(rv))
					{
						Statement s = ResourceFactory.createStatement( 
								ResourceFactory.createResource(t.getSubject().toString()),
								ResourceFactory.createProperty(t.getPredicate().toString()),
								ResourceFactory.createPlainLiteral(t.getObject().toString())
								);
						TarAdd_model.remove(s);

						if (!SyncTarAdd_model.contains(stmt))
							SyncTarAdd_model.add(stmt);

						if (SyncSrcDel_model.contains(stmt))
							SyncSrcDel_model.remove(stmt);
					} else if (args[1].equals(rv)) {

						SyncTarAdd_model.getGraph().add(t);

						Statement src_s = ResourceFactory.createStatement( 
								ResourceFactory.createResource(t.getSubject().toString()),
								ResourceFactory.createProperty(t.getPredicate().toString()),
								ResourceFactory.createTypedLiteral(t.getObject())
								);
						if (!SyncSrcAdd_model.contains(src_s))
							SyncSrcAdd_model.getGraph().add(t);
						SyncSrcDel_model.add(stmt);
					} else {

						Triple triple = Triple.create(stmt.getSubject().asNode(), stmt.getPredicate().asNode(), NodeFactory.createURI(rv));
						SyncTarAdd_model.getGraph().add(triple);
						SyncSrcAdd_model.getGraph().add(triple);
						SyncSrcDel_model.add(stmt);
						Statement s = ResourceFactory.createStatement( 
								ResourceFactory.createResource(t.getSubject().toString()),
								ResourceFactory.createProperty(t.getPredicate().toString()),
								ResourceFactory.createPlainLiteral(t.getObject().toString())
								);
						TarAdd_model.remove(s);
					}
					int counter = predicateFunctionUseCounter.get(current_Predicate) + 1;
					predicateFunctionUseCounter.put(current_Predicate, counter);

				}
			}
		}
	}

	public static boolean resolveGenerally (Statement stmt, List<Triple> conflictingTriplesAdditionTarget, String functionforPredicate) {							

		RDFNode   object    = stmt.getObject(); 
		boolean is_conflict = false;

		for (int j = 0; j < conflictingTriplesAdditionTarget.size(); j++) {
			//if (current_Predicate_Type.equals("F")) 
			Triple t= conflictingTriplesAdditionTarget.get(j);

			if (object.isURIResource()) {
				String rv = Conflict_Resolver.resolver.apply(functionforPredicate, getURIstoResolve (object, t), "String"); 

				if (t.getObject().toString().equals(rv))
				{
					Statement src_s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createTypedLiteral(t.getObject())
							);
					if (!SyncSrcAdd_model.contains(src_s))
						SyncSrcAdd_model.getGraph().add(t);
					SyncTarAdd_model.getGraph().add(t);
					
					if (!SyncSrcDel_model.contains(stmt))
						SyncSrcDel_model.add(stmt);
				} else if (stmt.getObject().toString().equals(rv)) {
					Statement s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createPlainLiteral(t.getObject().toString())
							);
					TarAdd_model.remove(s);
					if (!SyncTarAdd_model.contains(stmt))
						SyncTarAdd_model.add(stmt);

					if (SyncSrcDel_model.contains(stmt))		//multiple add/modify in a file
						SyncSrcDel_model.remove(stmt);

				} else {

					Triple triple = Triple.create(stmt.getSubject().asNode(), stmt.getPredicate().asNode(), NodeFactory.createURI(rv));	
					SyncTarAdd_model.getGraph().add(triple);
					SyncSrcAdd_model.getGraph().add(triple);
					SyncSrcDel_model.add(stmt);
					Statement s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createPlainLiteral(t.getObject().toString())
							);
					TarAdd_model.remove(s);
				}
				is_conflict = true;
			} else if (object.isLiteral()) {
				String type = getType(object.asLiteral().getDatatypeURI());
				String rv = Conflict_Resolver.resolver.apply(functionforPredicate, getLiteralstoResolve (object, t), type); 
				is_conflict = true;

				if (t.getObject().toString().equals(rv))
				{
					SyncTarAdd_model.getGraph().add(t);
					Statement src_s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createTypedLiteral(t.getObject())
							);
					if (!SyncSrcAdd_model.contains(src_s))
						SyncSrcAdd_model.getGraph().add(t);
					if (!SyncSrcDel_model.contains(stmt))
						SyncSrcDel_model.add(stmt);
				} else if (stmt.getObject().toString().equals(rv)) {
					Statement s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createPlainLiteral(t.getObject().toString())
							);
					TarAdd_model.remove(s);
					if (!SyncTarAdd_model.contains(stmt))
						SyncTarAdd_model.add(stmt);

					if (SyncSrcDel_model.contains(stmt))
						SyncSrcDel_model.remove(stmt);

				} else {
					Statement s = ResourceFactory.createStatement( 
							ResourceFactory.createResource(t.getSubject().toString()),
							ResourceFactory.createProperty(t.getPredicate().toString()),
							ResourceFactory.createPlainLiteral(t.getObject().toString())
							);
					TarAdd_model.remove(s);
					if (stmt.getLiteral().getLanguage()!=null)
						type = stmt.getLiteral().getLanguage();

					Triple triple = Triple.create(stmt.getSubject().asNode(), stmt.getPredicate().asNode(), SyncTarAdd_model.createTypedLiteral(rv, type).asNode());	
					SyncTarAdd_model.getGraph().add(triple);
					SyncSrcAdd_model.getGraph().add(triple);
					SyncSrcDel_model.add(stmt);
				}
				int counter = predicateFunctionUseCounter.get(current_Predicate) + 1;
				predicateFunctionUseCounter.put(current_Predicate, counter);
			}	

			//		number_Of_ConflictingTriples++;	
		}
		return is_conflict;
	}

	public static boolean resolveLabels(Statement stmt, List<Triple> conflictingTriplesAdditionTarget, String functionforPredicate) {
	//	int maxdiff = 0;
		Triple t;
		boolean is_conflict = false;
		Resource  subject   = stmt.getSubject();     // get the subject
		Property  predicate = stmt.getPredicate();   // get the predicate
		RDFNode   object    = stmt.getObject(); 

	//	int iteration = 0;
		for (int j = 0; j < conflictingTriplesAdditionTarget.size(); j++) {
			t = conflictingTriplesAdditionTarget.get(j);
			String s_value = object.asLiteral().getLexicalForm().toString();
			String t_value = t.getObject().getLiteralLexicalForm();
			int threshold = Math.max(s_value.length(), t_value.length())/2;
			int diff = StringUtils.getLevenshteinDistance(s_value, t_value); //greater the diff,lesser the similarity

			/*	if (diff >= threshold && diff > maxdiff) {		
				maxdiff = diff;
				iteration = j;	
			}*/
			if (diff >= threshold ){
				t = conflictingTriplesAdditionTarget.get(j);
				
				if (!SyncTarAdd_model.contains(stmt)) 
					SyncTarAdd_model.add(stmt);	
				SyncTarAdd_model.getGraph().add(t);					
				Statement src_s = ResourceFactory.createStatement( 
						ResourceFactory.createResource(t.getSubject().toString()),
						ResourceFactory.createProperty(t.getPredicate().toString()),
						ResourceFactory.createTypedLiteral(t.getObject())
						);
				if (!SyncSrcAdd_model.contains(src_s))
					SyncSrcAdd_model.getGraph().add(t);
			}
			/*	}									
		if (maxdiff > 0) {							// pick both that are least similar
			t = conflictingTriplesAdditionTarget.get(iteration);
			//Triple triple = Triple.create(subject.asNode(), predicate.asNode(), t.getObject());
			SyncSrcAdd_model.getGraph().add(t);
			//if (!contains (SyncTarAdd_model, triple)) 
			SyncTarAdd_model.getGraph().add(t);	
			SyncTarAdd_model.add(stmt);

		} */else if (Conflict_Finder.conflicts_Finder.resolve) {

			String type = getType(object.asLiteral().getDatatypeURI());
			String rv = Conflict_Resolver.resolver.apply(functionforPredicate, getLiteralstoResolve (object, t), type); //conflictingTriplesAdditionTarget

			is_conflict = true;	
			if (t.getObject().toString().equals(rv))
			{
				SyncTarAdd_model.getGraph().add(t);
				Statement src_s = ResourceFactory.createStatement( 
						ResourceFactory.createResource(t.getSubject().toString()),
						ResourceFactory.createProperty(t.getPredicate().toString()),
						ResourceFactory.createTypedLiteral(t.getObject())
						);
				if (!SyncSrcAdd_model.contains(src_s))
					SyncSrcAdd_model.getGraph().add(t);
				if (!SyncSrcDel_model.contains(stmt))
					SyncSrcDel_model.add(stmt);
			} else if (stmt.getObject().toString().equals(rv)) {
				if (!SyncTarAdd_model.contains(stmt))
					SyncTarAdd_model.add(stmt);
				Statement s = ResourceFactory.createStatement( 
						ResourceFactory.createResource(t.getSubject().toString()),
						ResourceFactory.createProperty(t.getPredicate().toString()),
						ResourceFactory.createPlainLiteral(t.getObject().toString())
						);
				TarAdd_model.remove(s);
			} else {
				if (stmt.getLiteral().getLanguage()!=null)
					type = stmt.getLiteral().getLanguage();


				Triple triple = Triple.create(subject.asNode(), predicate.asNode(), SyncTarAdd_model.createTypedLiteral(rv, type).asNode());	
				SyncTarAdd_model.getGraph().add(triple);
				SyncSrcAdd_model.getGraph().add(triple);
				SyncSrcDel_model.add(stmt);
				Statement s = ResourceFactory.createStatement( 
						ResourceFactory.createResource(t.getSubject().toString()),
						ResourceFactory.createProperty(t.getPredicate().toString()),
						ResourceFactory.createPlainLiteral(t.getObject().toString())
						);
				TarAdd_model.remove(s);
			}
		}
			int counter = predicateFunctionUseCounter.get(current_Predicate) + 1;
			predicateFunctionUseCounter.put(current_Predicate, counter);
			//	number_Of_ConflictingTriples++;
		}	

		return is_conflict;
	}

	public static String [] getLiteralstoResolve (RDFNode object, Triple t) {
		String [] args = new String [2];
		args [0] = object.asLiteral().getLexicalForm().toString();			
		args [1] = t.getObject().getLiteralLexicalForm().toString();		
		return args;
	}
	
	public static String [] getURIstoResolve (RDFNode object, Triple t) {
		String [] args = new String [2];	
		args [0] = object.asResource().getURI().toString();
		args [1] = t.getObject().getURI().toString();
		return args;
	}

	public static boolean isDisjoint(String tar, String src) throws OWLOntologyCreationException {
		boolean isDisjoint = false;
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		@Nonnull 
		File f = new File(configure.ontology);
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(f);

		OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
		OWLDataFactory fac = manager.getOWLDataFactory();		

		OWLClass sclass = fac.getOWLClass(IRI.create(src));
		OWLClass tclass = fac.getOWLClass(IRI.create(tar));

		NodeSet<OWLClass> c = reasoner.getDisjointClasses(sclass);
		if (c.containsEntity(tclass))
			isDisjoint = true;

		reasoner.dispose();	
		return isDisjoint;
	}

	/*Find conflicts for source deletions changeset: Pick each triple s1,p1,o1 from source deletion changeset 
and check for s1,p1,o2 in target changesets and initial target*/	

	public static void deletions_changeset() throws org.apache.jena.riot.RiotException, FileNotFoundException{
		if (configure.sourceDeletionsChangeset != null) {		
			Model SyncTarDel_model = FileManager.get().loadModel(configure.SyncTarDel, configure.fileSyntax);		
			Model SrcDel_model = FileManager.get().loadModel(configure.sourceDeletionsChangeset, configure.fileSyntax);
//			Model SyncSrcAdd_model = FileManager.get().loadModel(configure.SyncSrcAdd, configure.fileSyntax);
//			Model SyncTarAdd_model = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);
			StmtIterator iter = SrcDel_model.listStatements();

			while (iter.hasNext()) {
				Statement stmt      = iter.nextStatement();  // get next statement
				SyncTarDel_model.add(stmt);
				
/*				Resource  subject   = stmt.getSubject();     // get the subject
				Property  predicate = stmt.getPredicate();   // get the predicate
				List<Triple> conflictingTriplesTarget = findSimilarTriples(configure.initialTarget, subject, predicate, Node.ANY, false) ;
				List<Triple> conflictingTriplesAdditionTarget = findSimilarTriples(configure.targetAdditionsChangeset, subject, predicate, Node.ANY, true) ;
				List<Triple> conflictingTriplesDeletionTarget = findSimilarTriples(configure.targetDeletionsChangeset, subject, predicate, Node.ANY, false) ;

				boolean flag_T = false, flag_AT = false, flag_DT = false; 
				if(conflictingTriplesTarget.size() > 0)
					flag_T = true;
				if(conflictingTriplesAdditionTarget.size() > 0)
					flag_AT = true;
				if(conflictingTriplesDeletionTarget.size() > 0)
					flag_DT = true;
				
				if (!flag_AT && flag_T && !flag_DT)  	//deleted by source
					; 									
				else if (!flag_AT && flag_T && flag_DT)  //deleted by source and target			
					;
				else if (flag_AT && !flag_T && !flag_DT)  //deleted by source and added by target			
				{ 
					for (Triple t : conflictingTriplesAdditionTarget) {
					SyncTarAdd_model.getGraph().add(t);
					SyncSrcAdd_model.getGraph().add(t);
				}
				}
				else if (flag_AT && flag_T && flag_DT) { //deleted by source and modified by target
					for (Triple t : conflictingTriplesAdditionTarget) {
					SyncTarAdd_model.getGraph().add(t);	
					SyncSrcAdd_model.getGraph().add(t);
				}
				}
	*/		} 			
	//		SyncTarAdd_model.write(new FileOutputStream(configure.SyncTarAdd), configure.fileSyntax);
	//		SyncTarAdd_model.close();
			SrcDel_model.close();	
			SyncTarDel_model.write(new FileOutputStream(configure.SyncTarDel), configure.fileSyntax);
			SyncTarDel_model.close();
//			SyncSrcAdd_model.write(new FileOutputStream(configure.SyncSrcAdd), configure.fileSyntax);
//			SyncSrcAdd_model.close();					
		}
	}	

	/*			Find conflicting triples in target*/

	public static List<Triple> findSimilarTriples(String filename, Resource subject, Property predicate, Node object, Boolean remove) throws org.apache.jena.riot.RiotException, FileNotFoundException {

		List<Triple> conflictingTriples = new ArrayList<Triple>();

		if (filename!=null){
			Model model = FileManager.get().loadModel(filename, configure.fileSyntax);

			ExtendedIterator<Triple> results = model.getGraph().find(subject.asNode(), predicate.asNode(), object); 
			while (results.hasNext()) {
				Triple t = results.next();
				conflictingTriples.add(t); 
			}
			if (remove) {
				for (Triple deleteConflict : conflictingTriples) 
					model.getGraph().delete(deleteConflict);

				model.write(new FileOutputStream(filename), configure.fileSyntax);
			}
			model.close();
		}
		return conflictingTriples;
	}

	public static String getType(String type) {

		int index = type.indexOf("#");
		if (index == -1)
			index = type.indexOf(":");
		index = index + 1;

		int size = type.length();
		return type.substring(index, size);
	}

	public static void setPredicateFunctionUseCounter () {
		Iterator<String> predicateList = configure.predicateList.iterator();
		while (predicateList.hasNext()) {
			Conflict_Finder.source_Delta.predicateFunctionUseCounter.put(predicateList.next(), 0);
		}
	}	

	public static void getPredicateFunctionUseCounter () {
		Map<String, Integer> usedFunction = new HashMap<String, Integer>();
		Iterator<String> predicateList = configure.predicateList.iterator();
		while (predicateList.hasNext()) {
			String predicate = predicateList.next();
			int counter = Conflict_Finder.source_Delta.predicateFunctionUseCounter.get(predicate);
			String functionforPredicate = statistics.resolutionFunctionforPredicate.get(predicate);
			if(usedFunction.containsKey(functionforPredicate)) {
				counter = usedFunction.get(functionforPredicate) + counter;
				usedFunction.remove(functionforPredicate); 
			}
			usedFunction.put(functionforPredicate, counter);
		}
		System.out.println("Function, #of triples resolved using this function");
		Set <String> funList= usedFunction.keySet();		
		for (String fun : funList) {
			System.out.println(fun+ ", " + usedFunction.get(fun));
		}
		usedFunctions = usedFunction;
	}	

}
