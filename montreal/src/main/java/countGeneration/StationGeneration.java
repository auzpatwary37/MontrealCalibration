package countGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;

public class StationGeneration {
public static void main(String[] args) {
	String stationFileName = "src/main/resources/linkMatching_mael_cleaned.csv";
	try {
		BufferedReader bf = new BufferedReader(new FileReader(new File(stationFileName)));
		bf.readLine();//get rid of the header
		String line = null;
		while((line = bf.readLine())!=null) {
			String[] part = line.split(",");
			String stationId = part[0]+"___"+part[1]+"___"+part[2];
			Coord coord = new Coord(Double.parseDouble(part[3]),Double.parseDouble(part[4]));
			String matchedLink = part[5];
			matchedLink = matchedLink.replaceAll("[\\[\\]]", "");
			System.out.println("Matched Link - "+matchedLink);
		}
		
		bf.close();
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
