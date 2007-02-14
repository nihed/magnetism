package com.dumbhippo.server.applications;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import com.dumbhippo.persistence.ValidationException;

public class AppinfoFile extends JarFile {
	private Properties properties;
	private String appId;
	private String name;
	private String description;
	private Set<String> categories;
	private Set<String> wmClasses;
	private Set<String> titlePatterns;
	private Set<AppinfoIcon> icons;
	
	static final private Pattern ID_REGEX = Pattern.compile("[A-Za-z0-9-]+");
	static final private Pattern NAME_REGEX = Pattern.compile(".+");
	static final private Pattern DESCRIPTION_REGEX = null;
	static final private Pattern CATEGORY_REGEX = Pattern.compile("[A-Za-z0-9-]+");
	static final private Pattern WM_CLASS_REGEX = Pattern.compile(".+");
	static final private Pattern TITLE_PATTERN_REGEX = Pattern.compile(".+");
	
	static final private Pattern ICON_KEY_REGEX = Pattern.compile("icon.([A-Za-z0-9-_]+)(?:\\.(\\d+x\\d+|scalable))?");

	public AppinfoFile(File file) throws IOException, ValidationException {
		super(file);
		
		try {
			readProperties();
		} catch (IOException e) {
			close();
			throw e;
		} catch (ValidationException e) {
			close();
			throw e;
		} catch (RuntimeException e) {
			close();
			throw e;
		}
	}
	
	private void readProperties() throws ValidationException, IOException {
		ZipEntry propertiesEntry = getEntry("application.properties");
		if (propertiesEntry == null) 
			throw new ValidationException("appinfo file doesn't contain application.properties");

		InputStream propertiesStream = getInputStream(propertiesEntry);
		properties = new Properties();
		properties.load(propertiesStream);
		propertiesStream.close();
		
		appId = getStringProperty("id", ID_REGEX);
		name = getStringProperty("name", NAME_REGEX);
		description = getStringProperty("description", DESCRIPTION_REGEX);
		categories = getSetProperty("categories", CATEGORY_REGEX);
		wmClasses = getSetProperty("wmclass", WM_CLASS_REGEX);
		titlePatterns = getSetProperty("titlepattern", TITLE_PATTERN_REGEX);
		
		icons = new HashSet<AppinfoIcon>();
		for (Object o : properties.keySet()) {
			String key = (String)o;
			
			if (key.startsWith("icon.")) {
				Matcher m = ICON_KEY_REGEX.matcher(key);
				if (!m.matches()) 
					throw new ValidationException("Invalid icon property key '" + key+ '"');
				String theme = m.group(1);
				if ("generic".equals(theme))
					theme = null;
				String size = m.group(2);
				
				String path = properties.getProperty(key).trim();
				if (getEntry(path) == null)
					throw new ValidationException("icon property '" + key + "' doesn't point to an icon in the appinfo file");
				
				icons.add(new AppinfoIcon(theme,size, path));
			}
		}
	}
	
	private String getStringProperty(String propertyName, Pattern mustMatch) throws ValidationException {
		String str = properties.getProperty(propertyName);
		
		if (str == null)
			throw new ValidationException("Property '" + propertyName + "' is required");
		
		str = str.trim();
		
		if (mustMatch != null && !mustMatch.matcher(str).matches())
			throw new ValidationException("'" + str + "' is not a valid value for property '" + propertyName + "'");
		
		return str;
	}
	
	private Set<String> getSetProperty(String propertyName, Pattern mustMatch) throws ValidationException {
		String str = properties.getProperty(propertyName);
		
		if (str != null) {
			str = str.trim();
			
			String[] values = str.split("\\s*;\\s*");
			Set<String> result = new HashSet<String>();
			for (String v : values) {
				if (mustMatch != null && !mustMatch.matcher(v).matches())
					throw new ValidationException("'" + str + "' is not a valid value for property '" + propertyName + "'");

				result.add(v);
			}
			
			return result;
		} else{
			return Collections.emptySet();
		}
	}
	
	public String getAppId() {
		return appId;
	}
	
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public Set<String> getWmClasses() {
		return wmClasses;
	}

	public Set<String> getTitlePatterns() {
		return titlePatterns;
	}
	
	public Set<AppinfoIcon> getIcons() {
		return icons;
	}
	
	public InputStream getIconStream(AppinfoIcon info) throws IOException {
		ZipEntry entry = getEntry(info.getPath());
		if (entry == null)
			throw new IOException("Entry not found for '" + info.getPath() + "'");
		
		return getInputStream(entry);
	}
}
