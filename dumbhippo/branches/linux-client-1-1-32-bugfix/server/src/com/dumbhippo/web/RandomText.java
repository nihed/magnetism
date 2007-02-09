package com.dumbhippo.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import com.dumbhippo.StreamUtils;
import com.dumbhippo.XmlBuilder;

class RandomText {
	
	public interface OutputFilter {
		String filter(String text);
	}
	
	private static class Transition {
		private int count;
		private Node dest;
		
		Transition(Node dest) {
			this.dest = dest;
		}
		
		int getCount() {
			return count;
		}
		
		Node getDest() {
			return dest;
		}
		
		void incrementCount() {
			count += 1;
		}
	}
	
	private static class Node {
		private String text;
		int totalCount;
		private Map<Node,Transition> transitions;
		
		Node(String text) {
			this.text = text;
			transitions = new HashMap<Node,Transition>();
		}
		
		String getText() {
			return text;
		}
	
		int getTransitionCount() {
			return transitions.size();
		}
		
		void incrementDestination(Node dest) {
			Transition t = transitions.get(dest);
			if (t == null) {
				t = new Transition(dest);
				transitions.put(dest, t);
			}
			t.incrementCount();
			totalCount += 1;
		}
		
		boolean isDeadEnd() {
			return totalCount == 0;
		}
		
		Node findNext(Random r) {
			if (isDeadEnd())
				throw new RuntimeException("dead end");
			
			// pick i in [0,totalCount)
			int i = r.nextInt(totalCount);
			int pos = 0;
			for (Transition t : transitions.values()) {
				int next = pos + t.getCount();
				if (i >= pos && i < next)
					return t.getDest();
				pos = next;
			}
			throw new RuntimeException("Broken, no destination matched");
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{'");
			sb.append(text);
			sb.append("' => ");
			for (Transition t : transitions.values()) {
				double fraction = (double) t.getCount() / (double) totalCount;
				sb.append(Integer.toString((int) (fraction * 100.0)));
				sb.append("% '");
				sb.append(t.getDest().getText());
				sb.append("' ");
			}
			sb.append("}");
			
			return sb.toString();
		}
	}
	
	private OutputFilter filter;
	private Map<String,Node> nodes;
	
	RandomText(OutputFilter filter) {
		nodes = new HashMap<String,Node>();
		this.filter = filter;
	}
	
	static private boolean isSentencePunctuation(char c) {
		switch (c) {
		case '.':
		case '?':
		case '!':
			return true;
		default:
			return false;
		}
	}
	
	static private boolean isSentencePunctuation(String s) {
		return s.length() == 1 && isSentencePunctuation(s.charAt(0));
	}
	
	void seed(InputStream is) throws IOException {
		seed(StreamUtils.readStreamUTF8(is));
	}
	
	Node seedToken(Node previousNode, String token) {
		
		if (token.contains("\"")) {
			// take out quote marks unless it's an irony word
			if (token.startsWith("\"") && token.endsWith("\""))
				; // nothing
			else {
				token.replace("\"", "");
				if (token.length() == 0)
					return null;
			}
		}
		
		Node node = nodes.get(token);
		if (node == null) {
			node = new Node(token);
			nodes.put(token, node);
		}
		
		if (previousNode != null) {
			previousNode.incrementDestination(node);
			//System.err.println("       '" + previousNode.getText() + "' - '" + node.getText() + "'");
		}
		
		return node;
	}
	
	private String makeTwoWordText(Node one, Node two) {
		StringBuilder twoWords = new StringBuilder();
		twoWords.append(one.getText());
		if (!isSentencePunctuation(two.getText()))
			twoWords.append(" ");
		twoWords.append(two.getText());
		return twoWords.toString();
	}
	
	private void seedSentenceFragment(String s, char punctuation) {
		//System.err.println("Fragment:    '" + s + "'");
		//System.err.println("Punctuation: '" + punctuation + "'");
		Node morePreviousNode = null;
		Node previousNode = null;
		// special "empty string" node is start of a sentence fragment
		Node currentNode = seedToken(null, "");
		Node currentTwoWordNode = null;
		StringTokenizer st = new StringTokenizer(s, " \t\n\r\f<>()[]{}?.!;:,");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			
			//System.err.println("  token: '" + token + "'");
			
			// if we've accumulated three one-word nodes, do a transition from 
			// word one to the pair of two and three, a "1-2" transition
			if (morePreviousNode != null && 
					previousNode != null &&
					currentNode != null) {
				String twoWords = makeTwoWordText(previousNode, currentNode);
				
				// add a two-word pair as one option after morePreviousNode one-word
				// node
				currentTwoWordNode = seedToken(morePreviousNode, twoWords);
			}
			
			// this is the "1-1" transition, newNode comes after currentNode
			Node newNode = seedToken(currentNode, token); 
			
			if (newNode != null) {
				morePreviousNode = previousNode;
				previousNode = currentNode;
				currentNode = newNode; 
				
				// if the two nodes before token were added as a two-word node, then 
				// also put this token as an option after them. a "2-1" transition
				seedToken(currentTwoWordNode, token);
			}
			
			// we don't make the two-word to two-word connections, only the 
			// 2-1, 1-2, and 1-1, not 2-2
		}
		if (punctuation != '\0')
			seedToken(currentNode, new String(Character.toString(punctuation)));
	}
	
