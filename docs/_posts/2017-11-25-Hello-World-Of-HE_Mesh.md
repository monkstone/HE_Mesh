---
layout: post
title: HE_Mesh
---

![](http://www.wblut.com/blog/wp-content/2009/05/WindsweptBeethoven_00403.png)

HE_Mesh\* is a Java library for creating and manipulating polygonal meshes. Containing no display functionality of its own, it is meant to be used with [Processing](http://processing.org/).

My code quality warnings for the constructs are doubly valid here. HE_Mesh is an after-hours hobby project aggregating years of code snippets and 3rd part libraries. It is by no means production strength, neither in robustness, performance, consistency or maintenance. Consider it a toy, playground, a sandbox\*\* that allows me to explore a range of possibilities that I wouldn't be able to otherwise. Friends\*\*\* asked me to share this toy, this library.

Since I cannot give you any guarantees or reliable support,  the code I write for the HE_Mesh library is public domain. Very little in the library is *invented* by me. The knowledge required to create it is all out there.

\* Full name: *HE_Mesh*, short name/tag: *hemesh*

\*\* If you ever played in a sandbox, you will remember some of the nastier things hiding in the sand. The analogy is apt.

\*\*\* With friends like that, who needs...

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

-   [wblut.external.constrainedDelaunay](https://www2.eecs.berkeley.edu/Pubs/TechRpts/2009/EECS-2009-56.html)
-   [wblut.external.Delaunay](https://github.com/visad/visad)
-   [wblut.external.ProGAL](http://www.diku.dk/~rfonseca/ProGAL/)
-   [wblut.external.straightskeleton](https://code.google.com/p/campskeleton/)
-   [wblut.external.QuickHull3D](https://www.cs.ubc.ca/~lloyd/java/quickhull3d.html)

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

<script src="https://pastebin.com/embed_js/GKCvpRmi"></script>

### Hello world!

![](http://www.wblut.com/blog/wp-content/2016/01/screen-900x506.png)

<script src="https://pastebin.com/embed_js/50HPP265"></script>

[download](http://wblut.com/tutorial/basic/HelloWorld.zip)