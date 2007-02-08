package com.dumbhippo.server.blocks;

public enum AccountQuestion {
	APPLICATION_USAGE {
		public String getTitle() {
			return "Share your application usage";
		}
		
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
		
		public AccountQuestionButton[] getButtons() {
			return new AccountQuestionButton[] {
				new AccountQuestionButton("Share my application usage", "yes"),
				new AccountQuestionButton("No, thanks", "no")
			};
		}
		
		public String getMoreLink() {
			return "/application-usage";
		}
	};
	
	abstract public String getTitle();
	abstract public String getDescription(String answer);
	abstract public AccountQuestionButton[] getButtons();
	abstract public String getMoreLink();
}
