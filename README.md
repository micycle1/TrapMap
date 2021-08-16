[![](https://jitpack.io/v/micycle1/TrapezoidalMap.svg)](https://jitpack.io/#micycle1/TrapezoidalMap) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=micycle1_TrapMap&metric=ncloc)](https://sonarcloud.io/dashboard?id=micycle1_TrapMap)


# TrapMap

*Trapezoidal Map* library — a data structure for fast point location queries.

## Overview

The problem of planar partitioning and planar point location is a fundamental problem in computational geometry. Given a partition of the plane into disjoint regions, we may want to determine in which region a query point lies.

*TrapMap* pre-processes a partitioning of the plane (given as individual line segments, or polygons), decomposing regions into simpler trapezoidal cells upon which a search structure (a directed acyclic graph) is constructed. This structure facilitates the search of the trapezoid (hence the region) containing a query point in *O(log n)* time. The trapezoidal map and the search structure are built via randomized incremental construction.

*TrapMap* is based on _Tyler Chenhall's_ trapezoidal map [implementation](https://github.com/TylerChenhall/TrapezoidalMap). It has been built for interoperability with [Processing](https://processing.org/) so results can be visualised quickly.

## Usage

* `findTrapezoid()` find the trapezoid containing the query point.
* `findTrapezoids()` finds the trapezoids which constitute the polygonal face in which the query point lies.
  * This face is "emergent" from the line segments, depending on their intersection.

Note: Handles vertical lines.

Illustrating the containing the trapezoid and the containing polygon face.

<p float="middle">
  <a href="examples/partitionSmooth"><img src="resources/leaf.gif" width="50%"/></a <a href="examples/partitionSmooth"><img src="resources/dino.gif" width="50%"/></a>
</p>

### From line segments

### From polygonal shapes
Notably, trapezoids have a pointer back to the original, accessible via `findFace()`.