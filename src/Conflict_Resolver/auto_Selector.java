package Conflict_Resolver;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class auto_Selector {

	public static void record () {

		File file = new File("auto_FunctionSelector.xml");

		if(!file.exists()) 	
			create();									
		else 
			modify();			
	}

	public static void select () {	
		try {
			Map<String, String> resolutionFunctionforPredicate  = new HashMap<String, String>();
			List<String> predicateList = co_evolution_Manager.configure.predicateList; // get the required predicates to be extracted from auto_selector file
		
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("auto_FunctionSelector.xml"));
			doc.getDocumentElement().normalize();

			int numberofIterations = Integer.parseInt(doc.getDocumentElement().getAttribute("numberofIterations")) ;	

			NodeList predList = doc.getElementsByTagName("Predicate");

			for (int temp = 0; temp < predList.getLength(); temp++) {	    				

				Element p = (Element) predList.item(temp);
				String predicate = p.getAttribute("name");	

				if (predicateList.contains(predicate))
				{					
					NodeList fList = p.getElementsByTagName("Function");
					Element f = (Element) fList.item(0);

					// pick the function with highest score
					String prefferedfname = f.getAttribute("name");					
					Double maxscore = Double.parseDouble(f.getAttribute("score"));

					for (int temps = 1; temps < fList.getLength(); temps++) {
						f = (Element) fList.item(temps);

						Double score = Double.parseDouble(f.getAttribute("score"));
						if (score > maxscore) {
							maxscore = score;
							prefferedfname = f.getAttribute("name");
						}
					}

					//create element in manual.xml
					resolutionFunctionforPredicate.put(predicate, prefferedfname); 
					predicateList.remove(predicate);
				}
			}

			// Set rest of the predicates with function Any	
			Iterator <String> notFound =predicateList.iterator(); 
			while (notFound.hasNext()) {

				String predicate = notFound.next().toString();

				if (predicate != null) {		
					resolutionFunctionforPredicate.put(predicate, "any");


					Element e = predicateElement (doc, predicate);
					doc.getDocumentElement().appendChild(e);
					String selectedFunction = "any";
					e.setAttribute("function", selectedFunction);
				//	doc.getDocumentElement().setAttribute("numberofIterations", String.valueOf(numberofIterations + 1));

					//set scores for all functions against this predicate
					int size = resolver.availableResolutionFunctions.length;
					for (int i = 0; i< size; i++) {
						Double score;
						String availableFunction = resolver.availableResolutionFunctions[i];
						if (selectedFunction.equals(availableFunction))
							score = 1.0 / ( numberofIterations );
						else
							score =	0.0; 						

						e.appendChild(scoreElement(doc, "Function", availableFunction, score.toString()));			
					}
				}
			}
			manual_Selector.filename = "manual_FunctionSelector_"+ co_evolution_Manager.configure.initialTarget+".xml";
			manual_Selector.create (resolutionFunctionforPredicate);
			writeXML (doc, "auto_FunctionSelector.xml");
		} catch (Exception e) {
			System.out.println(""+e);
			e.printStackTrace();
		}
	}



	public static void modify () {
		try {

			List<String> predicateList = co_evolution_Manager.configure.predicateList;  // get the required predicates to be extracted from auto_selector file

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("auto_FunctionSelector.xml"));
			doc.getDocumentElement().normalize();

			int numberofIterations = Integer.parseInt(doc.getDocumentElement().getAttribute("numberofIterations")) ;	 
			doc.getDocumentElement().setAttribute("numberofIterations", String.valueOf(numberofIterations + 1));
			NodeList predList = doc.getElementsByTagName("Predicate");

			for (int temp = 0; temp < predList.getLength(); temp++) {

				Element p = (Element) predList.item(temp);
				String predicate = p.getAttribute("name");

				if (predicateList.contains(predicate))
				{	
					String ifunction = statistics.resolutionFunctionforPredicate.get(predicate);

					NodeList fList = p.getElementsByTagName("Function");

					for (int temps = 0; temps < fList.getLength(); temps++) {

						Element f = (Element) fList.item(temps);
						Double score = Double.parseDouble(f.getAttribute("score"));

						String function= f.getAttribute("name");
						if (function.equals(ifunction))
							score = (score * numberofIterations + 1) / (numberofIterations + 1);
						else
							score = (score * numberofIterations ) / (numberofIterations + 1);
						f.setAttribute("score", score.toString());
					}
					predicateList.remove(predicate);
				}									
			}

			// Set rest of the predicates which are not in auto_selector yet
			Iterator <String> notFound =predicateList.iterator(); 
			while (notFound.hasNext()) {
				String name = notFound.next().toString();	

				if (name != null) {	
					Element e = predicateElement (doc, name);
					doc.getDocumentElement().appendChild(e);		

					//set scores for all functions against this predicate
					String selectedFunction = statistics.resolutionFunctionforPredicate.get(name);
					int size = resolver.availableResolutionFunctions.length;
					for (int i = 0; i< size; i++) {
						Double score;
						String availableFunction = resolver.availableResolutionFunctions[i];
						if (selectedFunction.equals(availableFunction))
							score = 1.0 / ( numberofIterations + 1 );
						else
							score =	0.0; 						

						e.appendChild(scoreElement(doc, "Function", availableFunction, score.toString()));

					}
				}
			}
			writeXML (doc, "auto_FunctionSelector.xml");
		} catch (Exception e) {
			System.out.println(""+e);
			e.printStackTrace();
		}
	}	

	public static void create () {

		int size = resolver.availableResolutionFunctions.length;

		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("auto_FunctionSelector");
			rootElement.setAttribute("numberofIterations", "1");
			doc.appendChild(rootElement);

			for (String key: statistics.resolutionFunctionforPredicate.keySet()) {
				String predicate = key;				
				Element e = predicateElement (doc, predicate);
				rootElement.appendChild(e);
				String selectedFunction = statistics.resolutionFunctionforPredicate.get(key);
				for (int i = 0; i< size; i++) {
					Double score;
					String availableFunction = resolver.availableResolutionFunctions[i];
					if (selectedFunction.equals(availableFunction))
						score = 1.0;
					else
						score =	0.0; 						

					e.appendChild(scoreElement(doc, "Function", availableFunction, score.toString()));
				}
			}
			writeXML (doc, "auto_FunctionSelector.xml");

		} catch (ParserConfigurationException | TransformerException e) {
			System.out.println(""+e);
			e.printStackTrace();
		}

	}
	public static Element predicateElement ( Document doc, String a1)
	{
		Element e = doc.createElement("Predicate");
		e.setAttribute("name", a1);	
		return e;
	}
	public static Element scoreElement ( Document doc, String e, String a1, String a2) 
	{
		Element af = doc.createElement(e);
		af.setAttribute("name", a1);
		af.setAttribute("score", a2);	
		return af;
	}

	public static void set (String p, String rf){	
		statistics.resolutionFunctionforPredicate.put(p, rf);  
	}

	public static void writeXML (Document doc, String filename) throws TransformerException {

		Transformer transformer = TransformerFactory.newInstance().newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(filename));

		transformer.transform(source, result);
	}
}
