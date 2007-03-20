package com.dumbhippo.server.downloads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dumbhippo.persistence.ValidationException;

public class Download {
	private List<Field> matchFields = new ArrayList<Field>();
	private Map<String, Field> substituteFields = new HashMap<String, Field>();
	
	String url;
	
	private void addField(Field parameter, boolean matchAgainst) {
		substituteFields.put(parameter.getName(), parameter);
		if (matchAgainst)
			matchFields.add(parameter);
	}
	
	private void addLiteralField(String name, String value, boolean matchAgainst) {
		addField(new LiteralField(name, value), matchAgainst);
	}
	
	private void addGlobField(String name, String value, boolean matchAgainst) {
		if (value.indexOf("*") >= 0 || value.indexOf("?") >= 0)
			addField(new GlobField(name, value), matchAgainst);
		else
			addField(new ListField(name, value), matchAgainst);
	}
	
	public Download(DownloadPlatform platform, String distribution, String osVersion, String architecture, String release) {
		addLiteralField("platform", platform.getName(), true);
		addLiteralField("version", platform.getVersion(), false);

		if (distribution != null)
			addGlobField("distribution", distribution, true);
		if (osVersion != null)
			addGlobField("osVersion", osVersion, true);
		if (architecture != null)
			addGlobField("architecture", architecture, true);
		
		if (release != null)
			addLiteralField("release", release, false);
	}
	
	private Map<String, String> makeParameters(String platform, String distribution, String osVersion, String architecture) {
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		if (platform != null)
			parameters.put("platform", platform);
		if (distribution != null)
			parameters.put("distribution", distribution);
		if (osVersion != null)
			parameters.put("osVersion", osVersion);
		if (architecture != null)
			parameters.put("architecture", architecture);
		
		return parameters;
	}
	
	public boolean matches(Map<String, String> parameters) {
		for (Field field : matchFields) {
			String parameter = parameters.get(field.getName());
			if (parameter == null)
				return false;
			
			if (!field.matches(parameter))
				return false;
		}
		
		return true;
	}

	public boolean matches(String platform, String distribution, String osVersion, String architecture) {
		return matches(makeParameters(platform, distribution, osVersion, architecture));
	}
	
	private static final Pattern PARAMETER_REGEX = Pattern.compile("%\\{([a-zA-z]+)\\}");
	
	private String escapeReplacement(String replacement) {
		return replacement.replace("$", "\\$");
	}
	
	public String expand(String toExpand, Map<String, String> parameters) {
		StringBuffer result = new StringBuffer();
		Matcher m = PARAMETER_REGEX.matcher(toExpand);
		
		while (m.find()) {
			String fieldName = m.group(1);
			Field field = substituteFields.get(fieldName);

			if (parameters.containsKey(fieldName))
				m.appendReplacement(result, escapeReplacement(parameters.get(fieldName)));
			else {
				m.appendReplacement(result, escapeReplacement(((LiteralField)field).getValue()));
			}
		}
		
		m.appendTail(result);
		
		return result.toString();
	}
	
	public String getUrl(String platform, String distribution, String osVersion, String architecture) {
		return expand(url, makeParameters(platform, distribution, osVersion, architecture));
	}
	
	private void checkExpansion(String toExpand) throws ValidationException {
		Matcher m = PARAMETER_REGEX.matcher(toExpand);
		
		while (m.find()) {
			String fieldName = m.group(1);
			Field field = substituteFields.get(fieldName); 
			
			if (field == null)
				throw new ValidationException("Parameter '" + fieldName + "' in expansion isn't recognized");
			if (field.isGlobbed())
				throw new ValidationException("Expansion of globbed parameter '" + fieldName + "' isn't allowed");
		}
		
	}

	public void setUrl(String url) throws ValidationException {
		checkExpansion(url);
		this.url = url;
	}
	
	///////////////////////////////////////////////////////////////////////
	
	static final private Pattern SEPARATOR_REGEX = Pattern.compile("\\s*,\\s*");
	
	private static abstract class Field {
		String name;
		
		Field(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isGlobbed() {
			return false;
		}
		
		public abstract boolean matches(String str);
	}
	
	private static class LiteralField extends Field {
		String value;
		
		public LiteralField(String name, String str) {
			super(name);
			this.value = str;
		}
		
		public String getValue() {
			return value;
		}
		
		@Override
		public boolean matches(String str) {
			return value.equals(str);
		}
	}
	
	private static class ListField extends Field {
		String[] values;

		public ListField(String name, String str) {
			super(name);
			values = SEPARATOR_REGEX.split(str);
		}

		@Override
		public boolean matches(String str) {
			for (int i = 0 ; i < values.length; i++)
				if (values[i].equals(str))
					return true;
			
			return false;
		}
	}
	
	private static class GlobField extends Field {
		private Pattern[] patterns;
		
		private GlobField(String name, String str) {
			super(name);
			String[] globs = SEPARATOR_REGEX.split(str);
			patterns = new Pattern[globs.length];
			for (int i = 0; i < globs.length; i++)
				patterns[i] = patternFromGlob(globs[i].trim());
		}
		
		private static Pattern patternFromGlob(String glob) {
			StringBuilder pat = new StringBuilder();
			
			for (int i = 0; i < glob.length(); i++) {
				char c = glob.charAt(i);
				switch (c) {
				case '*':
					pat.append(".*");
					break;
				case '?':
					pat.append('.');
					break;
				case '+':
				case '|':
				case '(':
				case ')':
				case '{':
				case '}':
				case '$':
				case '^':
				case '\\':
					pat.append('\\');
					pat.append(c);
					break;
				default:
					pat.append(c);
					break;
				}
			}
			
			return Pattern.compile(pat.toString());
		}

		@Override
		public boolean matches(String str) {
			for (int i = 0; i < patterns.length; i++)
				if (patterns[i].matcher(str).matches())
					return true;
			
			return false;
		}

		@Override
		public boolean isGlobbed() {
			return true;
		}
	}


}
