package micycle.trapmap;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import micycle.trapmap.graph.Leaf;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Represents a trapezoid object in the trapezoidal map or search structure.
 * <p>
 * Each trapezoid ∆ is determined by:
 * <li>A bottom segment bottom(∆)</li>
 * <li>A top segment top(∆)</li>
 * <li>A left vertex leftp(∆)</li>
 * <li>A right vertex rightp(∆)</li>
 *
 * @author Tyler Chenhall
 * @author Michael Carleton
 */
public final class Trapezoid {

	// Neighbors of this trapezoid
	// Two trapezoids are neighbors if they share a vertical edge
	private Trapezoid neighborUL; // upper left
	private Trapezoid neighborLL; // lower left
	private Trapezoid neighborUR; // upper right
	private Trapezoid neighborLR; // lower right
	private Leaf owner;

	// variables describing the trapezoid shape
	private PVector leftP;
	private PVector rightP;
	private Segment topSeg;
	private Segment botSeg;
	private PShape poly; // polygonal representation of trapezoid
	private List<PVector> polyVertices;

	/**
	 * Boolean flag that indicates whether the mapping to the polygonal face this
	 * trapezoid belongs to has been computed.
	 */
	boolean computedFace = false;
	/**
	 * The original polygon face/cell this trapezoid belongs to (computed lazily).
	 * May remain null (and will always be null if TrapMap was created from segments
	 * only).
	 */
	private PShape face = null;

	/**
	 * Constructs a trapezoid object based on the x boundaries and bounding
	 * segments. Sets the neighbor trapezoids to null currently
	 *
	 * @param left   Left x boundary
	 * @param right  Right x boundary
	 * @param top    Segment determining the upper boundary
	 * @param bottom Segment determining the lower boundary
	 */
	Trapezoid(PVector left, PVector right, Segment top, Segment bottom) {
		leftP = left;
		rightP = right;
		topSeg = top;
		botSeg = bottom;

		neighborUL = null;
		neighborLL = null;
		neighborUR = null;
		neighborLR = null;
		owner = null;
	}

	/**
	 * Get the left bounding point
	 *
	 * @return The left vertex
	 */
	public PVector getLeftBound() {
		return leftP;
	}

	/**
	 * Get the right bounding point
	 *
	 * @return The right bounding vertex
	 */
	public PVector getRightBound() {
		return rightP;
	}

	/**
	 * Get the lower bounding segment
	 *
	 * @return The lower segment
	 */
	public Segment getLowerBound() {
		return botSeg;
	}

	/**
	 * Get the upper bounding segment for the trapezoid
	 *
	 * @return The upper segment
	 */
	public Segment getUpperBound() {
		return topSeg;
	}

	/**
	 * Get the trapezoid which lies to the left of this trapezoid below the left
	 * boundary vertex
	 *
	 * @return The lower left neighbor (possibly null)
	 */
	public Trapezoid getLowerLeftNeighbor() {
		return neighborLL;
	}

	/**
	 * Get the trapezoid which lies to the left of this one, above the left boundary
	 * vertex
	 *
	 * @return the upper left neighbor trapezoid (possibly null)
	 */
	public Trapezoid getUpperLeftNeighbor() {
		return neighborUL;
	}

	public Trapezoid getLowerRightNeighbor() {
		return neighborLR;
	}

	public Trapezoid getUpperRightNeighbor() {
		return neighborUR;
	}

	void setLowerLeftNeighbor(Trapezoid t) {
		neighborLL = t;
	}

	void setUpperLeftNeighbor(Trapezoid t) {
		neighborUL = t;
	}

	void setLowerRightNeighbor(Trapezoid t) {
		neighborLR = t;
	}

	void setUpperRightNeighbor(Trapezoid t) {
		neighborUR = t;
	}

	/**
	 * Set the leaf which contains this trapezoid
	 *
	 * @param l The leaf containing this trapezoid
	 */
	void setLeaf(Leaf l) {
		owner = l;
	}

	/**
	 * Get the leaf containing this trapezoid
	 *
	 * @return The leaf pointing to this trapezoid
	 */
	Leaf getLeaf() {
		return owner;
	}

