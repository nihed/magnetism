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
import com.dumbhippo.dm.fetch.BoundFetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.dm.parser.ParseException;

public class AbstractFetchTests extends AbstractSupportedTests {
	static private final Logger logger = GlobalSetup.getLogger(AbstractFetchTests.class);

	private Map<String, FetchResult> expectedResults;
	private String filename;

	protected AbstractFetchTests(String filename) {
		this.filename = filename;
	}
	
	@Override
	protected void setUp() {
		super.setUp();
		
		if (expectedResults == null)
			readFetchResults();
	}
	
	private void readFetchResults() {
		expectedResults = new HashMap<String, FetchResult>();
		
		URL resource = this.getClass().getResource("/" + filename);
		if (resource == null)
			throw new RuntimeException("Cannot find " + filename);
		
		try {
			InputStream input = resource.openStream();
			for (FetchResult result : FetchResultHandler.parse(input)) {
				expectedResults.put(result.getId(), result);
			}
			input.close();
		} catch (IOException e) {
			throw new RuntimeException("Error reading " + filename, e);
		} catch (SAXException e) {
			if (e instanceof SAXParseException) {
				SAXParseException pe = (SAXParseException)e;
				logger.error("{}:{}:{}: {}",
						     new Object[] { filename, pe.getLineNumber(), pe.getColumnNumber(), pe.getMessage() });
				throw new RuntimeException("Cannot parse " + filename, e);
			}
			
			throw new RuntimeException("Error parsing " + filename, e);
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
					logger.error("<reconverted>:{}:{}: {}",
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
	
	protected FetchResult getExpected(String resultId, String... parameters) {
		Map<String, String> parametersMap = new HashMap<String, String>();
		for (int i = 0; i  < parameters.length; i += 2)
			parametersMap.put(parameters[i], parameters[i + 1]);
		
		FetchResult raw = expectedResults.get(resultId);
		if (raw == null)
			throw new RuntimeException("No expected result set with id='" + resultId + "'");
		
		return raw.substitute(parametersMap);
	}
	
	protected <K,T extends DMObject<K>> void doFetchTest(Class<K> keyClass, Class<T> objectClass, T object, String fetchString, String resultId, String... parameters) throws ParseException, FetchValidationException {
		FetchNode fetchNode = FetchParser.parse(fetchString);
		BoundFetch<K,T> fetch = fetchNode.bind(support.getModel().getClassHolder(keyClass, objectClass));
		
		FetchResultVisitor visitor = new FetchResultVisitor();
		support.currentSessionRO().visitFetch(object, fetch, visitor);
		
		FetchResult expected = getExpected(resultId, parameters);
		
		logger.debug("Result for {} is {}", resultId, visitor.getResult());
		visitor.getResult().validateAgainst(expected);
	}
}
