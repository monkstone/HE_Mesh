import wblut.math.*;
import wblut.processing.*;
import wblut.core.*;
import wblut.hemesh.*;
import wblut.geom.*;

HE_Mesh mesh, invertedMesh;
WB_Render render;

void setup() {
  size(1000, 1000, P3D);
  smooth(8);
  createMesh();

  HEM_SphericalInversion modifier=new HEM_SphericalInversion();
  modifier.setRadius(170);
  modifier.setCenter(0, 0, 0);
  //also accepts a WB_Point
  modifier.setCutoff(1000);// maximum distance outside the inversion sphere
  modifier.setLinear(false);// if true, mirrors a point across the sphere surface instead of a true spherical inversion
  invertedMesh=mesh.get();
  invertedMesh.modify(modifier);

  render=new WB_Render(this);
}

void draw() {
  background(55);
  directionalLight(255, 255, 255, 1, 1, -1);
  directionalLight(127, 127, 127, -1, -1, 1);
  translate(width/2, height/2);
  rotateY(mouseX*1.0f/width*TWO_PI);
  rotateX(mouseY*1.0f/height*TWO_PI);
  fill(255);
  noStroke();
  render.drawFaces(invertedMesh);
  stroke(0);
  render.drawEdges(invertedMesh);
  stroke(255,0,0);
  render.drawEdges(mesh);
}


void createMesh() {
  HEC_Cube creator=new HEC_Cube(150, 1, 1, 1);
  mesh=new HE_Mesh(creator);
  HEM_Extrude ext=new HEM_Extrude().setDistance(37.5);
  mesh.modify(ext);
  mesh.getSelection("extruded").modify(ext);
  mesh.getSelection("extruded").modify(ext);
  mesh.getSelection("extruded").modify(ext);
  HEM_Extrude ext2=new HEM_Extrude().setDistance(0).setChamfer(0.5);
  mesh.getSelection("extruded").modify(ext2);
  ext=new HEM_Extrude().setDistance(-37.5);
  mesh.getSelection("extruded").modify(ext);
  mesh.getSelection("extruded").modify(ext);
  mesh.getSelection("extruded").modify(ext);
  mesh.getSelection("extruded").modify(ext);
//  mesh.modify(new HEM_Lattice().setWidth(15).setDepth(15));
  mesh.subdivide(new HES_Planar());
  mesh.smooth(2);
}