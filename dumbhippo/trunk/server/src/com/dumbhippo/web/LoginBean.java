package com.dumbhippo.web;

import com.dumbhippo.persistence.*;

/** 
 * LoginBean allows a user to login with a username/password, until we decide
 * that we absolutely don't want to do that.  Consider it testing for now.
 * 
 * @author dff
 *
 */

public class LoginBean {
   private String name;
   private String password;

   // PROPERTY: name
   public String getName() { return name; }
   public void setName(String newValue) { name = newValue; }

   // PROPERTY: password
   public String getPassword() { return password; }
   public void setPassword(String newValue) { password = newValue; }
}
