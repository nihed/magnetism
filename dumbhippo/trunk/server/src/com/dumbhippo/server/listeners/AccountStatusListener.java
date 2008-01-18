package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.Account;
import com.dumbhippo.server.views.UserViewpoint;

public interface AccountStatusListener {
	public void onAccountDisabledToggled(Account account);
	public void onAccountAdminDisabledToggled(Account account);
	public void onMusicSharingToggled(UserViewpoint viewpoint);
	public void onApplicationUsageToggled(UserViewpoint viewpoint);
	public void onFacebookApplicationEnabled(UserViewpoint viewpoint);
}
