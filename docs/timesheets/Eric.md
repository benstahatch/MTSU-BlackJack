9/28 9:01PM
	Finish creating basic LoginView.kt
		Figuring out how to make it connect to server
10/12 6:03PM
	Updated MainApp.kt and LoginView.kt to make it more
	complete. LoginView.kt is mostly complete with it being
	styling need more work and needing to connect to the sever.
	 CreateUser.kt is also mostly done with it only needing to 
	connect to server and styling needed. LobbyView.kt is mostly done 
	but need to create a sence/place where lobby need to go and need
	 to connect and create servers when requested.
10/13 8:25PM
	Created the AdminView.kt and make buttons to look at
	stuff that admin should be looking at. However, I did 
	not implement the buttons that admin view should look at
	I am waiting for others before implementing it. Also update
	the login view to temporarily include the admin screen because
	the database is not yet created.
11/2 6:20
	Created the blackjackTableView.kt which have the blackjack background
	and have the card gain from the image. Does not include chips and 
	animations. The payout is currently always 1 to 2 ratio. does
	include split and double yet. Only have 1 player instead of 4.
11/9 7:48
	Updated BlackjackTableView.kt to make the hidden dealer card the same 
	size as the visible dealer cards. Added a player total label to display 
	the value of the player's hand, including proper Ace calculations and
	split-hand totals. Also change most of the .server folder to support 
	multiplayer game play and add double down. NetworkMessage and userName in
	blackjacktableview.kt currently does nothing.
11/11 7:16
	Add adminview subview for it. Add a true multi-table set up instead of
	having a hardcoded table. Made it so that a user can be an admin using 
	terminal commands. Add a flip sound effect when the dealer deals cards
	