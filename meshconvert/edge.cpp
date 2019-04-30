#ifndef _EDGE_H_
#define _EDGE_H_

#include "vertex.h"
#include "edge.h"


// ===========
// CONSTRUCTOR
Edge::Edge(Vertex *vs, Vertex *ve, Triangle *t) {
  start_vertex = vs;
  end_vertex = ve;
  triangle = t;
  next = NULL;
  opposite = NULL;
}

// ========
// ACCESSOR
float Edge::Length() const {
  Vec3f diff = start_vertex->getPos() - end_vertex->getPos();
  return diff.Length();
}

#endif
