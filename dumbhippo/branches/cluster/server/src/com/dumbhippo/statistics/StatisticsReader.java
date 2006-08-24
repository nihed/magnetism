package com.dumbhippo.statistics;

import java.io.IOException;
import java.io.RandomAccessFile;

public class StatisticsReader extends StatisticsSet {
	
	private RandomAccessFile input;
	
	public StatisticsReader(String filename) {
		this.filename = filename;
		try {
			input = new RandomAccessFile(filename, "r");
		} catch (IOException e) {
			throw new RuntimeException("Can't open input file");
		}
		
		try {
		    header = Header.read(input);
		} catch (ParseException e) {
			throw new RuntimeException("ParseException creating header for the input", e);			
		} catch (IOException e) {
			throw new RuntimeException("IOException creating header for the input", e);
		}
		
		rowStore = RowStore.createReadOnly(input.getChannel(), header.getHeaderSize(), 
				                           header.getColumns().size(), header.getNumRecords());
	}
}
