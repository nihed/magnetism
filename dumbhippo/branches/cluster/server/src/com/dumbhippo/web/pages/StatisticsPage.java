package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.HAPartition;

import com.dumbhippo.server.util.ClusterUtil;
import com.dumbhippo.statistics.StatisticsService;
import com.dumbhippo.statistics.StatisticsSet;


public class StatisticsPage {
    StatisticsService statisticsService;	
    List<String> fileNames;
    List<String> servers;
    String thisServer;
    
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
		
		HAPartition partition = ClusterUtil.getPartition();
		
		servers = new ArrayList<String>();
		for (ClusterNode node : partition.getClusterNodes()) {
			servers.add(node.getIpAddress().getHostAddress());
		}
		
		thisServer = partition.getClusterNode().getIpAddress().getHostAddress();
	}

	public List<String> getFileOptions() {
		return fileNames;
	}
	
	public List<String> getServers() {
		return servers;
	}
	
	public String getThisServer() {
		return thisServer;
	}
}
