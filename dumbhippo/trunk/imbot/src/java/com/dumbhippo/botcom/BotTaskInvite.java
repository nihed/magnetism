package com.dumbhippo.botcom;

public class BotTaskInvite extends BotTask {

	private static final long serialVersionUID = 0;

	private String inviteUrl;
	private String fromDisplayName;
	private String fromAimName;
	private String inviteeAimName;
	
	/**
	 * @param inviteUrl url to click to accept the invite
	 * @param fromDisplayName human-readable name of the inviter
	 * @param fromAimName AIM address of the inviter
	 * @param inviteeAimName AIM address of the invitee
	 */
	public BotTaskInvite(String inviteUrl, String fromDisplayName, String fromScreenName, String inviteeScreenName) {
		this.inviteUrl = inviteUrl;
		this.fromDisplayName = fromDisplayName;
		this.fromAimName = fromScreenName;
		this.inviteeAimName = inviteeScreenName;
	}
	
	public String getFromDisplayName() {
		return fromDisplayName;
	}
	public String getInviteUrl() {
		return inviteUrl;
	}

	public String getFromAimName() {
		return fromAimName;
	}

	public String getInviteeAimName() {
		return inviteeAimName;
	}
}
