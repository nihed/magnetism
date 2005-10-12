package com.dumbhippo.web;


public class AddClientBean {
	private String email;
	private SigninBean signin;

	public void setSignin(SigninBean signin) {
		this.signin = signin;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public String doAddClient() {
		signin.initNewAccountFromEmail(getEmail());
		return "main";
	}
}
