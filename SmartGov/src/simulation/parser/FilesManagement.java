package simulation.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Static functions to write and parse files and data.
 * Read CVS and other files
 * 
 * <href=https://www.mkyong.com/java/how-to-read-and-parse-csv-file-in-java/>
 * @author Simon
 *
 */
public class FilesManagement {

	public static void writeToFile(String pathfile, String filename, List<String> lines ) {
		try {
			Files.write(Paths.get(pathfile+filename), lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			System.out.println("Error while writing file.");
			e.printStackTrace();
		}
	}
	
	public static void writeToFile(String pathfile, String filename, String content ) {
		try {
			FileWriter fw = new FileWriter(pathfile + filename, false);
			fw.write(content);
			fw.close();
		} catch (IOException e) {
			System.out.println("Error while writing file.");
			e.printStackTrace();
		}
	}
	
	public static void appendToFile(String pathfile, String filename, String content ) {
		try {
			if(checkIfFileIsEmpty(pathfile, filename)) {
				FileWriter fw = new FileWriter(pathfile + filename, false);
				fw.write(content);
				fw.close();
			} else {
				FileWriter fw = new FileWriter(pathfile + filename, true);
				fw.write("\n");
				fw.write(content);
				fw.close();
			}
		} catch (IOException e) {
			System.out.println("Error while writing file.");
			e.printStackTrace();
		}
	}
	
	public static void appendToFile(String pathfile, String filename, List<String> lines ) {
		for(String content : lines) {
			try {
				if(checkIfFileIsEmpty(pathfile, filename)) {
					FileWriter fw = new FileWriter(pathfile + filename, false);
					fw.write(content);
					fw.close();
				} else {
					FileWriter fw = new FileWriter(pathfile + filename, true);
					fw.write("\n");
					fw.write(content);
					fw.close();
				}
			} catch (IOException e) {
				System.out.println("Error while writing file.");
				e.printStackTrace();
			}
		}
	}
	
	private static boolean checkIfFileIsEmpty(String pathfile, String filename) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(pathfile + filename));
			if (br.readLine() == null) {
				br.close();
				return true;
			}
		} catch (FileNotFoundException fnfe) {
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return false;
		
	}
	
	public static List<String[]> parseCVS(String csvFile, String lineSeparator, String cvsSplitBy){
		List<String[]> information = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			while ((lineSeparator = br.readLine()) != null) {
				information.add(lineSeparator.split(cvsSplitBy));
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return information;
	}
	
	public static List<String> readFile(String pathfile, String filename) {
		List<String> lines = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(pathfile + filename));
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();
		    while (line != null) {
		    	lines.add(line);
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    br.close();
		} catch(Exception e){
			//e.printStackTrace();
		}
		return lines;
	}

}
