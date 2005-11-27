package com.dumbhippo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtils {

	public static String readStreamUTF8(InputStream input) throws IOException {
		InputStreamReader reader = new InputStreamReader(input, "UTF-8");
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[256];
		int numRead = reader.read(buf);
		while (numRead >= 0) {
			sb.append(buf, 0, numRead);
			numRead = reader.read(buf);
		}
		return sb.toString();
	}
	
}
