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

The client defaults to `127.0.0.1:5555`. For local play, start `./gradlew runServer` in one terminal and `./gradlew run` in another. Configure the server's database environment first using [database setup](database/README.md).

For a server on another computer, set the address without editing source:

```bash
TABLE21_SERVER_HOST=192.168.1.25 ./gradlew run
```

In PowerShell:

```powershell
$env:TABLE21_SERVER_HOST = "192.168.1.25"
.\gradlew.bat run
```

`TABLE21_SERVER_PORT` optionally overrides the client port (default `5555`; invalid values fall back to `5555`). The current server listens on `5555`; a different client port is useful with port forwarding. Blank host values fall back to localhost.

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
- The lobby displays the last server-reported player balance, or waits for the first table join before showing a balance.
- House accounting requires MySQL and the house migration. Bets use whole cents; fractional-cent blackjack payouts round half up to cents for both wallets.
- Admin requests the house balance separately; it is never included in table broadcasts. Server-side admin authorization is still a TODO, so the command is not yet access-controlled.
- If the house cannot pay or the database is unavailable at settlement, the round remains pending. After correcting the problem, use **Retry Payout** (`START_ROUND`) to retry settlement. There is no automatic refill.
- House database transfers and in-memory player wallets cannot form a durable transaction. A server crash or an uncertain database commit can require manual reconciliation; persistent player identities and wallets remain a separate task.
