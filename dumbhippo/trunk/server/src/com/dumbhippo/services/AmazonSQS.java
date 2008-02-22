package com.dumbhippo.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlUtils;
import com.dumbhippo.XmlUtils.XmlParseData;

public class AmazonSQS {
	static private final Logger logger = GlobalSetup.getLogger(AmazonSQS.class);
	static private final String baseUrl = "http://queue.amazonaws.com";
	
	public static String createQueue(AWSCredentials creds, String queueName) throws TransientServiceException {
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(baseUrl);

		Map<String,String> params = new HashMap<String,String>();
		params.put("Version", "2008-01-01");
		params.put("QueueName", queueName);
		logger.debug("doing CreateQueue name={}", queueName);		
				
		try {
			AWSRequestUtil.setupMethod(post, creds.getAccessKey(), creds.getSecretKey(), "CreateQueue", params, null);			
			int status = client.executeMethod(post);
			if (status != HttpStatus.SC_OK) {
				throw new TransientServiceException("Failed to createQueue; response code=" + status);
			}
			
			XmlParseData parsed = XmlUtils.parseXml(post.getResponseBodyAsStream(), new String[] { "q", "http://queue.amazonaws.com/doc/2008-01-01/" });
			return ((Node) parsed.xpath.evaluate("/q:CreateQueueResponse/q:CreateQueueResult/q:QueueUrl", parsed.doc, XPathConstants.NODE)).getTextContent();
		} catch (HttpException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new TransientServiceException(e);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new TransientServiceException(e);
		} finally {
			post.releaseConnection();
		}
	}
	
	public static void sendMessage(AWSCredentials creds, String queueUrl, String msg) throws TransientServiceException {
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(queueUrl);

		Map<String,String> params = new HashMap<String,String>();
		params.put("Version", "2008-01-01");		
		params.put("MessageBody", msg);
		logger.debug("doing SendMessage target={} contents={}", queueUrl, msg);		
			
		try {
			AWSRequestUtil.setupMethod(post, creds.getAccessKey(), creds.getSecretKey(), "SendMessage", params, null);			
			int status = client.executeMethod(post);
			if (status != HttpStatus.SC_OK) {
				throw new TransientServiceException("Failed to sendMessage; response code=" + status);
			}			
		} catch (HttpException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new TransientServiceException(e);
		} finally {
			post.releaseConnection();
		}
	}
	
	public static class Message {
		protected Message(String body, String receiptId) {
			this.body = body;
			this.receiptId = receiptId;
		}
		public String body;
		public String receiptId;
	}
	
	public static Message[] receiveMessages(AWSCredentials creds, String queueUrl, int max) throws TransientServiceException {
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(queueUrl);

		Map<String,String> params = new HashMap<String,String>();
		params.put("Version", "2008-01-01");
		if (max > 0)
			params.put("MaxNumberOfMessages", Integer.toString(max));
		logger.debug("doing ReceiveMessages target={}", queueUrl);		
			
		try {
			AWSRequestUtil.setupMethod(post, creds.getAccessKey(), creds.getSecretKey(), "ReceiveMessage", params, null);			
			int status = client.executeMethod(post);
			if (status != HttpStatus.SC_OK) {
				throw new TransientServiceException("Failed to sendMessage; response code=" + status);
			}
			XmlParseData parsed = XmlUtils.parseXml(post.getResponseBodyAsStream(), new String[] { "q", "http://queue.amazonaws.com/doc/2008-01-01/" });			
			NodeList msgNodes = (NodeList) parsed.xpath.evaluate("/q:ReceiveMessageResponse/q:ReceiveMessageResult/q:Message", parsed.doc, XPathConstants.NODESET);
			Message[] msgResult = new Message[msgNodes.getLength()];
			for (int i = 0; i < msgNodes.getLength(); i++) {
				Node node = msgNodes.item(i);
				msgResult[i] = new Message(parsed.xpath.evaluate("q:Body", node),
						                   parsed.xpath.evaluate("q:ReceiptHandle", node));
			}
			return msgResult;
		} catch (HttpException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new TransientServiceException(e);
		} catch (SAXException e) {
			throw new TransientServiceException(e);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} finally {
			post.releaseConnection();
		}
	}

	public static Message[] receiveMessages(AWSCredentials creds, String queueUrl) throws TransientServiceException {
		return receiveMessages(creds, queueUrl, -1);
	}
	
	public static void deleteMessage(AWSCredentials creds, String queueUrl, String receiptHandle) throws TransientServiceException {
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(queueUrl);

		Map<String,String> params = new HashMap<String,String>();
		params.put("Version", "2008-01-01");
		params.put("ReceiptHandle", receiptHandle);
		logger.debug("doing DeleteMessage target={} receiptHandle={}", queueUrl, receiptHandle);		
			
		try {
			AWSRequestUtil.setupMethod(post, creds.getAccessKey(), creds.getSecretKey(), "DeleteMessage", params, null);			
			int status = client.executeMethod(post);
			if (status != HttpStatus.SC_OK) {
				throw new TransientServiceException("Failed to sendMessage; response code=" + status);
			}
		} catch (HttpException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new TransientServiceException(e);
		} finally {
			post.releaseConnection();
		}
	}	
}
