package com.dumbhippo.polling;

import java.net.MalformedURLException;
import java.net.URL;

import com.dumbhippo.persistence.ExternalAccountType;

public enum ServicePollingRestrictions {
	DELICIOUS(ExternalAccountType.DELICIOUS) {
		@Override
		public int getMaxRateSeconds() {
			return 30 * 60;
		}
	};
	
	private String domain;	
	
	@SuppressWarnings("unused")
	ServicePollingRestrictions(String domain) {
		this.domain = domain;
	}
	
	ServicePollingRestrictions(ExternalAccountType acct) {
		try {
			domain = new URL(acct.getSiteLink()).getHost();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public int getMaxRateSeconds() { return -1; };
	
	public static ServicePollingRestrictions lookupByDomain(String domain) {
		for (ServicePollingRestrictions restrict : ServicePollingRestrictions.values()) {
			if (domain.endsWith(restrict.domain)) {
				return restrict;
			}
		}
		return null;
	}
}
