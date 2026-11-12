# Benjamin Timesheet

## September 16th - 2 hours

Accomplishments:
Set up the initial GitHub repository, organized the project structure,
and assigned team responsibilities.

What needs to be done next:
Begin networking and integration work.

## October 8th - 2 hours

Accomplishments:
Built the basic Kotlin server, added client connections, tested multiple
clients, created network message handling, and started table session
management.

What needs to be done next:
Connect table sessions to client commands and begin betting/game service
integration.

## October 13th - 2 hours

Accomplishments:
Connected JavaFX login to the Kotlin casino server using CasinoClient,
passed the client connection between views, and verified communication
with a successful PING & PONG.

What needs to be done next:
Connect the lobby buttons to the server commands, display the player
chip balance, and continue betting and game integration.

## November 10 - 2 hours

Integrated the multiplayer UI with the latest database and server code,
removed the hard coded server address so local connections work by default,
added configurable server host and port settings, and verified the full project
with 87 passing tests.

Added player balance synchronization, a persistent $100,000 house balance,
server sided wager and payout transfers, Admin house balance display, and fixed
Blackjack table so the interface scales correctly when resizing the window.

What needs to be done next:
Merge the integration branch into main, test multiple clients at the same table,
verify multiplayer across separate computers, and continue connecting
persistent player wallets and game history to the multiplayer server.
