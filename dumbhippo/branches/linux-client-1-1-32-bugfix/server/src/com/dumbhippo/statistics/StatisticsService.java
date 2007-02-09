package com.dumbhippo.statistics;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;


import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LiveStatistics;
import com.dumbhippo.server.NotFoundException;
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
	private Map<String, WeakReference<StatisticsSet>> cachedSets = new HashMap<String, WeakReference<StatisticsSet>>();
	
	
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
		
		cachedSets.put(statisticsWriter.getFilename(), new WeakReference<StatisticsSet>(statisticsWriter));
		
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
    	        return (name.endsWith(".stats") && !statisticsWriter.getFilename().equals(name));
    	    }
    	};
    	String[] filenames = dir.list(filter);
        
    	if (filenames == null) {
            throw new RuntimeException("Either " + dir.getAbsolutePath() + " does not exist or is not a directory");
        }
        
        for (String filename : filenames) {
    		try {
    			sets.add(getSet(filename));
    		} catch (NotFoundException e) {
    			logger.warn("Error listing statistics set: ", e);
    		}
        }
        sets.add(statisticsWriter);
    	
        return sets;
    }
    
    public synchronized StatisticsSet getSet(String filename) throws NotFoundException {
    	StatisticsSet set = null;
    	
    	if (filename.indexOf('/') >= 0)
    		throw new NotFoundException("Can't read statistics set '" + filename + "' from a different directory");
    	
    	if (cachedSets.containsKey(filename)) {
    		set = cachedSets.get(filename).get();
    		logger.debug("Found {} in hash, still there? {}", filename, set != null);
    	}
    	
    	if (set == null) {    		
    		try {
				set = new StatisticsReader(filename);
			} catch (IOException e) {
				throw new NotFoundException("Error reading set '" + filename + "'", e);
			} catch (ParseException e) {
				throw new NotFoundException("Can't parse set '" + filename + "'", e);
			}
    		cachedSets.put(filename, new WeakReference<StatisticsSet>(set));
    	}
    	
    	return set;
    }
    
    public StatisticsSet getCurrentSet() {
    	return statisticsWriter;
    }
}
