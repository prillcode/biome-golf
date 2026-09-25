package pro.apdev.biomegolf.hole;

import java.util.Objects;

import pro.apdev.biomegolf.course.CourseScorecard;
import pro.apdev.biomegolf.course.PlayerCourseState;

/** Server-owned display projection shared by compatibility text and the enhanced HUD payload. */
public record HoleScoringDisplay(
	int acceptedShots,
	int strokes,
	int strokeLimit,
	int penalties,
	boolean terminal,
	int scoreToPar,
	GolfScoreTerm scoreTerm,
	boolean hasCourseScore,
	int courseStrokes,
	int coursePar,
	int courseTotalPar
) {
	public static HoleScoringDisplay from(PlayerHoleState current, PlayerCourseState courseState) {
		Objects.requireNonNull(current, "current");
		int courseStrokes = 0;
		int coursePar = 0;
		int courseTotalPar = 0;
		boolean hasCourseScore = false;
		if (courseState != null) {
			courseTotalPar = courseState.scheduledPar();
			if (courseState.isComplete()) {
				CourseScorecard scorecard = courseState.finalScorecard();
				courseStrokes = scorecard.totalStrokes();
				coursePar = scorecard.totalPar();
				hasCourseScore = true;
			} else {
				courseStrokes = courseState.completedStrokes();
				coursePar = courseState.completedPar();
				hasCourseScore = !courseState.completedHoles().isEmpty();
				if (current.isComplete()) {
					courseStrokes += current.strokes();
					coursePar += current.hole().par();
					hasCourseScore = true;
				}
			}
		}
		return new HoleScoringDisplay(current.acceptedShots(), current.strokes(),
			current.hole().strokeLimit(), current.penaltyStrokes(), current.isComplete(),
			current.isComplete() ? current.scoreToPar() : 0,
			current.isComplete() ? current.scoreTerm() : null, hasCourseScore,
			courseStrokes, coursePar, courseTotalPar);
	}

	public String holeText() {
		if (terminal) {
			return formatToPar(scoreToPar) + " " + scoreTerm;
		}
		return "Shots attempted: " + acceptedShots;
	}

	public String actionBarText() {
		return "[golf] " + holeText() + " | strokes " + strokes + "/" + strokeLimit
			+ (penalties == 0 ? "" : " | penalties " + penalties) + courseText();
	}

	public String courseText() {
		return hasCourseScore
			? " | course " + courseStrokes + " strokes (" + formatToPar(courseStrokes - coursePar) + ")"
			: "";
	}

	public static String formatToPar(int scoreToPar) {
		if (scoreToPar == 0) return "E";
		return scoreToPar > 0 ? "+" + scoreToPar : Integer.toString(scoreToPar);
	}
}
