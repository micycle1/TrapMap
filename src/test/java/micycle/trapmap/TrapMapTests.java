package micycle.trapmap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import processing.core.PConstants;
import processing.core.PShape;

class TrapMapTests {

	@Test
	void testPointLocationFromSegments() {
		final List<Segment> segments = new ArrayList<>();

		// larger box
		Segment s1 = new Segment(0, 0, 100, 0); // horizontal top
		Segment s2 = new Segment(0, 100, 100, 100); // horizontal bottom
		Segment s3 = new Segment(0, 0, 0, 100); // vertical left
		Segment s4 = new Segment(100, 0, 100, 100); // vertical right
		segments.addAll(Arrays.asList(s1, s2, s3, s4));

		// smaller (nested) box
		Segment s5 = new Segment(25, 25, 75, 25); // horizontal top
		Segment s6 = new Segment(25, 75, 75, 75); // horizontal bottom
		Segment s7 = new Segment(25, 25, 25, 75); // vertical left
		Segment s8 = new Segment(75, 25, 75, 75); // vertical right
		segments.addAll(Arrays.asList(s5, s6, s7, s8));

		final TrapMap trapMap = new TrapMap(segments);

		// smaller box region
		Set<Trapezoid> t1 = trapMap.findFaceTrapezoids(26.1, 26.2);
		Set<Trapezoid> t2 = trapMap.findFaceTrapezoids(30, 30);
		Set<Trapezoid> t3 = trapMap.findFaceTrapezoids(60, 60);
		assertEquals(true, t1.equals(t2));
		assertEquals(true, t2.equals(t3));
		assertEquals(0, trapMap.findFaceTrapezoids(999, 999).size());

		// larger box region
		Set<Trapezoid> t4 = trapMap.findFaceTrapezoids(20.1, 20.1);
		Set<Trapezoid> t5 = trapMap.findFaceTrapezoids(50, 90);
		Set<Trapezoid> t6 = trapMap.findFaceTrapezoids(80, 80);
		assertEquals(true, t4.equals(t5));
		assertEquals(true, t5.equals(t6));

		assertEquals(false, t1.equals(t4));
	}

	@Test
	void testPointLocationFromQuads() {

		// top and bottom share a horizontal edge [(0,0) -> (100,0)]

		final PShape top = new PShape();
		top.setFamily(PShape.PATH);
		top.beginShape();
		top.vertex(0, 0);
		top.vertex(100, 0);
		top.vertex(100, -100);
		top.vertex(0, -100);
		top.endShape(PConstants.CLOSE);

		final PShape bottom = new PShape();
		bottom.setFamily(PShape.PATH);
		bottom.beginShape();
		bottom.vertex(0, 0);
		bottom.vertex(100, 0);
		bottom.vertex(150, 50);
		bottom.vertex(100, 100);
		bottom.vertex(0, 100);
		bottom.endShape(PConstants.CLOSE);

		final List<PShape> polygons = new ArrayList<>(Arrays.asList(top, bottom));
		final TrapMap trapMap = new TrapMap(polygons);

		assertEquals(top, trapMap.findContainingPolygon(50, -50));
		assertEquals(bottom, trapMap.findContainingPolygon(50, 50));
		assertNull(trapMap.findContainingPolygon(999, 999)); // test query point outside polygons
		assertNull(trapMap.findContainingPolygon(-0.00001f, 0)); // test query point outside polygons
//		assertEquals(5, trapMap.getAllTrapezoids().size());
	}

	@Test
	void testPointLocationFromTriangles() {

		// top and bottom share an vertical edge [(0,-50) -> (0,50)]

		final PShape left = new PShape();
		left.setFamily(PShape.PATH);
		left.beginShape();
		left.vertex(0, -50);
		left.vertex(0, 50);
		left.vertex(-50, 0);
		left.endShape(PConstants.CLOSE);

		final PShape right = new PShape();
		right.setFamily(PShape.PATH);
		right.beginShape();
		right.vertex(0, -50);
		right.vertex(0, 50);
		right.vertex(50, 0);
		right.endShape(PConstants.CLOSE);

		final List<PShape> polygons = new ArrayList<>(Arrays.asList(left, right));
		final TrapMap trapMap = new TrapMap(polygons);

		assertEquals(left, trapMap.findContainingPolygon(-25, 0));
		assertEquals(right, trapMap.findContainingPolygon(25, 0));
		assertNull(trapMap.findContainingPolygon(999, 999)); // test query point outside polygons
		assertNull(trapMap.findContainingPolygon(50.00001, 0)); // test query point outside polygons
	}

