package com.dumbhippo.statistics;

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
    	return null;
    }
    
    public StatisticsSet getSet(String filename) {
    	return null;
    }
    
    public StatisticsSet getCurrentSet() {
    	return statisticsWriter;
    }
}
