package edu.columbia.threescompany.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import edu.columbia.threescompany.client.communication.AuthenticationException;
import edu.columbia.threescompany.client.communication.AuthenticationObject;
import edu.columbia.threescompany.client.communication.ExecuteMoveMessage;
import edu.columbia.threescompany.client.communication.GameOverMessage;
import edu.columbia.threescompany.client.communication.ServerConnection;
import edu.columbia.threescompany.client.communication.ServerMessage;
import edu.columbia.threescompany.client.communication.TurnChangeMessage;
import edu.columbia.threescompany.client.communication.UpdateStateMessage;
import edu.columbia.threescompany.common.Coordinate;
import edu.columbia.threescompany.game.GameMove;
import edu.columbia.threescompany.game.Player;
import edu.columbia.threescompany.game.graphics.GUIGameMove;
import edu.columbia.threescompany.gameobjects.Blob;
import edu.columbia.threescompany.gameobjects.GameObject;
import edu.columbia.threescompany.graphics.Gui;
import edu.columbia.threescompany.graphics.PlayerInfoGui;
import edu.columbia.threescompany.graphics.PreGameGui;
import edu.columbia.threescompany.server.BlobsServer;
import edu.columbia.threescompany.server.CommunicationConstants;
import edu.columbia.threescompany.sound.SoundEngine;

public class BlobsClient {
	private static LocalGameState _gameState;
	private static ServerConnection _serverConnection;
	private static ChatThread _chatThread;
	private static Gui _gui;
	private static List<Player> _players;
	private static boolean isAutoMode = false;
	private static int moveCount = 0;
	private static SoundEngine soundEngine;
	
	
	public static void main(String[] args) throws Exception {
		startSoundEngine();
		doPlayerSetup();
		if (args.length == 0)
			args = new String[] {PlayerInfoGui.getServerAddress(), PlayerInfoGui.getServerPort()};
		
		try {
			Object[] streams = authenticatePlayers(args);
			_chatThread = new ChatThread(streams);
		} catch (ConnectException e) {
				JOptionPane.showMessageDialog(null, "Blobs could not connect to the server. You need a server running " +
				                              "even for a hotseat game.");
				return;
		} catch (AuthenticationException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(0);
		}
		_gui = Gui.getInstance(_chatThread, _players);

		_chatThread.setGui(_gui);
		_chatThread.start();

		connectToServer(args);
		
		ServerMessage message;
		
		try {
			while ((message = _serverConnection.receiveMessage()) != null)
				handleMessage(message);
		}
		catch (SocketException e) { 
			playerDisconnectedDialogAndClose();
		}
	}
		
	public static void playerDisconnectedDialogAndClose() {
		JOptionPane.showMessageDialog(null, "Your opponent disconnected from the game. Blobs will now close.",
											"Player Disconnected",
											JOptionPane.ERROR_MESSAGE );
		System.exit(1);
	}

	private static void startSoundEngine() {
		soundEngine = new SoundEngine();
		soundEngine.start();
	}

	public static Object[] authenticatePlayers(String[] args, List<Player> players) throws NumberFormatException, AuthenticationException, IOException {
		_players = players;
		return authenticatePlayers(args);
	}
	
