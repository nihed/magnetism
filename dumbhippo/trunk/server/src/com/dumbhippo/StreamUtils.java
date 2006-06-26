package com.dumbhippo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtils {

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
		try {
			InputStreamReader reader = new InputStreamReader(input, "UTF-8");
			StringBuilder sb = new StringBuilder();
			char[] buf = new char[256];
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
			int numRead = input.read(buf);
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
}
