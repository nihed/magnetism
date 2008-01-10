package com.dumbhippo.server;

import javax.ejb.Local;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.email.MessageContent;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Local
public interface Mailer {

	enum SpecialSender {
		INVITATION("Mugshot Invitation <invitations@mugshot.org>",
				"GNOME Online Invitation <online@gnome.org>"),
		LOGIN("Mugshot Login <logins@mugshot.org>",
				"GNOME Online Login <online@gnome.org>"),
		VERIFIER("Mugshot Address Verifier <verifier@mugshot.org>",
				"GNOME Online Address Verifier <online@gnome.org>"),
		SITE("Mugshot <mugshot@mugshot.org>",
				"GNOME Online <online@gnome.org>"),
		NOBODY("nobody@mugshot.org", "noreply@gnome.org");
		
		static private final Logger logger = GlobalSetup.getLogger(SpecialSender.class);
		
		private String mugshotAddress;
		private String gnomeAddress;
		
		SpecialSender(String mugshotAddress, String gnomeAddress) {
			this.mugshotAddress = mugshotAddress;
			this.gnomeAddress = gnomeAddress;
		}
		
		public String getSiteAddress(Site site) {
			if (site == Site.GNOME)
				return gnomeAddress;
			else
				return mugshotAddress;
		}
		 
		@Override
		public String toString() {
			// this toString override can be nuked if we aren't seeing the warning below.
			// fix any occurrences of the warning by switching to getSiteAddress().
			logger.warn("Should not be using SpecialSender.toString(), FIXME");
			return this.mugshotAddress;
		}
	};
	
	public MimeMessage createMessage(UserViewpoint from, SpecialSender viewpointFallbackAddress, String to);
	public MimeMessage createMessage(UserViewpoint from, String to);
	public MimeMessage createMessage(Viewpoint viewpoint, SpecialSender from, String to);
	public MimeMessage createMessage(SpecialSender from, UserViewpoint viewpointReplyTo, 
			                         SpecialSender viewpointFallbackAddress, String to);
	
	public void setMessageContent(MimeMessage message, Site site, String subject, String bodyText, String bodyHtml, boolean htmlUsesMugshotLogo);
	
	public void setMessageContent(MimeMessage message, Site site, MessageContent content);
	
	public void sendMessage(MimeMessage message);
}
