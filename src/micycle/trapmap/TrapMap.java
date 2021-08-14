package micycle.trapmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import micycle.trapmap.graph.Leaf;
import micycle.trapmap.graph.Node;
import micycle.trapmap.graph.XNode;
import micycle.trapmap.graph.YNode;
import processing.core.PShape;
import processing.core.PVector;

/**
 * This class represents the Trapezoidal Map for a given set of segments in the
 * plane. The constructor accepts a list of segments which follow the following
 * criteria: 1) Segments are non-crossing 2) Segment interiors are disjoint, but
 * segments may meet at endpoints (to allow closed figures) [PSLG?]. Both the
 * physical map and search structure are represented.
 *
 *
 * @author Tyler Chenhall (core algorithm)
 * @author Michael Carleton (improvements + Processing integration)
 */
public class TrapMap {

	// TODO HANDLE POINT INPUT FOR NEAREST NEIGHBOUR

	private Node root; // root of trapezoid history graph

	/**
	 * 
	 * @param polygons a list of disjoint polygonal shapes. Shapes may share edges /
	 *                 touch (forming a 'Planar graph') but interiors cannot
	 *                 overlap. assuming non-nested/non-overlapping polygons
	 *                 (mesh-like, at most (if share edges)
	 */
	public TrapMap(List<PShape> polygons) {
		final Map<Integer, Segment> segments = new HashMap<>(polygons.size() * 3);
		for (PShape polygon : polygons) {
			if (polygon.getFamily() == PShape.PRIMITIVE || polygon.getFamily() == PShape.GROUP) {
				continue; // process polygonal shapes only
			}
			for (int i = 0; i < polygon.getVertexCount(); i++) {
				Segment s = null;
				if (i < polygon.getVertexCount() - 1) {
					s = new Segment(polygon.getVertex(i), polygon.getVertex(i + 1), polygon);
				} else { // at last vertex
					if (polygon.isClosed() || !polygon.getVertex(0).equals(polygon.getVertex(polygon.getVertexCount() - 1))) {
						// create a segment between first and last vertices to close shape
						s = new Segment(polygon.getVertex(polygon.getVertexCount() - 1), polygon.getVertex(0), polygon);
					} else {
						continue;
					}
				}

				if (segments.putIfAbsent(s.hashCode(), s) != null) {
					final Segment other = segments.get(s.hashCode());
					if (other.faceA != polygon) { // this should never be false
						other.faceB = polygon; // link the polygon twinned with this edge
					}
				}
			}
		}

		process(segments.values());
	}

	/**
	 * Builds the trapezoidal map search structure from the collection of segments.
	 * This constructor builds both the physical map structure (creating the
	 * neighbor links for each trapezoid) and also incrementally builds the map
	 * search structure (using nodes in a pseudo-tree).
	 *
	 * @param segments The list of segments to build a search structure for /
	 *                 segments that partition the plane into cells
	 */
	public TrapMap(Collection<Segment> segments) {
		if (segments instanceof Set == false) {
			// hash to both remove possible duplicates & shuffle the collection
			segments = new HashSet<>(segments);
		}
		process(segments);
	}

