[![](https://jitpack.io/v/micycle1/TrapezoidalMap.svg)](https://jitpack.io/#micycle1/TrapezoidalMap)

# TrapMap

A Trapezoidal Map library; a data structure for fast point location queries.

Trapezoidal Map from a set of non-intersecting lines (a *planar straight-line graph*)

A demonstration of the Randomized Trapezoidal Map for Point Search.

The expected query time for an arbitrary but fixed query point is O(log n)

Given a partition of the space into disjoint regions, to determine the region where a query point lies.

A point location query is to ask in which region (face) of a map a given point lie.

arbitrary polygons (not just triangles for example)

The algorithm has been extracted from AWT Tyler Chenhall's Java [applet](https://github.com/TylerChenhall/TrapezoidalMap) and turned into this usable library.

The arrangement faces are decomposed into simpler cells each of constant complexity, known as pseudo-trapezoids, and a search structure (a directed acyclic graph) is constructed on top of these cells, facilitating the search of the pseudo trapezoid (hence the arrangement cell) containing a query point in expected logarithmic time. The trapezoidal map and the search structure are built by a randomized incremental construction algorithm (RIC).

## Project Description
This repository demonstrates the Randomized Trapezoidal Map, as described in de Berg's Computational Geometry textbook. The project shuffles the segment list and builds a trapezoidal map (in order to achieve expected construction and query times of O(nlog(n)) and O(log(n)), respectively, where n is the number of segments.

## Degenerate

In degenerate situations the query point can lie on an edge, or coincide with a vertex???
Vertical line???


## Resources

[Here](http://cgm.cs.mcgill.ca/~athens/cs507/Projects/2002/JukkaKaartinen/)

or 

[here](http://janrollmann.de/projects/thesis/)