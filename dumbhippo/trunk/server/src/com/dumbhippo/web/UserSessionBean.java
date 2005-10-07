package com.dumbhippo.web;

import com.dumbhippo.persistence.*;

/**
 * UserSessionBean is a session scoped bean that holds information
 * about the currently logged in user.
 * 
 * @author dff
 *
 */

public class UserSessionBean {
   private Person loggedInUser;
   private String lastSentAuthKey; // TODO: move this elsewhere

   // PROPERTY: userName
   /**
    * Get the currently logged in user.
    * 
    * @return Person object corresponding to user, or null if nobody is logged in.
    */
   public Person getLoggedInUser() { return loggedInUser; }
   public void setLoggedInUser(Person newValue) { loggedInUser = newValue; }
   
   public String getLastSentAuthKey() { return lastSentAuthKey; }
   public void setLastSentAuthKey(String newValue) { lastSentAuthKey = newValue; }

}