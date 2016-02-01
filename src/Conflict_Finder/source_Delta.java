package Conflict_Finder;

import Conflict_Resolver.resolver;
import Conflict_Resolver.statistics;
import strategy.strategy;
import co_evolution_Manager.configure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
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
	public static long funobj_Triples=0;
	public static String current_Predicate = "";
	public static String current_Predicate_Type = "";	
	public static long number_Of_ConflictingTriples=0;
	public static long number_Of_conflictingTriples_S3=0, duplicate_Triples=0;

	static String functionforPredicate = "";

	public static void apply (Property property) throws RiotException, FileNotFoundException, AddDeniedException, OWLOntologyCreationException {

		if (configure.sourceAdditionsChangeset != null) 
			additions_changeset(property);
		if (configure.sourceDeletionsChangeset != null)
			deletions_changeset(property);	

		if (configure.targetAdditionsChangeset!=null) 
			strategy.TarAdd_model.write(new FileOutputStream(configure.targetAdditionsChangeset), configure.fileSyntax); //resolved values are of source
	}

	/*Find conflicts for source additions changeset: Pick each triple s1,p1,o1 from source additions changeset and
	  check for s1,p1,o2 in target changesets and inital target*/

	public static void additions_changeset (Property property) throws FileNotFoundException, org.apache.jena.riot.RiotException, AddDeniedException, OWLOntologyCreationException {		

		Map<Resource, List<Property>> SubPred = new HashMap<Resource, List<Property>>();
		StmtIterator iter = strategy.SrcAdd_model.listStatements((Resource)null, property, (RDFNode)null);
		current_Predicate = property.toString(); 

		if(statistics.predicateType.get(current_Predicate)!=null)
			current_Predicate_Type = statistics.predicateType.get(current_Predicate);

		while (iter.hasNext()) {

			Statement stmt      = iter.nextStatement();  
			Resource  subject   = stmt.getSubject();     

			if (SubPred==null || SubPred.isEmpty() || (SubPred!=null && !(SubPred.get(subject).contains(property))) ) {

				if (resolver.auto_selector) {
					if (stmt.getObject().isLiteral() &&
							!statistics.resolutionFunctionforPredicate.containsKey(current_Predicate)) {
						if (stmt.getObject().asLiteral().getDatatypeURI().contains("String")) 
							statistics.resolutionFunctionforPredicate.put(current_Predicate, "longest");	
						else //todo
							statistics.resolutionFunctionforPredicate.put(current_Predicate, "max");	
					} else if (stmt.getObject().isURIResource() &&
							!statistics.resolutionFunctionforPredicate.containsKey(current_Predicate))
						statistics.resolutionFunctionforPredicate.put(current_Predicate, "first");	
				}
				functionforPredicate = statistics.resolutionFunctionforPredicate.get(current_Predicate);

				List<Statement> conflictingTriplesAdditionSource = new ArrayList<Statement>(), conflictingTriplesDeletionSource = null, 
						conflictingTriplesTarget = null, 
						conflictingTriplesAdditionTarget = null, 
						conflictingTriplesDeletionTarget = null;

				boolean flag_DS = false, flag_T = false, flag_AT = false, flag_DT = false;
				conflictingTriplesAdditionSource = findSimilarTriples(strategy.SrcAdd_model, configure.sourceAdditionsChangeset, subject, property, (RDFNode) null, false) ;
				conflictingTriplesDeletionSource = findSimilarTriples(strategy.SrcDel_model, configure.sourceDeletionsChangeset, subject, property, (RDFNode) null, false) ;
				conflictingTriplesTarget = findSimilarTriples(strategy.Init_model, configure.initialTarget, subject, property, (RDFNode) null, false) ;
				conflictingTriplesAdditionTarget = findSimilarTriples(strategy.TarAdd_model, configure.targetAdditionsChangeset, subject, property, (RDFNode) null, false) ;
				conflictingTriplesDeletionTarget = findSimilarTriples(strategy.TarDel_model, configure.targetDeletionsChangeset, subject, property, (RDFNode) null, false) ;

				List <Property> newList = new ArrayList <Property>(); 
				if (conflictingTriplesAdditionSource.size() > 1) {
					if (SubPred.get(stmt.getSubject())!=null) 					
						newList = SubPred.get(stmt.getSubject());						
					newList.add(stmt.getPredicate());
					SubPred.put(stmt.getSubject(), newList);
				}
				boolean isMultiple = false;
				int repeatedItem = conflictingTriplesAdditionSource.size();
				int currentItem = 0;

				if(conflictingTriplesDeletionSource.size() > 0)
					flag_DS = true;
				if(conflictingTriplesTarget.size() > 0)
					flag_T = true;
				if(conflictingTriplesAdditionTarget.size() > 0)
					flag_AT = true;
				if(conflictingTriplesDeletionTarget.size() > 0)
					flag_DT = true;

				do {
					if ( !flag_DT && !flag_AT) 	{	//added/modified by source
						if (!flag_T) {
							if (!conflictingTriplesDeletionSource.contains(stmt.asTriple()))  //may be modified multiple times
								strategy.SyncTarAdd_model.add(stmt);
						} else if (flag_T && !conflictingTriplesTarget.contains(stmt.asTriple())) { ///need to check
							strategy.SyncTarAdd_model.add(stmt);
						//	conflictingTriplesTarget = findSimilarTriples(strategy.Init_model, configure.initialTarget, subject, property, (RDFNode) null, true) ;
						//	resolve(stmt, conflictingTriplesTarget, isMultiple);	
						} else if (conflictingTriplesTarget.contains(stmt.asTriple()) ) 
							duplicate_Triples++;

					} else if (!flag_DS && flag_T && flag_DT && !flag_AT) {	//added by source and deleted by target
						if (conflictingTriplesDeletionTarget.contains(stmt.asTriple())) 
							strategy.SyncSrcDel_model.add(stmt);						
						else 
							strategy.SyncTarAdd_model.add(stmt);
						
					} else if (flag_DS && flag_T && flag_DT && !flag_AT) {	//modified by source and deleted by target
						if (conflictingTriplesDeletionTarget.contains(stmt.asTriple())) 
							strategy.SyncSrcDel_model.add(stmt);						
						else 
							strategy.SyncTarAdd_model.add(stmt);		
						
					} else if (!flag_DS && !flag_DT && flag_AT) 	// added by source and target && !flag_T
						resolve(stmt, conflictingTriplesAdditionTarget, isMultiple);	
					
					else if( (flag_DS && flag_T && flag_DT && flag_AT) || (flag_DS && !flag_T && flag_DT && flag_AT)) 	// modified by source and modified by target	
						resolve(stmt, conflictingTriplesAdditionTarget,isMultiple);
					
					else if (!flag_DS && flag_T && flag_DT && flag_AT) { 	//added by source and modified by target				
						if (conflictingTriplesDeletionTarget.contains(stmt.asTriple())) 
							strategy.SyncSrcDel_model.add(stmt); // target triple will be add in last step
						else 		
							resolve(stmt, conflictingTriplesAdditionTarget, isMultiple);	
					
					} else if (flag_DS && !flag_DT && flag_AT && !flag_T)  	// modified by source and added by target
						resolve(stmt, conflictingTriplesAdditionTarget, isMultiple);			

					if (!conflictingTriplesAdditionSource.isEmpty() && currentItem < repeatedItem) 
						stmt = conflictingTriplesAdditionSource.get(currentItem); 			

					currentItem++;
					isMultiple = true;
				} while (currentItem < repeatedItem);	
			}	
		}
	}		

	public static void resolve (Statement stmt, List<Statement> conflictingTriples, boolean flag) throws FileNotFoundException, AddDeniedException, RiotException, OWLOntologyCreationException { 

		if (current_Predicate_Type.equals("OF")){
			funobj_Triples++;
			for (int i = 0; i < conflictingTriples.size(); i++) {

				Statement t = conflictingTriples.get(i);
				if(stmt.equals(t)) {
					duplicate_Triples++;
					if (!strategy.SyncTarAdd_model.contains(stmt))
						strategy.SyncTarAdd_model.add(stmt);		

					if (configure.targetAdditionsChangeset!=null)
						strategy.TarAdd_model.remove(stmt);		// avoid duplicate insertion
				} else {
					if (!strategy.SyncTarAdd_model.contains(stmt))
						strategy.SyncTarAdd_model.add(stmt);	

					if (!strategy.SyncTarAdd_model.contains(t)) 
						strategy.SyncTarAdd_model.add(t);
					if (!strategy.SyncSrcAdd_model.contains(t))
						strategy.SyncSrcAdd_model.add(t);
					funobj_Triples++;
				}
			}
		} else {
			if (current_Predicate.equals("http://www.w3.org/2000/01/rdf-schema#label")) 
				resolveLabels(stmt, conflictingTriples, functionforPredicate, flag);
			else if (current_Predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") ) 
				resolveType (stmt, conflictingTriples,functionforPredicate, flag);
			else if (current_Predicate.equals("http://www.w3.org/2002/07/owl#sameAs")) 
				resolveSameAs (stmt, conflictingTriples, flag);
			else if (configure.predicateList.contains("http://www.w3.org/2002/07/owl#sameAs")	
					&& stmt.getObject().isURIResource())  // if we have sameAs triples, check if both objects are in sameas binding
				resolveURI(stmt, conflictingTriples, flag);
			else 			// prev case of sameAs works only for object URIs, whereas we can also have literals
				resolveGenerally (stmt, conflictingTriples, flag);	

		}
	}

	public static void resolveType (Statement stmt, List<Statement> conflictingTriples, String functionforPredicate, boolean flag) throws AddDeniedException, OWLOntologyCreationException { 
		int conflictingTriplesSize = conflictingTriples.size();
		boolean is_conflict = false;
		for (int i = 0; i < conflictingTriplesSize; i++) {
			Statement t = conflictingTriples.get(i);
			if(stmt.asTriple().equals(t)) {		//same values
				duplicate_Triples++;
				if (!strategy.SyncTarAdd_model.contains(stmt))
					strategy.SyncTarAdd_model.add(stmt);		
				if (configure.targetAdditionsChangeset!=null)
					strategy.TarAdd_model.remove(stmt);		// avoid duplicate insertion
			} else if (!isDisjoint(t.getObject().asResource().getURI(), stmt.getObject().asResource().getURI())) {
				if (!strategy.SyncTarAdd_model.contains(stmt))
					strategy.SyncTarAdd_model.add(stmt);

				strategy.SyncTarAdd_model.add(t);

				if (!strategy.SyncSrcAdd_model.contains(t))
					strategy.SyncSrcAdd_model.add(t);

			} else if (Conflict_Finder.conflicts_Finder.resolve) {
				String rv = Conflict_Resolver.resolver.apply(functionforPredicate, getURIstoResolve (stmt.getObject(), t), "String"); 
				if (!flag)
					number_Of_ConflictingTriples ++;
				is_conflict=true;
				if (t.getObject().toString().equals(rv)) {
					strategy.SyncTarAdd_model.add(t);

					if (!strategy.SyncSrcAdd_model.contains(t))
						strategy.SyncSrcAdd_model.add(t);
					if (!strategy.SyncSrcDel_model.contains(stmt))
						strategy.SyncSrcDel_model.add(stmt);
				} else if (stmt.getObject().toString().equals(rv)) {
					if (!strategy.SyncTarAdd_model.contains(stmt))
						strategy.SyncTarAdd_model.add(stmt);

					if (configure.targetAdditionsChangeset!=null)
						strategy.TarAdd_model.remove(t);					

					if (strategy.SyncSrcDel_model.contains(stmt))		
						strategy.SyncSrcDel_model.remove(stmt);

				} else {
					Triple triple = Triple.create(stmt.getSubject().asNode(), stmt.getPredicate().asNode(), strategy.SyncTarAdd_model.createLiteral(rv).asNode());	
					strategy.SyncTarAdd_model.getGraph().add(triple);
					strategy.SyncSrcAdd_model.getGraph().add(triple);
					strategy.SyncSrcDel_model.add(stmt);

					if (configure.targetAdditionsChangeset!=null)
						strategy.TarAdd_model.remove(t);			
				}
			} else if (!Conflict_Finder.conflicts_Finder.resolve) {
				if (!flag)
					number_Of_conflictingTriples_S3++;
				is_conflict =true;
			}
		}
		if (is_conflict) {
			if (Conflict_Finder.conflicts_Finder.resolve)
				number_Of_ConflictingTriples++;
			else
				number_Of_conflictingTriples_S3++;
		}
	}

	public static void resolveSameAs (Statement stmt, List<Statement> conflictingTriples, boolean flag) {
		int conflictingTriplesSize = conflictingTriples.size();
		for (int i = 0; i < conflictingTriplesSize; i++) {
			Statement t = conflictingTriples.get(i);
			if (!strategy.SyncTarAdd_model.contains(stmt))
				strategy.SyncTarAdd_model.add(stmt);
			strategy.SyncTarAdd_model.add(t); 
			if (!strategy.SyncSrcAdd_model.contains(t))
				strategy.SyncSrcAdd_model.add(t);
		}
	}

	public static void resolveURI(Statement stmt, List<Statement> conflictingTriples, boolean flag) throws RiotException, FileNotFoundException{ 
		boolean is_conflict=false;
		int conflictingTriplesSize = conflictingTriples.size();
		for (int i = 0; i < conflictingTriplesSize; i++) {
			Statement t = conflictingTriples.get(i);
			if(stmt.asTriple().equals(t)) {		//same values
				duplicate_Triples++;
				if (!strategy.SyncTarAdd_model.contains(stmt))
					strategy.SyncTarAdd_model.add(stmt);		
				if (configure.targetAdditionsChangeset!=null)
					strategy.TarAdd_model.remove(stmt);		// avoid duplicate insertion				
			} else {
				Property p = ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#sameAs");

				List<Statement> sameAsInSourceAdd = findSimilarTriples(strategy.SrcAdd_model, configure.sourceAdditionsChangeset, stmt.getSubject().asResource(), p, t.getObject(), false) ;
				List<Statement> sameAsInTargetAdd = findSimilarTriples(strategy.TarAdd_model, configure.targetAdditionsChangeset, stmt.getSubject().asResource(), p, t.getObject(), false) ;
				List<Statement> sameAsInTargetDel = findSimilarTriples(strategy.TarDel_model, configure.targetDeletionsChangeset, stmt.getSubject().asResource(), p, t.getObject(), false) ;	
				List<Statement> sameAsInSourceDel = findSimilarTriples(strategy.SrcDel_model, configure.sourceDeletionsChangeset, stmt.getSubject().asResource(), p, t.getObject(), false) ;	
				List<Statement> sameAsInTarget = findSimilarTriples(strategy.Init_model, configure.initialTarget, stmt.getSubject().asResource(), p, t.getObject(), false) ;	

				if (!sameAsInSourceAdd.isEmpty() || !sameAsInTargetAdd.isEmpty() || !sameAsInTargetDel.isEmpty() || !sameAsInSourceDel.isEmpty() || !sameAsInTarget.isEmpty()) {									
					if (!strategy.SyncTarAdd_model.contains(stmt))
						strategy.SyncTarAdd_model.add(stmt);

					strategy.SyncTarAdd_model.add(t);
					if (!strategy.SyncSrcAdd_model.contains(t))
						strategy.SyncSrcAdd_model.add(t);

				} else	{
					Resource r = ResourceFactory.createResource(t.getObject().toString());
					sameAsInSourceAdd = findSimilarTriples(strategy.SrcAdd_model, configure.sourceAdditionsChangeset, r, p, stmt.getObject(), false) ;		
					sameAsInTargetAdd = findSimilarTriples(strategy.TarAdd_model, configure.targetAdditionsChangeset, r, p, stmt.getObject(), false) ;
					sameAsInTarget = findSimilarTriples(strategy.Init_model, configure.initialTarget, r, p, stmt.getObject(), false) ;
					sameAsInTargetDel = findSimilarTriples(strategy.TarDel_model, configure.targetDeletionsChangeset, r, p, stmt.getObject(), false) ; 
					sameAsInSourceDel = findSimilarTriples(strategy.SrcDel_model, configure.sourceDeletionsChangeset, r, p, stmt.getObject(), false) ;	

					if (!sameAsInSourceAdd.isEmpty() || !sameAsInTargetAdd.isEmpty() || !sameAsInTargetDel.isEmpty() || !sameAsInSourceDel.isEmpty() || !sameAsInTarget.isEmpty()) {

						if (!strategy.SyncTarAdd_model.contains(stmt))
							strategy.SyncTarAdd_model.add(stmt);

						strategy.SyncTarAdd_model.add(t);	

						if (!strategy.SyncSrcAdd_model.contains(t))
							strategy.SyncSrcAdd_model.add(t);

					} else if (Conflict_Finder.conflicts_Finder.resolve) {
						String [] args = {"",""};
						args [0] = stmt.getObject().asResource().getURI().toString();
						args [1] = t.getObject().asResource().getURI().toString() ;
						String rv = Conflict_Resolver.resolver.apply(functionforPredicate, args, "String"); 
						if (!flag)
							number_Of_ConflictingTriples ++;
						is_conflict=true;
						if (args[0].equals(rv))
						{
							if (configure.targetAdditionsChangeset!=null)
								strategy.TarAdd_model.remove(t);

							if (!strategy.SyncTarAdd_model.contains(stmt))
								strategy.SyncTarAdd_model.add(stmt);

							if (strategy.SyncSrcDel_model.contains(stmt))
								strategy.SyncSrcDel_model.remove(stmt);
						} else if (args[1].equals(rv)) {
							strategy.SyncTarAdd_model.add(t);

							if (!strategy.SyncSrcAdd_model.contains(t))
								strategy.SyncSrcAdd_model.add(t);
							strategy.SyncSrcDel_model.add(stmt);
						} else {

							Triple triple = Triple.create(stmt.getSubject().asNode(), stmt.getPredicate().asNode(), NodeFactory.createURI(rv));
							strategy.SyncTarAdd_model.getGraph().add(triple);
							strategy.SyncSrcAdd_model.getGraph().add(triple);
							strategy.SyncSrcDel_model.add(stmt);

							if (configure.targetAdditionsChangeset!=null)
								strategy.TarAdd_model.remove(t);
						}
					} else if (!Conflict_Finder.conflicts_Finder.resolve) {
						if (!flag)
							number_Of_conflictingTriples_S3++;
						is_conflict =true;
					}
				}
			}
		}
		if (is_conflict) {
			if (Conflict_Finder.conflicts_Finder.resolve)
				number_Of_ConflictingTriples++;
			else
				number_Of_conflictingTriples_S3++;
		}
	}

	public static void resolveGenerally (Statement stmt, List<Statement> conflictingTriples, boolean flag) {							

		RDFNode   object    = stmt.getObject(); 
		int size = conflictingTriples.size();
		boolean is_conflict = false;
		System.out.println(size);
		for (int j = 0; j < size; j++) {
			Statement t = conflictingTriples.get(j);
			if(stmt.asTriple().equals(t)) {		//same values
				duplicate_Triples++;
				if (!strategy.SyncTarAdd_model.contains(stmt))
					strategy.SyncTarAdd_model.add(stmt);		
				if (configure.targetAdditionsChangeset!=null) {
					strategy.TarAdd_model.remove(stmt);		// avoid duplicate insertion
				}
			} else if (Conflict_Finder.conflicts_Finder.resolve)  {
				String rv = "", type = "";
				if (object.isURIResource()) 
					rv = Conflict_Resolver.resolver.apply(functionforPredicate, getURIstoResolve (object, t), "String"); 
				else if (object.isLiteral()) {
					type = getType(object.asLiteral().getDatatypeURI());
					rv = Conflict_Resolver.resolver.apply(functionforPredicate, getLiteralstoResolve (object, t), type); 
				}
				System.out.println("rv="+rv);
				if (!flag)
					number_Of_ConflictingTriples ++;
				is_conflict=true;
				if (t.getObject().toString().equals(rv)) {
					if (!strategy.SyncSrcAdd_model.contains(t))
						strategy.SyncSrcAdd_model.add(t);
					strategy.SyncTarAdd_model.add(t);

					if (!strategy.SyncSrcDel_model.contains(stmt))
						strategy.SyncSrcDel_model.add(stmt);
				} else if (stmt.getObject().toString().equals(rv)) {

					if (configure.targetAdditionsChangeset!=null)
						strategy.TarAdd_model.remove(t);
					if (!strategy.SyncTarAdd_model.contains(stmt))
						strategy.SyncTarAdd_model.add(stmt);

					if (strategy.SyncSrcDel_model.contains(stmt))		//multiple add/modify in a file
						strategy.SyncSrcDel_model.remove(stmt);

				} else {
					Triple triple = null;

					if (object.isLiteral()) {
						if (stmt.getLiteral().getLanguage()!=null)
							type = stmt.getLiteral().getLanguage();
						triple = Triple.create(stmt.getSubject().asNode(), stmt.getPredicate().asNode(), strategy.SyncTarAdd_model.createTypedLiteral(rv, type).asNode());	
					} else if (object.isURIResource()) 
						triple = Triple.create(stmt.getSubject().asNode(), stmt.getPredicate().asNode(), NodeFactory.createURI(rv));	
					strategy.SyncTarAdd_model.getGraph().add(triple);
					strategy.SyncSrcAdd_model.getGraph().add(triple);
					strategy.SyncSrcDel_model.add(stmt);
					if (configure.targetAdditionsChangeset!=null)
						strategy.TarAdd_model.remove(t);
				}
			} else if (!Conflict_Finder.conflicts_Finder.resolve) {
				if (!flag)
					number_Of_conflictingTriples_S3++;
				is_conflict =true;
			}
		}
		if (is_conflict) {
			if (Conflict_Finder.conflicts_Finder.resolve)
				number_Of_ConflictingTriples++;
			else
				number_Of_conflictingTriples_S3++;
		}
	}

	public static void resolveLabels(Statement stmt, List<Statement> conflictingTriples, String functionforPredicate, boolean flag) {
		Statement t;
		boolean is_conflict = false;
		Resource  subject   = stmt.getSubject();     // get the subject
		Property  predicate = stmt.getPredicate();   // get the predicate
		RDFNode   object    = stmt.getObject(); 

		for (int j = 0; j < conflictingTriples.size(); j++) {
			t = conflictingTriples.get(j);
			if(stmt.asTriple().equals(t)) {		//same values
				duplicate_Triples++;
				if (!strategy.SyncTarAdd_model.contains(stmt))
					strategy.SyncTarAdd_model.add(stmt);		
				if (configure.targetAdditionsChangeset!=null){
					strategy.TarAdd_model.remove(stmt);		// avoid duplicate insertion
				}
			} else {
				String s_value = object.asLiteral().getLexicalForm().toString();
				String t_value = t.getObject().asLiteral().getLexicalForm();
				int threshold = Math.max(s_value.length(), t_value.length())/2;
				int diff = StringUtils.getLevenshteinDistance(s_value, t_value); //greater the diff,lesser the similarity

				if (diff >= threshold ) {
					t = conflictingTriples.get(j);

					if (!strategy.SyncTarAdd_model.contains(stmt)) 
						strategy.SyncTarAdd_model.add(stmt);	
					strategy.SyncTarAdd_model.add(t);					
 
					if (!strategy.SyncSrcAdd_model.contains(t))
						strategy.SyncSrcAdd_model.add(t);
				} else if (Conflict_Finder.conflicts_Finder.resolve) {

					String type = getType(object.asLiteral().getDatatypeURI());
					String rv = Conflict_Resolver.resolver.apply(functionforPredicate, getLiteralstoResolve (object, t), type);
					if (!flag)
						number_Of_ConflictingTriples ++;
					is_conflict = true;	
					if (t.getObject().toString().equals(rv))
					{
						strategy.SyncTarAdd_model.add(t);
						if (!strategy.SyncSrcAdd_model.contains(t))
							strategy.SyncSrcAdd_model.add(t);
						if (!strategy.SyncSrcDel_model.contains(stmt))
							strategy.SyncSrcDel_model.add(stmt);
					} else if (stmt.getObject().toString().equals(rv)) {
						if (!strategy.SyncTarAdd_model.contains(stmt))
							strategy.SyncTarAdd_model.add(stmt); 
						if (configure.targetAdditionsChangeset!=null)
							strategy.TarAdd_model.remove(t);
					} else {
						if (stmt.getLiteral().getLanguage()!=null)
							type = stmt.getLiteral().getLanguage();

						Triple triple = Triple.create(subject.asNode(), predicate.asNode(), strategy.SyncTarAdd_model.createTypedLiteral(rv, type).asNode());	
						strategy.SyncTarAdd_model.getGraph().add(triple);
						strategy.SyncSrcAdd_model.getGraph().add(triple);
						strategy.SyncSrcDel_model.add(stmt);
						if (configure.targetAdditionsChangeset!=null)
							strategy.TarAdd_model.remove(t);
					}
				} else if (!Conflict_Finder.conflicts_Finder.resolve) {
					if (!flag)
						number_Of_conflictingTriples_S3++;
					is_conflict =true;
				}
			}
		}
		if (is_conflict) {
			if (Conflict_Finder.conflicts_Finder.resolve)
				number_Of_ConflictingTriples++;
			else
				number_Of_conflictingTriples_S3++;
		}
	}

	public static String [] getLiteralstoResolve (RDFNode object, Statement t) {
		String [] args = new String [2];
		args [0] = object.asLiteral().getLexicalForm().toString();		
		if (t.getObject().isLiteral())
			args [1] = t.getObject().asLiteral().getLexicalForm().toString();		
		else 
			args [1] = t.getObject().toString();
		return args;
	}

	public static String [] getURIstoResolve (RDFNode object, Statement t) {
		String [] args = new String [2];	
		args [0] = object.asResource().getURI().toString();
		if (t.getObject().isURIResource())
			args [1] = t.getObject().asResource().getURI().toString();
		else
			args [1] = t.getObject().toString();
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


	public static void deletions_changeset(Property property) throws org.apache.jena.riot.RiotException, FileNotFoundException {

		StmtIterator iter = strategy.SrcDel_model.listStatements((Resource)null, property, (RDFNode)null);
		strategy.SyncTarDel_model.add(iter.toList());
		Conflict_Finder.conflicts_Finder.increaseDelTriples(iter.toList().size());
	}	

	/*			Find conflicting triples in target*/

	public static List<Statement> findSimilarTriples(Model model, String filename, Resource subject, Property predicate, RDFNode object, Boolean remove) throws org.apache.jena.riot.RiotException, FileNotFoundException {

		List<Statement> conflictingStatements = new ArrayList<Statement>();

		if (model!=null){
			StmtIterator iter = model.listStatements(subject, predicate, object); 
			conflictingStatements = iter.toList();

			if (remove) {
				model.remove(conflictingStatements);			
				model.write(new FileOutputStream(filename), configure.fileSyntax);
			}
		}
		return conflictingStatements;
	}
	

	public static String getType(String type) {

		
		int index = type.indexOf("#");
		if (index == -1)
			index = type.indexOf(":");
		index = index + 1;

		int size = type.length();
		return type.substring(index, size);
	}
}
