package edu.columbia.threescompany.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.columbia.threescompany.common.Force;
import edu.columbia.threescompany.game.EventMove;
import edu.columbia.threescompany.game.GameMove;
import edu.columbia.threescompany.game.PhysicalMove;
import edu.columbia.threescompany.game.Player;
import edu.columbia.threescompany.game.graphics.GUIGameMove;
import edu.columbia.threescompany.gameobjects.Blob;
import edu.columbia.threescompany.gameobjects.GameObject;
import edu.columbia.threescompany.gameobjects.GameParameters;
import edu.columbia.threescompany.gameobjects.PushBlob;
import edu.columbia.threescompany.graphics.Gui;

public class LocalGameState implements Serializable {
	private static final long serialVersionUID = 8708609010775403554L;
	
	public void executeMove(GameMove move, Gui gui) {
		/* t is our time variable -- basically, we execute GRANULARITY tiny
		 * moves, sequentially. */
		for (int t = 0; t < GameParameters.GRANULARITY_OF_PHYSICS; t++)
			executeMoveStep(move, gui, t);
		deactivateBlobs();
	}

	private void executeMoveStep(GameMove move, Gui gui, int t) {
		for (EventMove instantEvent : move.instantMovesAt(t))
			// These will fire once
			instantEvent.execute();
		for (PhysicalMove granularEvent : move.granularMovesAt(t))
			// These will fire over and over
			granularEvent.execute();
		
		applyForces();
		checkCollisions();
		
		if (gui != null) {
			gui.drawState(this);
			try {
				/* TODO: Adjust this value so moves animate nicely. */
				Thread.sleep(20);
			} catch (InterruptedException exception) {}
		}
	}
	
	private void checkCollisions() {
		for (GameObject obj1 : _gameObjects)
		for (GameObject obj2 : _gameObjects)
			obj1.checkCollision(obj2);
	}

	private void applyForces() {
		for (GameObject obj1 : _gameObjects)
		for (GameObject obj2 : _gameObjects) {
			Force f = obj1.actOn(obj2);
			
			/* Newton's 3rd law: */
			obj1.applyForce(f.inverse());
			obj2.applyForce(f);
		}
	}

	private void deactivateBlobs() {
		for (GameObject obj : _gameObjects)
			if (obj instanceof Blob)
				((Blob) obj).activate(false);
	}
	
	public List<GameObject> getObjects() {
		return _gameObjects;
	}

	public List<Player> getPlayers() {
		return _players;
	}
	
	public int getAP() {
		return _activePlayer.getAbilityPoints();
	}
	
	public void updateActivePlayer(String id) {
		for (Player player : _players) {
			if (id.equals(player.getName())) {
				_activePlayer = player;
				return;
			}
		}
		throw new RuntimeException("Can't set active player to " + id +
								   ", that player doesn't exist!");
	}

	public boolean gameOver() {
		// TODO do all blobs on the board belong to one player?
		return false;
	}

	public static LocalGameState getInitialGameState(List<Player> players) {
		LocalGameState initialGameState =  new LocalGameState();
		initialGameState._activePlayer = players.get(0);
		initialGameState._players = players;
		initialGameState._gameObjects = new ArrayList<GameObject>();
		
		initialGameState._gameObjects.add(new PushBlob(1, 1, players.get(0)));
		initialGameState._gameObjects.add(new PushBlob(3, 3, players.get(1)));
		
		return initialGameState;
	}
	
	public LocalGameState predictOutcome(GUIGameMove guiMove) {
		GameMove move = new GameMove(guiMove);
		LocalGameState clonedState = clone();
		clonedState.executeMove(move, null);
		return clonedState;
	}
	
	protected LocalGameState clone() {
		LocalGameState state = new LocalGameState();
		state._players = _players;
		state._gameObjects = new ArrayList<GameObject>(_gameObjects.size());
		for (GameObject object : _gameObjects)
			state._gameObjects.add(object.clone());
		return state;
	}
	
	private List<GameObject> _gameObjects;
	private List<Player> _players;
	private Player _activePlayer;
}