	/**
	 * Polygon with redundant collinear vertex along an edge. Many polygon pipelines
	 * generate this; trapezoidal decomposition should remain robust.
	 */
	@Test
	void testPointLocationWithRedundantCollinearVertex() {
		final PShape poly = new PShape();
		poly.setFamily(PShape.PATH);
		poly.beginShape();
		poly.vertex(0, 0);
		poly.vertex(50, 0); // redundant point on the bottom edge
		poly.vertex(100, 0);
		poly.vertex(100, 100);
		poly.vertex(0, 100);
		poly.endShape(PConstants.CLOSE);

		final TrapMap trapMap = new TrapMap(List.of(poly));

		assertEquals(poly, trapMap.findContainingPolygon(10, 10));
		assertNull(trapMap.findContainingPolygon(-1, 10));
		assertTrue(trapMap.getAllTrapezoids().size() > 0);
	}

	/**
	 * Query exactly on a shared polygon edge (boundary case). Also verifies
	 * near-boundary classification remains sensible.
	 */
	@Test
	void testPointLocationOnSharedEdge() {
		final PShape top = new PShape();
		top.setFamily(PShape.PATH);
		top.beginShape();
		top.vertex(0, 0);
		top.vertex(100, 0);
		top.vertex(100, -100);
		top.vertex(0, -100);
		top.endShape(PConstants.CLOSE);

		final PShape bottom = new PShape();
		bottom.setFamily(PShape.PATH);
		bottom.beginShape();
		bottom.vertex(0, 0);
		bottom.vertex(100, 0);
		bottom.vertex(100, 100);
		bottom.vertex(0, 100);
		bottom.endShape(PConstants.CLOSE);

		final TrapMap trapMap = new TrapMap(List.of(top, bottom));

		// exactly on the shared edge y=0
		assertDoesNotThrow(() -> trapMap.findContainingPolygon(50, 0));

		// just above/below should classify deterministically
		assertEquals(top, trapMap.findContainingPolygon(50, -0.00001));
		assertEquals(bottom, trapMap.findContainingPolygon(50, 0.00001));
	}

	@Test
	void testPointLocationTriangleFan() {

		// 4-triangle fan around the origin, covering a diamond:
		// outer ring points: (100,0) -> (0,100) -> (-100,0) -> (0,-100) -> back

		final PShape t0 = new PShape();
		t0.setFamily(PShape.PATH);
		t0.beginShape();
		t0.vertex(0, 0);
		t0.vertex(100, 0);
		t0.vertex(0, 100);
		t0.endShape(PConstants.CLOSE);

		final PShape t1 = new PShape();
		t1.setFamily(PShape.PATH);
		t1.beginShape();
		t1.vertex(0, 0);
		t1.vertex(0, 100);
		t1.vertex(-100, 0);
		t1.endShape(PConstants.CLOSE);

		final PShape t2 = new PShape();
		t2.setFamily(PShape.PATH);
		t2.beginShape();
		t2.vertex(0, 0);
		t2.vertex(-100, 0);
		t2.vertex(0, -100);
		t2.endShape(PConstants.CLOSE);

		final PShape t3 = new PShape();
		t3.setFamily(PShape.PATH);
		t3.beginShape();
		t3.vertex(0, 0);
		t3.vertex(0, -100);
		t3.vertex(100, 0);
		t3.endShape(PConstants.CLOSE);

		final TrapMap trapMap = new TrapMap(List.of(t0, t1, t2, t3));

		// points strictly inside each triangle (not on shared edges)
		assertSame(t0, trapMap.findContainingPolygon(50, 25));
		assertSame(t1, trapMap.findContainingPolygon(-25, 50));
		assertSame(t2, trapMap.findContainingPolygon(-50, -25));
		assertSame(t3, trapMap.findContainingPolygon(25, -50));

		// outside the fan coverage
		assertNull(trapMap.findContainingPolygon(100, 100));
		assertNull(trapMap.findContainingPolygon(-100, -100));

		// shared apex (degenerate boundary case): should not throw
		assertDoesNotThrow(() -> trapMap.findContainingPolygon(0, 0));

		// sanity: decomposition produced something
		assertTrue(trapMap.getAllTrapezoids().size() > 0);
	}

