import wblut.math.*;
import wblut.processing.*;
import wblut.core.*;
import wblut.hemesh.*;
import wblut.geom.*;



HE_Mesh mesh;
WB_Render render;
HEM_FlatExtrude modifier;
HE_Selection sel;
void setup() {
  size(1000, 1000, P3D);
  smooth(8);
  createMesh();
  modifier=new HEM_FlatExtrude();
  modifier.setRelative(false);
  modifier.setChamfer(10);
  modifier.setHardEdgeChamfer(20);
  modifier.setThresholdAngle(1.5*HALF_PI);
 
  mesh.modify(modifier);

  render=new WB_Render(this);
}

void draw() {
  background(120);
  directionalLight(255, 255, 255, 1, 1, -1);
  directionalLight(127, 127, 127, -1, -1, 1);
  translate(width/2, height/2);
  rotateY(mouseX*1.0f/width*TWO_PI);
  rotateX(mouseY*1.0f/height*TWO_PI);
  fill(255);
  noStroke();
  render.drawFaces(mesh);
  fill(255, 0, 0);
  render.drawFaces(mesh.getSelection("walls"));
   fill(0, 0, 255);
  render.drawFaces(mesh.getSelection("extruded"));
  stroke(0);
  render.drawEdges(mesh);
}


void createMesh() {
  HEC_Cube creator=new HEC_Cube(300, 4, 4, 4);
  mesh=new HE_Mesh(creator);
  WB_Plane[] planes;
  int numPlanes;
  HEM_MultiSliceSurface modifier;
  numPlanes=10;
  modifier=new HEM_MultiSliceSurface();
  planes=new WB_Plane[numPlanes];
  for (int i=0; i<numPlanes; i++) {
    planes[i]=new WB_Plane(0, 0, random(-50, 50), random(-1, 1), random(-1, 1), random(-1, 1));
  } 
  modifier.setPlanes(planes);
  mesh.modify(modifier);
  
}

void keyPressed(){
 mesh.clean(); 
}