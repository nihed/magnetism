package com.dumbhippo.identity20;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.dumbhippo.Base64;


public class AuthenticationToken implements Serializable {
	private static final long serialVersionUID = 1L;
	private Date startTime;
	private Date endTime;

	public AuthenticationToken(int milliseconds) {
		startTime = new Date();
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(startTime);
		cal.add(GregorianCalendar.MILLISECOND, milliseconds);
		endTime = cal.getTime();
	}
	
	public AuthenticationToken(String serialized) {
		parseString(serialized);
	}
	
	private void parseString(String serialized) throws IllegalArgumentException {
		int idx = serialized.indexOf('/');
		if (idx < 0)
			throw new IllegalArgumentException("Invalid serialized authentication token \"" + serialized + "\"");
		try {
			startTime = parseLongDate(serialized.substring(0, idx));
			endTime = parseLongDate(serialized.substring(idx + 1));
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Invalid serialized authentication token \"" + serialized + "\"");
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid serialized authentication token \"" + serialized + "\"");
		}
	}

	private Date parseLongDate(String str) {	
		long startMillis = Long.parseLong(str);
		return new Date(startMillis);
	}

	public boolean isValid(Date d) {
		return (startTime.compareTo(d) <= 0
				&& endTime.compareTo(d) > 0);
	}
	
	@Override
	public String toString() {
		return startTime.getTime() + "/" + endTime.getTime();
	}
	
	public String toBase64String() {
		String strForm = toString();
		return Base64.encodeBytes(strForm.getBytes());
	}

}
