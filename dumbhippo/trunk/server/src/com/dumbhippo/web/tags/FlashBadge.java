package com.dumbhippo.web.tags;

public enum FlashBadge {	

	USER_SUMMARY_250_74 {
		@Override
		public String getOptionName() {
			return "Minimal";
		}
		
		@Override
		public String getRelativeUrl() {
			// should NOT have the build stamp, since it gets cut-and-pasted to people's blogs
			return "/flash/userSummary.swf";			
		}
		
		@Override
		public int getWidth() {
			return 250;
		}
		
		@Override
		public int getHeight() {
			return 74;
		}
		
		@Override
		public int getMinFlashVersion() {
			return 8;
		}		
	},
	USER_SUMMARY_250_180 {
		@Override
		public String getOptionName() {
			return "3 recent updates";
		}
		
		@Override
		public String getRelativeUrl() {
			// should NOT have the build stamp, since it gets cut-and-pasted to people's blogs
			return "/flash/userSummary.swf";						
		}
		
		@Override
		public int getWidth() {
			return 250;
		}
		
		@Override
		public int getHeight() {
			return 180;
		}
		
		@Override
		public int getMinFlashVersion() {
			return 8;
		}		
	},
	USER_SUMMARY_250_255 {
		@Override
		public String getOptionName() {
			return "5 recent updates";
		}
		
		@Override
		public String getRelativeUrl() {
			// should NOT have the build stamp, since it gets cut-and-pasted to people's blogs
			return "/flash/userSummary.swf";						
		}
		
		@Override
		public int getWidth() {
			return 250;
		}
		
		@Override
		public int getHeight() {
			return 255;
		}		
		
		@Override
		public int getMinFlashVersion() {
			return 8;
		}
	},
	NOW_PLAYING_440_120 {
		@Override
		public String getOptionName() {
			return "What I'm listening to";
		}
		
		@Override
		public String getRelativeUrl() {
			// should NOT have the build stamp, since it gets cut-and-pasted to people's blogs
			return "/flash/nowPlaying.swf";
		}
		
		@Override
		public int getWidth() {
			return 440;
		}
		
		@Override
		public int getHeight() {
			return 120;
		}
		
		@Override
		public int getMinFlashVersion() {
			return 7;
		}		
	};
	
	// rename this method so it works as a java bean
	public final String getName() {
		return name();
	}
	// name to appear next to radio button
	public abstract String getOptionName();
	public abstract String getRelativeUrl();
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract int getMinFlashVersion();
}
