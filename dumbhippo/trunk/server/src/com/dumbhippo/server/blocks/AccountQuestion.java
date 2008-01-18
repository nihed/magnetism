package com.dumbhippo.server.blocks;

public enum AccountQuestion {
	APPLICATION_USAGE {
		@Override
		public String getTitle() {
			return "Application Statistics";
		}
		
		@Override
		public String getDescription(String answer) {
			if ("yes".equals(answer))
				return 
					"Thanks for sharing your application usage statistics. " +
				    "You may change this setting at any time by visiting your account page.";
			
			else if ("no".equals(answer))
				return 
					"Your application usage statistics will not be shared. " +
			    	"You may change this setting at any time by visiting your account page.";
			else
				return 
					"The Mugshot and Fedora developers are working on new ways " + 
					"to browse available applications based on the applications that " +
					"users use most. Help out by contributing your application " +
					"usage statistics.";
		}
		
		@Override
		public AccountQuestionButton[] getButtons() {
			return new AccountQuestionButton[] {
				new AccountQuestionButton("Share my application usage", "yes"),
				new AccountQuestionButton("No, thanks", "no")
			};
		}
		
		@Override
		public String getMoreLink(String moreParam) {
			return "/applications";
		}
	},
	FACEBOOK_APPLICATION {
		@Override
		public String getTitle() {
			return "Check out Mugshot Application on Facebook";
		}
		
		@Override
		public String getDescription(String answer) {
			if ("yes".equals(answer))
				return 
					"Thank you for adding Mugshot application on Facebook!";		
			else if ("no".equals(answer))
				return 
					"You can find the Mugshot application on Facebook in the future by browsing " +
					"for it in the Facebook application directory.";
			else
				return 
					"We now have a Mugshot application for Facebook that displays your public Mugshot " +
					"activity on your Facebook profile page.";
		}
		
		@Override
		public AccountQuestionButton[] getButtons() {
			return new AccountQuestionButton[] {
				new AccountQuestionButton("No, thanks", "no")
			};
		}
		
		@Override
		public String getMoreLink(String moreParam) {
			return "http://www.facebook.com/add.php?api_key=" + moreParam;
		}
	};
	
	abstract public String getTitle();
	abstract public String getDescription(String answer);
	abstract public AccountQuestionButton[] getButtons();
	abstract public String getMoreLink(String moreParam);
}
