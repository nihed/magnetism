package com.dumbhippo.web.pages;

import com.dumbhippo.statistics.StatisticsService;
import com.dumbhippo.statistics.StatisticsSet;


public class StatisticsPage {
    StatisticsService statisticsService;	
    StatisticsSet currentSet;
    
	public StatisticsPage() {
	    statisticsService = StatisticsService.getInstance();
	    if (statisticsService == null)
		    throw new RuntimeException("Statistics Service isn't started");
	    currentSet = statisticsService.getCurrentSet();
	}
	
	public StatisticsSet getCurrentSet() {
		return currentSet;
	}
	
	public String getFileOption() {
		String fullName = currentSet.getFilename(); 
		return fullName.substring(fullName.indexOf("/")+1, fullName.lastIndexOf("."));
	}
}
