package com.dumbhippo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class StreamUtils {

	public static final int ONE_KILOBYTE = 1024;
	public static final int ONE_KILOCHAR = 1024;
	public static final int ONE_MEGABYTE = 1024 * 1024;
	public static final int ONE_MEGACHAR = 1024 * 1024;
	
	/**
	 * Slurps a stream into a string, taking the stream as UTF-8. Stops after maxChars. 
	 * Always closes the stream after slurping, whether it succeeds or not
	 * 
	 * @param input the input stream
	 * @param maxChars max chars to slurp
	 * @return the slurped string
	 * @throws IOException if anything goes wrong (also closes the stream...)
	 */
	public static String readStreamUTF8(InputStream input, int maxChars) throws IOException {
		char[] buf = new char[256];		
		try {
			InputStreamReader reader = new InputStreamReader(input, "UTF-8");
			StringBuilder sb = new StringBuilder();
			int numRead = reader.read(buf);
			while (numRead >= 0 && sb.length() < maxChars) {
				sb.append(buf, 0, numRead);
				numRead = reader.read(buf);
			}
			if (sb.length() > maxChars)
				sb.setLength(maxChars);
			return sb.toString();
		} finally {
			input.close();
		}
	}

	public static String readStreamUTF8(InputStream input) throws IOException {
		return readStreamUTF8(input, Integer.MAX_VALUE);
	}
	
	public static byte[] readStreamBytes(InputStream input, long maxBytes) throws IOException {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[256];
			int numRead = 0;
			while (numRead >= 0 && out.size() < maxBytes) {
				long remaining = maxBytes - out.size();
				if (numRead > remaining)
					numRead = (int) remaining;
				out.write(buf, 0, numRead);
				numRead = input.read(buf);
			}
			return out.toByteArray();
		} finally {
			input.close();
		}
	}
	
	/**
	 * Copies the input stream to the output stream. Does not close or flush 
	 * either stream.
	 * 
	 * @param input
	 * @param output
	 * @return number of bytes copied
	 * @throws IOException
	 */
	public static long copy(InputStream input, OutputStream output) throws IOException {
		return copy(input, output, Long.MAX_VALUE);
	}
	
	/**
	 * Copies the input to the output stream. Does not close or flush either stream.
	 * Copies at most maxSize bytes.
	 * 
	 * @param input
	 * @param output
	 * @param maxSize
	 * @return
	 * @throws IOException
	 */
	public static long copy(InputStream input, OutputStream output, long maxSize) throws IOException {
		long total = 0;
		byte[] buf = new byte[512];
		int numRead = 0;
		while (numRead >= 0 && total < maxSize) {
			long remaining = maxSize - total;
			if (numRead > remaining)
				numRead = (int) remaining;
			output.write(buf, 0, numRead);
			total += numRead;
			numRead = input.read(buf, 0, (int) Math.min(buf.length, remaining));
		}
		return total;
	}
}
