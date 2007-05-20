package com.dumbhippo.dm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.dm.dm.TestGroupDMO;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.dm.persistence.TestGroup;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.dm.persistence.TestUser;
import com.dumbhippo.identity20.Guid;

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
	
	public <T extends DMObject> void doTest(Class<T> clazz, T object, String fetchString, String resultId, String... parameters) throws RecognitionException, TokenStreamException, FetchValidationException {
		FetchNode fetchNode = FetchParser.parse(fetchString);
		Fetch<T> fetch = fetchNode.bind(DataModel.getInstance().getDMClass(clazz));
		
		FetchResultVisitor visitor = new FetchResultVisitor();
		ReadOnlySession.getCurrent().visitFetch(object, fetch, visitor);
		
		Map<String, String> parametersMap = new HashMap<String, String>();
		for (int i = 0; i  < parameters.length; i += 2)
			parametersMap.put(parameters[i], parameters[i + 1]); 
		
		FetchResult expected = expectedResults.get(resultId).substitute(parametersMap);
		logger.debug("Result for {} is {}", resultId, visitor.getResult());
		
		visitor.getResult().validateAgainst(expected);
	}

	private void createData(Guid bobId, Guid janeId, Guid groupId) {
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		em = support.beginTransaction();

		TestUser bob = new TestUser("Bob");
		bob.setId(bobId.toString());
		em.persist(bob);
		
		TestUser jane = new TestUser("Jane");
		jane.setId(janeId.toString());
		em.persist(jane);

		TestGroup group = new TestGroup("BobAndJane");
		group.setId(groupId.toString());
		em.persist(group);
		
		TestGroupMember groupMember;
		
		groupMember = new TestGroupMember(group, bob);
		em.persist(groupMember);
		group.getMembers().add(groupMember);

		groupMember = new TestGroupMember(group, jane);
		em.persist(groupMember);
		group.getMembers().add(groupMember);

		em.getTransaction().commit();
	}
	
	// Basic operation of fetching, no subclassing, no defaults
	public void testBasicFetch() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid groupId = Guid.createNew();
		
		createData(bobId, janeId, groupId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(viewpoint);
		
		TestGroupDMO groupDMO = ReadOnlySession.getCurrent().findMustExist(TestGroupDMO.class, groupId);
		doTest(TestGroupDMO.class, groupDMO, "name;members member name", "bobAndJane",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());
		
		em.getTransaction().commit();
	}
	
	// Like testBasicFetch but using defaulted fetches
	public void testDefaultFetch() throws Exception {
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		EntityManager em;
		
		/////////////////////////////////////////////////
		// Setup

		Guid bobId = Guid.createNew();
		Guid janeId = Guid.createNew();
		Guid groupId = Guid.createNew();
		
		createData(bobId, janeId, groupId);
		
		//////////////////////////////////////////////////
		
		em = support.beginSessionRO(viewpoint);
		
		TestGroupDMO groupDMO = ReadOnlySession.getCurrent().findMustExist(TestGroupDMO.class, groupId);
		doTest(TestGroupDMO.class, groupDMO, "+;members +", "bobAndJane",
				"group", groupId.toString(),
				"bob", bobId.toString(),
				"jane", janeId.toString());
		
		em.getTransaction().commit();
	}

	// TODO: add tests here for fetching against subclassed objects, with defaults, etc
}
