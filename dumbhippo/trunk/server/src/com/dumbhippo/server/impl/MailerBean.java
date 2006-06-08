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
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.UserViewpoint;


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
	
	@EJB
	private Configuration configuration;
	
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

	private MimeMessage createMessage(InternetAddress fromAddress, InternetAddress replyToAddress, InternetAddress toAddress) {
		MimeMessage msg = createMessage(fromAddress, toAddress);
		
		try {
			msg.setReplyTo(new InternetAddress[]{replyToAddress});
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		
		return msg;
    }
	
	private InternetAddress createAddressFromViewpoint(UserViewpoint viewpoint, SpecialSender fallbackAddress) {
		PersonView fromViewedBySelf = identitySpider.getPersonView(viewpoint, viewpoint.getViewer(), PersonViewExtra.PRIMARY_EMAIL);	
	
		InternetAddress internetAddress;
		
		try {
			if ((fromViewedBySelf.getEmail() != null) && (fromViewedBySelf.getEmail().getEmail() != null)) {
				String niceName = fromViewedBySelf.getName();
				String address = fromViewedBySelf.getEmail().getEmail();
				internetAddress = new InternetAddress(address, niceName);
			} else {
				// theoretically, we might have users in the system who do not have an e-mail, but 
				// have an AIM, when we allow such users in practice, we can change this to possibly
				// use users's @aol.com address, though it's quite possible that the person does not
				// really use this address
				internetAddress = new InternetAddress(fallbackAddress.toString());
			}
		} catch (AddressException e) {
			throw new RuntimeException(e);	
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}				
		
		return internetAddress;
	}
	
	private InternetAddress createAddressFromString(String address) {
		try {
			InternetAddress internetAddress = new InternetAddress(address);		
			return internetAddress;
		} catch (AddressException e) {
			throw new RuntimeException(e);
		}
	}

	public MimeMessage createMessage(UserViewpoint viewpoint, SpecialSender viewpointFallbackAddress, String to) {
		
		InternetAddress fromAddress = createAddressFromViewpoint(viewpoint, viewpointFallbackAddress);
		InternetAddress toAddress = createAddressFromString(to);
		
		return createMessage(fromAddress, toAddress);
	}
	
	public MimeMessage createMessage(UserViewpoint viewpoint, String to) {
		return createMessage(viewpoint, SpecialSender.MUGSHOT, to);
	}

	public MimeMessage createMessage(SpecialSender from, String to) {
		
		InternetAddress fromAddress = createAddressFromString(from.toString());
		InternetAddress toAddress = createAddressFromString(to);
		
		return createMessage(fromAddress, toAddress);
	}

	public MimeMessage createMessage(SpecialSender from, UserViewpoint viewpointReplyTo, 
			                         SpecialSender viewpointFallbackAddress, String to) {
		
		InternetAddress fromAddress = createAddressFromString(from.toString());
		InternetAddress replyToAddress = createAddressFromViewpoint(viewpointReplyTo, viewpointFallbackAddress);
		InternetAddress toAddress = createAddressFromString(to);
		
		return createMessage(fromAddress, replyToAddress, toAddress);
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
		// The primary reason for DISABLE_EMAIL is for test configurations, so
		// we check only at the end of the process to catch bugs earlier
		// in the process.
		if (!configuration.getProperty(HippoProperty.DISABLE_EMAIL).equals("true")) {
			try {
				logger.debug("Sending email...");
				Transport.send(message);
			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
