#include <iostream>
#include <string>

#include "mesh.h"
#include "triangle.h"

int sample_size;

void sepPathAndFile(const std::string &input, std::string &path, std::string &file) {
  // we need to separate the filename from the path
  // (we assume the vertex & fragment shaders are in the same directory)
  // first, locate the last '/' in the filename
  size_t last = std::string::npos;  
  while (1) {
    int next = input.find('/',last+1);
    if (next != (int)std::string::npos) { 
      last = next;
      continue;
    }
    next = input.find('\\',last+1);
    if (next != (int)std::string::npos) { 
      last = next;
      continue;
    }
    break;
  }
  if (last == std::string::npos) {
    // if there is no directory in the filename
    file = input;
    path = ".";
  } else {
    // separate filename & path
    file = input.substr(last+1,input.size()-last-1);
    path = input.substr(0,last);
  }
}

std::string parse_args(int argc, const char *argv[]) {
  std::string input_file;
  std::string path = ".";
  
  // parse the command line arguments
  for (int i = 1; i < argc; i++) {
    if (std::string(argv[i]) == std::string("-input") || 
        std::string(argv[i]) == std::string("-i")) {
      i++; assert (i < argc); 
      sepPathAndFile(argv[i],path,input_file);
    } else if( std::string(argv[i]) == std::string("-num_samples")) {
      i++; assert (i < argc);
      sample_size = atoi(argv[i]);
    } else {
      std::cout << "ERROR: unknown command line argument " 
                << i << ": '" << argv[i] << "'" << std::endl;
      exit(1);
    }
  }

  return path+"/"+input_file;
}

int pickRandomIndex( std::vector<std::pair<double,bool> > &areas, int n ) {
  double p = rand() % 100;
  double cumulativeProbability = 0.0;
  for( int i = 0; i < areas.size(); i++ ) {
    cumulativeProbability += areas[i].first;
    if (p <= cumulativeProbability && !areas[i].second ) {
      areas[i].second = true;
      return i;
    }
  }
  // else pick a somewhat random index number
  return (rand() % static_cast<int>(n + 1));
}


int main( int argc, const char * argv[] ) {
  // load in file
    std::string file_path = parse_args( argc, argv );
    
    Mesh *m = new Mesh();
    m->Load(file_path);

  // convert to point cloud
    int n = m->numTriangles();
    std::vector<Vec3f> points;
    // calculate areas of all triangles
    std::vector<std::pair<double,bool> > areas;
    double total_area;
    total_area = m->calculateAreas(areas);
    // calculate probabilities for each
    for( int i = 0; i < areas.size(); i++ ) {
      areas[i].first /= total_area;
      areas[i].first *= 100;
    }
    
    for( int i = 0; i < sample_size; i++ ) {
      //pick triangle based on probability
      int tIndex = pickRandomIndex(areas,n);
      // double check to make sure index is valid
      if( tIndex >= n ) {
        tIndex -= n;
      } else if( tIndex < 0 ) {
        tIndex = 0 - tIndex;
      }
      // compute random u & v (0,1)
      double u = (double)rand() / ((double)RAND_MAX + 1);
      double v = (double)rand() / ((double)RAND_MAX + 1);
      if ((u + v) > 1) {
        u = 1 - u;
        v = 1 - v;
      }

      double w = 1 - (u + v);
      assert(u + v + w == 1);
      // std::cout << "imma get that t" << tIndex << std::endl;
      Triangle* t = m->getTriangle(tIndex);
      Vec3f p1 = t->operator[](0)->getPos();
      Vec3f p2 = t->operator[](1)->getPos();
      Vec3f p3 = t->operator[](2)->getPos();
      // std::cout << "imma push it back" <<std::endl;
      points.push_back(p1 * u + p2 * v + p3 * w);
    }
    
    for( int i = 0; i < points.size(); i++ ) {
      //std::cout << points[i].x() << " " << points[i].y() << " " << points[i].z() << std::endl;
      // for easy GMSH viewing
      std::cout << "Point (" << i << ") = {" << points[i].x() << ", " << points[i].y() << ", " << points[i].z() << ", 1.0};" << std::endl;
    }
    
    
}