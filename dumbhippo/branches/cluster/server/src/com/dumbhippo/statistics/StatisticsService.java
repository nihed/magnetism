package com.dumbhippo.statistics;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;


import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LiveStatistics;
import com.dumbhippo.statistics.ServerStatistics;
import com.dumbhippo.statistics.StatisticsWriter;
import com.dumbhippo.web.WebStatistics;

/**
 * The central point of control for statistics collection. This class acts as an MBean
 * so that it can be started and shut down with our service and also provides methods
 * for getting access to the currently-being-recorded data set and previously
 * recorded data sets. 
 */ 
public class StatisticsService extends ServiceMBeanSupport implements StatisticsServiceMBean {
	static private StatisticsService instance; 
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(StatisticsService.class);

	private StatisticsWriter statisticsWriter;
	
	public static StatisticsService getInstance() {
		return instance;
	}
	
	@Override
	protected void startService() {
		statisticsWriter = new StatisticsWriter();
		statisticsWriter.addSource(new ServerStatistics());
		statisticsWriter.addSource(new LiveStatistics());
		statisticsWriter.addSource(WebStatistics.getInstance());
		statisticsWriter.start();
		
		instance = this;
    }
	
    @Override
	protected void stopService() {
    	instance = null;
    	
		statisticsWriter.shutdown();
    }
    
    public List<StatisticsSet> listSets() {
    	List<StatisticsSet> sets = new ArrayList<StatisticsSet>(); 
    	File dir = new File("statistics");
    	
    	FilenameFilter filter = new FilenameFilter() {
    	    public boolean accept(File dir, String name) {
    	        return (name.endsWith(".stats") && !statisticsWriter.getFilename().endsWith(name));
    	    }
    	};
    	String[] filenames = dir.list(filter);
        
    	if (filenames == null) {
            throw new RuntimeException("Either " + dir.getAbsolutePath() + " does not exist or is not a directory");
        }
        
        for (String filename : filenames) {
        	if (!filename.equals(statisticsWriter.getFilename()))
        		sets.add(new StatisticsReader("statistics/" + filename));          
        }
        sets.add(statisticsWriter);
    	
        return sets;
    }
    
    public StatisticsSet getSet(String filename) {
    	if (!filename.equals(statisticsWriter.getFilename()))
            return new StatisticsReader(filename);
    	else 
    		return statisticsWriter;
    }
    
    public StatisticsSet getCurrentSet() {
    	return statisticsWriter;
    }
}
