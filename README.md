[![](https://jitpack.io/v/micycle1/TrapezoidalMap.svg)](https://jitpack.io/#micycle1/TrapezoidalMap)

# TrapMap

A Trapezoidal Map library; a data structure for fast point-in-region (point location) queries.

Trapezoidal Map from a set of non-intersecting lines (a *planar straight-line graph*)

A demonstration of the Randomized Trapezoidal Map for Point Search.

The expected query time for an arbitrary but fixed query point is O(log n)

Given a partition of the space into disjoint regions, to determine the region where a query point lies.

A point location query is to ask in which region (face) of a map a given point lie.

The algorithm has been extracted from AWT Tyler Chenhall's Java [applet](https://github.com/TylerChenhall/TrapezoidalMap) and turned into a library.

Originally created during Spring 2014 for Com S 418: Computational Geometry at Iowa State University

## Project Description
This repository demonstrates the Randomized Trapezoidal Map, as described in de Berg's Computational Geometry textbook. The project shuffles the segment list and builds a trapezoidal map (in order to achieve expected construction and query times of O(nlog(n)) and O(log(n)), respectively, where n is the number of segments.

## Resources

[Here](http://cgm.cs.mcgill.ca/~athens/cs507/Projects/2002/JukkaKaartinen/)

or 

[here](http://janrollmann.de/projects/thesis/)