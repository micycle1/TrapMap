package micycle.trapmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
		List<Segment> segments = new ArrayList<>();

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
		Set<Trapezoid> t1 = trapMap.findTrapezoids(50.1f, 50.2f);
		Set<Trapezoid> t2 = trapMap.findTrapezoids(30, 30);
		Set<Trapezoid> t3 = trapMap.findTrapezoids(60, 60);
		assertEquals(true, t1.equals(t2));
		assertEquals(true, t2.equals(t3));
		assertEquals(0, trapMap.findTrapezoids(999, 999).size());
		System.out.println(trapMap.findTrapezoids(999, 999).size());

		Set<Trapezoid> t4 = trapMap.findTrapezoids(20.1f, 20.1f);
		Set<Trapezoid> t5 = trapMap.findTrapezoids(50, 90);
		Set<Trapezoid> t6 = trapMap.findTrapezoids(80, 80);
		assertEquals(true, t4.equals(t5));
		assertEquals(true, t5.equals(t6));
		assertEquals(false, t1.equals(t4));
	}

	@Test
	void testPointLocationFromPolygons() {

		// top and bottom share an edge [(0,0) -> (100,0)]

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
		bottom.vertex(150,50);
		bottom.vertex(100, 100);
		bottom.vertex(0, 100);
		bottom.endShape(PConstants.CLOSE);

		List<PShape> polygons = new ArrayList<>(Arrays.asList(top, bottom));
		
		final TrapMap trapMap = new TrapMap(polygons);
		
		assertEquals(top, trapMap.findFace(50, -50));
		assertEquals(bottom, trapMap.findFace(50, 50));
		assertNull(trapMap.findFace(999, 999));
	}

}
