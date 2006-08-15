package com.dumbhippo.statistics;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

class FileUtils {
	/**
	 * Read a string from an input stream in the format we use for serialization:
	 * UTF-8 data proceeded by a 32-bit byte length, and padded out to a multiples
	 * of 32-bits.
	 * @param input input stream to read from
	 * @return the read string
	 * @throws IOException
	 * @throws ParseException
	 */
	static public String readString(DataInput input) throws IOException, ParseException {
		int length = input.readInt();
		
		int paddedLength = (length + 3) & ~3;
		if (length < 0 || paddedLength < 0)
			throw new ParseException("Bad Length");
		
		byte[] bytes = new byte[length];
		input.readFully(bytes);
		
		CharBuffer result = Charset.forName("UTF-8").decode(ByteBuffer.wrap(bytes));
		input.skipBytes(paddedLength - length);
		
		return result.toString();
	}
	
	/**
	 * Write a string to an output stream in our serialization format. See
	 * {@link readString}.
	 * @param output stream to write to
	 * @param s string to write
	 * @throws IOException
	 */
	static void writeString(DataOutput output, String s) throws IOException {
		ByteBuffer buffer = Charset.forName("UTF-8").encode(s);
		byte[] bytes = buffer.array();
		int paddedLength = ((bytes.length + 3) & ~3);
		output.writeInt(bytes.length);
		output.write(bytes);
		if (paddedLength > bytes.length) {
			byte[] padBytes = new byte[] { 0, 0, 0 };
			output.write(padBytes, 0, paddedLength - bytes.length);
		}
	}

	/**
	 * Read an enumeration value from an input stream. The enumeration is serialized as
	 * a 32-bit integer holding the ordinal value of the enumeration
	 * @param <T>
	 * @param input input stream to read from
	 * @param clazz enumeration class
	 * @return the enumeration vlaue
	 * @throws IOException
	 * @throws ParseException
	 */
	static public <T extends Enum> T readEnum(DataInput input, Class<T> clazz) throws IOException, ParseException {
		int intValue = input.readInt();
		for (T t : clazz.getEnumConstants()) {
			if (t.ordinal() == intValue)
				return t;
		}
		
		throw new ParseException("Enum value " + intValue + "unknown for enum " + clazz.getName());
	}

	/**
	 * Write an enumeration value to an ouput stream; see {@link readEnum}.
	 * @param output the output stream to write to
	 * @param e
	 * @throws IOException
	 */
	static void writeEnum(DataOutput output, Enum e) throws IOException {
		output.writeInt(e.ordinal());
	}
}
