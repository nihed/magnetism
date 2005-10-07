package com.dumbhippo.web;

/**
 * InviteBean corresponds to the invite JSF page.
 * 
 * @author dff
 *
 */

public class InviteBean {
   private String fullName;
   private String email;

   // PROPERTY: fullName
   public String getFullName() { return fullName; }
   public void setFullName(String newValue) { fullName = newValue; }

   // PROPERTY: email
   public String getEmail() { return email; }
   public void setEmail(String newValue) { email = newValue; }
}