	@Test
	void testShearBoundsCheckWithHighYPolygon() {
		// This catches the classic bug: bounds are computed in sheared-x space
		// but query points are tested against bounds using unsheared x.
		// If the TrapMap forgets to shear query-x for the bounds check, this will
		// return null.

		final PShape poly = new PShape();
		poly.setFamily(PShape.PATH);
		poly.beginShape();
		poly.vertex(0, 1000);
		poly.vertex(100, 1000);
		poly.vertex(100, 1100);
		poly.vertex(0, 1100);
		poly.endShape(PConstants.CLOSE);

		final TrapMap trapMap = new TrapMap(List.of(poly));

		assertEquals(poly, trapMap.findContainingPolygon(50, 1050)); // inside
		assertNull(trapMap.findContainingPolygon(50, 999)); // below
		assertNull(trapMap.findContainingPolygon(50, 1101)); // above
	}

	@Test
	void testShearBoundsCheckWithHighYSegments() {
		// Same idea as above, but for the segments constructor (findFaceTrapezoids uses
		// findContainingTrapezoid).

		final List<Segment> segments = new ArrayList<>();
		segments.add(new Segment(0, 1000, 100, 1000));
		segments.add(new Segment(0, 1100, 100, 1100));
		segments.add(new Segment(0, 1000, 0, 1100));
		segments.add(new Segment(100, 1000, 100, 1100));

		final TrapMap trapMap = new TrapMap(segments);

		assertFalse(trapMap.findFaceTrapezoids(50, 1050).isEmpty()); // inside face exists
		assertTrue(trapMap.findFaceTrapezoids(50, 0).isEmpty()); // definitely outside bounds

		Set<Trapezoid> inside = trapMap.findFaceTrapezoids(50, 1050);
		Set<Trapezoid> outside = trapMap.findFaceTrapezoids(50, 999);

		assertFalse(inside.isEmpty());
		assertFalse(outside.isEmpty());
		assertNotEquals(inside, outside);
	}

	@Test
	void testDuplicateAndReversedSegmentsDoNotBreakConstruction() {
		// Duplicates and reversed duplicates are common in real-world segment sets.
		// Should not throw and should still produce a valid subdivision.

		final List<Segment> segments = new ArrayList<>();

		// rectangle, but with duplicates and reversed duplicates
		segments.add(new Segment(0, 0, 100, 0));
		segments.add(new Segment(100, 0, 0, 0)); // duplicate reversed

		segments.add(new Segment(0, 100, 100, 100));
		segments.add(new Segment(0, 100, 100, 100)); // exact duplicate

		segments.add(new Segment(0, 0, 0, 100));
		segments.add(new Segment(0, 100, 0, 0)); // duplicate reversed

		segments.add(new Segment(100, 0, 100, 100));
		segments.add(new Segment(100, 100, 100, 0)); // duplicate reversed

		assertDoesNotThrow(() -> new TrapMap(segments));

		final TrapMap trapMap = new TrapMap(segments);
		assertFalse(trapMap.findFaceTrapezoids(50, 50).isEmpty());
	}

	@Test
	void testCollinearSplitEdgeAsTwoSegments() {
		// Equivalent to a redundant collinear vertex, but expressed as segments:
		// bottom edge is split into two collinear segments.
		// This should still behave like a rectangle.

		final List<Segment> segments = new ArrayList<>();

		// bottom split into two segments
		segments.add(new Segment(0, 0, 50, 0));
		segments.add(new Segment(50, 0, 100, 0));

		// top (single)
		segments.add(new Segment(0, 100, 100, 100));

		// sides
		segments.add(new Segment(0, 0, 0, 100));
		segments.add(new Segment(100, 0, 100, 100));

		final TrapMap trapMap = new TrapMap(segments);

		assertFalse(trapMap.findFaceTrapezoids(50, 50).isEmpty());
		assertTrue(trapMap.findFaceTrapezoids(999, 999).isEmpty());
	}

	@Test
	void testNearSplitterPointsLandInDifferentFaces() {
		// Strengthens testPointLocationWithInternalVerticalSegmentSplit:
		// points extremely close to the splitter but not on it should be in different
		// faces.

		final List<Segment> segments = new ArrayList<>();
		segments.add(new Segment(0, 0, 100, 0));
		segments.add(new Segment(0, 100, 100, 100));
		segments.add(new Segment(0, 0, 0, 100));
		segments.add(new Segment(100, 0, 100, 100));
		segments.add(new Segment(50, 0, 50, 100)); // splitter

		final TrapMap trapMap = new TrapMap(segments);

		final Set<Trapezoid> left = trapMap.findFaceTrapezoids(49.9999, 50);
		final Set<Trapezoid> right = trapMap.findFaceTrapezoids(50.0001, 50);

		assertFalse(left.isEmpty());
		assertFalse(right.isEmpty());
		assertNotEquals(left, right);
	}

