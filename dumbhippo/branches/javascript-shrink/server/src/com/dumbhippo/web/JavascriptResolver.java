package com.dumbhippo.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;

public final class JavascriptResolver {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(JavascriptResolver.class);
	
	private String scriptRoot;
	private String buildStamp;
	private Map<String,List<String>> fileDependencies; // map from file to dependency files
	private Map<String,String> moduleMap;              // map from module name to its file
	
	static public JavascriptResolver newInstance(String scriptRoot, String buildStamp, File dependenciesFile, File moduleMapFile) throws IOException {
		return new JavascriptResolver(scriptRoot, buildStamp, dependenciesFile, moduleMapFile);
	}
	
	public Page newPage() {
		return new Page();
	}
	
	// each page can be in a different thread; we rely on the singleton
	// JavascriptResolver being immutable, so there's no need to lock it
	public class Page {
		private Set<String> filesUsed;
		private String globalRequires;
		private String firstPartOfTag;
		private String lastPartOfTag; 
		
		private Page() {
			this.filesUsed = new HashSet<String>();
			
			// FIXME we should change to just outputting the _content_ of config.js here inline,
			// and dojo.js should be replaced with just loading the hostenv/bootstrap files directly
			this.globalRequires =
				"<script type=\"text/javascript\" src=\"" + scriptRoot + "/" + buildStamp + "/config.js\"></script>\n" +
				"<script type=\"text/javascript\" src=\"" + scriptRoot + "/" + buildStamp + "/dojo.js\"></script>\n";
			
			this.firstPartOfTag = 
				"<script type=\"text/javascript\" src=\"" + scriptRoot + "/" + buildStamp + "/";
			this.lastPartOfTag = 
				"\"></script>\n";
		}
		
		public void includeModule(String module, Writer htmlWriter) throws IOException {
			String file = moduleMap.get(module);
			if (file == null) {
				logger.error("Attempt to include unknown javascript module '{}'", module);
				throw new IOException("Unknown javascript module " + module);
			}
			
			includeFile(file, htmlWriter);
		}
		
		public void includeFile(String filename, Writer htmlWriter) throws IOException {			
			if (filesUsed.contains(filename)) {
				return;
			}
			
			// The first time we include anything, include the global requirements
			if (filesUsed.isEmpty())
				htmlWriter.write(globalRequires);
			
			filesUsed.add(filename);
			
			List<String> dependencies = fileDependencies.get(filename);
			if (dependencies == null) {
				logger.error("Attempt to include unknown javascript file '{}' in {}",
						filename, scriptRoot);
				throw new IOException("Unknown javascript file " + filename);
			}
			
			for (String dep : dependencies) {
				includeFile(dep, htmlWriter);
			}
			
			htmlWriter.write(firstPartOfTag);
			htmlWriter.write(filename);
			htmlWriter.write(lastPartOfTag);
		}
	}
	
	private JavascriptResolver(String scriptRoot, String buildStamp, 
			File dependenciesFile, File moduleMapFile) throws IOException {
		String dependenciesContent = StreamUtils.readStreamUTF8(new FileInputStream(dependenciesFile));
		String moduleMapContent = StreamUtils.readStreamUTF8(new FileInputStream(moduleMapFile));
		
		this.scriptRoot = scriptRoot;
		this.buildStamp = buildStamp;
		this.fileDependencies = parseStringListMap(dependenciesContent);
		this.moduleMap = parseStringStringMap(moduleMapContent);
	}
	
	static private String[] tokenizeMapEntry(String line) throws IOException {
		String[] tokens = line.split("\\s");
		if (tokens.length < 2) {
			throw new IOException("Line should have at least a key and a colon after it '" + line + "'");
		}
		if (!tokens[1].equals(":"))
			throw new IOException("Line should have a colon after first token '" + line + "'");
		
		return tokens;
	}
	
	// the file format is "key : value value value\n" with no escaping
	// (space/colon just isn't expected in the keys/values)
	static private Map<String,List<String>> parseStringListMap(String content) throws IOException {
		Map<String,List<String>> map = new HashMap<String,List<String>>();
		String[] lines = content.split("\n");
		for (String line : lines) {
			String[] tokens = tokenizeMapEntry(line);
			List<String> value = new ArrayList<String>();
			// loop may run 0 times if no values
			for (int i = 2; i < tokens.length; ++i) {
				value.add(tokens[i]);
			}
			map.put(tokens[0], value);
		}
		
		return map;
	}
	
	static private Map<String,String> parseStringStringMap(String content) throws IOException {
		Map<String,String> map = new HashMap<String,String>();
		String[] lines = content.split("\n");
		for (String line : lines) {
			String[] tokens = tokenizeMapEntry(line);
			if (tokens.length != 3) {
				throw new IOException("line should have one key and one value '" + line + "'");
			}
			map.put(tokens[0], tokens[2]);
		}
		
		return map;
	}
	
	public static void main(String[] args) throws IOException {
		String scriptRoot = System.getenv("SCRIPT_ROOT");
		if (scriptRoot == null) {
			System.err.println("Set SCRIPT_ROOT to e.g. trunk/server/target/javascript in the Eclipse Run As... dialog");
		}
		
		// note that SCRIPT_ROOT is really the absolute one, while JavascriptResolver wants
		// something relative, eh, it's test code
		JavascriptResolver resolver = JavascriptResolver.newInstance("/javascript",
				"{buildStamp}",
				new File(scriptRoot + "/file-dependencies.txt"),
				new File(scriptRoot + "/module-file-map.txt"));
		
		Writer out = new BufferedWriter(new OutputStreamWriter(System.out));
		Page page = resolver.newPage();
		page.includeModule("dh.account", out);
		page.includeFile("dh/framer.js", out);
		out.close();
	}
}
