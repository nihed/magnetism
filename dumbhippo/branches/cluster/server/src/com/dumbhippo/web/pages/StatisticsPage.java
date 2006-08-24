package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dumbhippo.statistics.StatisticsService;
import com.dumbhippo.statistics.StatisticsSet;


public class StatisticsPage {
    StatisticsService statisticsService;	
    List<String> fileNames;
    
	public StatisticsPage() {
	    statisticsService = StatisticsService.getInstance();
	    if (statisticsService == null)
		    throw new RuntimeException("Statistics Service isn't started");
	    
	    fileNames = new ArrayList<String>();	
		
		for (StatisticsSet set : statisticsService.listSets()) {
		    String fullName = set.getFilename(); 
		    fileNames.add(fullName.substring(fullName.indexOf("/")+1, fullName.lastIndexOf(".")));
		}
		
		Collections.sort(fileNames, new Comparator<String>() {
			public int compare(String filename1, String filename2) {
				return - String.CASE_INSENSITIVE_ORDER.compare(filename1, filename2);
			}			
		});
	}

	public List<String> getFileOptions() {
		return fileNames;
	}
}
