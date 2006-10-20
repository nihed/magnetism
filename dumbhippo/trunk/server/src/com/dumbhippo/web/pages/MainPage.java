package com.dumbhippo.web.pages;

import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.views.PersonMugshotView;
import com.dumbhippo.web.WebEJBUtil;

public class MainPage extends AbstractSigninOptionalPage {
		@SuppressWarnings("unused")
		static private final Logger logger = GlobalSetup.getLogger(MusicGlobalPage.class);
		
		protected Stacker stacker;
		
		private List<PersonMugshotView> recentActivity;
		
		public MainPage() {
			stacker = WebEJBUtil.defaultLookup(Stacker.class);
		}
		
		public List<PersonMugshotView> getRecentActivity() {
			if (recentActivity == null) {
				// we want 4 users with 1 activity block for each one of them
				recentActivity = stacker.getRecentUserActivity(getViewpoint(), 0, 4, 1, false);
			}
			return recentActivity;
		}
}