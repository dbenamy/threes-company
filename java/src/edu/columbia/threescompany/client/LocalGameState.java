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
import edu.columbia.threescompany.gameobjects.APCIPoint;
import edu.columbia.threescompany.gameobjects.AnchorPoint;
import edu.columbia.threescompany.gameobjects.Blob;
import edu.columbia.threescompany.gameobjects.DeathRayBlob;
import edu.columbia.threescompany.gameobjects.ExplodingBlob;
import edu.columbia.threescompany.gameobjects.ForceBlob;
import edu.columbia.threescompany.gameobjects.GameObject;
import edu.columbia.threescompany.gameobjects.GameParameters;
import edu.columbia.threescompany.gameobjects.Hole;
import edu.columbia.threescompany.gameobjects.PullBlob;
import edu.columbia.threescompany.gameobjects.PushBlob;
import edu.columbia.threescompany.gameobjects.SlipperyBlob;
import edu.columbia.threescompany.graphics.Gui;
import edu.columbia.threescompany.sound.SoundEngine;

public class LocalGameState implements Serializable {
	private static final long serialVersionUID = 8708609010775403554L;
	
	public void executeMove(GameMove move, Gui gui) {
		boolean playSounds = gui != null;
		/* t is our time variable -- basically, we execute GRANULARITY tiny
		 * moves, sequentially. */
		double turnLength = move.getDuration();
		double initialTurnLength = turnLength;
		if (move.hasActivations())
			turnLength += GameParameters.ADDITIONAL_SIMULATION_LENGTH;
		
		for (int t = 0; t < turnLength; t++) {
			if (t < initialTurnLength && !move.hasMoves()) {
				t = move.firstActivation();	/* Skip to simulation phase */;
				initialTurnLength = move.firstActivation();
			}
			
			/* Issue 63: If all relevant blobs are dead, stop simulating! */
			if (t > initialTurnLength && turnLength > initialTurnLength && !move.hasActivationsAfter(t)) {
				break;
			}
			executeMoveStep(move, gui, t, (int) turnLength);
		}
		
		deactivateBlobs();
		growBlobs();
		resetAP();
		checkCollisions(playSounds);
		
		System.out.println("Game objects after executing move:");
		for (GameObject item : _gameObjects) {
			System.out.println(item);			
		}
	}

	private void resetAP() {
		for (Player player : _players) {
			player.setActionPoints(10.0d);
			
		}
	}

	public void executeMove(GameMove move) {
		executeMove(move, null);
	}

	private void growBlobs() {
		for (GameObject obj : _gameObjects) obj.grow();
	}

	private void executeMoveStep(GameMove move, Gui gui, int t, int tmax) {
		boolean playSounds = gui != null;
		long startTime = System.currentTimeMillis();
		
		for (PhysicalMove granularMove : move.granularMovesAt(t))
			granularMove.execute(this);
		for (EventMove eventMove : move.eventMovesAt(t)) {
			eventMove.execute(this);
			if (playSounds && eventMove.getMoveType() == EventMove.MOVE_TYPE.ACTIVATE) {
				if (eventMove.getTarget() instanceof DeathRayBlob)
					BlobsClient.getSoundEngine().play(SoundEngine.LASER);
				else if (eventMove.getTarget() instanceof ExplodingBlob)
					BlobsClient.getSoundEngine().play(SoundEngine.EXPLODE);
			}
		}
			
		applyForces(t, tmax);
		checkCollisions(playSounds);
		
		if (gui != null) {
			gui.drawState(this);
			try {
				/* Smooth out the simulation by taking this into account
				 * (it's usually < 5 ms, though)
				 */
				long elapsedTime = System.currentTimeMillis() - startTime;
				long sleepTime = sleepTimeForFrame(t, tmax) - elapsedTime;
				if (sleepTime > 0)
					Thread.sleep(sleepTime);
			} catch (InterruptedException exception) {}
		}
	}

	private long sleepTimeForFrame(int t, int tmax) {
		/* Starts at 0.2 * GAP, ends at 3.3 * GAP, moves linearly */
		return GameParameters.AVERAGE_MS_FRAME_GAP;
		//return (long) (GameParameters.AVERAGE_MS_FRAME_GAP*
					  //(0.2 + 3.y * ((double) t / (double) tmax)));
	}

	private void checkCollisions(boolean playSounds) {
		List<GameObject> killList = new ArrayList<GameObject>();
		for (GameObject obj1 : _gameObjects)
		for (GameObject obj2 : _gameObjects) {
			if (obj1.isDead() || obj2.isDead()) continue;
			if (!obj1.checkCollision(obj2)) continue;
			
			/* So, living-obj1 asserts that it should hit living-obj2. */
			if (!(obj2 instanceof Blob)) continue;
			
			if (obj1 instanceof Hole) {
				holeCollidesWithBlob(killList, (Hole) obj1, (Blob) obj2);
				continue;
			}
			
			if (obj1 instanceof Blob)
				twoBlobsCollide(killList, obj1, obj2);
		}
		
		for (GameObject obj : killList) {
			obj.die();
			if (playSounds) 
				BlobsClient.getSoundEngine().play(SoundEngine.BUBBLE);
		}
	}

