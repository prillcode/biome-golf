package pro.apdev.biomegolf.net;

/**
 * Outcome of validating + executing a shot request. Returned to caller so the
 * networking/system path can give the player meaningful, non-replayable feedback
 * without leaking server decisions through game semantics (ARCH §8.2).
 */
public enum ShotOutcome {
	/** Ball not found in the acting player's level. */
	BALL_NOT_FOUND,
	/** The ball belongs to a different player. */
	NOT_YOUR_BALL,
	/** The ball is still moving and cannot be struck yet. */
	BALL_MOVING,
	/** The player is too far from the ball to strike it. */
	BALL_TOO_FAR,
	/** The player is not holding a recognised golf club. */
	NO_CLUB,
	/** Aim was too steep (aiming at the ground) to be a legal shot. */
	AIM_NOT_LEGAL,
	/** An active hole exists, but this is not its assigned ball. */
	NOT_ACTIVE_BALL,
	/** The assigned active-hole ball no longer exists and the attempt must be restarted. */
	MISSING_ACTIVE_BALL,
	/** The player's active hole is already complete or capped. */
	HOLE_COMPLETE,
	/** The driver is not allowed while the ball is supported by sand. */
	DRIVER_NOT_ALLOWED_ON_SAND,
	/** The requested trajectory is not legal for the held club. */
	INVALID_SHOT_TYPE,
	/** Shots off a club the player must be holding, and this is not one. */
	UNKNOWN,
	/** The player is a client-light visitor and cannot play. */
	VISITOR,
	/** Launch executed. */
	SUCCESS;

	/**
	 * Player-facing feedback for this outcome. {@link #SUCCESS} and {@link #UNKNOWN}
	 * are silent: success is observed through the authoritative ball motion, and
	 * {@code UNKNOWN} is an internal fall-through.
	 */
	public String description() {
		return switch (this) {
			case BALL_NOT_FOUND -> "[golf] that golf ball is not here";
			case NOT_YOUR_BALL -> "[golf] that golf ball belongs to another player";
			case BALL_MOVING -> "[golf] wait for the ball to stop";
			case BALL_TOO_FAR -> "[golf] walk closer to the ball before taking the next shot";
			case NO_CLUB -> "[golf] hold a golf club to take a shot";
			case AIM_NOT_LEGAL -> "[golf] aim level with the ground";
			case NOT_ACTIVE_BALL -> "[golf] use the ball assigned to the active hole attempt";
			case MISSING_ACTIVE_BALL -> "[golf] your assigned ball is missing; use /golf hole restart";
			case HOLE_COMPLETE -> "[golf] this hole is complete; use /golf hole restart, or /golf round restart after the round ends";
			case DRIVER_NOT_ALLOWED_ON_SAND -> "[golf] Driver cannot be used from sand; select another club";
			case INVALID_SHOT_TYPE -> "[golf] that shot type is not available for the held club";
			case VISITOR -> "[golf] visitors can watch but not play — join on Java to golf";
			case SUCCESS, UNKNOWN -> "";
		};
	}
}
