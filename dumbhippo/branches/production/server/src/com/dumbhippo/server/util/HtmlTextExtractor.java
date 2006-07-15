package com.dumbhippo.server.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.dumbhippo.EnumSaxHandler;

/**
 * Utility that given a fragment of vaguely XML-like stuff, converts it into plain text.
 * Currently, we strip out all tags other than <p>, and convert paragraph
 * breaks into double newlines. You could get fancier here and handle
 * <a>, <br/> and so forth.
 * 
 * Note that this is relatively expensive; it can take a good fraction of
 * a millisecond to run even for short strings.
 * 
 * @author otaylor
 */
public final class HtmlTextExtractor {
	
	static private class TextExtractor extends EnumSaxHandler<TextExtractor.Element> {
		
		// The enum names should match the xml element names (including case)
		enum Element {
			html,
			body,
			p,
			IGNORED // an element we don't care about
		}

		// Whether there is a paragraph open (possibly created for content outside <p></p>)
		private boolean atStart = true;
		private boolean haveOpenParagraph = false;

		// EnumSaxHandler default content handling doesn't handled mixed content, 
		// so we roll our own 
		private StringBuilder textContent = new StringBuilder();
		
		public TextExtractor() {
			super(Element.class, Element.IGNORED);
		}
		
		static private final char PARAGRAPH_SEPARATOR = '\u2029';
		
		private void openParagraph() {
			if (atStart)
				atStart = false;
			else
				textContent.append(PARAGRAPH_SEPARATOR);
			haveOpenParagraph = true;
		}
		
		private void closeParagraph() {
			haveOpenParagraph = false;
		}
		
		@Override
		protected void openElement(Element element) throws SAXException {
			if (!currentlyInside(Element.body))
				return;

			if (element == Element.p) {
				openParagraph();
			}
		}
		
		@Override
		protected void closeElement(Element element) throws SAXException {
			if (!currentlyInside(Element.body))
				return;
			
			if (element == Element.p) {
				closeParagraph();
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (!currentlyInside(Element.body))
				return;

			boolean allWhitespace = true;
			for (int i = 0; i < length; i++) {
				char c = ch[start + i];
				if ((c != '\r') && (c != '\n') && (c != '\t') && (c != ' ')) {
					allWhitespace = false;
					break;
				}
			}
			
			if (!allWhitespace && !haveOpenParagraph) {
				openParagraph();
			}
			
			textContent.append(ch, start, length);
		}
		
		private static final Pattern externalWhitespace = Pattern.compile("(^[\r\n\t ]+)|(^[\r\n\t ]+?$)");
		private static final Pattern internalWhitespace = Pattern.compile("[\r\n\t ]+");
		private static final Pattern paragraphSeparator = Pattern.compile("[\r\n\t ]*\u2029[\r\n\t ]*");
				
		public String getText() {
			String trimmed = externalWhitespace.matcher(textContent).replaceAll("");
			trimmed = internalWhitespace.matcher(trimmed).replaceAll(" ");
			return paragraphSeparator.matcher(trimmed).replaceAll("\n\n");
		}
	}
	
	public static String extractText(String htmlText) {
		XMLReader xmlReader = new Parser();
		TextExtractor extractor = new TextExtractor();
		xmlReader.setContentHandler(extractor);
		Reader reader = new StringReader(htmlText);
		
		try {
			xmlReader.parse(new InputSource(reader));
			return extractor.getText();
		} catch (IOException e) {
			return htmlText;
		} catch (SAXException e) {
			return htmlText;
		}
	}
}
