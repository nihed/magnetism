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
    List<SetInfo> sets;
    List<String> servers;
    String thisServer;
    
    public static class SetInfo {
    	String name;
    	String filename;
    	
    	public SetInfo(StatisticsSet set) {
    		filename = set.getFilename();
    		if (this.filename.endsWith(".stats"))
    			name = this.filename.substring(0, this.filename.length() - ".stats".length());
    		else
    			name = this.filename;
    	}
    	
    	public String getName() { 
			return name; 
    	}
    	
    	public String getFilename() {
    		return filename;
    	}
    }
    
	public StatisticsPage() {
	    statisticsService = StatisticsService.getInstance();
	    if (statisticsService == null)
		    throw new RuntimeException("Statistics Service isn't started");
	    
	    sets = new ArrayList<SetInfo>();	
		
		for (StatisticsSet set : statisticsService.listSets()) {
			if (!set.isCurrent())
				sets.add(new SetInfo(set));
		}

		Collections.sort(sets, new Comparator<SetInfo>() {
			public int compare(SetInfo set1, SetInfo set2) {
				return - String.CASE_INSENSITIVE_ORDER.compare(set1.getName(), set2.getName());
			}			
		});
		
		// Always put the current set at the front; we count on that in the javascript
		// code. It would normally work this way for our standard names, but people
		// can drop other files into the statistics/ directory.
		sets.add(0, new SetInfo(statisticsService.getCurrentSet()));
		
		HAPartition partition = ClusterUtil.getPartition();
		
		servers = new ArrayList<String>();
		for (ClusterNode node : partition.getClusterNodes()) {
			servers.add(node.getIpAddress().getHostAddress());
		}
		
		thisServer = partition.getClusterNode().getIpAddress().getHostAddress();
	}

	public List<SetInfo> getSets() {
		return sets;
	}
	
	public List<String> getServers() {
		return servers;
	}
	
	public String getThisServer() {
		return thisServer;
	}
}
