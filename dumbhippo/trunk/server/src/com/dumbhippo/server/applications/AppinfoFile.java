package com.dumbhippo.server.applications;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import com.dumbhippo.StreamUtils;
import com.dumbhippo.persistence.ValidationException;

public class AppinfoFile extends JarFile {
	private Properties properties;
	private String appId;
	private String name;
	private String description;
	private String desktopPath;
	private Set<String> categories;
	private Set<String> wmClasses;
	private Set<String> titlePatterns;
	private List<String> desktopNames;
	private List<AppinfoIcon> icons;
	
	static final private Pattern ID_REGEX = Pattern.compile("[A-Za-z0-9-.]+");
	static final private Pattern NAME_REGEX = Pattern.compile(".+");
	static final private Pattern DESCRIPTION_REGEX = null;
	static final private Pattern CATEGORY_REGEX = Pattern.compile("[A-Za-z0-9-]+");
	static final private Pattern WM_CLASS_REGEX = Pattern.compile(".+");
	static final private Pattern TITLE_PATTERN_REGEX = Pattern.compile(".+");
	static final private Pattern DESKTOP_NAME_REGEX = ID_REGEX;
	
	static final private Pattern ICON_KEY_REGEX = Pattern.compile("icon.([A-Za-z0-9-_]+)(?:\\.(\\d+x\\d+|scalable))?");
	
	static final private Pattern DESKTOP_REGEX = Pattern.compile("(?:.*/)?([A-Za-z0-9-.]+)\\.desktop");

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
		
		appId = getStringProperty("id", ID_REGEX, false);
		name = getStringProperty("name", NAME_REGEX, false);
		description = getStringProperty("description", DESCRIPTION_REGEX, true);
		categories = getSetProperty("categories", CATEGORY_REGEX);
		wmClasses = getSetProperty("wmclass", WM_CLASS_REGEX);
		titlePatterns = getSetProperty("titlepattern", TITLE_PATTERN_REGEX);
		desktopNames = getListProperty("desktopnames", DESKTOP_NAME_REGEX);
		desktopPath = getStringProperty("desktop", DESKTOP_REGEX, false);

		if (getEntry(desktopPath) == null)
			throw new ValidationException("desktop property doesn't point to a file in the appinfo file");

		// We form a fallback value of desktopNames from the application ID
		// and from the name of the desktop file in the appinfo file, if any.
		if (desktopNames.isEmpty()) {
			desktopNames = new ArrayList<String>();
			String desktopName = null;

			if (desktopPath != null) {
				Matcher m = DESKTOP_REGEX.matcher(desktopPath);
				if (!m.matches()) 
					throw new RuntimeException("desktop path no longer matches???");
				desktopName = m.group(1);
				desktopNames.add(desktopName);
			}

			if (desktopName == null || !desktopName.equals(appId))
				desktopNames.add(appId);
		}

		icons = new ArrayList<AppinfoIcon>();
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
		
