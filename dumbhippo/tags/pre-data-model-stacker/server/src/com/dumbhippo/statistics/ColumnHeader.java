package com.dumbhippo.statistics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

// Format:
//  COLUMN_HEADER_SIZE   4
//  UNIT                 4
//  TYPE                 4
//  ID_LENGTH            4
//  ID                   PAD(ID_LENGTH)
//  NAME_LENGTH          4
//  NAME                 PAD(NAME_LENGTH)

/**
 * The ColumnHeader class represents a column read from disk
 * the class includes static methods for both reading and writing columns.
 * @author otaylor
 */
public class ColumnHeader implements ColumnDescription {
	private String id;
	private String name;
	private ColumnUnit units;
	private ColumnType type;

	/**
	 * Read a single column from a serialized representation
	 * @param input input source to read the data from 
	 * @return the read column, will always return a column or throw an exception
	 * @throws IOException
	 * @throws ParseException
	 */
	static public ColumnHeader read(DataInput input) throws IOException, ParseException {
		// See format documented in Header.java
		ColumnHeader result = new ColumnHeader();
		
		int length = input.readInt();
		
		if (length < 4)
			throw new ParseException("Bad column header length");
		
		byte[] buffer = new byte[length - 4];
		input.readFully(buffer);
		
		DataInputStream columnStream = new DataInputStream(new ByteArrayInputStream(buffer));
		result.units = FileUtils.readEnum(columnStream, ColumnUnit.class);
		result.type = FileUtils.readEnum(columnStream, ColumnType.class);
		result.id = FileUtils.readString(columnStream);
		result.name = FileUtils.readString(columnStream);
		
		return result;
	}
	
	/**
	 * Write a column into a serialized representation 
	 * @param output output sink to write the column to
	 * @param description the column to write
	 * @throws IOException
	 */
	static public void write(DataOutput output, ColumnDescription description) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream columnStream = new DataOutputStream(byteStream);
		
		FileUtils.writeEnum(columnStream, description.getUnits());
		FileUtils.writeEnum(columnStream, description.getType());
		FileUtils.writeString(columnStream, description.getId());
		FileUtils.writeString(columnStream, description.getName());
		
		byte[] bytes = byteStream.toByteArray();
		output.writeInt(4 + bytes.length);
		output.write(bytes);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public ColumnUnit getUnits() {
		return units;
	}

	public ColumnType getType() {
		return type;
	}

}
