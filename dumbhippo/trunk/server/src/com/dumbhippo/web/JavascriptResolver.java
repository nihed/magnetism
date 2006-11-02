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
import com.dumbhippo.XmlBuilder;

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
		private Set<String> modulesLoaded;
		private String globalRequires;
		private String firstPartOfTag;
		private String lastPartOfTag; 
		
		// FIXME config.js could just be inlined here instead of in its own file, no?
		// Note: if you change the format remember to change the args to it when it's used...
		// also keep the XmlBuilder version in sync
		static private final String globalRequiresFormat = 
			"<script type=\"text/javascript\" src=\"%s/%s/config.js\"></script>\n" +
			"<script type=\"text/javascript\" src=\"%s/%s/dojo/bootstrap1.js\"></script>\n";
		
		static private final String loadModuleFormat = 
			"<script type=\"text/javascript\">%s._load();</script>\n";
		
		private Page() {
			this.filesUsed = new HashSet<String>();
			this.modulesLoaded = new HashSet<String>();
			
			this.globalRequires = String.format(globalRequiresFormat,
					scriptRoot, buildStamp,
					scriptRoot, buildStamp);
			
			this.firstPartOfTag = 
				"<script type=\"text/javascript\" src=\"" + scriptRoot + "/" + buildStamp + "/";
			this.lastPartOfTag = 
				"\"></script>\n";
		}
		
		private String moduleIdentifier(String module) {
			return module.replace("\\*", "_star");
		}

		private abstract class OutputAdapter {
			abstract void writeModuleLoad(String module) throws IOException;
			abstract void writeSrcFilename(String filename) throws IOException;
			abstract void writeGlobalRequires() throws IOException;
		}
		
		private class OutputAdapterWriter extends OutputAdapter {
			private Writer htmlWriter;
			OutputAdapterWriter(Writer htmlWriter) {
				this.htmlWriter = htmlWriter;
			}
			
			@Override
			void writeModuleLoad(String module) throws IOException {
				htmlWriter.write(String.format(loadModuleFormat, moduleIdentifier(module)));
			}
			@Override
			void writeSrcFilename(String filename) throws IOException {
				htmlWriter.write(firstPartOfTag);
				htmlWriter.write(filename);
				htmlWriter.write(lastPartOfTag);				
			}
			@Override
			void writeGlobalRequires() throws IOException {
				htmlWriter.write(globalRequires);
			}
		}
		
		private class OutputAdapterXml extends OutputAdapter {
			private XmlBuilder xml;
			OutputAdapterXml(XmlBuilder xml) {
				this.xml = xml;
			}
			
			private void appendFileNode(String filename) {
				// we put in some spaces as content to avoid xml-style tag closing (be sure we get </script>)
				xml.appendTextNode("script", "  ",
						"type", "text/javascript",
						"src", scriptRoot + "/" + buildStamp + "/" + filename);				
			}
			
			@Override
			void writeModuleLoad(String module) throws IOException {
				xml.appendTextNode("script", moduleIdentifier(module) + "._load();", "type", "text/javascript");
			}
			@Override
			void writeSrcFilename(String filename) throws IOException {
				appendFileNode(filename);
			}
			@Override
			void writeGlobalRequires() throws IOException {
				appendFileNode("config.js");
				appendFileNode("dojo/bootstrap1.js");
			}
		}
		
		private void includeModule(String module, OutputAdapter out) throws IOException {
			String file = moduleMap.get(module);
			if (file == null) {
				logger.error("Attempt to include unknown javascript module '{}'", module);
				throw new RuntimeException("Unknown javascript module " + module);
			}
			
			includeFile(file, out);
			
			if (!modulesLoaded.contains(module)) {
				modulesLoaded.add(module); 
				out.writeModuleLoad(module);
			}
		}
		
		public void includeModule(String module, Writer htmlWriter) throws IOException {
			includeModule(module, new OutputAdapterWriter(htmlWriter));
		}
		
		public void includeModule(String module, XmlBuilder xml) {
			try {
				includeModule(module, new OutputAdapterXml(xml));
			} catch (IOException e) {
				throw new RuntimeException("writing to xml builder should not create IOException", e);
			}
		}
		
		private void includeFile(String filename, OutputAdapter out) throws IOException {
			if (filesUsed.contains(filename)) {
				return;
			}
			
			boolean first = filesUsed.isEmpty();

			filesUsed.add(filename);
			
			// The first time we include anything, include the global requirements
			if (first) {
				out.writeGlobalRequires();
				includeModule("common", out);
			}
			
			List<String> dependencies = fileDependencies.get(filename);
			if (dependencies == null) {
				logger.error("Attempt to include unknown javascript file '{}' in {}",
						filename, scriptRoot);
				throw new RuntimeException("Unknown javascript file " + filename);
			}
			
			for (String dep : dependencies) {
				includeFile(dep, out);
			}
			
			out.writeSrcFilename(filename);
		}
		
		public void includeFile(String filename, Writer htmlWriter) throws IOException {			
			includeFile(filename, new OutputAdapterWriter(htmlWriter));
		}
		
		public void includeFile(String filename, XmlBuilder xml) {			
			try {
				includeFile(filename, new OutputAdapterXml(xml));
			} catch (IOException e) {
				throw new RuntimeException("writing to xml builder should not create IOException", e);
			}
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
