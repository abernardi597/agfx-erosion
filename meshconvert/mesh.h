#ifndef MESH_H
#define MESH_H

#include <cstdlib>
#include <vector>
#include <string>
#include <set>
#include "hash.h"

class Vertex;
class Edge;
class Triangle;

class Mesh {
  public:
    Mesh();
    void Load(const std::string &input_file);

    // get vertices, edges, triangles
    int numVertices() const { return vertices.size(); }
    Vertex* getVertex(int i) const {
        assert (i >= 0 && i < numVertices());
        Vertex *v = vertices[i];
        assert (v != NULL);
        return v; }
    int numEdges() const { return edges.size(); }
    Edge* getMeshEdge(Vertex *a, Vertex *b) const;
    int numTriangles() const { return triangles.size(); } 

    // set vertices, edges, triangles
    Vertex* addVertex(const Vec3f &pos);
    void addTriangle(Vertex *a, Vertex *b, Vertex *c);
    Triangle* getTriangle(int index) const;
    std::set<Triangle *> GetFaces(Edge *e);

    // calculate areas of triangles
    double calculateAreas(std::vector<std::pair<double, bool> > &areas);

private:
  Mesh(const Mesh &/*m*/) { assert(0); exit(0); }
  const Mesh& operator=(const Mesh &/*m*/) { assert(0); exit(0); }

    std::vector<Vertex*> vertices;
    edgeshashtype edges;
    triangleshashtype triangles;
};

#endif