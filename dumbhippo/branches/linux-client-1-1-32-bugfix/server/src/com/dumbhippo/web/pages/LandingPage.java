package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.PromotionCode;

public class LandingPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LandingPage.class);

	private int selfInvitations;
	
	public LandingPage() {
		selfInvitations = -1;
	}
	
	public int getSelfInvitations() {
		if (selfInvitations < 0) {
			selfInvitations = invitationSystem.getSelfInvitationCount();
		}
		return selfInvitations;
	}
	
	public String getPromotion() {
		return PromotionCode.GENERIC_LANDING_200606.getCode();
	}

	public String getSummitPromotion() {
		return PromotionCode.SUMMIT_LANDING_200606.getCode();
	}
	
	public String getOpenSignupPromotion() {
		return PromotionCode.OPEN_SIGNUP_200609.getCode();
	}
}
