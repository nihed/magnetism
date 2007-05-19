package com.dumbhippo.dm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;

public class FetchTests extends AbstractSupportedTests {
	static private final Logger logger = GlobalSetup.getLogger(BasicTests.class);
	private Map<String, FetchResult> expectedResults;
	
	@Override
	protected void setUp() {
		super.setUp();
		
		if (expectedResults == null) {
			expectedResults = new HashMap<String, FetchResult>();
			
			URL resource = this.getClass().getResource("/fetch-tests.xml");
			if (resource == null)
				throw new RuntimeException("Cannot find fetch-tests.xml");
			
			try {
				InputStream input = resource.openStream();
				for (FetchResult result : FetchResultHandler.parse(input)) {
					expectedResults.put(result.getId(), result);
				}
				input.close();
			} catch (IOException e) {
				throw new RuntimeException("Error reading fetch-tests.xml", e);
			} catch (SAXException e) {
				if (e instanceof SAXParseException) {
					SAXParseException pe = (SAXParseException)e;
					logger.error("fetch-tests.xml:{}:{}: {}",
							     new Object[] { pe.getLineNumber(), pe.getColumnNumber(), pe.getMessage() });
					throw new RuntimeException("Cannot parse fetch-tests.xml", e);
				}
				
				throw new RuntimeException("Error parsing fetch-tests.xml", e);
			}
		}
	}
	
	// Basic test of the test infrastructure; load all the fetch results, test
	// that they can be converted to XML and back and that the result of the
	// round-trip validates as the same thing as the original.
	public void testRoundTrip() {
		for (FetchResult result : expectedResults.values()) {
			XmlBuilder builder = new XmlBuilder();
			builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			builder.openElement("fetchResults");
			result.writeToXmlBuilder(builder);
			builder.closeElement();
			
			String asString = builder.toString();
			
			FetchResult roundTripped;
			try {
				InputStream input = new ByteArrayInputStream(asString.getBytes("UTF-8"));
				List<FetchResult> results = FetchResultHandler.parse(input);
				if (results.size() != 1)
					throw new RuntimeException("Round-trip of " + result.getId() + " to FetchResult gave " + results.size() + " results!");
				roundTripped = results.get(0);
				input.close();
				
			} catch (IOException e) {
				throw new RuntimeException("Error parsing recoverted " + result.getId(), e);
			} catch (SAXException e) {
				if (e instanceof SAXParseException) {
					SAXParseException pe = (SAXParseException)e;
					logger.error("fetch-tests.xml:{}:{}: {}",
							     new Object[] { pe.getLineNumber(), pe.getColumnNumber(), pe.getMessage() });
					throw new RuntimeException("Cannot parse recoverted " + result.getId(), e);
				}
				
				throw new RuntimeException("Error parsing recoverted " + result.getId(), e);
			}
			
			try {
				roundTripped.validateAgainst(result);
			} catch (FetchValidationException e) {
				throw new RuntimeException("Round-trip of " + result.getId() + " didn't validate " + e, e);
			}
		}
	}
}