	private void process(Collection<Segment> segments) {
		// 1. Determine a bounding box for the segments
		Trapezoid bounds = computeBounds(segments);
		Leaf f = new Leaf(bounds);
		bounds.setLeaf(f);
		root = f;

		// 2. shuffle the segments // NOTE SKIPPING SHUFFLE
		// "Size of D and query time depend on insertion order"
		// the array is first duplicated in case the ordering is important in the
		// original array
//		Segment[] arr = Arrays.copyOf(segs, segs.length);
//		Random r = new Random();
//		int rnd;
//		Segment temp;
//		// random shuffling
//		for (int i = arr.length - 1; i >= 1; i--) {
//			// determine a random index to swap with (index < i) and swap
//			// based on Chapter 4 of deBerg (p77)
//			rnd = r.nextInt(i);
//			temp = arr[i];
//			arr[i] = arr[rnd];
//			arr[rnd] = temp;
//		}

		Segment[] segs = segments.toArray(new Segment[segments.size()]); // relabel array

		// 3. incrementally make the trapezoidal map
		for (Segment seg : segs) {
			// find the trapezoids intersected by arr[i]
			Leaf[] list = followSegment(seg);

			if (list.length == 1) { // the segment is entirely within a single trapezoid

				// split into 4 sections
				Trapezoid old = list[0].getData();
				Trapezoid lefty = new Trapezoid(old.getLeftBound(), seg.getLeftPoint(), old.getUpperBound(), old.getLowerBound());
				Trapezoid righty = new Trapezoid(seg.getRightPoint(), old.getRightBound(), old.getUpperBound(), old.getLowerBound());
				Trapezoid top = new Trapezoid(seg.getLeftPoint(), seg.getRightPoint(), old.getUpperBound(), seg);
				Trapezoid bottom = new Trapezoid(seg.getLeftPoint(), seg.getRightPoint(), seg, old.getLowerBound());
				XNode ll = new XNode(seg.getLeftPoint());
				XNode rr = new XNode(seg.getRightPoint());
				YNode ss = new YNode(seg);

				Leaf leftyN = new Leaf(lefty);
				lefty.setLeaf(leftyN);
				Leaf rightyN = new Leaf(righty);
				righty.setLeaf(rightyN);
				Leaf topN = new Leaf(top);
				top.setLeaf(topN);
				Leaf bottomN = new Leaf(bottom);
				bottom.setLeaf(bottomN);
				if (!(lefty.hasZeroWidth() || righty.hasZeroWidth())) {

					// link all the nodes for the trapezoids
					ll.setLeftChildNode(leftyN);
					ll.setRightChildNode(rr);
					rr.setRightChildNode(rightyN);
					rr.setLeftChildNode(ss);
					ss.setLeftChildNode(topN);
					ss.setRightChildNode(bottomN);

					// connect the nodes to the old structure
					if (list[0].getParentNode() == null) {
						root = ll;
					} else {
						// the previous node might have more than one parent node
						List<Node> parents = list[0].getParentNodes();
						for (Node tempParent : parents) {
							if (tempParent.getLeftChildNode() == list[0]) {
								tempParent.setLeftChildNode(ll);
							} else {
								tempParent.setRightChildNode(ll);
							}
						}
					}

					// link the trapezoids together
					lowerLink(lefty, bottom);
					lowerLink(old.getLowerLeftNeighbor(), lefty);
					upperLink(lefty, top);
					upperLink(old.getUpperLeftNeighbor(), lefty);

					lowerLink(righty, old.getLowerRightNeighbor());
					lowerLink(bottom, righty);
					upperLink(righty, old.getUpperRightNeighbor());
					upperLink(top, righty);
				} else if (lefty.hasZeroWidth() && !righty.hasZeroWidth()) {// only left has zero width
					// link all the nodes for the trapezoids
					rr.setLeftChildNode(ss);
					rr.setRightChildNode(rightyN);
					ss.setLeftChildNode(topN);
					ss.setRightChildNode(bottomN);

					// connect the nodes to the old structure
					if (list[0].getParentNode() == null) {
						root = rr;
					} else {
						// the previous node might have more than one parent node
						List<Node> parents = list[0].getParentNodes();
						for (Node tempParent : parents) {
							if (tempParent.getLeftChildNode() == list[0]) {
								tempParent.setLeftChildNode(rr);
							} else {
								tempParent.setRightChildNode(rr);
							}
						}
					}

					// link the trapezoids together
					lowerLink(old.getLowerLeftNeighbor(), bottom);
					upperLink(old.getUpperLeftNeighbor(), top);

					lowerLink(righty, old.getLowerRightNeighbor());
					lowerLink(bottom, righty);
					upperLink(righty, old.getUpperRightNeighbor());
					upperLink(top, righty);
				} else if (righty.hasZeroWidth() && !lefty.hasZeroWidth()) {// only right has zero width
					// link all the nodes for the trapezoids
					ll.setLeftChildNode(leftyN);
					ll.setRightChildNode(ss);
					ss.setLeftChildNode(topN);
					ss.setRightChildNode(bottomN);

					// connect the nodes to the old structure
					if (list[0].getParentNode() == null) {
						root = ll;
					} else {
						// the previous node might have more than one parent node
						List<Node> parents = list[0].getParentNodes();
						for (Node tempParent : parents) {
							if (tempParent.getLeftChildNode() == list[0]) {
								tempParent.setLeftChildNode(ll);
							} else {
								tempParent.setRightChildNode(ll);
							}
						}
					}

					// link the trapezoids together
					lowerLink(lefty, bottom);
					lowerLink(old.getLowerLeftNeighbor(), lefty);
					upperLink(lefty, top);
					upperLink(old.getUpperLeftNeighbor(), lefty);

					lowerLink(bottom, old.getLowerRightNeighbor());
					upperLink(top, old.getUpperRightNeighbor());
				} else {
					// both have zero width

					// build the search structure
					ss.setLeftChildNode(topN);
					ss.setRightChildNode(bottomN);

					// connect the nodes to the old structure
					if (list[0].getParentNode() == null) {
						root = ss;
					} else {
						// the previous node might have more than one parent node
						List<Node> parents = list[0].getParentNodes();
						for (Node tempParent : parents) {
							if (tempParent.getLeftChildNode() == list[0]) {
								tempParent.setLeftChildNode(ss);
							} else {
								tempParent.setRightChildNode(ss);
							}
						}
					}

					// link the trapezoids together (this is nontrivial in degenerates cases)
					lowerLink(old.getLowerLeftNeighbor(), bottom);
					lowerLink(bottom, old.getLowerRightNeighbor());
					upperLink(old.getUpperLeftNeighbor(), top);
					upperLink(top, old.getUpperRightNeighbor());
				}

			} else { // (3 divisions for the first and last trapezoids, 2 for the middle ones)
				/*
				 * The first and last cases get broken into 3 parts wheras the middle ones are
				 * different. If the left segment endPVector is not leftP of list[0].getData(),
				 * then there is an extra trapezoid at the left end; likewise for rightP of
				 * list[n-1].getData(). For everything in the middle, we start with a single top
				 * and bottom trap for both then we merge trapezoids together as needed note
				 * that before merging, some trapezoids may have an end PVector which is null.
				 */
				Trapezoid[] topArr = new Trapezoid[list.length];
				Trapezoid[] botArr = new Trapezoid[list.length];
				for (int j = 0; j < list.length; j++) {
					// top is defined by the original upper segment, the new segment & two endpoints
					// left endpoint:
					/*
					 * if j==0, is segment's left endPVector else is old trap's left endPVector if
					 * it is above the segment
					 */
					// right endPVector is similar
					if (j == 0) {
						PVector rtP = null;
						if (isPointAboveLine(list[j].getData().getRightBound(), seg)) {
							rtP = list[j].getData().getRightBound();
						}
						topArr[j] = new Trapezoid(seg.getLeftPoint(), rtP, list[j].getData().getUpperBound(), seg);
					} else if (j == list.length - 1) {
						PVector ltP = null;
						if (isPointAboveLine(list[j].getData().getLeftBound(), seg)) {
							ltP = list[j].getData().getLeftBound();
						}
						topArr[j] = new Trapezoid(ltP, seg.getRightPoint(), list[j].getData().getUpperBound(), seg);
					} else {
						PVector rtP = null;
						if (isPointAboveLine(list[j].getData().getRightBound(), seg)) {
							rtP = list[j].getData().getRightBound();
						}
						PVector ltP = null;
						if (isPointAboveLine(list[j].getData().getLeftBound(), seg)) {
							ltP = list[j].getData().getLeftBound();
						}
						topArr[j] = new Trapezoid(ltP, rtP, list[j].getData().getUpperBound(), seg);
					}

					// the bottom array is constructed using a similar strategy
					if (j == 0) {
						PVector rtP = null;
						if (!isPointAboveLine(list[j].getData().getRightBound(), seg)) {
							rtP = list[j].getData().getRightBound();
						}
						botArr[j] = new Trapezoid(seg.getLeftPoint(), rtP, seg, list[j].getData().getLowerBound());
					} else if (j == list.length - 1) {
						PVector ltP = null;
						if (!isPointAboveLine(list[j].getData().getLeftBound(), seg)) {
							ltP = list[j].getData().getLeftBound();
						}
						botArr[j] = new Trapezoid(ltP, seg.getRightPoint(), seg, list[j].getData().getLowerBound());
					} else {
						PVector rtP = null;
						if (!isPointAboveLine(list[j].getData().getRightBound(), seg)) {
							rtP = list[j].getData().getRightBound();
						}
						PVector ltP = null;
						if (!isPointAboveLine(list[j].getData().getLeftBound(), seg)) {
							ltP = list[j].getData().getLeftBound();
						}
						botArr[j] = new Trapezoid(ltP, rtP, seg, list[j].getData().getLowerBound());
					}
				}

				// then merge degenerate trapezoids together (those with a null bounding point)
				int aTop = 0;
				int bTop;
				int aBot = 0;
				int bBot;
				for (int j = 0; j < list.length; j++) {
					if (topArr[j].getRightBound() != null) {
						bTop = j;
						// merge trapezoids aTop through bTop
						// we only want one trapezoid, so we just have bTop-aTop+1 pointers to it for
						// now
						Trapezoid tempMerge = new Trapezoid(topArr[aTop].getLeftBound(), topArr[bTop].getRightBound(),
								topArr[aTop].getUpperBound(), seg);
						for (int k = aTop; k <= bTop; k++) {
							// now there are duplicates of the same trapezoid unfortunately, but I think if
							// we link them together left to right
							// this shouldn't cause problems later...it just means a bit more storage use
							topArr[k] = tempMerge;
						}
						aTop = j + 1;
					}

					if (botArr[j].getRightBound() != null) {
						bBot = j;
						// merge trapezoids aBot through bBot
						Trapezoid tempMerge = new Trapezoid(botArr[aBot].getLeftBound(), botArr[bBot].getRightBound(), seg,
								botArr[aBot].getLowerBound());
						for (int k = aBot; k <= bBot; k++) {
							botArr[k] = tempMerge;
						}
						aBot = j + 1;
					}
				}

				// do trapezoid links...this should unlink the original trapezoids from the
				// physical structure except at the ends
				// do all left links before doing right links in order to avoid linking errors
				for (int j = 0; j < list.length; j++) {
					if (j != 0) {
						// update left links
						// link right to left
						// only recycle old links if they are not in the list to be removed

						// only when the trapezoids do not repeat
						if (topArr[j] != topArr[j - 1]) {
							lowerLink(topArr[j - 1], topArr[j]);
						}

						// leave the upper left neighbor null unless we have something to set it to
						Trapezoid temp2 = list[j].getData().getUpperLeftNeighbor();
						if (!list[j - 1].getData().equals(temp2)) {
							upperLink(temp2, topArr[j]);
						}

						// only do this for non-repeating trapezoids
						if (botArr[j] != botArr[j - 1]) {
							upperLink(botArr[j - 1], botArr[j]);
						}

						temp2 = list[j].getData().getLowerLeftNeighbor();
						if (!list[j - 1].getData().equals(temp2)) {
							lowerLink(temp2, botArr[j]);
						}

					}

				}
				for (int j = 0; j < list.length; j++) {
					if (j != topArr.length - 1) {
						// update right links

						// only for non-repeats
						if (topArr[j] != topArr[j + 1]) {
							lowerLink(topArr[j], topArr[j + 1]);
						}

						Trapezoid temp2 = list[j].getData().getUpperRightNeighbor();
						if (!list[j + 1].getData().equals(temp2)) {
							upperLink(topArr[j], temp2);
						}

						// only for non-repeats
						if (botArr[j] != botArr[j + 1]) {
							upperLink(botArr[j], botArr[j + 1]);
						}

						temp2 = list[j].getData().getLowerRightNeighbor();
						if (!list[j + 1].getData().equals(temp2)) {
							lowerLink(botArr[j], temp2);
						}
					}
				}

				// deal with the possible extra end trapezoids
				Trapezoid leftmost = null;
				Trapezoid rightmost = null;
				Trapezoid oldLeft = list[0].getData();
				Trapezoid oldRight = list[list.length - 1].getData();
				if (!seg.getLeftPoint().equals(oldLeft.getLeftBound())) {
					// there is a leftmost trapezoid
					leftmost = new Trapezoid(oldLeft.getLeftBound(), seg.getLeftPoint(), oldLeft.getUpperBound(), oldLeft.getLowerBound());
				}
				if (!seg.getRightPoint().equals(list[list.length - 1].getData().getRightBound())) {
					// there is a rightmost trapezoid
					rightmost = new Trapezoid(seg.getRightPoint(), oldRight.getRightBound(), oldRight.getUpperBound(),
							oldRight.getLowerBound());
				}

				// add remaining trapezoid links at the end
				if (leftmost != null) {
					lowerLink(oldLeft.getLowerLeftNeighbor(), leftmost);
					upperLink(oldLeft.getUpperLeftNeighbor(), leftmost);

					lowerLink(leftmost, botArr[0]);
					upperLink(leftmost, topArr[0]);
				} else // link top & bot arr with appropriate left links of oldLeft
				if (oldLeft.getUpperBound().getLeftPoint().equals(oldLeft.getLowerBound().getLeftPoint())) {
					// triangles, so no neighbors to worry about
				} else if (oldLeft.getUpperBound().getLeftPoint().equals(oldLeft.getLeftBound())) {
					// upper half degenerates to a triangle
					lowerLink(oldLeft.getLowerLeftNeighbor(), botArr[0]);
				} else if (oldLeft.getLowerBound().getLeftPoint().equals(oldLeft.getLeftBound())) {
					// lower half degenerates to a triangle
					upperLink(oldLeft.getUpperLeftNeighbor(), topArr[0]);
				} else {
					// neither degenerates to a triangle
					lowerLink(oldLeft.getLowerLeftNeighbor(), botArr[0]);
					upperLink(oldLeft.getUpperLeftNeighbor(), topArr[0]);
				}
				if (rightmost != null) {
					lowerLink(rightmost, oldRight.getLowerRightNeighbor());
					upperLink(rightmost, oldRight.getUpperRightNeighbor());

					lowerLink(botArr[botArr.length - 1], rightmost);
					upperLink(topArr[topArr.length - 1], rightmost);
				} else // link the top & bot arr with the appropriate right links of oldRight
				if (oldRight.getUpperBound().getRightPoint().equals(oldRight.getLowerBound().getRightPoint())) {
					// triangles, hence no right neighbors
				} else if (oldRight.getUpperBound().getRightPoint().equals(oldRight.getRightBound())) {
					// upper half degenerates to a triangle
					lowerLink(botArr[botArr.length - 1], oldRight.getLowerRightNeighbor());
				} else if (oldRight.getLowerBound().getRightPoint().equals(oldRight.getRightBound())) {
					// lower half degenerates to a triangle
					upperLink(topArr[topArr.length - 1], oldRight.getUpperRightNeighbor());
				} else {
					// neither degenerates to a triangle
					lowerLink(botArr[botArr.length - 1], oldRight.getLowerRightNeighbor());
					upperLink(topArr[topArr.length - 1], oldRight.getUpperRightNeighbor());
				}

				// create leaf structures ahead of time to deal with the duplication problem
				Leaf[] topLeaf = new Leaf[topArr.length];
				Leaf[] botLeaf = new Leaf[botArr.length];
				Leaf aa;
				for (int j = 0; j < topLeaf.length; j++) {
					if (j == 0 || topArr[j] != topArr[j - 1]) {
						// create a new topLeaf
						aa = new Leaf(topArr[j]);
						topArr[j].setLeaf(aa);
						topLeaf[j] = aa;
					} else {
						// reuse the old Leaf
						topLeaf[j] = topLeaf[j - 1];
					}

					if (j == 0 || botArr[j] != botArr[j - 1]) {
						// create a new botLeaf
						aa = new Leaf(botArr[j]);
						botArr[j].setLeaf(aa);
						botLeaf[j] = aa;
					} else {
						// reuse the old Leaf
						botLeaf[j] = botLeaf[j - 1];
					}
				}

				// then add nodes and node links...this should unlink the original trapezoids
				// from the physical structure
				Node[] newStructures = new Node[list.length];
				for (int j = 0; j < list.length; j++) {
					Node yy = new YNode(seg);
					if (j == 0 && leftmost != null) {
						XNode xx = new XNode(seg.getLeftPoint());
						aa = new Leaf(leftmost);
						leftmost.setLeaf(aa);
						xx.setLeftChildNode(aa);
						xx.setRightChildNode(yy);

						newStructures[j] = xx;
					} else if (j == newStructures.length - 1 && rightmost != null) {
						XNode xx = new XNode(seg.getRightPoint());
						aa = new Leaf(rightmost);
						rightmost.setLeaf(aa);
						xx.setRightChildNode(aa);
						xx.setLeftChildNode(yy);

						newStructures[j] = xx;
					} else {
						newStructures[j] = yy;
					}

					yy.setLeftChildNode(topLeaf[j]);

					yy.setRightChildNode(botLeaf[j]);

					// insert the new structure in place of the old one
					// Node parent = list[j].getParentNode();

					// now there may be many parents...
					List<Node> parents = list[j].getParentNodes();
					for (Node parent : parents) {
						if (parent.getLeftChildNode() == list[j]) {
							// replace left child
							parent.setLeftChildNode(newStructures[j]);
						} else {
							parent.setRightChildNode(newStructures[j]);
						}
					}
				}
			}
		}
	}

