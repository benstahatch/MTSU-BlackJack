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

1. Choose **Create Account**, enter a unique username and a password of at least 8 characters, and submit. The server creates the account and $1,000 wallet and signs you in. For an existing account, use **LOGIN**. Invalid credentials stay on the login screen.
2. Choose **Join Table** or **Create Table** to enter the shared table.
3. Each person chooses **Play This Round** and enters a fresh wager. **Set Bet** selects it without deducting chips; **Deal** submits the initiating player’s current input and starts the round. Other participating players must use **Set Bet** first. A person who does not opt in sits out.
4. Any connected player can press **Deal** after the players who want to participate have opted in and placed bets.
5. The server deals the cards and enforces turn order by seat: Player 1, Player 2, Player 3, Player 4, then Dealer.
6. Only the client whose player is currently active can use Hit, Stand, Split, or Double.
7. When a round ends, players must opt in again for the next round. Opting in clears the prior wager and hand. Invalid wagers block Deal; the server never falls back to a previous wager.

## Current scope / limitations

- This integration currently runs one shared table (`Table #1`) with up to four seats. The lobby's Join/Create actions both enter that shared table; a multi-table directory and independently created table IDs are not implemented yet.
- The server owns the shoe, hands, turns, bets, and payouts while the server process is running.
- Player wallets are loaded from MySQL by authenticated user ID at table join. The server saves the resulting balance after initial wagers, additional split/double wagers, push refunds, ordinary wins, and blackjack payouts. Zero-balance accounts remain valid.
- LOGIN and REGISTER send URL-safe Base64 encoded credentials to the server. Base64 is only protocol encoding, not security. TODO: production credentials need encrypted transport/TLS; TLS is not implemented here. Raw requests and passwords must never be logged.
- JOIN_TABLE uses the authenticated socket identity and ignores any supplied username. One account can have only one table session, including a disconnected session awaiting round settlement. Reconnect and log in again to establish a new socket session.
- The client retains the authenticated ID, username, and latest server-reported balance. Logout clears them. The lobby does not calculate wallet balances.
- House accounting requires MySQL and the house migration. Bets use whole cents; fractional-cent blackjack payouts round half up to cents for both wallets.
- Admin requests the house balance separately; it is never included in table broadcasts. Server-side admin authorization is still a TODO, so the command is not yet access-controlled.
- If the house cannot pay or the database is unavailable at settlement, the round remains pending. After correcting the problem, use **Retry Payout** (`START_ROUND`) to retry settlement. There is no automatic refill.
- House transfers, in-memory balances, and individual player wallet writes are not one durable transaction. A crash, partial write, or uncertain database commit can require manual reconciliation. If a player write fails after a transfer, the server reports an error and halts the table; do not retry payouts or restart play until the operator reconciles player and house balances. A server restart clears this halt, so reconciliation must happen first. No distributed transaction or crash recovery system is implemented.
- Run a single casino server against these wallets. Cross-server account locking, analytics, and game-history reporting are outside this task.

## Database access and manual persistence check

Only the **server** (and database integration tests) needs `TABLE21_DB_URL`,
`TABLE21_DB_USER`, and `TABLE21_DB_PASSWORD`. Follow [database setup](database/README.md)
to initialize MySQL. Clients never connect to MySQL and need only optional
`TABLE21_SERVER_HOST` and `TABLE21_SERVER_PORT`, defaulting to `127.0.0.1` and `5555`.

1. Start MySQL and `./gradlew runServer` with database environment variables configured.
2. In another terminal run `./gradlew run`; create an account with a unique username.
3. Verify the lobby balance is $1,000. Join, choose Play This Round, bet $10, and deal.
4. Play to completion and record the final balance. A normal win gives $1,010,
   a loss $990, a push $1,000, and a natural blackjack $1,015 (without extra wagers).
5. Leave the table, click Logout, and close the app. Run it again and log in with
   the same credentials. Verify the lobby and joined table both show the recorded balance.
6. Repeat with another account to verify its independent balance, then log back
   into the first account. Invalid passwords must not open the lobby.
7. Optionally restart the server between completed rounds and repeat login to
   verify that the balance comes from MySQL, not server memory.
