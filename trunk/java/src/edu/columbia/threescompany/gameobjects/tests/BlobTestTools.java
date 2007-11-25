package edu.columbia.threescompany.gameobjects.tests;

import edu.columbia.threescompany.game.Player;
import edu.columbia.threescompany.gameobjects.Blob;
import edu.columbia.threescompany.common.Force;
import edu.columbia.threescompany.gameobjects.GameObject;

public class BlobTestTools {
	public static final Player PLAYER = new Player(0, "Test Player");

	public static Blob getBoringBlob(double x, double y) {
		return new BoringBlob(x, y);
	}
	
	public static class BoringBlob extends Blob {
		private static final long serialVersionUID = 8759393276598506236L;

		public BoringBlob(double x, double y) {
			super(x, y, BlobTestTools.PLAYER);
		}
		
		public BoringBlob(double x, double y, double radius) {
			super(x, y, radius, BlobTestTools.PLAYER);
		}
		
		public Force actOn(GameObject obj) {
			return new Force(0, 0);
		}
		
		public void activate(boolean activated) {
			/* nada */
		}
		
		public Blob clone() {
			return new BoringBlob(_position.x, _position.y, _radius);
		}
	}

}