	/**
	 * Computes the rectangular bounding box for the set of segments.
	 */
	private Trapezoid computeBounds(Collection<Segment> segments) {
		// Compute bounding box so that there is no infinite face
		float minx = Float.MAX_VALUE;
		float maxx = -Float.MAX_VALUE;
		float miny = Float.MAX_VALUE;
		float maxy = -Float.MAX_VALUE;
		for (Segment seg : segments) {
			if (seg != null) {
				minx = Math.min(minx, seg.getMinX());
				maxx = Math.max(maxx, seg.getMaxX());
				miny = Math.min(miny, seg.getMinY());
				maxy = Math.max(maxy, seg.getMaxY());
			}
		}
		// create a trapezoid using the bounding box
		PVector left = new PVector(minx, miny);
		PVector right = new PVector(maxx, maxy);
		Trapezoid t = new Trapezoid(left, right, new Segment(new PVector(minx, maxy), new PVector(maxx, maxy)),
				new Segment(new PVector(minx, miny), new PVector(maxx, miny)));
		return t;
	}

	/**
	 * Link two neighboring trapezoids that are lower neighbors.
	 *
	 * @param left  The left trapezoid to link
	 * @param right The right trapezoid to link
	 */
	private static void lowerLink(Trapezoid left, Trapezoid right) {
		if (left != null) {
			left.setLowerRightNeighbor(right);
		}
		if (right != null) {
			right.setLowerLeftNeighbor(left);
		}
	}