	private void holeCollidesWithBlob(List<GameObject> killList, Hole hole, Blob blob) {
		hole.shrink(blob.getRadius());
		killList.add(blob);
		if (hole.getRadius() <= 0) killList.add(hole);
	}

	private void twoBlobsCollide(List<GameObject> killList, GameObject obj1, GameObject obj2) {
		if (killList.contains(obj1) || killList.contains(obj2)) return;
		
		if (obj2.getRadius() <= obj1.getRadius())
			addToKillList(killList, obj2);
			
		if (obj1.getRadius() <= obj2.getRadius() * (1 + GameParameters.PERCENTAGE_DIFFERENCE_FOR_KILL)) {
			addToKillList(killList, obj1);
		} else {
			obj1.setRadius((obj1.getRadius() - obj2.getRadius()) + 
					GameParameters.BLOB_INITIAL_SIZE / GameParameters.BLOB_GROWTH_FACTOR);
		}
	}
	
	private void addToKillList(List<GameObject> killList, GameObject gameObject) {
		if (gameObject.isDead() || killList.contains(gameObject)) return;
		killList.add(gameObject);
	}
	
	private void applyForces(int t, int tmax) {
		for (GameObject obj1 : _gameObjects) {
			for (GameObject obj2 : _gameObjects) {
				if (obj1.isDead() || obj2.isDead()) continue;
				if (!(obj1 instanceof Blob && obj2 instanceof Blob)) continue;
				
				if (obj1 instanceof ForceBlob &&
					obj1.distanceFrom(obj2) > GameParameters.FORCE_RADIUS)
					continue;
				
				Force f = obj1.actOn(obj2);
				
				/* Not a tweakable vvv -- don't change it! It's based on the
				 * integral int(0, 1) 2x dx = 1. */
				double modifier = 2.0 * (tmax - t) / (double) tmax;
				f = f.times(modifier);
				
				/* Newton's 3rd law: */
				obj1.applyForce(f.inverse());
				obj2.applyForce(f);
				
				checkOffBoard(obj1, obj2);
			}
		}
	}

