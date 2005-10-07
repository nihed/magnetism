package com.dumbhippo.web;

import com.dumbhippo.persistence.*;

/**
 * InviteBean corresponds to the verify account page, the page that
 * a user arrives at from their account confirmation email.
 * 
 * @author dff
 */

public class VerifyBean {
   private String authKey;

   // PROPERTY: authKey
   public String getAuthKey() { return authKey; }
   public void setAuthKey(String newValue) { authKey = newValue; }
}