package strategy;

import java.io.FileNotFoundException;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shared.AddDeniedException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import Conflict_Finder.conflicts_Finder;

public class strategy34 extends strategy{
	
	//Ti+1 = delta (Si) + delta (Ti) + Ti - X
	//Ti+1 = delta (Si) + delta (Ti) + Ti - X + NGT + ERT
	
	public static void apply(Property property, boolean resolve) {
		try {
			conflicts_Finder.identifyConflicts(property, resolve);
		} catch (RiotException | FileNotFoundException | AddDeniedException | OWLOntologyCreationException e) {
			e.printStackTrace();
		}

	}

}