	/**
	 * Returns the polygonal face (original {@link PShape}) associated with this
	 * trapezoid, if any.
	 * <p>
	 * <b>Important:</b> This value is derived from the trapezoid's boundary
	 * segments' {@code faceA/faceB} references and is therefore only a
	 * <i>topological association</i> rather than a guaranteed point-in-polygon
	 * classification.
	 * <p>
	 * In particular, for concave polygons (or more complex planar graphs), it is
	 * possible for a trapezoid that lies outside the polygon to still be bounded
	 * above and below by segments belonging to that polygon; in such cases this
	 * method may return a non-null face even though a query point in the trapezoid
	 * is not contained by the polygon.
	 * <p>
	 * If you need a strict containment result for a query point, use
	 * {@link micycle.trapmap.TrapMap#findContainingPolygon(double, double)
	 * findContainingPolygon()} (which should perform a geometric containment check)
	 * rather than relying on this method alone.
	 *
	 * @return the associated polygon face, or {@code null} if none is associated or
	 *         if the map was built from segments only.
	 */
	public PShape getFace() {
		if (!computedFace) {
			computedFace = true;
			if (topSeg == null || botSeg == null) {
				face = null;
				return null;
			}

			final PShape tA = topSeg.faceA;
			final PShape tB = topSeg.faceB;
			final PShape bA = botSeg.faceA;
			final PShape bB = botSeg.faceB;

			if (tA != null && (tA == bA || tA == bB)) {
				face = tA;
			} else if (tB != null && (tB == bA || tB == bB)) {
				face = tB;
			} else {
				face = null;
			}
		}
		return face;
	}

	/**
	 * Return the boundary polygon for this trapezoid
	 *
	 * @return The boundary Polygon
	 */
	public PShape getBoundaryPolygon() {
		if (poly == null) {
			poly = getPrivateBoundaryPolygon(leftP, rightP, topSeg, botSeg);
		}
		return poly;
	}

	/**
	 * Gets the four coordinates that make up this trapezoid (from top left
	 * clockwise).
	 *
	 * @return
	 */
	public List<PVector> getBoundaryVertices() {
		if (poly == null) {
			poly = getPrivateBoundaryPolygon(leftP, rightP, topSeg, botSeg);
		}
		return polyVertices;
	}

	/**
	 * Returns the boundary of the trapezoid as a Polygon object for easy display.
	 *
	 * @return The polygon object representing the boundary of the Trapezoid
	 */
	private PShape getPrivateBoundaryPolygon(PVector left, PVector right, Segment top, Segment bottom) {
		final PVector tl = top.intersect(left.x);
		final PVector tr = top.intersect(right.x);
		final PVector bl = bottom.intersect(left.x);
		final PVector br = bottom.intersect(right.x);
		polyVertices = Arrays.asList(tl, tr, br, bl);

		final PShape polygon = new PShape();
		polygon.setFamily(PShape.PATH);
		polygon.setFill(true);
		polygon.setFill(-255);
		polygon.beginShape();
		polygon.vertex(tl.x, tl.y);
		polygon.vertex(tr.x, tr.y);
		polygon.vertex(br.x, br.y);
		polygon.vertex(bl.x, bl.y);
		polygon.endShape(PConstants.CLOSE);
		return polygon;
	}

	/**
	 * Return true if this trapezoid has zero width
	 *
	 * @return True if the trapezoid is a sliver with zero width
	 */
	boolean hasZeroWidth() {
		// In this implementation left/right can be temporarily null during merging
		if (leftP == null || rightP == null) {
			return true;
		}
		// Under symbolic shear, equal-x alone is NOT "zero width" – only same vertex
		// is.
		return leftP.x == rightP.x && leftP.y == rightP.y;
	}

	boolean hasZeroHeight() {
		if (leftP == null || rightP == null) {
			return true;
		}
		// Approx: compare top/bottom at mid x
		final float midX = (leftP.x + rightP.x) * 0.5f;
		return Math.abs(topSeg.intersect(midX).y - botSeg.intersect(midX).y) < 1e-6f;
	}

	@Override
	public String toString() {
		final PVector tl = topSeg.intersect(leftP.x);
		final PVector tr = topSeg.intersect(rightP.x);
		final PVector bl = botSeg.intersect(leftP.x);
		final PVector br = botSeg.intersect(rightP.x);
		return String.join(", ", tl.toString(), tr.toString(), br.toString(), bl.toString());
	}

	@Override
	public int hashCode() {
		return Objects.hash(topSeg, botSeg, leftP, rightP);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Trapezoid)) {
			return false;
		}
		final Trapezoid t = (Trapezoid) o;
		return Objects.equals(topSeg, t.topSeg) && Objects.equals(botSeg, t.botSeg) && Objects.equals(leftP, t.leftP) && Objects.equals(rightP, t.rightP);
	}
}
