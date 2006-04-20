<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Link Swarm</title>
	<link rel="stylesheet" type="text/css" href="/css2/links.css"/>
</head>
<dht:twoColumnPage>
	<dht:sidebarColumn>
		<dht:sidebarBox boxClass="dh-profile-box" title="MY PROFILE">
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
		</dht:sidebarBox>
		<dht:sidebarBox boxClass="dh-controls-box" title="LINK SWARM CONTROLS" more="/spammers-and-freaks">
			<div>
				<input type="checkbox"/> Receive publicly shared links
			</div>
			<div class="dh-separator"><div></div></div>
			<div class="dh-title">
			FREAK LIST
			</div>
			<div>
				<input type="checkbox"/> Spammer McSpammy
			</div>
			<div>
				<input type="checkbox"/> Spams McSpam
			</div>
		</dht:sidebarBox>				
		<dht:sidebarBox boxClass="dh-groups-box" title="MY GROUPS" more="/groups">
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
		</dht:sidebarBox>
	</dht:sidebarColumn>
	<div id="dhContentColumn">
		<div class="dh-zone-box dh-color-web">
			<div class="dh-zone-box-header"><img src="/images2/header_link500.gif"/><div class="dh-zone-box-header-links">Jump to: <a href="">Music Radar</a> | <a href="">TV Party</a></div></div>
			<div class="dh-zone-box-border">
				<div class="dh-zone-box-content dh-color-normal">					
					<div class="dh-title dh-color-web-foreground">FAVES</div>
					<div class="dh-item">
						<div class="dh-title"><a href="">Space Monkeys Land in Harvard Square, Buy Magazines</a></div>
						<div class="dh-blurb">Little green monkies were seen falling from the sky in Harvard Square
						yesterday afternoon. The ones that weren't trapped in trees converged at the newstands.
						</div>
						<div class="dh-extra-info">
							<table cellpadding="0" cellspacing="0">
								<tbody>
									<tr>
										<td align="left">
											<div class="dh-attribution">sent by <a href="" class="dh-name-link">Hanzel</a></div>
										</td>
										<td align="right">
											<div class="dh-counts">2 views | 23 quips | <a href="">remove</a></div>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
					<div class="dh-item">
						<div class="dh-title"><a href="">Government Invests in Kitten Farming</a></div>
						<div class="dh-blurb">Senators from all fifty states convinced President Bush to drop everything and 
						focus on the sudden kitten shortage currently gripping the country.
						</div>
						<div class="dh-extra-info">
							<table cellpadding="0" cellspacing="0">
								<tbody>
									<tr>
										<td align="left">
											<div class="dh-attribution">sent by <a href="" class="dh-name-link">Gretzel</a></div>
										</td>
										<td align="right">
											<div class="dh-counts">42 views | 3 quips  | <a href="">remove</a></div>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
					<div class="dh-more"><a href="">MORE</a> <img src="/images2/arrow_right.gif"/></div>
					<div class="dh-separator"><div></div></div>
					<div class="dh-title dh-color-web-foreground">SHARED WITH ME</div>

					<div class="dh-item">
						<div class="dh-title"><a href="">Space Monkeys Land in Harvard Square, Buy Magazines</a></div>
						<div class="dh-blurb">Little green monkies were seen falling from the sky in Harvard Square
						yesterday afternoon. The ones that weren't trapped in trees converged at the newstands.
						</div>
						<div class="dh-extra-info">
							<table cellpadding="0" cellspacing="0">
								<tbody>
									<tr>
										<td align="left">
											<div class="dh-attribution">sent by <a href="" class="dh-name-link">Hanzel</a></div>
										</td>
										<td align="right">
											<div class="dh-counts">2 views | 23 quips | <a href="">add to faves</a></div>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
					<div class="dh-item">
						<div class="dh-title"><a href="">Government Invests in Kitten Farming</a></div>
						<div class="dh-blurb">Senators from all fifty states convinced President Bush to drop everything and 
						focus on the sudden kitten shortage currently gripping the country.
						</div>
						<div class="dh-extra-info">
							<table cellpadding="0" cellspacing="0">
								<tbody>
									<tr>
										<td align="left">
											<div class="dh-attribution">sent by <a href="" class="dh-name-link">Gretzel</a></div>
										</td>
										<td align="right">
											<div class="dh-counts">42 views | 3 quips  | <a href="">add to faves</a></div>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
					
					<div class="dh-more"><a href="">MORE</a> <img src="/images2/arrow_right.gif"/></div>

					<div class="dh-separator"><div></div></div>
					<div class="dh-title dh-color-web-foreground">SHARED BY ME</div>
					<div class="dh-item">
						<div class="dh-title"><a href="">Space Monkeys Land in Harvard Square, Buy Magazines</a></div>
						<div class="dh-blurb">Little green monkies were seen falling from the sky in Harvard Square
						yesterday afternoon. The ones that weren't trapped in trees converged at the newstands.
						</div>
						<div class="dh-extra-info">
							<table cellpadding="0" cellspacing="0">
								<tbody>
									<tr>
										<td align="left">
											<div class="dh-attribution">sent by <a href="" class="dh-name-link">Hanzel</a></div>
										</td>
										<td align="right">
											<div class="dh-counts">2 views | 23 quips | <a href="">add to faves</a></div>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
					<div class="dh-item">
						<div class="dh-title"><a href="">Government Invests in Kitten Farming</a></div>
						<div class="dh-blurb">Senators from all fifty states convinced President Bush to drop everything and 
						focus on the sudden kitten shortage currently gripping the country.
						</div>
						<div class="dh-extra-info">
							<table cellpadding="0" cellspacing="0">
								<tbody>
									<tr>
										<td align="left">
											<div class="dh-attribution">sent by <a href="" class="dh-name-link">Gretzel</a></div>
										</td>
										<td align="right">
											<div class="dh-counts">42 views | 3 quips  | <a href="">add to faves</a></div>
										</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
					
					<div class="dh-more"><a href="">MORE</a> <img src="/images2/arrow_right.gif"/></div>

				</div>
			</div>
			<div><img src="/images2/bottom_link500.gif"/></div>					
		</div>
	</div>
</dht:twoColumnPage>
</html>
