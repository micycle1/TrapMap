package micycle.trapezoidalmap.data;

import java.awt.geom.Line2D;

import processing.core.PVector;

/**
 * Represents a segment by its endpoints. Endpoints are stored in order as given
 * by the compareTo function of the Point class.
 *
 * @author Tyler Chenhall
 */
public class Segment {

	public PVector lpoint;
	public PVector rpoint;
	private Line2D.Double l;// for display purposes

	public Segment(PVector one, PVector two) {
		// we store the left, lower point as lpoint
		// the other point is stored as rpoint
		if (one.x == two.x) {
			System.err.println("same X");
		}
		if (compareTo(one, two) <= 0) {
			lpoint = one;
			rpoint = two;
		} else {
			lpoint = two;
			rpoint = one;
		}
		l = new Line2D.Double(lpoint.x, lpoint.y, rpoint.x, rpoint.y);
	}

	/**
	 * Get the left segment endpoint (as ordered by the compareTo function of the
	 * Point class).
	 *
	 * @return The left segment endpoint
	 */
	public PVector getLeftEndPoint() {
		return lpoint;
	}

	/**
	 * Get the right segment endpoint (as ordered by the compareTo function of the
	 * Point class).
	 *
	 * @return The right segment endpoint
	 */
	public PVector getRightEndPoint() {
		return rpoint;
	}

	/**
	 * Get the minimum x value for a point on the segment. Since the endpoints are
	 * ordered horizontally, this is easy
	 *
	 * @return The minimum x value
	 */
	public float getMinX() {
		return lpoint.x;
	}

	/**
	 * Get the maximum x value for a point on the segment.
	 *
	 * @return The maximum x value
	 */
	public float getMaxX() {
		return rpoint.x;
	}

	/**
	 * Get the minimum y value for a point on the segment.
	 *
	 * @return The minimum y value
	 */
	public float getMinY() {
		return Math.min(lpoint.y, rpoint.y);
	}

	/**
	 * Get the maximum y value for a point on the segment.
	 *
	 * @return The maximum y value
	 */
	public float getMaxY() {
		return Math.max(lpoint.y, rpoint.y);
	}

	/**
	 * Get the geometric object corresponding to the line segment. Doing this allows
	 * for easy display (built in!)
	 *
	 * @return The Line2D object representing this segment
	 */
	public Line2D.Double getline() {
		return l;
	}

	@Override
	public boolean equals(Object s) {
		if (!(s instanceof Segment) || s == null) {
			return false;
		}
		Segment ss = (Segment) s;
		return ss.lpoint.equals(this.lpoint) && ss.rpoint.equals(this.rpoint);
	}

	/**
	 * Returns the point on the segment at the given x value or the lower endpoint
	 * if the segment is vertical. The behavior for vertical segments may change
	 * later
	 *
	 * @param x The x-value to intersect the line at
	 * @return The point on the line (segment) at the given x-value
	 */
	public PVector intersect(float x) {
		if (lpoint.x != rpoint.x) {

			float ysum = ((x - lpoint.x)) * (rpoint.y) + ((rpoint.x - x)) * (lpoint.y);
			float yval = ysum / (rpoint.x - lpoint.x);
			return new PVector(x, yval);
		} else {
			return new PVector(lpoint.x, lpoint.y);
		}
	}

	/**
	 * Calculates the slope of a non vertical segment. If the segment might be
	 * vertical, isVertical should be checked first
	 *
	 * @return the slope (if not vertical) or 0 if it is vertical
	 */
	private double getSlope() {
		if (isVertical()) {
			return 0;
		}
		return (rpoint.y - lpoint.y) / ((double) (rpoint.x - lpoint.x));
	}

	/**
	 * Checks if this segment is vertical
	 *
	 * @return True if the segment is vertical
	 */
	private boolean isVertical() {
		return (rpoint.x == lpoint.x);
	}

	/**
	 * Checks to see if this segment object crosses another properly (not a shared
	 * endpoint)
	 *
	 * @param other The other segment to check against
	 * @return True if the segments intersect at a point which is not a common
	 *         vertex
	 */
	public boolean crosses(Segment other) {
		// check if x-ranges overlap
		if ((other.lpoint.x > this.rpoint.x) || (other.rpoint.x < this.lpoint.x)) {
			return false;
		}

		// at this point, the x-ranges overlap
		if (this.isVertical() && other.isVertical()) {// they must lie vertically aligned
			if (this.getMaxY() <= other.getMinY() || this.getMinY() >= other.getMaxY()) {
				return false;
			}
			return true;
		} else if (this.isVertical()) {
			PVector p = other.intersect(this.lpoint.x);
			return (p.y > this.getMinY()) && (p.y < this.getMaxY());
		} else {// neither is a vertical line
			// we use a bounding box technique instead of directly computing the
			// intersection
			// it is quite possible we aren't saving any time with this strategy

			// must find the intersection points
			double slope1 = this.getSlope();
			double slope2 = other.getSlope();
			// use slope1 to calculate 3 b's, same for slope2
			double b00 = this.lpoint.y - this.lpoint.x * slope1;
			double b01 = other.lpoint.y - other.lpoint.x * slope1;
			double b02 = other.rpoint.y - other.rpoint.x * slope1;

			double b10 = other.lpoint.y - other.lpoint.x * slope2;
			double b11 = this.lpoint.y - this.lpoint.x * slope2;
			double b12 = this.rpoint.y - this.rpoint.x * slope2;
			if (((b01 <= b00 && b00 <= b02) || (b01 >= b00 && b00 >= b02)) && ((b11 <= b10 && b10 <= b12) || b11 >= b10 && b10 >= b12)) {
				return this.equals(other) || !(this.lpoint.equals(other.lpoint) || this.lpoint.equals(other.rpoint)
						|| this.rpoint.equals(other.lpoint) || this.rpoint.equals(other.rpoint));

			}
		}

		return false;
	}

	@Override
	public String toString() {
		return lpoint + "     " + rpoint;
	}

	@Override
	public int hashCode() {
		return Float.floatToRawIntBits(lpoint.x + rpoint.x) ^ Float.floatToRawIntBits(lpoint.y + rpoint.y);
	}

	private static int compareTo(PVector a, PVector b) {
		if (a.x < b.x || (a.x == b.x && a.y < b.y)) {
			return -1;
		} else if ((a.x == b.x) && (a.y == b.y)) {
			return 0;
		} else {
			return 1;
		}
	}
}
