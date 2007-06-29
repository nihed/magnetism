package com.dumbhippo.server.blocks;

public class AccountQuestionButton {
	private String response;
	private String text;

	public AccountQuestionButton(String text, String response) {
		this.text = text;
		this.response = response;
	}

	public String getResponse() {
		return response;
	}

	public String getText() {
		return text;
	}
}
