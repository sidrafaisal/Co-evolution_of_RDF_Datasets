package Conflict_Resolver;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.FileManager;

public class statistics {

	public static Map<String, String> preferedSourceforPredicate  = new HashMap<String, String>();
	public static Map<String, String> resolutionFunctionforPredicate  = new HashMap<String, String>();	
	public static Map<String, String> mostFrequentValue = new HashMap<String, String>();
	public static Map<String, String> predicateType = new HashMap<String, String>();

	
	public static void globalVote ( String predicate ) {
		try 
		{
			Map<String, Integer> max  = new HashMap<String, Integer>();
			Property p = ResourceFactory.createProperty(predicate);

			if ( co_evolution_Manager.configure.sourceAdditionsChangeset != null) {
				Model smodel = FileManager.get().loadModel(co_evolution_Manager.configure.sourceAdditionsChangeset, co_evolution_Manager.configure.fileSyntax);
				ResIterator r = smodel.listResourcesWithProperty(p);
				while (r.hasNext()) {
					Resource res = r.nextResource();
					NodeIterator sni = smodel.listObjectsOfProperty(res, p);
					while (sni.hasNext()){
						String val = sni.nextNode().asLiteral().getValue().toString();		
						if(!max.containsKey(val))
							max.put(val, 1);
						else
						{
							Integer i = max.get(val) ;
							max.remove(val,i);
							max.put(val, i + 1);		
						}
					}
				}
				smodel.close();	
			}
			if (co_evolution_Manager.configure.targetAdditionsChangeset != null) {
				Model tmodel = FileManager.get().loadModel(co_evolution_Manager.configure.targetAdditionsChangeset, co_evolution_Manager.configure.fileSyntax);	
				ResIterator tr = tmodel.listResourcesWithProperty(p);
				while (tr.hasNext()) {
					Resource res = tr.nextResource();
					NodeIterator tni = tmodel.listObjectsOfProperty(res, p);
					while (tni.hasNext()){
						String val = tni.nextNode().asLiteral().getValue().toString();		
						if(!max.containsKey(val))
							max.put(val, 1);
						else
						{
							Integer i = max.get(val);
							max.remove(val, i);
							max.put(val, i + 1);		
						}
					}
				}
				tmodel.close();	
			}
			Integer highest = 0;
			String value = "";

			for (String v : max.keySet()) {
				if (highest < max.get(v)) {
					highest = max.get(v);
					value = v;
				}
			}		
			mostFrequentValue.put(predicate, value);
		} catch (org.apache.jena.riot.RiotException e) {
			System.out.println(""+e);
			e.printStackTrace();
		}
	}

	public static String findBlankNodes ( String predicate ) {
		try	{
			int blanksInSource = 0;
			int blanksInTarget = 0;

			Property p = ResourceFactory.createProperty(predicate);
			if (co_evolution_Manager.configure.sourceAdditionsChangeset != null) {
				Model smodel = FileManager.get().loadModel(co_evolution_Manager.configure.sourceAdditionsChangeset, co_evolution_Manager.configure.fileSyntax);
				ResIterator r = smodel.listResourcesWithProperty(p);
				while (r.hasNext()) {
					Resource res = r.nextResource();
					NodeIterator sni = smodel.listObjectsOfProperty(res, p);
					while (sni.hasNext()){
						if (sni.nextNode().asNode().isBlank())
							blanksInSource++;
					}
				}
				smodel.close();	
			}
			if (co_evolution_Manager.configure.targetAdditionsChangeset != null) {
				Model tmodel = FileManager.get().loadModel(co_evolution_Manager.configure.targetAdditionsChangeset, co_evolution_Manager.configure.fileSyntax);			
				ResIterator tr = tmodel.listResourcesWithProperty(p);
				while (tr.hasNext()) {
					Resource res = tr.nextResource();
					NodeIterator tni = tmodel.listObjectsOfProperty(res, p);
					while (tni.hasNext()){
						if (tni.nextNode().asNode().isBlank())		
							blanksInTarget++;
					}
				}
				tmodel.close();
			}
			if(blanksInSource <= blanksInTarget)
				return "source";
			else
				return "target";

		} catch (org.apache.jena.riot.RiotException e) {
			System.out.println(""+e);
			e.printStackTrace();
			return "source"; 	//default value
		}
	}
}