	/**
	 * Link two neighboring trapezoids that are upper neighbors.
	 *
	 * @param left  The left trapezoid to link
	 * @param right The right trapezoid to link
	 */
	private static void upperLink(Trapezoid left, Trapezoid right) {
		if (left != null) {
			left.setUpperRightNeighbor(right);
		}
		if (right != null) {
			right.setUpperLeftNeighbor(left);
		}
	}

	/**
	 * Get the list of trapezoids in the current structure intersected by the
	 * segment.
	 *
	 * @param s The query segment
	 * @return An array of trapezoids (Leaf array) intersected by the segment
	 */
	private Leaf[] followSegment(Segment s) {
		List<Leaf> list = new ArrayList<>();
		Leaf previous = findPoint(s.getLeftPoint(), s);
		// shift over leftward to make sure we have the first of any repeated trapezoids

		list.add(previous);
		while (compareTo(s.getRightPoint(), previous.getData().getRightBound()) > 0) {
			// choose the next trapezoid in the sequence
			if (TrapMap.isPointAboveLine(previous.getData().getRightBound(), s)) {
				previous = previous.getData().getLowerRightNeighbor().getLeaf();
			} else {
				previous = previous.getData().getUpperRightNeighbor().getLeaf();
			}
			list.add(previous);
		}

		return list.toArray(new Leaf[list.size()]);
	}

