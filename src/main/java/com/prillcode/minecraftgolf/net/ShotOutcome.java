package com.prillcode.minecraftgolf.net;

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
	/** The player's active hole is already complete or capped. */
	HOLE_COMPLETE,
	/** Shots off a club the player must be holding, and this is not one. */
	UNKNOWN,
	/** Launch executed. */
	SUCCESS
}
