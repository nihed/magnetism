package com.dumbhippo.web.tags;

public enum FlashBadge {
	
	NOW_PLAYING_440_120 {	
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
	},
	USER_SUMMARY_250_74 {
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
	};
	
	public abstract String getRelativeUrl();
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract int getMinFlashVersion();
}
