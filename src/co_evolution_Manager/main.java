package co_evolution_Manager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.semanticweb.owlapi.model.OWLException;

import co_evolution_Manager.configure;
import strategy.strategy;

public class main {
	
	public static Scanner scanner;
	public static configure config; 
	public static long duplicateTriples = 0, 
	unique_objects_synTar = 0, 
	unique_objects_inTar = 0,  
	unique_objects_srcchangeset = 0, 
	unique_objects_tarchangeset = 0, time = 0;
	
	public static void main (String[] args) {	
		try {
			
			write("input sizes.txt", "tar, srcAdd, srcDel, tarAdd, tarDel");
			write("exetimes.txt", "time_S1, time_S2, time_s3, time_s4, total in sec");
			write("datasize.txt", "S1_Add_triple, S1_Del_triple, S2_Add_triple, S2_Del_triple, S3_Add_triple, S3_Del_triple,"
					+ "S4_Add_triple, S4_Del_triple,"+ "conflictingTriples_S3, Conflicting_triples_s4, SyncSrcAdd, SyncSrcDel, SyncTarAdd, SyncTarDel");
	
		write("completeness.txt", "SynTar, duplicates, simlpedupicatein tar, funobj_Triples, unique objects in synTar, in initial dataset, in src changeset, in tar changeset");
		
			List <String> filenames = readDatasetsLocation ("DatasetsLocation");
			scanner = new Scanner(System.in);			
			
			config = new configure ("NT",filenames.get(4), filenames.get(5),filenames.get(6));
			crawl(new File(filenames.get(0)), filenames.get(0).length(), filenames.get(1), 
					filenames.get(2), filenames.get(3));
			recrawl(new File(filenames.get(3)));
			recrawl(new File(filenames.get(2)));
			 	
			scanner.close();

	
			
		} catch (IOException | OWLException e) { 
			e.printStackTrace();
		}
	}
	

	public static void recrawl(File tar) throws IOException{
		List<String> arr = new ArrayList<String>();
		if(tar.isDirectory()){
			String[] srcaddList = tar.list();
			for(String filename : srcaddList)
				recrawl(new File(tar, filename));
		} else if (tar.getName().contains("added.nt")) {
				String parent = tar.getAbsolutePath();
				arr.add(0, null);
				arr.add(1, null);
				arr.add(2, parent);
				arr.add(3, null);
				config.configureFiles (arr.get(0), arr.get(1), arr.get(2), arr.get(3));
				printInput();
				long time = System.currentTimeMillis();

				strategy.apply ();
				long endTime   = System.currentTimeMillis();
				time = (endTime - time);	
				
				printStats();	
				applySyncDelta();

				arr.add(configure.SyncSrcAdd);
				arr.add(configure.SyncSrcDel);
				emptyResources (arr);	
				resetcounters();				
//				find_unique_objects(arr);

			} else if (tar.getName().contains("removed.nt")) {
				String parent = tar.getAbsolutePath();
				arr.add(0, null);
				arr.add(1, null);
				arr.add(2, null);
				arr.add(3, parent);
				config.configureFiles (arr.get(0), arr.get(1), arr.get(2), arr.get(3));
				printInput();
				long time = System.currentTimeMillis();

				strategy.apply ();
				long endTime   = System.currentTimeMillis();
				time = (endTime - time);	
				
				printStats();	
				applySyncDelta();

				arr.add(configure.SyncSrcAdd);
				arr.add(configure.SyncSrcDel);
				emptyResources (arr);	
				resetcounters();				
//				find_unique_objects(arr);
			}
	}	
	
