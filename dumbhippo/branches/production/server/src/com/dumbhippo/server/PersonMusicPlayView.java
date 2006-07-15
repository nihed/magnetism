package com.dumbhippo.server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PersonMusicPlayView {
	
	private PersonView person;
    private Date lastPlayed;
	
	public PersonMusicPlayView() {
	}
	
	public PersonMusicPlayView(PersonView person, Date lastPlayed) {
		this.person = person;
		this.lastPlayed = lastPlayed;
	}

	public PersonView getPerson() {
		return person;
	}
	
	public void setPerson(PersonView person) {
		this.person = person;
	}
	
	public Date getLastPlayed() {
		return lastPlayed;
	}
	
	public void setLastPlayed(Date lastPlayed) {
		this.lastPlayed = lastPlayed;
	}
	
	/**
	 * Returns date last played in the following format: "April 14, 2006"
	 * 
	 * @return date last played in the desired format
	 */
	public String getFormattedLastPlayed() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM d, yyyy");
        if (lastPlayed != null) {
        	return dateFormat.format(lastPlayed);
        }
		return "n/a";
	}
}