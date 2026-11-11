# Table21 multiplayer setup (first integration)

## Start the server

1. On the Windows computer that hosts the game, open a terminal in the project folder.
2. Run:

   ```powershell
   .\gradlew.bat runServer
   ```

3. Leave that terminal open while people play. Only one server should be running on port `5555`.
4. If Windows Firewall asks, allow Java on **Private networks**. Do not turn the firewall off.

The server listens on all network interfaces so other computers on the same Wi-Fi can connect.

## Start each game client

On each computer, open the project and run:

```powershell
.\gradlew.bat run
```

The default client address in `src/main/kotlin/com/mtsu/table21/client/CasinoClient.kt` is `10.1.132.232`, the Windows host address used during the connection test. If the host's local IP changes, update `host` to the host computer's current IPv4 address. The port is `5555`.

All computers must be on the same Wi-Fi/LAN. The server computer must remain on and the server program must remain running.

## Play a round

1. Enter a username and connect.
2. Choose **Join Table** or **Create Table** to enter the shared table.
3. Each person chooses **Play This Round** and sets their own bet. A person who does not opt in sits out.
4. Any connected player can press **Deal** after the players who want to participate have opted in and placed bets.
5. The server deals the cards and enforces turn order by seat: Player 1, Player 2, Player 3, Player 4, then Dealer.
6. Only the client whose player is currently active can use Hit, Stand, Split, or Double.
7. When a round ends, players must opt in again for the next round.

## Current scope / limitations

- This integration currently runs one shared table (`Table #1`) with up to four seats. The lobby's Join/Create actions both enter that shared table; a multi-table directory and independently created table IDs are not implemented yet.
- The server owns the shoe, hands, turns, bets, and payouts while the server process is running.
- Wallet balances are currently in memory. They are not yet loaded from or saved to the database, and a player who leaves and rejoins receives the default starting balance.
- The login password and Create Account screen are placeholders. The current connection identifies players by username but does not authenticate or persist accounts.
- The client/server source was checked by compiling the server and game model sources locally. A full Gradle/JavaFX build could not be run in this environment because the project's Gradle 9.7 distribution and JavaFX dependencies were not available for download. Run the Gradle commands above in your configured JDK 25 development environment and share any build error if one appears.
