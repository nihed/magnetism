package com.dumbhippo.dm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.dumbhippo.GlobalSetup;

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
			
			InputStream input;
			try {
				input = resource.openStream();
				for (FetchResult result : FetchResultHandler.parse(input)) {
					expectedResults.put(result.getId(), result);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				if (e instanceof SAXParseException) {
					SAXParseException pe = (SAXParseException)e;
					logger.error("fetch-tests.xml:{}:{}: {}",
							     new Object[] { pe.getLineNumber(), pe.getColumnNumber(), pe.getMessage() });
					throw new RuntimeException("Cannot parse fetch-tests.xml", e);
				}
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void testGroupFetch() {
	}
}
