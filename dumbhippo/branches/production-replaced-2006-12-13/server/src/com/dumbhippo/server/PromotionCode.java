package com.dumbhippo.server;

/**
 * A promotion code identifies a particular campaign to sign 
 * people up or whatever; it's not really supposed to be secure
 * (people can see it and post it on the internet), but it is 
 * supposed to be obscure for a short time when first introduced.
 * This also lets us track where people are coming from 
 * (e.g. a different code for people coming from myspace, etc.)
 * 
 * This should probably be in a config file eventually, but 
 * not worth it for now. Or perhaps it should be a 
 * database table; we might also associate promotion codes with 
 * WantsIn email address entries.
 * 
 * @author hp
 *
 */
public enum PromotionCode {
	MUSIC_INVITE_PAGE_200602("music_invite_page_200602"),
	GENERIC_LANDING_200606("generic_landing_page_200606"),
	SUMMIT_LANDING_200606("summit_landing_page_200606"),
	OPEN_SIGNUP_200609("open_signup_200609");
	
	private String code;
	
	private PromotionCode(String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
	public boolean isFromMySpace() {
		switch(this) {
		case MUSIC_INVITE_PAGE_200602:
			return true;
		default:
			return false;
		}
	}

	public static PromotionCode check(String code) throws NotFoundException {
		for (PromotionCode c : PromotionCode.values()) {
			if (c.getCode().equals(code))
				return c;
		}
		throw new NotFoundException("Unknown promotion code '" + code + "'");
	}
}
