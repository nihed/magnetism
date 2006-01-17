package com.dumbhippo.server.impl;

import java.io.UnsupportedEncodingException;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.Viewpoint;


/**
 * This is only truly @Stateless if the same mailSession gets injected into all instances, 
 * I guess, since a MimeMessage points back to the session. Though as long as sendMessage()
 * doesn't refer to mailSession it would be OK either way.
 * 
 * @author hp
 *
 */
@Stateless
public class MailerBean implements Mailer {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MailerBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
	
	@javax.annotation.Resource(name="java:/Mail")
	private Session mailSession;

	private MimeMessage createMessage(InternetAddress fromAddress, InternetAddress toAddress) {
			MimeMessage msg;
			
			msg = new MimeMessage(mailSession);
			
			try {
				// sender the recipient will see
				msg.setFrom(fromAddress);
				// sender the mail system will verify against etc.
				msg.setSender(new InternetAddress(SpecialSender.NOBODY.toString()));
				msg.setRecipient(Message.RecipientType.TO, toAddress);
			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}
			
			return msg;
	}
	
	private MimeMessage createMessage(String from, String to) {
		try {
			InternetAddress toAddress = new InternetAddress(to);
			InternetAddress fromAddress = new InternetAddress(from);
		
			return createMessage(fromAddress, toAddress);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	public MimeMessage createMessage(User from, String to) {
		Viewpoint viewpoint = new Viewpoint(from);
		PersonView fromViewedBySelf = identitySpider.getPersonView(viewpoint, from, PersonViewExtra.PRIMARY_EMAIL);
		
		InternetAddress fromAddress;
		InternetAddress toAddress;
		try {
			String niceName = fromViewedBySelf.getName();
			String address = fromViewedBySelf.getEmail().getEmail();
			fromAddress = new InternetAddress(address, niceName);
			
			toAddress = new InternetAddress(to);
		} catch (AddressException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return createMessage(fromAddress, toAddress);
	}

	public MimeMessage createMessage(SpecialSender from, String to) {
		return createMessage(from.toString(), to);
	}
	
	public void setMessageContent(MimeMessage message, String subject, String bodyText, String bodyHtml) {
		try {
			message.setSubject(subject);
			
			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setText(bodyText.toString(), "UTF-8");
			
			MimeBodyPart htmlPart = new MimeBodyPart();
			htmlPart.setContent(bodyHtml.toString(), "text/html; charset=UTF-8");
			
			MimeMultipart multiPart = new MimeMultipart();
			// "alternative" means display only one or the other, "mixed" means both
			multiPart.setSubType("alternative");
			
			// I read something on the internet saying to put the text part first
			// so sucktastic mail clients see it first
			multiPart.addBodyPart(textPart);
			multiPart.addBodyPart(htmlPart);
			
			message.setContent(multiPart);
		} catch (MessagingException e) {
			throw new RuntimeException("failed to put together MIME message", e);
		}
	}
	
	public void sendMessage(MimeMessage message) {
		try {
			logger.debug("Sending email...");
			Transport.send(message);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
