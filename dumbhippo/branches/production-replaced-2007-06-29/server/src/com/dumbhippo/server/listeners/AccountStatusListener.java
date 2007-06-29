package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.Account;

public interface AccountStatusListener {
	public void onAccountDisabledToggled(Account account);
	public void onAccountAdminDisabledToggled(Account account);
	public void onMusicSharingToggled(Account account);
	public void onApplicationUsageToggled(Account account);
}
