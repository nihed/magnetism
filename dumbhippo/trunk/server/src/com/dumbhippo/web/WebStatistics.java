package com.dumbhippo.web;

import com.dumbhippo.statistics.Column;
import com.dumbhippo.statistics.ColumnType;
import com.dumbhippo.statistics.ColumnUnit;
import com.dumbhippo.statistics.StatisticsSource;

/**
 * Statistics source with columns providing statitics about access to the
 * server via the web interface. 
 * @author otaylor
 */
public class WebStatistics implements StatisticsSource {
	static private WebStatistics instance = new WebStatistics();
	
	private long jspPagesServed;
	private long jspPageErrors;
	private long httpMethodsServed;
	private long httpMethodErrors;
	private long pageServeTime;
	
	static public WebStatistics getInstance() {
		return instance;
	}
	
	@Column(id="jspPagesServed",
			name="JSP Pages Served", 
			units=ColumnUnit.EVENTS, 
			type=ColumnType.CUMULATIVE)
	public synchronized long getJspPagesServed() {
		return jspPagesServed;
	}
	
	public synchronized void incrementJspPagesServed() {
		jspPagesServed++;
	}
	
	@Column(id="jspPageErrors",
			name="JSP Page Errors", 
			units=ColumnUnit.EVENTS, 
			type=ColumnType.CUMULATIVE)
	public synchronized long getJspPageErrors() {
		return jspPageErrors;
	}
	
	public synchronized void incrementJspPageErrors() {
		jspPageErrors++;
	}
	
	@Column(id="httpMethodsServed",
			name="HTTP Methods Served", 
			units=ColumnUnit.EVENTS, 
			type=ColumnType.CUMULATIVE)
	public synchronized long getHttpMethodsServed() {
		return httpMethodsServed;
	}
	
	public synchronized void incrementHttpMethodsServed() {
		httpMethodsServed++;
	}
	
	@Column(id="httpMethodErrors",
			name="HTTP Method Errors", 
			units=ColumnUnit.EVENTS, 
			type=ColumnType.CUMULATIVE)
	public synchronized long getHttpMethodsErrors() {
		return httpMethodErrors;
	}
	
	public synchronized void incrementHttpMethodErrors() {
		httpMethodErrors++;
	}

	@Column(id="pageServeTime",
			name="HTTP Request Time", 
			units=ColumnUnit.EVENTS, 
			type=ColumnType.CUMULATIVE)
	public synchronized long getPageServeTime() {
		return pageServeTime;
	}
	
	public synchronized void addPageServeTime(long serveTime) {
		pageServeTime += serveTime;
	}
}
