---
layout: post
title: HE_Mesh!
---

![](http://www.wblut.com/blog/wp-content/2009/05/WindsweptBeethoven_00403.png)

HE_Mesh* is a Java library for creating and manipulating polygonal meshes. Containing no display functionality of its own, it is meant to be used with [Processing](http://processing.org/).

My code quality warnings for the constructs are doubly valid here. HE_Mesh is an after-hours hobby project aggregating years of code snippets and 3rd part libraries. It is by no means production strength, neither in robustness, performance, consistency or maintenance. Consider it a toy, playground, a sandbox** that allows me to explore a range of possibilities that I wouldn't be able to otherwise. Friends*** asked me to share this toy, this library.

Since I cannot give you any guarantees or reliable support,  the code I write for the HE_Mesh library is public domain. Very little in the library is *invented* by me. The knowledge required to create it is all out there.

* Full name: *HE_Mesh*, short name/tag: *hemesh\
*** If you ever played in a sandbox, you will remember some of the nastier things hiding in the sand. The analogy is apt.\
** With friends like that, who needs...

Support
-------

If you wish to support the further development of HE_Mesh, it helped you somehow in a project, or you still need some help, you might consider [this innocent little Paypal link](https://www.paypal.me/wblut).

License
-------

HE_Mesh, with the below exceptions, is dedicated to the public domain. To the extent possible under law, I, Frederik Vanhoutte, have waived all copyright and related or neighboring rights to HE_Mesh. This work is published from Belgium. (<http://creativecommons.org/publicdomain/zero/1.0/>)

The following classes are subject to the license agreement of their original authors, included in the source file:

-   wblut.geom.WB_Earcut
-   wblut.geom.WB_PolygonDecomposer
-   wblut.geom.WB_PolygonSplitter
-   wblut.geom.WB_ShapeReader
-   wblut.hemesh.HEC_SuperDuper
-   wblut.hemesh.HET_FaceSplitter
-   wblut.math.WB_DoubleDouble
-   wblut.math.WB_Ease
-   wblut.math.WB_MTRandom
-   wblut.math.WB_OSNoise
-   wblut.math.WB_PNoise
-   wblut.math.WB_SNoise

The following packages are part of hemesh-external.jar and are subject to the license agreement of their original authors:

-   wblut.external.constrainedDelaunay <https://www2.eecs.berkeley.edu/Pubs/TechRpts/2009/EECS-2009-56.html>
-   wblut.external.Delaunay <https://github.com/visad/visad>
-   wblut.external.ProGAL <http://www.diku.dk/~rfonseca/ProGAL/>
-   wblut.external.straightskeleton <https://code.google.com/p/campskeleton/>
-   wblut.external.QuickHull3D <https://www.cs.ubc.ca/~lloyd/java/quickhull3d.html>

The modified code is available on request.

Getting HE_Mesh
---------------

### Download

-   Latest release (6.0.1): [download](http://wblut.com/hemesh/hemesh.zip)
-   Build of the day (2017-11-06): [download](http://wblut.com/hemesh/hemesh20171105.zip)
-   Repository: [github](https://github.com/wblut/HE_Mesh)

### Installation for Processing

Processing documentation: [How to Install a Contributed Library](https://github.com/processing/processing/wiki/How-to-Install-a-Contributed-Library)

If you want to keep up with the build of the day, manual installation is necessary.

Contributed libraries for Processing are installed inside the *libraries* subfolder of the sketchbook. If you don't know where Processing saves your sketches you can check this in the File->Preferences... menu.

Inside the downloaded zip-file is a single directory *hemesh* with several subfolders. Unzip this folder to the *libraries *subfolder of your sketchbook. Make sure you retain the entire directory structure.

Always close the Processing IDE and delete your previous hemesh installation before installing a new version.

### Setting up a HE_Mesh Processing sketch

This is my minimal framework for a Processing 3 sketch using HE_Mesh.

DATA HOSTED WITH  BY [PASTEBIN.COM](https://pastebin.com/) - [DOWNLOAD RAW](https://pastebin.com/raw/GKCvpRmi) - [SEE ORIGINAL](https://pastebin.com/GKCvpRmi)

1.  import wblut.nurbs.*;

2.  import wblut.hemesh.*;

3.  import wblut.core.*;

4.  import wblut.geom.*;

5.  import wblut.processing.*;

6.  import wblut.math.*;

8.  WB_Render3D render;

10. void setup() {

11. fullScreen(P3D);

12. smooth(8);

13. render=new WB_Render3D(this);

14. }

16. void draw() {

17. background(55);

18. directionalLight(255, 255, 255, 1, 1, -1);

19. directionalLight(127, 127, 127, -1, -1, 1);

20. translate(width/2, height/2, 0);

21. rotateY(map(mouseX,0,width,-PI,PI));

22. rotateX(map(mouseY,0,height,PI,-PI));

23. }

### Hello world!

![](http://www.wblut.com/blog/wp-content/2016/01/screen-900x506.png)

DATA HOSTED WITH  BY [PASTEBIN.COM](https://pastebin.com/) - [DOWNLOAD RAW](https://pastebin.com/raw/50HPP265) - [SEE ORIGINAL](https://pastebin.com/50HPP265)

1.  import wblut.core.*;

2.  import wblut.geom.*;

3.  import wblut.hemesh.*;

4.  import wblut.math.*;

5.  import wblut.nurbs.*;

6.  import wblut.processing.*;

8.  WB_Render3D render;

9.  HE_Mesh mesh;

11. void setup() {

12. fullScreen(P3D);

13. smooth(8);

14. render=new WB_Render3D(this);

15. create();

16. }

18. void create(){

19. HEC_Geodesic creator=new HEC_Geodesic().setRadius(250);

20. mesh=new HE_Mesh(creator);

21. }

23. void draw() {

24. background(55);

25. directionalLight(255, 255, 255, 1, 1, -1);

26. directionalLight(127, 127, 127, -1, -1, 1);

27. translate(width/2, height/2, 0);

28. rotateY(map(mouseX,0,width,-PI,PI));

29. rotateX(map(mouseY,0,height,PI,-PI));

30. noStroke();

31. fill(255);

32. render.drawFaces(mesh);

33. stroke(0);

34. noFill();

35. render.drawEdges(mesh);

36. }

[download](http://wblut.com/tutorial/basic/HelloWorld.zip)