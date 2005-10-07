package com.dumbhippo.web;

import com.dumbhippo.persistence.*;

/**
 * AddLinkBean corresponds to the "add a link" JSF page.
 * 
 * @author dff
 */

public class AddLinkBean {
   private String url;
   private String comment;
   private String recipients;	// TODO: turn this into some collection

   // PROPERTY: url
   public String getURL() { return url; }
   public void setURL(String newValue) { url = newValue; }
   
   // PROPERTY: comment
   public String getComment() { return comment; }
   public void setComment(String newValue) { comment = newValue; }
   
   // PROPERTY: recipients
   public String getRecipients() { return recipients; }
   public void setRecipients(String newValue) { recipients = newValue; }
}