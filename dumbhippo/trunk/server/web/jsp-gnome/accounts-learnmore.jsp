<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<head>
	<title>Accounts Service</title>
	<gnome:stylesheet name="site" iefixes="true"/>	
</head>

<body>
	<gnome:page currentPageLink="applications">
	    <h1>Accounts Service</h1>
   		<div class="gnome-learn-more-text">
   		    <h2>Accounts Service on the Desktop</h2>
		    <p>		    
		      	The GNOME desktop now has a centralized service that stores user's web accounts, 
		      	such as Google or Twitter. The service stores the accounts in a central place on the 
		      	desktop and lets multiple applications use the same account without having the user 
		      	enter it twice. The service also provides a dialog that applications can use to let 
		      	the user manage their accounts.
		    </p>
		    <h2>Accounts Service on the Server</h2>  	
		    <p>
		        Your set of web accounts can be saved on the server, so that they are synchronized 
		        between all the desktops you use. We only save your usernames on the server, and never 
		        send your passwords to the server. To enable this feature, you need to create a 
		        <a href="/account">GNOME Online</a> account and check the "Save Accounts Online" option
		        in the accounts dialog on the desktop. After that, you can always review your set
		        of saved accounts on <a href="/account">your account page</a>.    
		    </p>    	
		    <h2>Accounts Service Information for Application Developers</h2>
		    <p>	        	    
		        Online Accounts is a new DBus service available at org.gnome.OnlineAccounts that 
		        desktop applications can use to create and retrieve user's web accounts.
		        <br> 
		        <br>
		        The Online Accounts service:
                <ul>
                <li> Stores online accounts information in GConf and passwords in GNOME Keyring.
                <li> Provides a dialog for adding and removing online accounts, as well as supplying passwords for existing accounts. 
                <li> Gets the accounts information from online.gnome.org server if the user opted in to use that service, otherwise relies on GConf. 
                </ul>               
                The benefits of using the Online Accounts service:
                <ul>
                <li>Users only have to enter or update information for a particular account type once.
                <li>You do not need to do own GConf and GNOME Keyring manipulations.
                <li>You do not need to create a custom dialog for getting the username and a password. 
                </ul>                
                You can find a sample usage of the Online Accounts service in the <a href="http://svn.gnome.org/svn/bigboard/trunk/">code for the Bigboard sidebar</a>.
                You can add new account types by using <a href="/account-type-add">this form</a>, after which they will show up on the <a href="/account">account page</a>. 
                The new account types will be available on the desktop once you restart the desktop-data-engine and web-login-driver processes. Please check in with us 
                to see if the account type you want to create already exists. Feel free use <a href="http://mail.gnome.org/mailman/listinfo/online-desktop-list">online-desktop-list@gnome.org mailing list</a>
                or irc.gnome.org #online-desktop channel for any questions.
            </p>    
	    </div> 
	</gnome:page>
</body>
</html>