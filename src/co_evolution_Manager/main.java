package co_evolution_Manager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.semanticweb.owlapi.model.OWLException;

import strategy.strategy;

public class main {
	
	public static Scanner scanner;
	
	public static String [] Hours = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15",
			"16", "17", "18", "19", "20", "21", "22", "23", "24"};
	public static String [] Days = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15",
			"16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30"};
	public static String [] Months = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
	
	public static void main (String[] args) {	
		try {
			List <String> filenames = readDatasetsLocation ("DatasetsLocation");
			scanner = new Scanner(System.in);			
			configure config = new configure ("NT",filenames.get(4), filenames.get(5));
			crawl(new File(filenames.get(0)), filenames.get(0).length(), filenames.get(1), 
					filenames.get(2), filenames.get(3), filenames.get(6));
		//	crawl(filenames.get(0), filenames.get(0).length(), filenames.get(1), 
			//				filenames.get(2), filenames.get(3), new File(filenames.get(6)));
			recrawl(new File(filenames.get(3)),filenames.get(6));
			recrawl(new File(filenames.get(2)),filenames.get(6));

			scanner.close();
			
		} catch (IOException | OWLException e) {
			e.printStackTrace();
		}
	}
	

	public static void recrawl(File tar, String t) throws FileNotFoundException{
		List<String> arr = new ArrayList<String>();
		if(tar.isDirectory()){
			String[] srcaddList = tar.list();
			for(String filename : srcaddList){

				
				recrawl(new File(tar, filename), t);
				
			}
		} else if (tar.getName().contains("added.nt")) {
				String parent = tar.getAbsolutePath();
				arr.add(0, null);
				arr.add(1, null);
				arr.add(2, parent);
				arr.add(3, null);
			//	System.out.println(arr.get(0)+ arr.get(1)+ arr.get(2)+ arr.get(3)+ t);

				///////////////////////////////// perform strategy

				configure.configureFiles (arr.get(0), arr.get(1), arr.get(2), arr.get(3), t);

				strategy.apply ();
				
				emptyResources (arr);//renameOutput (t);
			//	printStats();	

			} else if (tar.getName().contains("removed.nt")) {
				String parent = tar.getAbsolutePath();
				arr.add(0, null);
				arr.add(1, null);
				arr.add(2, null);
				arr.add(3, parent);
			//	System.out.println(arr.get(0)+ arr.get(1)+ arr.get(2)+ arr.get(3)+ t);

				///////////////////////////////// perform strategy

				configure.configureFiles (arr.get(0), arr.get(1), arr.get(2), arr.get(3), t);

				strategy.apply ();
		
				emptyResources (arr);//renameOutput (t);
			//	printStats();		

		}
	}
	/*public static void crawl(String srcadd, int t, String srcdel, String taradd, String tardel, File p){

for (int i = 0 ; i < Months.length ; i++)
	for (int j = 0 ; j < Days.length ; j++)
		for (int k = 0 ; k < Hours.length ; k++) {
			System.out.print(Months[i]+"/"+Days[j]+"/"+Hours[k]+"/");
	
			String[] src_additionList = null;
//			List <String> src_additionList = new ArrayList <String> ();
			File src_addition = new File ( srcadd+Months[i]+"/"+Days[j]+"/"+Hours[k] );
			if(src_addition.isDirectory()){
				src_additionList  = src_addition.list();
	//			for(String filename : list){
	//				src_additionList.add(filename);
	//			}
				}
			
			String[] src_deletionList= null; 
			//List <String> src_deletionList = new ArrayList <String> ();			
			File src_deletion = new File (srcdel+Months[i]+"/"+Days[j]+"/"+Hours[k]);
			if(src_deletion.isDirectory()){
				src_deletionList = src_deletion.list();
			//	for(String filename : list){
			//		src_deletionList.add(filename);
			//	}
				}
			String[] tar_additionList = null;
//			List <String> tar_additionList = new ArrayList <String> ();				
			File tar_addition = new File (taradd+Months[i]+"/"+Days[j]+"/"+Hours[k]);
			if(tar_addition.isDirectory()){
				tar_additionList = tar_addition.list();
	//			for(String filename : list){
	//				tar_additionList.add(filename);
	//			}
				}
			String[] tar_deletionList = null;
		//	List <String> tar_deletionList = new ArrayList <String> ();			
			File tar_deletion = new File(tardel+Months[i]+"/"+Days[j]+"/"+Hours[k]);
			if(tar_deletion.isDirectory()){
				tar_deletionList = tar_deletion.list();
			//	for(String filename : list){
			//		tar_deletionList.add(filename);
			//	}
				}
			int sa=0,sd=0,ta=0,td=0, max = Math.max(src_deletionList.length, src_additionList.length);
			for (;sa < ; sd < ;) {
				
			}

		}
	}*/

	
	
	public static void crawl(File srcadd, int i, String srcdel, String taradd, String tardel, String t) throws FileNotFoundException{

		List<String> arr = new ArrayList<String>();
		if(srcadd.isDirectory()){
			String[] srcaddList = srcadd.list();
			for(String filename : srcaddList){
				crawl(new File(srcadd, filename), i, srcdel, taradd, tardel, t);	
			
			}


			if (srcadd.getAbsolutePath().length()>i) {
				File sdfile = new File(srcdel + (srcadd.getAbsolutePath()).substring(i));
				if(sdfile.isDirectory()){
					String[] addList = sdfile.list();
					for(String filename : addList){
						if(filename.contains(".removed.nt")) {
							arr.add(0, null);
							arr.add(1, sdfile+"/"+filename);							

							String tanode = taradd + (srcadd.getAbsolutePath()).substring(i)+ "/" + filename;
							int tindex = tanode.indexOf(".removed.nt");
							tanode = tanode.substring(0, tindex)+".added.nt";
							File tafile = new File(tanode);
							if (tafile.exists()) 
								arr.add(2, tafile.getAbsolutePath());
							else 
								arr.add(2, null);
							
							String tdnode = tardel + (srcadd.getAbsolutePath()).substring(i)+ "/" + filename;
							File tdfile = new File(tdnode);
							if (tdfile.exists()) 
								arr.add(3, tdfile.getAbsolutePath());
							else 
								arr.add(3, null);
					//		System.out.println(arr.get(0)+ arr.get(1)+ arr.get(2)+ arr.get(3)+ t);
							configure.configureFiles (arr.get(0), arr.get(1), arr.get(2), arr.get(3), t);	
						
							strategy.apply ();

							emptyResources (arr);
							//renameOutput (t);
					//		printStats();	
							

						}
					}
				}
			} 
		} else {
			if (srcadd.getName().contains("added.nt")) {

				String parent = srcadd.getAbsolutePath();
				arr.add(0, parent);
				//	System.out.println(parent);

				String sdnode = srcdel + parent.substring(i);
				int sindex = sdnode.indexOf(".added.nt");
				sdnode = sdnode.substring(0, sindex)+".removed.nt";
				File sdfile = new File(sdnode);
				if (sdfile.exists()) 
					arr.add(1, sdfile.getAbsolutePath());
				else 
					arr.add(1, null);

				String tanode = taradd + parent.substring(i);
				File tafile = new File(tanode);
				if (tafile.exists()) 
					arr.add(2, tafile.getAbsolutePath());
				else 
					arr.add(2, null);

				String tdnode = tardel + parent.substring(i);
				int tindex = tdnode.indexOf(".added.nt");
				tdnode = tdnode.substring(0, tindex)+".removed.nt";
				File tdfile = new File(tdnode);
				if (tdfile.exists()) 
					arr.add(3, tdfile.getAbsolutePath());
				else 
					arr.add(3, null);


			//	System.out.println(arr.get(0)+ arr.get(1)+ arr.get(2)+ arr.get(3)+ t);
				///////////////////////////////// perform strategy

				configure.configureFiles (arr.get(0), arr.get(1), arr.get(2), arr.get(3), t);

				strategy.apply ();

							emptyResources (arr);
							//renameOutput (t);
		//		printStats();	

			} 
		}
	}
	
public static void write(String f, String content) throws IOException {
content = "\n"+ content ;
			File file = new File("/Users/sidra/Desktop/"+f);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
	}
	
	public static List<String> readDatasetsLocation (String filename) throws IOException{
		
	BufferedReader	br = new BufferedReader(new FileReader(filename));
	List <String> filenames = new ArrayList <String>();
		String line = null;
		while ((line = br.readLine()) != null) {
			filenames.add(line);			 					
		}
		br.close();
		return filenames;
	}
	public static void emptyResources (List<String> f) {
		try {
			File file;

			for (int i = 0; i < f.size() ; i++ ) {
				if (f.get(i)!=null) {
					file = new File (f.get(i));
					file.delete();
				}		
			}
		} catch(Exception e){  		
			e.printStackTrace();
		}
	}

}
