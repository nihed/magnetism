package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.views.GroupMugshotView;
import com.dumbhippo.server.views.PersonMugshotView;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;

public class MainPage extends AbstractSigninOptionalPage {
		@SuppressWarnings("unused")
		static private final Logger logger = GlobalSetup.getLogger(MainPage.class);
		
		private static final String[] MUGSHOTS = {
			"arthur",
			"lucie",
			"moose",
			"oliver",
			"roni"
		};
		
		protected Stacker stacker;
		
		private ListBean<PersonMugshotView> recentUserActivity;
		private ListBean<GroupMugshotView> recentGroupActivity;
		
		public MainPage() {
			stacker = WebEJBUtil.defaultLookup(Stacker.class);
		}
		
		public ListBean<PersonMugshotView> getRecentUserActivity() {
			if (recentUserActivity == null) {
				// we want 2 users with 1 activity block for each one of them
				recentUserActivity = new ListBean<PersonMugshotView>(stacker.getRecentUserActivity(getViewpoint(), 0, 2, 1, false));
			}
			return recentUserActivity;
		}
		
		public ListBean<GroupMugshotView> getRecentGroupActivity() {
			if (recentGroupActivity == null) {
				// we want 2 groups with 1 activity block for each one of them
				recentGroupActivity = new ListBean<GroupMugshotView>(stacker.getRecentGroupActivity(getViewpoint(), 0, 2, 1));
			}
			return recentGroupActivity;			
		}		
		
		public String getSampleMugshot() {
			return StringUtils.getRandomString(MUGSHOTS);		
		}
		
}