	private static Object[] authenticatePlayers(String[] args) throws AuthenticationException, NumberFormatException, IOException {
		InetAddress addr;
		try {
			addr = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e1) {
			throw new AuthenticationException("Unknown host : " + e1);
		}
		
		Socket socket = new Socket(addr, Integer.valueOf(args[1]));
		Object[] streams = new Object[]{null,null};
		
		try {
			streams = startStreams(socket);
		} catch (IOException e) {
			try {
				socket.close();
			} catch (IOException e2) {
				System.err.println("Socket not closed");
			}
			throw(e);
		} catch (InterruptedException e) {}
		
		AuthenticationObject ao = new AuthenticationObject(_players.toArray(), true);
		ObjectOutputStream ooStream = new ObjectOutputStream(socket.getOutputStream());
		ooStream.writeObject(ao);
		
		BufferedReader in = (BufferedReader) streams[0];
		while (!in.ready()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
		String response = in.readLine();
     	if (!response.equals(CommunicationConstants.AUTHENTICATION_OK)) {
     		throw new AuthenticationException(response);
     	}
     	return streams;
	}
	
	private static Object[] startStreams(Socket socket) throws IOException, InterruptedException {
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		// Enable auto-flush:
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		return new Object[] {in,out};
	}
	
	private static void connectToServer(String[] args) throws UnknownHostException, IOException {
		if (args.length == 3 && args[2].equals("auto")) {
			isAutoMode = true;
			_serverConnection = connectFromHostAndPort(args[0], args[1]);
		} else if (args.length == 2)
			_serverConnection = connectFromHostAndPort(args[0], args[1]);
		else if (args.length == 1)
			_serverConnection = connectFromHost(args[0]);
		else
			_serverConnection = new ServerConnection();

		_serverConnection.sendPlayers(_players, false);
	}

	private static ServerConnection connectFromHost(String host)
	throws IOException {
		return new ServerConnection(host);
	}

	private static ServerConnection connectFromHostAndPort(String host, String port)
	throws IOException {
		return new ServerConnection(host, Integer.valueOf(port));
	}

	private static void gameOverDialog(Player winner) {
		JOptionPane.showMessageDialog(null, (winner.getName() + " is the champion!"),
											"Game over",
											JOptionPane.INFORMATION_MESSAGE );
	}

	private static void doPlayerSetup() {
		GameType gameType = PreGameGui.getGameType();
		
		// We'll always run the embedded server for now. If they're acting as
		// a network client, it won't be used. No biggie.
		// TODO Add explicit support for being a network server or client. 
		runEmbeddedServer();
		if (gameType == GameType.HOTSEAT) {
			_players = PlayerInfoGui.getPlayers(2);
		} else if (gameType == GameType.NETWORK) {
			_players = PlayerInfoGui.getPlayers(1);
		} else {
			throw new RuntimeException("Unknown game type!");
		}
	}
	
	private static void runEmbeddedServer() {
		new Thread(new Runnable() {
			public void run() {
				try {
					new BlobsServer().run();
				} catch (Exception e) {
					e.printStackTrace();
					String message = "Embedded server crashed:\n";
					message += e.toString();
					JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
 				}
			}
		}).start();
	}

	private static void handleMessage(ServerMessage message) throws IOException {
		if (message instanceof UpdateStateMessage) {
			updateState(message);
		} else if (message instanceof TurnChangeMessage) {
			Player activePlayer = ((TurnChangeMessage) message).whoseTurn();
			_gameState.updateActivePlayer(activePlayer);
			if (isLocalPlayer(activePlayer)) {
				yourMove(activePlayer);
			} else {
				notYourTurnDialog();
			}
		} else if (message instanceof ExecuteMoveMessage) {
			if (_players.size() > 1)
				throw new RuntimeException("Shouldn't be receiving ExecuteMoveMessages in hotseat play!");
			
			GameMove move = ((ExecuteMoveMessage) message).getMove();
			_gameState = ((ExecuteMoveMessage) message).getInitialState();
			System.err.println("Received move: " + move);
			_gameState.executeMove(move, _gui);
		} else if (message instanceof GameOverMessage) {
			_gui.addChatLine("Game Over!");
			Player winner = ((GameOverMessage) message).getWinner();
			BlobsClient.getSoundEngine().play(SoundEngine.GAMEOVER);
			gameOverDialog(winner);
		}
	}

	private static boolean isLocalPlayer(Player activePlayer) {
		for (Player player : _players)
		if (activePlayer.equals(player)) return true;
		return false;
	}

	private static void updateState(ServerMessage message) {
		/* This should *never* differ from our state in simulation -- unless
		 * we are still waiting for our move to simulate. */
	
		if (_gameState == null)
			_gameState = ((UpdateStateMessage) message).getGameState();
		_gui.drawState(_gameState);
		
//		System.err.println("Received new state: " + _gameState);
//		if (! _gameState.equals(((UpdateStateMessage) message).getGameState()))
//			throw new RuntimeException("Incorrect game state received!");
	}

	private static void yourMove(Player activePlayer) throws IOException {
		GameMove move = null;
		if (isAutoMode) {
			move = new GameMove(getAutoGuiMove(activePlayer));
		} else {
			move = new GameMove(_gui.getMoveFor(activePlayer));
		}
		// sendMove used to happen in a new thread spawned here for that. But this was broken because the state could
		// change before the send thread sent. I tried to fix this by clone()ing the state and sending a copy. But 
		// then the move references the wrong blobs and life is bad. So for now we'll just send synchronously before
		// we change anything.
		_serverConnection.sendMove(move, _gameState);
		_gameState.executeMove(move, _gui);
	}

	private static GUIGameMove getAutoGuiMove(Player activePlayer) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}
		double offset;
		if (moveCount%4 == 1.0 || moveCount%4 == 2.0) {
			offset = 1.0;
		} else {
			offset = -1.0;
		}
		HashMap<Blob, Coordinate> finalPositions = new HashMap<Blob, Coordinate>();
		for (GameObject gameObject : _gameState.getObjects()) {
			if (gameObject instanceof Blob && activePlayer.equals(gameObject.getOwner())) {
				finalPositions.put((Blob) gameObject, new Coordinate(gameObject.getPosition().x+offset, gameObject.getPosition().y));
			}
		}
		moveCount++;
		return new GUIGameMove(finalPositions);
	}

	private static void notYourTurnDialog() {
		JOptionPane.showMessageDialog(null, "Not your turn", 
											"Hold your horses!",
											JOptionPane.ERROR_MESSAGE );
	}

	public static LocalGameState getGameState() {
		return _gameState;
	}
	
	public static SoundEngine getSoundEngine() {
		return soundEngine;
	}

}
