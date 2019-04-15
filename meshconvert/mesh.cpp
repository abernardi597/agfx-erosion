#include <iostream>
#include <fstream>
#include <sstream>
#include <iomanip>
#include <cstring>

#include "mesh.h"
#include "edge.h"
#include "vertex.h"
#include "triangle.h"

// to give a unique id number to each triangles
int Triangle::next_triangle_id = 0;

Mesh::Mesh(){}

// add parts of mesh
Vertex* Mesh::addVertex(const Vec3f &pos) {
  int index = numVertices();
  Vertex *v = new Vertex(index, pos);
  vertices.push_back(v);
  return v;
}

void Mesh::addTriangle(Vertex *a, Vertex *b, Vertex *c) {
  // create the triangle
  Triangle *t = new Triangle();
  // create the edges
  Edge *ea = new Edge(a,b,t);
  Edge *eb = new Edge(b,c,t);
  Edge *ec = new Edge(c,a,t);
  // point the triangle to one of its edges
  t->setEdge(ea);
  // connect the edges to each other
  ea->setNext(eb);
  eb->setNext(ec);
  ec->setNext(ea);
  // verify these edges aren't already in the mesh 
  // (which would be a bug, or a non-manifold mesh)
  assert (edges.find(std::make_pair(a,b)) == edges.end());
  assert (edges.find(std::make_pair(b,c)) == edges.end());
  assert (edges.find(std::make_pair(c,a)) == edges.end());
  // add the edges to the master list
  edges[std::make_pair(a,b)] = ea;
  edges[std::make_pair(b,c)] = eb;
  edges[std::make_pair(c,a)] = ec;
  // connect up with opposite edges (if they exist)
  edgeshashtype::iterator ea_op = edges.find(std::make_pair(b,a)); 
  edgeshashtype::iterator eb_op = edges.find(std::make_pair(c,b)); 
  edgeshashtype::iterator ec_op = edges.find(std::make_pair(a,c)); 
  if (ea_op != edges.end()) { ea_op->second->setOpposite(ea); }
  if (eb_op != edges.end()) { eb_op->second->setOpposite(eb); }
  if (ec_op != edges.end()) { ec_op->second->setOpposite(ec); }
  // add the triangle to the master list
  assert (triangles.find(t->getID()) == triangles.end());
  triangles[t->getID()] = t;
}

// helper function for accessing hash table data
Edge* Mesh::getMeshEdge(Vertex *a, Vertex *b) const {
  edgeshashtype::const_iterator iter = edges.find(std::make_pair(a,b));
  if (iter == edges.end()) return NULL;
  return iter->second;
}

Triangle* Mesh::getTriangle(int index) const {
  triangleshashtype::const_iterator itr = triangles.find(index);
  if( itr == triangles.end() ) {
    return NULL;
  } else {
    return itr->second;
  }
}

//
// Returns set of faces surrounding an edge
//
std::set<Triangle *> Mesh::GetFaces(Edge *e) {
  std::set<Triangle *> t;
  Edge *oe = e;

  do {
    if (e->getOpposite() != NULL) {
      e = e->getOpposite();
    } else { break; }

    Triangle *new_t = e->getTriangle();
    t.insert(new_t);

    e = e->getNext();
  } while (e != oe);

  return t;
}


#define MAX_CHAR_PER_LINE 200

void Mesh::Load(const std::string &input_file) {
    std::ifstream istr(input_file.c_str());
    if (!istr)
    {
        std::cout << "ERROR! CANNOT OPEN: " << input_file << std::endl;
        return;
    }

    char line[MAX_CHAR_PER_LINE];
    std::string token, token2;
    float x, y, z;
    int a, b, c;
    int index = 0;
    int vert_count = 0;
    int vert_index = 1;

    while (istr.getline(line, MAX_CHAR_PER_LINE)) {
        std::stringstream ss;
        ss << line;

        // check for blank line
        token = "";
        ss >> token;
        if (token == "") continue;

        if (token == std::string("usemtl") ||
            token == std::string("g")) {
            vert_index = 1;
            index++;
        } else if (token == std::string("v")) {
            vert_count++;
            ss >> x >> y >> z;
            addVertex(Vec3f(x, y, z));
        } else if (token == std::string("f")) {
            a = b = c = -1;
            ss >> a >> b;
            // handle faces with > 3 vertices
            // assume the face can be triangulated with a triangle fan
            while (ss >> c)
            {
                int a_ = a - vert_index;
                int b_ = b - vert_index;
                int c_ = c - vert_index;
                assert(a_ >= 0 && a_ < numVertices());
                assert(b_ >= 0 && b_ < numVertices());
                assert(c_ >= 0 && c_ < numVertices());
                addTriangle(getVertex(a_), getVertex(b_), getVertex(c_));
                b = c;
            }
        } else if (token == std::string("e")) {
            a = b = -1;
            ss >> a >> b >> token2;
            // whoops: inconsistent file format, don't subtract 1
            assert(a >= 0 && a <= numVertices());
            assert(b >= 0 && b <= numVertices());
            if (token2 == std::string("inf"))
                x = 1000000; // this is close to infinity...
            x = atof(token2.c_str());
            Vertex *va = getVertex(a);
            Vertex *vb = getVertex(b);
            Edge *ab = getMeshEdge(va, vb);
            Edge *ba = getMeshEdge(vb, va);
        } else if (token == std::string("vt")) {
        } else if (token == std::string("vn")) {
        } else if (token[0] == '#') {
        } else {
            printf("LINE: '%s'", line);
        }
    }

    assert(numTriangles() > 0);

}

double Mesh::calculateAreas(std::vector<std::pair<double,bool> > &areas ) {
  double total;
  triangleshashtype::iterator itr = triangles.begin();
  while( itr != triangles.end() ) {
    double area;
    Vec3f a = itr->second->operator[](0)->getPos();
    Vec3f b = itr->second->operator[](1)->getPos();
    Vec3f c = itr->second->operator[](2)->getPos();

    Vec3f ab = b - a;
    Vec3f ac = c - a;
    Vec3f n;
    Vec3f::Cross3(n,ab,ac);

    area = n.Length() / 2;
    areas.push_back(std::make_pair(area,false));
    total += area;

    itr++;
  }

  return total;
}

// other math
Vec3f ComputeNormal(const Vec3f &p1, const Vec3f &p2, const Vec3f &p3) {
  Vec3f v12 = p2;
  v12 -= p1;
  Vec3f v23 = p3;
  v23 -= p2;
  Vec3f normal;
  Vec3f::Cross3(normal,v12,v23);
  normal.Normalize();
  return normal;
}
