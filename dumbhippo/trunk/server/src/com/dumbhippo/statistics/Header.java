package com.dumbhippo.statistics;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

// Format of header is:
//  MAGIC (0x30655407)      4
//  HEADER_SIZE             4 
//  VERSION (0x00010000)    4
//  N_COLUMNS               4
//  START_TIME              8
//  INTERVAL                8
//  N_RECORDS               8
//  HOST_NAME_LENGTH        4
//  HOST_NAME               PAD(HOST_NAME_LENGTH)
//  COLUMN_HEADER (x N_COLUMNS) (See ColumnHeader.java)

/**
 * The Header class represents global information stored for an entire
 * data set. As well as accessors for information such as the hostname
 * and data collection interval, it contains methods to read and write
 * the header information to disk. 
 */
public class Header {
	static private final int MAGIC = 0x30655407; 
	static private final int VERSION = 0x00010000;
	static private final int N_RECORDS_OFFSET = 32;
	
	private ColumnMap columns;
	private String hostName;
	@SuppressWarnings("unused")
	private int version = VERSION;
	private int headerSize;
	private long numRecords;
	private Date startTime ;
	private long interval;
	
	public Header() {
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public int getHeaderSize() {
		return headerSize;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public long getNumRecords() {
		return numRecords;
	}

	public void setNumRecords(long records) {
		numRecords = records;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	
	public void setColumns(ColumnMap columns) {
		this.columns = columns;
	}

	static public Header read(DataInput input) throws IOException, ParseException  {
		Header result = new Header();
		
		int magic = input.readInt();
		if (magic != MAGIC) {
			throw new ParseException("Unrecognized file format");
		}
		
		result.headerSize = input.readInt();
		if (result.headerSize < 8)
			throw new ParseException("Bad header length");
		
		result.version = input.readInt();
		
		int nColumns = input.readInt();
		if (nColumns < 0)
			throw new ParseException("Bad number of columns");
		
		result.startTime = new Date(input.readLong());
		result.interval = input.readLong();
		result.numRecords = input.readLong();
		if (result.numRecords < 0)
			throw new ParseException("Bad number of records");
		
		result.hostName = FileUtils.readString(input);
		
		result.columns = new ColumnMap();
		for (int i = 0; i < nColumns; i++)
			result.columns.add(ColumnHeader.read(input));
		
		return result;
	}

	public void write(DataOutput output) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream headerStream = new DataOutputStream(byteStream);
		
		headerStream.writeInt(VERSION);
		headerStream.writeInt(columns.size());
		headerStream.writeLong(startTime.getTime());
		headerStream.writeLong(interval);
		headerStream.writeLong(numRecords);
		FileUtils.writeString(headerStream, hostName);
		
		for (ColumnDescription column : columns) {
			ColumnHeader.write(headerStream, column);
		}

		byte[] bytes = byteStream.toByteArray();
		headerSize = (8 + bytes.length + 7) & ~7;
		
		output.writeInt(MAGIC);
		output.writeInt(headerSize);
		output.write(bytes);
		
		if (headerSize > bytes.length + 8) {
			byte[] padBytes = new byte[] { 0, 0, 0, 0, 0, 0, 0 };
			output.write(padBytes, 0, headerSize - (bytes.length + 8));
		}

	}
	
	public void update(MappedByteBuffer buffer) {
		buffer.putLong(N_RECORDS_OFFSET, numRecords);
	}
	
	public String makeFilename() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startTime);
		
		Formatter formatter = new Formatter();
		formatter.format("statistics/%s-%04d%02d%02d-%02d:%02d:%02d.stats",
						 hostName,
					     calendar.get(Calendar.YEAR),
					     calendar.get(Calendar.MONTH),
					     calendar.get(Calendar.DAY_OF_MONTH),
					     calendar.get(Calendar.HOUR),
					     calendar.get(Calendar.MINUTE),
					     calendar.get(Calendar.SECOND));

		return formatter.toString();
	}
}
