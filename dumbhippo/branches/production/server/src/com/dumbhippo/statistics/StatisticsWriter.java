package com.dumbhippo.statistics;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.util.EJBUtil;

/**
 * This class implements recording data collected from  number of statistics sources
 * into an on-disk file.
 *  
 * @author otaylor
 */
public class StatisticsWriter implements StatisticsSet {
	static private final Logger logger = GlobalSetup.getLogger(StatisticsWriter.class);	

	static final int INTERVAL = 15000; 

	private List<ColumnSource> columnSources = new ArrayList<ColumnSource>();
	private ColumnMap columnMap = new ColumnMap();
	private WriterThread writerThread = new WriterThread();
	private String filename;
	private Header header;
	private RandomAccessFile output;
	private MappedByteBuffer headerBuffer;
	private RowStore rowStore;
	private boolean started = false;
	private long position = 0;
	
	/**
	 * Start recording data. All data sources must already have been added with
	 * addSource() before calling this method.
	 */
	public void start() {
		if (started)
			throw new RuntimeException("Already started");
		
		header = new Header();
		
		Configuration configuration = EJBUtil.defaultLookup(Configuration.class);
		header.setHostName(configuration.getBaseUrl().getHost());
		
		header.setInterval(INTERVAL);
		header.setStartTime(new Date());
		header.setColumns(columnMap);
		
		filename = header.makeFilename();
		try {
			output = new RandomAccessFile(filename, "rw");
		} catch (IOException e) {
			throw new RuntimeException("Can't open output file");
		}
		
		try {
			header.write(output);
		} catch (IOException e) {
			throw new RuntimeException("Error writing header");
		}
		
		try {
			headerBuffer = output.getChannel().map(MapMode.READ_WRITE, 0, header.getHeaderSize());
		} catch (IOException e) {
			throw new RuntimeException("Can't map header for updates");
		}
		
		rowStore = RowStore.createReadWrite(output.getChannel(), header.getHeaderSize(), columnSources.size(), 0);
		
		writerThread.start();
	}
	
	/**
	 * Stop recording data. 
	 */
	public void shutdown() {
		writerThread.interrupt();
		
		try {
			writerThread.join();
		} catch (InterruptedException e) {
			// Shouldn't happen, just ignore
		}
		
		try {
			output.close();
		} catch (IOException e) {
			logger.error("Error closing output stream", e);
		}
	}
	
	/**
	 * Add a source to the list of sources from which data will be collected
	 */
	public void addSource(StatisticsSource source) {
		if (started)
			throw new RuntimeException("Can't add a source after starting");
		
		for (Method method : source.getClass().getMethods()) {
			ColumnSource columnSource = ColumnSource.forMethod(source, method);
			if (columnSource != null) {
				columnSources.add(columnSource);
				columnMap.add(columnSource);
			}
		}
	}
	
	public String getHostName() {
		return header.getHostName();
	}
	
	public Date getStartDate() {
		return header.getStartTime();
	}
	
	public Date getEndDate() {
		return new Date(header.getStartTime().getTime() + INTERVAL * position);
	}
	
	public String getFilename() {
		return filename; 
	}
	
	public ColumnMap getColumns() {
		return columnMap;
	}
	
	public Iterator<Row> getIterator(Date startDate, Date endDate, Timescale timescale, int[] columnIndexes) {
		long interval = timescale.getSeconds() * 1000;
		long fileStartTime = header.getStartTime().getTime();
		long fileEndTime = fileStartTime + INTERVAL * position;
		long startTime = startDate.getTime();
		long endTime = endDate.getTime();
		
		if (startTime < fileStartTime)
			startTime = fileStartTime;
		if (endTime > fileEndTime)
			endTime = fileEndTime;
		
		long startIndex = (startTime - fileStartTime) / interval;
		long endIndex = (endTime + interval - 1 - fileStartTime) / interval;
		
		int factor = (timescale.getSeconds() * 1000) / INTERVAL;
		
		long startRow = startIndex * factor;
		long endRow = endIndex * factor;
		RowIterator baseIterator = rowStore.getIterator(startRow, endRow);

		if (factor == 1)
			return new FilteredRowIterator(baseIterator, columnIndexes);
		else {
			Date resultStartDate = new Date(fileStartTime + startIndex * interval);
			return new AggregatedRowIterator(baseIterator, columnMap, columnIndexes, resultStartDate, timescale);
		}
	}

	private void appendRow(long time) {
		SimpleRow row = new SimpleRow(columnSources.size());
		row.setDate(new Date(time));
		for (int i = 0; i < columnSources.size(); i++)
			row.setValue(i, columnSources.get(i).getValue());
		
		rowStore.appendRow(row);
		position++;
	}
	
	private void appendSpacerRow() {
		SimpleRow row = new SimpleRow( columnSources.size());
		rowStore.appendRow(row);
		position++;
	}

	private class WriterThread extends Thread {
		WriterThread() {
			setName("StatisticsWriter");
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					long time = System.currentTimeMillis();
					long newPosition = (time - header.getStartTime().getTime() + INTERVAL / 2) / INTERVAL;
					
					if (newPosition > position) {
						while (newPosition > position + 1)
							appendSpacerRow();
						appendRow(time);
						header.setNumRecords(position);
						header.update(headerBuffer);
					}
						
					long newTime = header.getStartTime().getTime() + (newPosition + 1) * INTERVAL;
					
					Thread.sleep(newTime - time);
				}
			} catch (InterruptedException e) {
				// done
			}
		}
	}
}