	public static void crawl(File srcadd, int i, String srcdel, String taradd, String tardel) throws IOException{

		List<String> arr = new ArrayList<String>();
		if(srcadd.isDirectory()){
			String[] srcaddList = srcadd.list();
			for(String filename : srcaddList)
				crawl(new File(srcadd, filename), i, srcdel, taradd, tardel);	

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
							config.configureFiles (arr.get(0), arr.get(1), arr.get(2), arr.get(3));
							printInput();
							long time = System.currentTimeMillis();

							strategy.apply ();
							long endTime   = System.currentTimeMillis();
							time = (endTime - time);	
							
							printStats();	
							applySyncDelta();

							arr.add(configure.SyncSrcAdd);
							arr.add(configure.SyncSrcDel);
							emptyResources (arr);	
							resetcounters();				
//							find_unique_objects(arr);
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
				config.configureFiles (arr.get(0), arr.get(1), arr.get(2), arr.get(3));
				printInput();
				long time = System.currentTimeMillis();

				strategy.apply ();
				long endTime   = System.currentTimeMillis();
				time = (endTime - time);	
				
				printStats();	
				applySyncDelta();

				arr.add(configure.SyncSrcAdd);
				arr.add(configure.SyncSrcDel);
				emptyResources (arr);	
				resetcounters();				
//				find_unique_objects(arr);
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
	
	public static List<String> readDatasetsLocation (String filename) throws IOException {		
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
	public static void printInput() throws IOException{
		write("input file names.txt", configure.sourceAdditionsChangeset+"--"+ configure.sourceDeletionsChangeset+"--"+ 
				configure.targetAdditionsChangeset+"--"+  configure.targetDeletionsChangeset);
		write("input sizes.txt", Long.toString(configure.getDatasetSize(configure.initialTarget)) +", "+
				Long.toString(configure.getDatasetSize(configure.sourceAdditionsChangeset))+ ", " +
				Long.toString(configure.getDatasetSize(configure.sourceDeletionsChangeset))+ ", " +
				Long.toString(configure.getDatasetSize(configure.targetAdditionsChangeset))+ ", " +
				Long.toString(configure.getDatasetSize(configure.targetDeletionsChangeset)));			
	}
	
	public static void printStats() throws IOException{
		String	time_S1 = String.format("%d", TimeUnit.MILLISECONDS.toSeconds(configure.time_S1)), 
				time_S2 = String.format("%d", TimeUnit.MILLISECONDS.toSeconds(configure.time_S2)),
				time_S3 = String.format("%d", TimeUnit.MILLISECONDS.toSeconds(Conflict_Finder.conflicts_Finder.time_S3)),
				time_S4 = String.format("%d", TimeUnit.MILLISECONDS.toSeconds(Conflict_Finder.conflicts_Finder.time_S4)),
		total_time = String.format("%d", TimeUnit.MILLISECONDS.toSeconds(time));
		
		write("exetimes.txt", time_S1+ ", " + time_S2+", " + time_S3+", "+ time_S4+", "+ total_time);
		
		Model SyncSrcAdd_model = FileManager.get().loadModel(configure.SyncSrcAdd, configure.fileSyntax);
		Model SyncSrcDel_model = FileManager.get().loadModel(configure.SyncSrcDel, configure.fileSyntax);
		Model SyncTarAdd_model = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);
		Model SyncTarDel_model = FileManager.get().loadModel(configure.SyncTarDel, configure.fileSyntax);			

		write("datasize.txt", Long.toString(configure.S1_Add_triplesize)+", "+Long.toString(configure.S1_Del_triplesize)+","+
				Long.toString(configure.S2_Add_triplesize)+","+Long.toString(configure.S2_Del_triplesize)+","+ 
				Long.toString(Conflict_Finder.conflicts_Finder.S3_Add_triplesize) +", "+ 
				Long.toString(Conflict_Finder.conflicts_Finder.S3_Del_triplesize) +", "+ 
				Long.toString(Conflict_Finder.conflicts_Finder.S4_Add_triplesize) +", "+ 
				Long.toString(Conflict_Finder.conflicts_Finder.S4_Del_triplesize) +", "+ 
				Long.toString(Conflict_Finder.source_Delta.number_Of_conflictingTriples_S3) +", "+
				Long.toString(Conflict_Finder.source_Delta.number_Of_ConflictingTriples) +", "+
				
				Long.toString(SyncSrcAdd_model.size()) + ", "+  Long.toString(SyncSrcDel_model.size())+", "+ 
				Long.toString(SyncTarAdd_model.size()) + ", "+ Long.toString(SyncTarDel_model.size()));
		
		unique_objects_srcchangeset = SyncSrcAdd_model.listObjects().toList().size();
		unique_objects_tarchangeset = SyncTarAdd_model.listObjects().toList().size();

		SyncSrcAdd_model.close();			
		SyncSrcDel_model.close();	
		SyncTarAdd_model.close();
		SyncTarDel_model.close();
	}
	
	public static void applySyncDelta () throws IOException {
		Model SyncTarAdd_model = FileManager.get().loadModel(configure.SyncTarAdd, configure.fileSyntax);
		Model SyncTarDel_model = FileManager.get().loadModel(configure.SyncTarDel, configure.fileSyntax);			
		Model Tar_model = FileManager.get().loadModel(configure.initialTarget, configure.fileSyntax);
//		System.out.println("before"+configure.getDatasetSize(configure.initialTarget));
//		Tar_model.add(SyncTarAdd_model);	
		
	//	Tar_model = FileManager.get().loadModel(configure.initialTarget, configure.fileSyntax);
		unique_objects_inTar = Tar_model.listObjects().toList().size();

		StmtIterator iter = SyncTarAdd_model.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();  // get next statement		 
			if (Tar_model.contains(stmt))
				duplicateTriples++;
			Tar_model.add(stmt);
		}
	
		iter = SyncTarDel_model.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();  // get next statement		 
			Tar_model.remove(stmt);
		}
		Tar_model.write(new FileOutputStream(configure.initialTarget), configure.fileSyntax);
		SyncTarAdd_model.close();
		SyncTarDel_model.close();
		Tar_model.close();	
	//	System.out.println("after"+configure.getDatasetSize(configure.initialTarget));
		
		Tar_model = FileManager.get().loadModel(configure.initialTarget, configure.fileSyntax);
		
		unique_objects_synTar = Tar_model.listObjects().toList().size();
		Tar_model.close();
		write("completeness.txt",
				Long.toString(configure.getDatasetSize(configure.initialTarget)) +", "+
				Long.toString(duplicateTriples) + ", " +Long.toString(Conflict_Finder.source_Delta.duplicate_Triples)+", "+
				Long.toString(Conflict_Finder.source_Delta.funobj_Triples)+", "+
				Long.toString(unique_objects_synTar)+", "+ Long.toString(unique_objects_inTar)+", "+ 
				Long.toString(unique_objects_srcchangeset)+", "+ Long.toString(unique_objects_tarchangeset));
		
		File nt = new File(configure.SyncTarAdd);
		nt.delete();
		if(!nt.exists())
			nt.createNewFile();		
		
		nt = new File(configure.SyncTarDel);
		nt.delete();
		if(!nt.exists())
			nt.createNewFile();
	}
	public static void resetcounters(){
		duplicateTriples = 0;
				unique_objects_synTar=0; 
				unique_objects_inTar=0;
				unique_objects_srcchangeset=0; 
				unique_objects_tarchangeset=0;
	}
}