	private void checkOffBoard(GameObject obj1, GameObject obj2) {
		if (obj1.getPosition().x < 0 || obj1.getPosition().x >= GameParameters.BOARD_SIZE ||
				obj1.getPosition().y < 0 || obj1.getPosition().y >= GameParameters.BOARD_SIZE)
			obj1.die();
		if (obj2.getPosition().x < 0 || obj2.getPosition().x >= GameParameters.BOARD_SIZE ||
				obj2.getPosition().y < 0 || obj2.getPosition().y >= GameParameters.BOARD_SIZE)
			obj2.die();
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
	
	public double getAP() {
		return _activePlayer.getActionPoints();
	}
	
	public void updateActivePlayer(Player newPlayer) {
		for (Player player : _players) {
			if (newPlayer.equals(player)) {
				_activePlayer = player;
				return;
			}
		}
		throw new RuntimeException("Can't set active player to " + newPlayer.getName() +
								   ", that player doesn't exist!");
	}

	public boolean gameOver() {
		int livingPlayers = 0;
		for (Player player : _players)
			if (hasAnyBlobsLeft(player)) livingPlayers++;
		
		return (livingPlayers < _players.size());
	}

	private boolean hasAnyBlobsLeft(Player player) {
		for (GameObject obj : _gameObjects)
			if (obj instanceof Blob && !obj.isDead() && obj.getOwner().equals(player))
				return true;
		
		return false;
	}
	
	private LocalGameState() {
		_gameObjects = new ArrayList<GameObject>();
		_players = new ArrayList<Player>();
	}
	
	public static LocalGameState getSpecifiedGameState(List<GameObject> objects) {
		LocalGameState state = new LocalGameState();
		for (GameObject obj : objects) state.addObject(obj);
		
		return state;
	}
	
	/* For spawning, hole/spot creation, and testing. */
	public void addObject(GameObject obj) {
		_gameObjects.add(obj);
		Player owner = obj.getOwner();
		if (owner != Player.NULL_PLAYER && !_players.contains(owner))
			_players.add(owner);
	}
	
	public static LocalGameState getInitialGameState(List<Player> players) {
		LocalGameState initialGameState =  new LocalGameState();
		initialGameState._activePlayer = players.get(0);
		initialGameState._players = players;
		initialGameState._gameObjects = new ArrayList<GameObject>();
		
		initialGameState.addObject(new PushBlob((GameParameters.BOARD_SIZE/10), GameParameters.BOARD_SIZE*1/8, getRandomBlobSize(), players.get(0)));
		initialGameState.addObject(new PushBlob((GameParameters.BOARD_SIZE/10), GameParameters.BOARD_SIZE*3/8, getRandomBlobSize(), players.get(0)));
		initialGameState.addObject(new PullBlob((GameParameters.BOARD_SIZE/10), GameParameters.BOARD_SIZE*5/8, getRandomBlobSize(), players.get(0)));
		initialGameState.addObject(new PullBlob((GameParameters.BOARD_SIZE/10), GameParameters.BOARD_SIZE*7/8, getRandomBlobSize(), players.get(0)));
		initialGameState.addObject(new DeathRayBlob((GameParameters.BOARD_SIZE/5), GameParameters.BOARD_SIZE*2/8, getRandomBlobSize(), players.get(0)));
		initialGameState.addObject(new SlipperyBlob((GameParameters.BOARD_SIZE/5), GameParameters.BOARD_SIZE*4/8, getRandomBlobSize(), players.get(0), initialGameState));
		initialGameState.addObject(new ExplodingBlob((GameParameters.BOARD_SIZE/5), GameParameters.BOARD_SIZE*6/8, getRandomBlobSize(), players.get(0), initialGameState));
		
		initialGameState.addObject(new PushBlob(GameParameters.BOARD_SIZE-(GameParameters.BOARD_SIZE/10), GameParameters.BOARD_SIZE*1/8, getRandomBlobSize(), players.get(1)));
		initialGameState.addObject(new PushBlob(GameParameters.BOARD_SIZE-(GameParameters.BOARD_SIZE/10), GameParameters.BOARD_SIZE*3/8, getRandomBlobSize(), players.get(1)));
		initialGameState.addObject(new PullBlob(GameParameters.BOARD_SIZE-(GameParameters.BOARD_SIZE/10), GameParameters.BOARD_SIZE*5/8, getRandomBlobSize(), players.get(1)));
		initialGameState.addObject(new PullBlob(GameParameters.BOARD_SIZE-(GameParameters.BOARD_SIZE/10), GameParameters.BOARD_SIZE*7/8, getRandomBlobSize(), players.get(1)));
		initialGameState.addObject(new DeathRayBlob(GameParameters.BOARD_SIZE-(GameParameters.BOARD_SIZE/5), GameParameters.BOARD_SIZE*2/8, getRandomBlobSize(), players.get(1)));
		initialGameState.addObject(new SlipperyBlob(GameParameters.BOARD_SIZE-(GameParameters.BOARD_SIZE/5), GameParameters.BOARD_SIZE*4/8, getRandomBlobSize(), players.get(1), initialGameState));
		initialGameState.addObject(new ExplodingBlob(GameParameters.BOARD_SIZE-(GameParameters.BOARD_SIZE/5), GameParameters.BOARD_SIZE*6/8, getRandomBlobSize(), players.get(1), initialGameState));
		
		initialGameState.addObject(new AnchorPoint(GameParameters.BOARD_SIZE/2, GameParameters.BOARD_SIZE/4));
		initialGameState.addObject(new APCIPoint(GameParameters.BOARD_SIZE/2, GameParameters.BOARD_SIZE*3/4));
		initialGameState.addObject(new Hole(GameParameters.BOARD_SIZE/2, GameParameters.BOARD_SIZE/2, GameParameters.BLOB_SIZE_LIMIT * 2));
		
		return initialGameState;
	}

	private static double getRandomBlobSize() {
		return Math.random() *
			   (GameParameters.BLOB_SIZE_LIMIT - GameParameters.BLOB_INITIAL_SIZE) / 2 + 
			   GameParameters.BLOB_INITIAL_SIZE;
	}
	
	public LocalGameState predictOutcome(GUIGameMove guiMove) {
		LocalGameState clonedState = clone();
		clonedState.executeMove(new GameMove(guiMove));
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
	
	public String toString() {
		/* This is debug only */
		StringBuilder s = new StringBuilder("GameState:\n");
		for (GameObject obj : _gameObjects)
			s.append(obj.toString() + " at " + obj.getPosition().toString() + 
					" owned by " + obj.getOwner() + "\n");
		return s.toString();
	}
	
	public Player getWinner() {
		/* Precondition: Game is over. */
		for (Player player : _players)
			if (hasAnyBlobsLeft(player)) return player;
		
		return Player.NULL_PLAYER;
	}
	
	private List<GameObject> _gameObjects;
	private List<Player> _players;
	private Player _activePlayer;

}
