package com.dumbhippo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

public class StringUtils {
	public static String hexEncode(byte[] strs) {
		StringWriter str = new StringWriter();
		PrintWriter out = new PrintWriter(str);
		for (int i = 0; i < strs.length; i++) {
			int first = (byte) ((strs[i] & 0xF0) >> 4);
			int second = (byte) (strs[i] & 0x0F);
			out.printf("%x%x", first, second);
		}
		
		return str.toString();
	}
	
	public static String join(List<String> strings, String separator) {
		StringBuilder builder = new StringBuilder();
		Iterator<String> iterator = strings.iterator();
		while (iterator.hasNext()) {
			boolean hasNext = true;
			String s = iterator.next();
			builder.append(s);
			if (hasNext)
				builder.append(separator);
		}
		return builder.toString();
	}
}