		Collections.sort(icons, new Comparator<AppinfoIcon>() {
			private int compareStrings(String a, String b) {
				if (a == b || (a != null && a.equals(b)))
					return 0;
				else if (a == null)
					return -1;
				else if (b == null)
					return 1;
				else
					return a.compareTo(b);
			}
			
			private int getSize(AppinfoIcon icon) {
				if ("scalable".equals(icon.getSize()))
					return Integer.MAX_VALUE;
				else
					return icon.getNominalSize();
			}
			
			private int compareSize(AppinfoIcon a, AppinfoIcon b) {
				int sizeA = getSize(a);
				int sizeB = getSize(b);
				
				return sizeA < sizeB ? -1 : (sizeA == sizeB ? 0 : 1);
			}

			public int compare(AppinfoIcon a, AppinfoIcon b) {
				int o = compareStrings(a.getTheme(), b.getTheme());
				if (o != 0)
					return o;
				return compareSize(a, b);
			}
		});
	}

	private String getStringProperty(String propertyName, Pattern mustMatch, boolean nullOk) throws ValidationException {
		String str = properties.getProperty(propertyName);
		
		return validateString(str, propertyName, mustMatch, nullOk);
		
	}
	
	private Set<String> getSetProperty(String propertyName, Pattern mustMatch) throws ValidationException {
		return setFromString(properties.getProperty(propertyName), propertyName, mustMatch);
	}
	
	private List<String> getListProperty(String propertyName, Pattern mustMatch) throws ValidationException {
		return listFromString(properties.getProperty(propertyName), propertyName, mustMatch);
	}
	
	private void setProperty(String propertyName, String value) {
		if (value != null)
			properties.setProperty(propertyName, value);
		else
			properties.remove(propertyName);
	}
	
	public String getAppId() {
		return appId;
	}
	
	// THIS IS NOT A REAL OVERRIDE, but I don't care about the JarFile method
	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name) throws ValidationException {
		this.name = validateString(name, "name", NAME_REGEX, false);
		setProperty("name", getName());
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) throws ValidationException {
		this.description = validateString(name, "description", DESCRIPTION_REGEX, true);
		setProperty("description", getDescription());
	}

	public Set<String> getCategories() {
		return categories;
	}
	
	public String getCategoriesString() {
		return setToString(getCategories());
	}

	public void setCategoriesString(String categories) throws ValidationException {
		this.categories = setFromString(categories, "categories", CATEGORY_REGEX);
		setProperty("categories", getCategoriesString());
	}

	public Set<String> getWmClasses() {
		return wmClasses;
	}
	
	public String getWmClassesString() {
		return setToString(wmClasses);
	}

	public void setWmClassesString(String wmClasses) throws ValidationException {
		this.wmClasses = setFromString(wmClasses, "wmClasses", WM_CLASS_REGEX);
		setProperty("wmclass", getWmClassesString());
	}

	public Set<String> getTitlePatterns() {
		return titlePatterns;
	}
	
	public String getTitlePatternsString() {
		return setToString(titlePatterns);
	}

	public void setTitlePatternsString(String titlePatterns) throws ValidationException {
		this.titlePatterns = setFromString(titlePatterns, "titlePatterns", TITLE_PATTERN_REGEX);
		setProperty("titlepattern", getTitlePatternsString());
	}

	public List<String> getDesktopNames() {
		return desktopNames;
	}
	
	public String getDesktopNamesString() {
		return listToString(desktopNames);
	}

	public void setDesktopNamesString(String desktopNames) throws ValidationException {
		this.desktopNames = listFromString(desktopNames, "desktopNames", DESKTOP_NAME_REGEX);
		setProperty("desktopnames", getDesktopNamesString());
	}

	public List<AppinfoIcon> getIcons() {
		return icons;
	}
	
	private String iconPropertyName(String theme, String size) {
		StringBuilder name = new StringBuilder("icon.");
		
		if (theme != null)
			name.append(theme);
		else
			name.append("generic");
		
		if (size != null) {
			name.append(".");
			name.append(size);
		}
		
		return name.toString();
	}
	
	public void deleteIcon(String theme, String size) {
		for (int i = 0; i < icons.size(); i++) {
			if (icons.get(i).matches(theme, size)) {
				icons.remove(i);
				properties.remove(iconPropertyName(theme, size));
				break;
			}
		}
	}
	
	public void addIcon(String theme, String size, String extension, byte[] contents) {
		deleteIcon(theme, size);
		
		StringBuilder path = new StringBuilder();
		
		if (theme != null)
			path.append(theme + "/");
		else
			path.append("generic/");
		
		if (size != null)
			path.append(size + "/");
		
		path.append(appId);
		path.append(".");
		path.append(extension);
		
		AppinfoIcon icon = new AppinfoIcon(theme, size, path.toString());
		icon.setContents(contents);
		
		icons.add(icon);

		properties.setProperty(iconPropertyName(theme, size), path.toString());
	}
	
	public InputStream getIconStream(AppinfoIcon info) throws IOException {
		if (info.getContents() != null)
			return new ByteArrayInputStream(info.getContents());
		
		ZipEntry entry = getEntry(info.getPath());
		if (entry == null)
			throw new IOException("Entry not found for '" + info.getPath() + "'");
		
		return getInputStream(entry);
	}
	
	public void write(OutputStream out) throws IOException {
		JarOutputStream jarstream = new JarOutputStream(out);
		ZipEntry entry;
		
		entry = new ZipEntry("application.properties");
		jarstream.putNextEntry(entry);
		properties.store(jarstream, null);
		
		if (desktopPath != null) {
			entry = new ZipEntry(desktopPath);
			jarstream.putNextEntry(entry);
			
			ZipEntry inEntry = getEntry(desktopPath);
			if (entry == null)
				throw new IOException("Entry not found for '" + desktopPath + "'");
			
			InputStream in = getInputStream(inEntry);
			StreamUtils.copy(in, jarstream);
			in.close();
		}
		
		for (AppinfoIcon icon : icons) {
			entry = new ZipEntry(icon.getPath());
			jarstream.putNextEntry(entry);
			
			InputStream in = getIconStream(icon);
			StreamUtils.copy(in, jarstream);
			in.close();
		}
		
		jarstream.finish();
	}
	
	////////////////////////////////////////////////////
	
	private String validateString(String str, String propertyName, Pattern mustMatch, boolean nullOk) throws ValidationException {
		if (str == null) {
			if (nullOk)
				return null;
			
			throw new ValidationException("Property '" + propertyName + "' is required");
		}
		
		str = str.trim();
		
		if (mustMatch != null && !mustMatch.matcher(str).matches())
			throw new ValidationException("'" + str + "' is not a valid value for property '" + propertyName + "'");
		
		return str;
	}
	
	private List<String> listFromString(String str, String propertyName, Pattern mustMatch) throws ValidationException {
		if (str != null) {
			str = str.trim();
			if (str.length() == 0)
				return Collections.emptyList();
			
			String[] values = str.split("\\s*;\\s*");
			List<String> result = new ArrayList<String>();
			for (String v : values) {
				if (mustMatch != null && !mustMatch.matcher(v).matches())
					throw new ValidationException("'" + str + "' is not a valid value for property '" + propertyName + "'");

				result.add(v);
			}
			
			return result;
		} else{
			return Collections.emptyList();
		}
	}
	
	private Set<String> setFromString(String str, String propertyName, Pattern mustMatch) throws ValidationException {
		if (str != null) {
			str = str.trim();
			if (str.length() == 0)
				return Collections.emptySet();
			
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
	
	private String listToString(List<String> list) {
		if (list.isEmpty())
			return null;
		
		StringBuilder builder = new StringBuilder();
		
		for (String t : list) {
			if (builder.length() > 0)
				builder.append(";");
			builder.append(t);
		}
		
		return builder.toString();
	}
	
	private String setToString(Set<String> set) {
		List<String> sortedElements = new ArrayList<String>(set);
		Collections.sort(sortedElements);
		
		return listToString(sortedElements);
	}
}