	/**
	 * Find the trapezoid in the trapezoidal map which contains the search point.
	 *
	 * @param p The PVector to search for
	 * @return The trapezoid containing the query point
	 */
	private Leaf findPoint(PVector p, Segment s) {
		Node current = root;
		while (!(current instanceof Leaf)) {
			if (current instanceof XNode) {
				final int val = compareTo(p, ((XNode) current).getData());
				if (val < 0) {
					current = current.getLeftChildNode();
				} else {
					current = current.getRightChildNode();
				}
			} else // we are searching for a point, without segment information
			// we are searching for a PVector on one of the segments
			if (isPointAboveLine2(p, ((YNode) current).getData(), s)) {
				current = current.getLeftChildNode();
			} else {
				current = current.getRightChildNode();
			}
		}
		return ((Leaf) current);
	}

	/**
	 * Locates the trapezoid in which the given point resides.
	 * 
	 * @param p The PVector to query
	 * @return The trapezoid containing the point
	 */
	public Trapezoid findTrapezoid(PVector p) {
		Node current = root;
		while (!(current instanceof Leaf)) {
			if (current instanceof XNode) { // point query: does p lie to the left or the right of a given point?
				final int val = compareTo(p, ((XNode) current).getData());
				if (val < 0) {
					current = current.getLeftChildNode();
				} else {
					current = current.getRightChildNode();
				}
			} else // we are searching for a point, without segment information
					// segment query: does p lie above or below a given line segment?
			if (isPointAboveLine(p, ((YNode) current).getData())) {
				current = current.getLeftChildNode();
			} else {
				current = current.getRightChildNode();
			}
		}
		return ((Leaf) current).getData();
	}

