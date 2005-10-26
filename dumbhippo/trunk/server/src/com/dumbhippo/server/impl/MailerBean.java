package com.dumbhippo.server.impl;

import java.io.UnsupportedEncodingException;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.PersonView;


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
	static private final Log logger = GlobalSetup.getLog(MailerBean.class);
	
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

	public MimeMessage createMessage(Person from, Person to) {
		PersonView fromViewedBySelf = identitySpider.getViewpoint(from, from);
		PersonView toViewedByFrom = identitySpider.getViewpoint(from, to);
		
		InternetAddress fromAddress;
		InternetAddress toAddress;
		try {
			String niceName = fromViewedBySelf.getHumanReadableName();
			String address = fromViewedBySelf.getEmail().getEmail();
			fromAddress = new InternetAddress(address, niceName);
			
			niceName = toViewedByFrom.getHumanReadableName();
			address = toViewedByFrom.getEmail().getEmail();
			toAddress = new InternetAddress(address, niceName);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return createMessage(fromAddress, toAddress);
	}

	public MimeMessage createMessage(SpecialSender from, String to) {
		return createMessage(from.toString(), to);
	}

	public void sendMessage(MimeMessage message) {
		try {
			Transport.send(message);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
