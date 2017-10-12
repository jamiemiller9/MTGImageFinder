package primary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtFinder {
	
	private static final String MTGJSON = "AllCards.json";
	private static final String TABLENAME = "cards";
	private static final String OUTPUTFILE = "cards.txt";

	public static void main(String[] args) throws IOException {
		
		List<String> errorMessages = new LinkedList<String>();
		
		//Read in the file and parse the lines
		File file = new File(MTGJSON);
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		String allCards = new String(data, "UTF-8");
		String[] lines = allCards.split("\n");
		
		//Create a pattern to search for
		String pattern = "(?<=\"name\": \")(.*)(?=\")";
		Pattern r = Pattern.compile(pattern);
		
		//Write the first lines of SQL
		PrintWriter writer = new PrintWriter(OUTPUTFILE, "UTF-8");
		writer.println("INSERT INTO " + TABLENAME + " (name, art) VALUES ");
		
		boolean firstElementInserted = false;
		
		//For each line, search for a name. If found connect to mtginfo and find a link to an image of the card
		for(int i = 0; i<lines.length; i++){
			Matcher m = r.matcher(lines[i]);
			if (m.find()){
				String url = "https://magiccards.info/query?q=" + m.group(0).replaceAll(" ","+") + "&v=card&s=cname";
				Document doc = Jsoup.connect(url).get();
				Elements links = doc.select("img[height=445]");
				if(links.size() > 0){
					String linkSrc = links.get(0).attr("src");
					if(firstElementInserted)
						writer.println(",");
					else
						firstElementInserted = true;
					writer.print("('" + m.group(0).replaceAll("'", "\\\\'") + "', 'https://magiccards.info" + linkSrc.replaceAll("'", "\'") + "')");
				} else {
					errorMessages.add("Error finding picture for " + m.group(0) + " at " + url);
				}
				
			}
			if(i%500 == 0)
				System.out.println(Math.floor((i * 100)/lines.length) + "%");
		}
		
		writer.println(";");
		writer.close();
		System.out.println("Finished");
		
		for (String message : errorMessages) {
            System.out.println(message);
        }

	}

}