	/**
	 * Locates all the trapezoids belonging to the polygon in which the given point
	 * resides.
	 * 
	 * @param p
	 * @return
	 */
	public Set<Trapezoid> findTrapezoids(PVector p) {
		Set<Trapezoid> set = new HashSet<>();
		recursePolygon(findTrapezoid(p), set);
		// TODO find way to map group back to original polygons, then map single
		// MAP OF TRAPEZOID -> TRAPEZOID GROUP -> ORIGINAL FACE
		// trapezoid -> original polygon.
		return set;

	}

	public PShape findFace(PVector p) {
		return findTrapezoid(p).getFace();
	}

	private void recursePolygon(Trapezoid t, Set<Trapezoid> pp) {
		if (t != null && !pp.contains(t)) {
			pp.add(t);
			recursePolygon(t.getLowerLeftNeighbor(), pp);
			recursePolygon(t.getLowerRightNeighbor(), pp);
			recursePolygon(t.getUpperLeftNeighbor(), pp);
			recursePolygon(t.getUpperRightNeighbor(), pp);
		}
	}

	/**
	 * Checks to see if a PVector is above the segment. Works by calculating y of
	 * the segment at x of the point
	 *
	 * @param p The PVector of interest
	 * @param s The segment of interest
	 * @return True if on or above the segment; false otherwise
	 */
	private static boolean isPointAboveLine(PVector p, Segment s) {
		float x = p.x;
		float y = p.y;
		return (x - s.getLeftPoint().x) * s.getRightPoint().y + (s.getRightPoint().x - x) * s.getLeftPoint().y < y
				* (s.getRightPoint().x - s.getLeftPoint().x);
	}

