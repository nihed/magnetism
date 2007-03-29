package com.dumbhippo.server.downloads;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dumbhippo.persistence.ValidationException;

public class Download {
	private Map<String, Field> matchFields = new HashMap<String, Field>();
	private Map<String, Field> substituteFields = new HashMap<String, Field>();
	
	private DownloadDistribution distribution;
	private String url;
	private String architecture;
	
	public Download(DownloadDistribution distribution) {
		this.distribution = distribution;
		
		DownloadPlatform platform = distribution.getPlatform();
		
		addLiteralField("platform", platform.getName(), true, true);
		addLiteralField("version", platform.getVersion(), false, true);

		if (distribution.getName() != null)
			addLiteralField("distribution", distribution.getName(), true, true);

		if (distribution.getOsVersionPattern() != null) {
			addPatternField("osVersion", distribution.getOsVersionPattern(), true, false);
			addLiteralField("osVersion", distribution.getOsVersionPattern(), false, true);
		} else if (distribution.getOsVersion() != null) {
			addLiteralField("osVersion", distribution.getOsVersion(), true, true);
		}
		
		if (distribution.getRelease() != null)
			addLiteralField("release", distribution.getRelease(), false, true);
	}
	
	public DownloadDistribution getDistribution() {
		return distribution;
	}
	
	public void setArchitecture(String architecture) {
		this.architecture = architecture;
		
		if (architecture != null)
			addLiteralField("architecture", architecture, true, true);
		else
			removeField("architecture", true, true);
	}
	
	public String getArchitecture() {
		return architecture;
	}

	public String getUrl() {
		return expand(url);
	}
	
	public void setUrl(String url) throws ValidationException {
		checkExpand(url);
		this.url = url;
	}
	
	///////////////////////////////////////////////////////////////////////
	
	private void addField(Field parameter, boolean matchAgainst, boolean substitute) {
		if (matchAgainst)
			matchFields.put(parameter.getName(), parameter);
		if (substitute)
			substituteFields.put(parameter.getName(), parameter);
	}
	
	private void removeField(String name, boolean matchAgainst, boolean substitute) {
		if (matchAgainst)
			matchFields.remove(name);
		if (substitute)
			substituteFields.remove(name);
	}

	private void addLiteralField(String name, String value, boolean matchAgainst, boolean substitute) {
		addField(new LiteralField(name, value), matchAgainst, substitute);
	}
	
	private void addPatternField(String name, String value, boolean matchAgainst, boolean substitute) {
		if (substitute)
			throw new RuntimeException("Cannot substitute on pattern field");
		
		if (value.indexOf("*") >= 0 || value.indexOf("?") >= 0)
			addField(new PatternField(name, value), matchAgainst, substitute);
		else
			addField(new LiteralField(name, value), matchAgainst, substitute);
	}
	
	public boolean matches(Map<String, String> parameters) {
		for (Field field : matchFields.values()) {
			String parameter = parameters.get(field.getName());
			if (parameter == null)
				return false;
			
			if (!field.matches(parameter))
				return false;
		}
		
		return true;
	}

	public boolean matches(String platform, String distribution, String osVersion, String architecture) {
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		if (platform != null)
			parameters.put("platform", platform);
		if (distribution != null)
			parameters.put("distribution", distribution);
		if (osVersion != null)
			parameters.put("osVersion", osVersion);
		if (architecture != null)
			parameters.put("architecture", architecture);
		
		return matches(parameters);
	}
	
	private static final Pattern PARAMETER_REGEX = Pattern.compile("%\\{([a-zA-z]+)\\}");
	
	public void checkExpand(String toExpand) throws ValidationException {
		Matcher m = PARAMETER_REGEX.matcher(toExpand);
		
		while (m.find()) {
			String fieldName = m.group(1);
			Field field = substituteFields.get(fieldName);

			if (field == null)
				throw new ValidationException("Parameter '" + fieldName + "' in expansion isn't recognized");
		}
	}
	
	private String escapeReplacement(String replacement) {
		return replacement.replace("$", "\\$");
	}
	
	public String expand(String toExpand) {
		StringBuffer result = new StringBuffer();
		Matcher m = PARAMETER_REGEX.matcher(toExpand);
		
		while (m.find()) {
			String fieldName = m.group(1);
			Field field = substituteFields.get(fieldName);

			m.appendReplacement(result, escapeReplacement(((LiteralField)field).getValue()));
		}
		
		m.appendTail(result);
		
		return result.toString();
	}
	
	///////////////////////////////////////////////////////////////////////
	
	private static abstract class Field {
		String name;
		
		Field(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
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
	
	private static class PatternField extends Field {
		private Pattern pattern;
		
		private PatternField(String name, String str) {
			super(name);
			pattern = Pattern.compile(str);
		}
		
		@Override
		public boolean matches(String str) {
			return pattern.matcher(str).matches();
		}
	}
}
