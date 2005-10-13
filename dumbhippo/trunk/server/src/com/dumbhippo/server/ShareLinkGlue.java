package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

@Local
public interface ShareLinkGlue extends LoginRequired {
	public List<String> freeformRecipientsToIds(List<String> userEnteredRecipients) throws UnknownPersonException;
	public void shareLink(String url, List<String> recipientIds, String description);
}