	/**
	 * Checks if the input PVector on the given old segment lies above or below the
	 * new segment. If the input PVector lies on the new segment, we determine
	 * above/below by which segment has the higher slope.
	 * 
	 * @TODO Check this for nondegenerate case?
	 * @param p    The PVector under consideration
	 * @param old  The segment which the PVector lies on
	 * @param pseg The segment to compare the PVector to
	 * @return True if the PVector lies above segment pseg, or the PVector lies on
	 *         pseg, on a segment of higher slope
	 */
	private static boolean isPointAboveLine2(PVector p, Segment old, Segment pseg) {
		// check if p is on segment old
		/*
		 * long x1 = p.x; long x2 = old.getLeftEndPoint().x; long x3 =
		 * old.getRightEndPoint().x; long y1 = p.y; long y2 = old.getLeftEndPoint().y;
		 * long y3 = old.getRightEndPoint().y; long result = (x2-x1)*(y3-y1) -
		 * (x3-x1)*(y2-y1);
		 */
		// according to the textbook, p can only lie on segment old if it is the left
		// endpoint
		if (p.equals(old.getLeftPoint())) {
			// compare slopes
			float x1 = p.x;
			float x2 = old.getRightPoint().x;
			float x3 = pseg.getRightPoint().x;
			float y1 = p.y;
			float y2 = old.getRightPoint().y;
			float y3 = pseg.getRightPoint().y;
			float result = (x2 - x1) * (y3 - y1) - (x3 - x1) * (y2 - y1);
			return result > 0;
		}
		// if not, call isPointAboveLine
		return isPointAboveLine(p, old);
	}

	public void getAllTrapezoids() {
		// TODO return all leaf trapezoids
	}

	private static int compareTo(PVector a, PVector b) {
		// Handle degeneracies by using comparison rules to mimic x-coordinate shearing
		if (a.x < b.x || (a.x == b.x && a.y < b.y)) {
			return -1;
		} else if ((a.x == b.x) && (a.y == b.y)) {
			return 0;
		} else {
			return 1;
		}
	}
}