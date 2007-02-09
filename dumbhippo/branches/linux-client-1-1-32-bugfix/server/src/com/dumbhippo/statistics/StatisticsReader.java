package com.dumbhippo.statistics;

import java.io.IOException;
import java.io.RandomAccessFile;

public class StatisticsReader extends StatisticsSet {
	
	private RandomAccessFile input;
	
	@Override
	public boolean isCurrent() {
		return false;
	}
	
	public StatisticsReader(String filename) throws IOException, ParseException {
		this.filename = filename;
		input = new RandomAccessFile("statistics/" + filename, "r");
		
	    header = Header.read(input);
		
		rowStore = RowStore.createReadOnly(input.getChannel(), header.getHeaderSize(), 
				                           header.getColumns().size(), header.getNumRecords());
	}
	
	@Override
	public void finalize() {
		// Unlike FileInputStream/FileOutputStream, RandomAccessFile doesn't have an 
		// "emergency" finalizer that closes the file. (See bugs.sun.com: 4081750)
		try {
			input.close();
		} catch (IOException e) {
		}
	}
}