	@Test
	void testDiamondFanFromSegmentsSharedXRobustness() {
		// Segment-only version of the triangle fan: lots of shared endpoints and shared
		// x-values.
		// This is a good stress test for degeneracy handling (shear, tie-breaking,
		// etc.).

		final List<Segment> segments = new ArrayList<>();

		// outer diamond
		segments.add(new Segment(100, 0, 0, 100));
		segments.add(new Segment(0, 100, -100, 0));
		segments.add(new Segment(-100, 0, 0, -100));
		segments.add(new Segment(0, -100, 100, 0));

		// spokes from origin
		segments.add(new Segment(0, 0, 100, 0));
		segments.add(new Segment(0, 0, 0, 100));
		segments.add(new Segment(0, 0, -100, 0));
		segments.add(new Segment(0, 0, 0, -100));

		final TrapMap trapMap = new TrapMap(segments);

		final Set<Trapezoid> f0 = trapMap.findFaceTrapezoids(25, 25); // inside top-right triangle
		final Set<Trapezoid> f1 = trapMap.findFaceTrapezoids(-25, 25); // top-left
		final Set<Trapezoid> f2 = trapMap.findFaceTrapezoids(-25, -25); // bottom-left
		final Set<Trapezoid> f3 = trapMap.findFaceTrapezoids(25, -25); // bottom-right

		assertFalse(f0.isEmpty());
		assertFalse(f1.isEmpty());
		assertFalse(f2.isEmpty());
		assertFalse(f3.isEmpty());

		// all four bounded faces should be distinct
		assertNotEquals(f0, f1);
		assertNotEquals(f0, f2);
		assertNotEquals(f0, f3);
		assertNotEquals(f1, f2);
		assertNotEquals(f1, f3);
		assertNotEquals(f2, f3);

		// shared vertex boundary query should not throw
		assertDoesNotThrow(() -> trapMap.findFaceTrapezoids(0, 0));
		assertFalse(trapMap.findFaceTrapezoids(0, 0).isEmpty());
	}

	/**
	 * Investigates a potential false-positive in polygon classification: a point is
	 * outside a concave polygon, but still lies in a trapezoid whose top and bottom
	 * boundaries are polygon edges (same polygon).
	 *
	 * If findContainingPolygon() returns the polygon for the "gap" region, that's a
	 * bug.
	 */
	@Test
	void testFindContainingPolygonNoFalsePositiveInConcaveCShapeGap() {
		// A simple concave "C"-shaped polygon (open to the right).
		// Vertical line at x=50 intersects boundary 4 times (y=0,20,80,100).
		// Therefore the band 20<y<80 at x=50 is OUTSIDE the polygon.
		final PShape c = new PShape();
		c.setFamily(PShape.PATH);
		c.beginShape();
		c.vertex(0, 0);
		c.vertex(100, 0);
		c.vertex(100, 20);
		c.vertex(20, 20);
		c.vertex(20, 80);
		c.vertex(100, 80);
		c.vertex(100, 100);
		c.vertex(0, 100);
		c.endShape(PConstants.CLOSE);

		final TrapMap trapMap = new TrapMap(List.of(c));

		// Sanity: points clearly inside different parts of the C-shape
		assertSame(c, trapMap.findContainingPolygon(10, 50)); // left vertical bar
		assertSame(c, trapMap.findContainingPolygon(50, 10)); // bottom bar
		assertSame(c, trapMap.findContainingPolygon(50, 90)); // top bar

		// This point is in the "gap" of the C and should be OUTSIDE the polygon
		// (but still within the map bounding box, so findContainingTrapezoid is
		// non-null).
		assertNotNull(trapMap.findContainingTrapezoid(50, 50));
		assertNull(trapMap.findContainingPolygon(50, 50)); // <-- if this fails, it's a false positive
	}

	@Test
	void segmentHashCodeTest() {
		assertNotEquals(new Segment(1, 0, 0, 0).hashCode(), new Segment(0, 0, 1, 0));
		assertNotEquals(new Segment(1, 2, 3, 4).hashCode(), new Segment(2, 1, 4, 3));
		assertNotEquals(new Segment(-1, -2, -3, -4).hashCode(), new Segment(1, 2, 3, 4));
	}

}