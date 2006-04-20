<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Home</title>
	<link rel="stylesheet" type="text/css" href="/css2/home.css"/>
</head>
<body>
	<div id="dhPage">
		<div id="dhPageHeader">
			<div id="dhHeaderLogo"><img src="/images2/mugshot_logo.gif"/></div>
			<div id="dhSearchBox">
				Search: <input type="text" class="dh-text-input"/> <input type="button" value="Go"/>
			</div>
		</div>
		<div id="dhPageContent">
			<div id="dhSidebarColumn">
				<div class="dh-sidebar-box dh-profile-box">
					<div class="dh-title">
					MY PROFILE
					</div>
					<div class="dh-item">
						<table cellpadding="0" cellspacing="0">
							<tbody>
								<tr valign="top">
									<td>
										<div class="dh-image">
										</div>
									</td>
									<td>
										<div class="dh-next-to-image">
											<div class="dh-name">Monkey Mouth</div>
											<div class="dh-action-link"><a href="/account">Edit account</a></div>
											<div class="dh-action-link"><a href="">Sign out</a></div>
										</div>
									</td>
								</tr>
							</tbody>
						</table>
						<div class="dh-bio">
							I am so special. This is my biography. Please read more about me!
						</div>
					</div>
				</div>
				<div class="dh-sidebar-box dh-groups-box">
					<div class="dh-title">
					MY GROUPS
					</div>
					<div class="dh-item">
						<table cellpadding="0" cellspacing="0">
							<tbody>
								<tr valign="top">
									<td>
										<div class="dh-image">
										</div>
									</td>
									<td>
										<div class="dh-next-to-image">
											<div class="dh-name"><a href="">Sky Devils</a></div>
											<div class="dh-info">7 members</div>
											<div class="dh-info">12 posts</div>
										</div>
									</td>
								</tr>
							</tbody>
						</table>
					</div>
					<div class="dh-item">
						<table cellpadding="0" cellspacing="0">
							<tbody>
								<tr valign="top">
									<td>
										<div class="dh-image">
										</div>
									</td>
									<td>
										<div class="dh-next-to-image">
											<div class="dh-name"><a href="">Happy Onions</a></div>
											<div class="dh-info">17 members</div>
											<div class="dh-info">2 posts</div>
										</div>
									</td>
								</tr>
							</tbody>
						</table>
					</div>
					<div class="dh-more"><a href="">MORE</a></div>
				</div>
			</div>
			<div id="dhContentColumn">
				<div class="dh-zone-box dh-color-web">
					<div class="dh-zone-box-header"><img src="/images2/header_link500.gif"/><div class="dh-zone-box-header-links"><a href="">MORE</a></div></div>				
					<div class="dh-zone-box-border">
						<div class="dh-zone-box-content dh-color-normal">					
							<div class="dh-title dh-color-web-foreground">FAVES</div>
							<div class="dh-shared-link"><a href="">Talking pants all the rage with kids these days</a></div>
							<div class="dh-shared-link"><a href="">Just discovered awesome new kid-with-lightsaber vid</a></div>
							<div class="dh-shared-link"><a href="">Invest in kittens!</a></div>
							<div class="dh-separator"><div></div></div>
							<div class="dh-title dh-color-web-foreground">SHARED WITH ME</div>
							<div class="dh-shared-link"><a href="">Space monkeys land in Harvard Square</a></div>
							<div class="dh-shared-link"><a href="">Government does shady business</a></div>
							<div class="dh-shared-link"><a href="">Biggest ball of wax you've ever seen</a></div>
						</div>
					</div>
					<div><img src="/images2/bottom_link500.gif"/></div>					
				</div>
				<div class="dh-zone-box dh-color-music">
					<div class="dh-zone-box-header"><img src="/images2/header_music500.gif"/><div class="dh-zone-box-header-links"><a href="">MORE</a></div></div>				
					<div class="dh-zone-box-border">
						<div class="dh-zone-box-content dh-color-normal">					
							<div class="dh-title dh-color-music-foreground">CURRENTLY LISTENING TO</div>
							<div class="dh-nowplaying"><div></div></div>
							<div class="dh-separator"><div></div></div>
							<div class="dh-title dh-color-music-foreground">MY RECENT SONGS</div>							
							<div class="dh-song"><a href="">Ice Ice Baby</a>
								<span class="dh-song-details">by <a href="">Vanilla Ice</a> | Play at <a href="">iTunes</a> | <a href="">Yahoo!</a></span>
							</div>
							<div class="dh-song"><a href="">Faraway</a>
								<span class="dh-song-details">by <a href="">Sleater-Kinney</a> | Play at <a href="">iTunes</a> | <a href="">Rhapsody</a></span>
							</div>							
						</div>
					</div>
					<div><img src="/images2/bottom_music500.gif"/></div>					
				</div>				
				<div class="dh-zone-box dh-color-tv">
					<div class="dh-zone-box-header"><img src="/images2/header_tvparty500.gif"/><div class="dh-zone-box-header-links"><a href="">MORE</a></div></div>				
					<div class="dh-zone-box-border">
						<div class="dh-zone-box-content dh-color-normal">					
							<div class="dh-title dh-color-tv-foreground">COMING SOON</div>
						</div>
					</div>
					<div><img src="/images2/bottom_tvparty500.gif"/></div>					
				</div>
			</div>
		</div>
		<dht:footer/>
	</div>
</body>
</html>