	void seed(String s) {
		int start = 0;
		int i = 0;
		for (char c : s.toCharArray()) {
			if (isSentencePunctuation(c)) {
				seedSentenceFragment(s.substring(start, i), c);
				start = i + 1;
			}
			++i;
		}
		seedSentenceFragment(s.substring(start, i), '\0');
	}
	
	private Node startNode(Random r) {
		
		// "" is the "start of sentence" node; we should pretty much 
		// always have one, but if not we pick at random
		Node node = nodes.get("");
		if (node != null)
			return node;
		
		// pick a random start node
		int i = r.nextInt(nodes.size());
		for (Node n : nodes.values()) {
			if (i == 0)
				return n;
			--i;
		}
		throw new RuntimeException("no random node found");
	}
	
	private void endSentence(StringBuilder sb, int lastPunctuation, Random r) {
		if (lastPunctuation >= 0) {
			sb.setLength(lastPunctuation + 1);
			if (sb.charAt(sb.length() - 1) == '!' && r.nextBoolean()) {
				sb.append("!!");
			}
		} else {
			sb.setLength(sb.length() - 1); // chop trailing space
		}
	}
	
	void generate(Writer writer, int maxLength, boolean escapeXml) throws IOException {
		
		if (nodes.size() < 3)
			throw new RuntimeException("Not enough seed data");
		
		Random r = new Random();

		Node node = startNode(r);
		
		int lastPunctuation = -1;
		StringBuilder sb = new StringBuilder();
		while (true) {
			String text = node.getText();
			
			if (filter != null)
				text = filter.filter(text);
			
			if (sb.length() + text.length() > maxLength)
				break;
			
			if (isSentencePunctuation(text) &&
					sb.charAt(sb.length() - 1) == ' ') {
				sb.setLength(sb.length() - 1); // chop trailing space
				lastPunctuation = sb.length();
			}
			
			sb.append(text);
			sb.append(" ");
			
			if (node.isDeadEnd()) {
				endSentence(sb, lastPunctuation, r);
				node = startNode(r);
			} else {
				node = node.findNext(r);
			}
		}
		
		endSentence(sb, lastPunctuation, r);
		
		if (escapeXml) {
			String xml = XmlBuilder.escape(sb.toString());
			writer.write(xml);
		} else {
			writer.write(sb.toString());
		}
	}
	
	private void dumpGraph(Writer writer) throws IOException {
		List<Node> sorted = new ArrayList<Node>();
		sorted.addAll(nodes.values());
		Collections.sort(sorted, new Comparator<Node>() {
			public int compare(Node a, Node b) {
				if (a.getTransitionCount() < b.getTransitionCount())
					return -1;
				else if (a.getTransitionCount() > b.getTransitionCount())
					return 1;
				else
					return 0;
			}
			
		});
		for (Node n : sorted) {
			writer.write(n.toString());
			writer.write("\n");
		}
		writer.write("Dead Ends\n===\n");
		for (Node n : sorted) {
			if (n.isDeadEnd()) {
				writer.write(n.getText());
				writer.write(" ");
			}
		}
	}
	
	static final private String[] ARTISTS = {
		"U2",
		"Arctic Monkeys",
		"Lawrence Welk",
		"Beatles",
		"Strokes",
		"Michael Bolton",
		"Pixies"
	};
	
	public static void main(String[] args) throws IOException {
		RandomText generator = new RandomText(new OutputFilter() {
			private Random r;
						
			public String filter(String text) {
				if (r == null)
					r = new Random();
				String artist = ARTISTS[r.nextInt(ARTISTS.length)];
				return text.replace("ARTIST", artist);
			}
			
		});
		for (String arg : args) {
			System.out.println("Seeding with '" + arg + "'");
			InputStream is = new FileInputStream(arg);
			generator.seed(is);
		}
		Writer w = new OutputStreamWriter(System.out);
		
		for (int i = 0; i < 10; ++i) {
			System.out.print(Integer.toString(i) + ": ");
			generator.generate(w, 256, false);
			w.flush();
			System.out.print("\n");
		}
		
		generator.dumpGraph(w);
		w.flush();
	}
}
