package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.badlogic.gdx.utils.StringBuilder;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.DelaunayCell;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;

import java.util.ArrayList;

//voronoi/polygon research:
//END GOAL: https://www.youtube.com/watch?v=pe4_Dimk7v0
//https://github.com/libgdx/libgdx/wiki/Circles%2C-planes%2C-rays%2C-etc.
//https://en.wikipedia.org/wiki/Circumscribed_circle
//http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect/565282#565282
//http://stackoverflow.com/questions/31021968/correct-use-of-polygon-triangulators-in-libgdx
//https://github.com/mjholtzem/Unity-2D-Destruction
//todo:
// [x] fix grabbing points, focus point, don't lose it
// [x] highlight focused point
// [ ] highlight when near grab-able point, or highlighatble info
// [x] center points
// [x] discard points if too close? epsilon check remove duplicate points
// [x] fix scaling for window resize
// [ ] fix voronoi cells for edge cases
// [ ] extract voronoi cells
// [ ] display points as array both input and output
// [x] display centroid delaunay
// [x] display centroid for sub delaunay
// [x] display centroid voronoi
// [x] display centroid for sub voronoi
// [x] basic shape loader, centered with reasonable scale
// [ ] ability to scale shape, maybe [ALT + L-Click]
//          triangle, rectangle, at least to octagon (or higher since this isn't limited by box2d currently) 12 or allow user to input num sides
// [ ] ability to delete point. maybe [SHIFT + R-Click]
// [ ] editable points list in VisUI textbox
// [...] shatter button
//      [x] place new point at each circumcenter
//      [ ] shatter at Voronoi Center
//      [ ] shatter at Midpoints
//      [ ]
//      [ ] add points at excircle or escribed circle: https://en.wikipedia.org/wiki/Incircle_and_excircles#Excircles_and_excenters
//      [ ] if we think of any more...
//      [x] limit duplicates...
// [x] render to file: Pixmap -> PNG. how to render shaperenderer to file?
//      [x transparency
//      [x] shape only crop? currently is full screen capture.
// [ ] render to file: create animation (https://github.com/tommyettinger/anim8-gdx): base shape, shatter iteration 1-10 (or stop when duplicate points = too small to shatter further)
// [ ] pool cells
// [ ] investigate: sometimes triangulation returns artifacts
//      seems to be more pronounced with equilaterals or concyclic (or cocyclic)
//      or when points line up perfectly on y axis?
// [x] draw Incircle
// [ ] draw Excircle
// [ ] draw Gergonne triangle: contact triangle or intouch triangle of △ A B C
// [ ] draw tangent circles: https://en.wikipedia.org/wiki/Nine-point_circle
// [ ] draw anticomplementary triangle: https://mathworld.wolfram.com/AnticomplementaryTriangle.html
// [ ] color palate: render
// [ ] color pallet: VisUI color picker dialog select render colors
// [ ] color pallet: background color options or checkered tile?
// [ ] render grid lines
// [ ] render grid axis X,Y
// [ ] draw triangle weight graph: area/totalArea
// [ ] render triangle by area relative to total hull area
// [ ] ensure all types can be rendered https://en.wikipedia.org/wiki/Triangle_center
// [ ] project 3D cube to 2D?
// [ ] grid generation!
// [ ] scale
//      rotate 90, 45, user defined?
// [ ] rotate //modifier key + click should scale + rotate when a hull point selected?
// [ ] flip X,Y
// [ ] snap modifier: snap to grid
// [ ] snap modifier: snap to neares center (any of the centers: highlight)
// [ ] tileable voronoi! 2D voronoi wrapped on a 4D torus
// [ ] mst!!! krustal?
// [ ] complete graph: a simple undirected graph in which every pair of distinct vertices is connected by a unique edge.
//      A complete digraph is a directed graph in which every pair of distinct vertices is connected by a pair of unique edges (one in each direction)
//      https://en.wikipedia.org/wiki/Complete_graph
// [ ] Utility graph: ?
//     https://en.wikipedia.org/wiki/Three_utilities_problem
//      see also: https://en.wikipedia.org/wiki/Toroidal_graph
// centers: (in the case of an equilateral, these 4 centers are the same point!)
// [x] incenter -> inscribed
// [x] cicumcenter
// [x] centroid
// [x] orthocenter
// [ ] are there more centers? yes!
// [x] and their respective graphs connecting centers
//      [ ] fix performance, allow select center variant

// Pythagorean triples; A right triangle where the sides are in the ratio of integers
//  eg: 3:4:5 , 6:8:10 , 5:12:13 , 9:12:15 , 8:15:17
// neat: https://www.youtube.com/watch?v=oXcCAAEDte0&t=905s
//https://en.wikipedia.org/wiki/Dual_graph
//hmm, what are "forbidden"?
// https://en.wikipedia.org/wiki/Forbidden_graph_characterization

public class TestVoronoiScreen extends MyScreenAdapter {

    //rendering
    final Matrix4 projectionMatrix = new Matrix4();
    BitmapFont text = FontLoader.createFont(FontLoader.fontBitstreamVMBold, 20);
    BitmapFont dataFont = FontLoader.createFont(FontLoader.fontBitstreamVM, 12);
    GlyphLayout layout = new GlyphLayout();

    //all points that define a polygon and any point inside the polygon
    final FloatArray points = new FloatArray(true, 500/*maxVerticies*/);
    final Array<Color> colorTest = new Array<>();

    //triangulation
    final DelaunayTriangulator delaunay = new DelaunayTriangulator();
    final int maxVertices = 32767;//todo: this is odd number why? there will always be even number of points as xy pairs.
    ShortArray triangles;

    final ArrayList<DelaunayCell> dCells = new ArrayList<>();

    final DelaunayTriangulator dualaunay = new DelaunayTriangulator();// dual delaunay ;)
    final FloatArray centroidPoints = new FloatArray();
    ShortArray centroidGraph;

    //convex hull
    float[] hull;
    final Polygon hullPoly = new Polygon();
    final Vector2 centroid = new Vector2();
    final ConvexHull convex = new ConvexHull();

    final Vector2 cacheVec = new Vector2();
    final Color cacheColor = new Color();
    
    //toggles
    boolean debugPointOrder = false,
            drawCircumcircle = false,
            drawCircumcenter = false,
            drawPoints = true,
            drawDelaunay = true,
            drawVoronoi = false,
            drawMidpoints = false,
            drawMidGraph = false,
            drawCenteroidPointGraph = false,
            drawHull = true,
            drawCentroid = false,
            drawTriangleQuality = false,
            drawTriangleInfo = false,
            voronoiRender = false,
            drawInTriangle = false,
            drawInRadius = false,
            drawInCenter = false,
            drawOrtho = false,
            drawCentroidDelaunay = false;
    
    int pSize = 5;
    float dragRadius = 10;
    int focusedPoint = -1;//no index

    boolean isDrag = false;
    Vector2 dragStart = new Vector2();

    final StringBuilder stringBuilder = new StringBuilder();

    //distance based voronoi render style
    enum DistanceCheck {
        euclidean, manhattan, //todo: minkowski,
        chess, antiChess, multiplyXY, divXY, divYX,
        divMinMax, divMaxMin, minMin, minMax, subXY, subYX;

        private static final DistanceCheck[] VALUES = values();

        public DistanceCheck next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }
    private DistanceCheck renderStyle = DistanceCheck.euclidean;

    //where to add points
    enum TriangleCenter {
        centroid, circumcircle, incenter, orthocenter;
        //todo: gergonne, nagel, ninepoint, excenter, symmedian;

        private static final TriangleCenter[] VALUES = values();

        public TriangleCenter next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }
    private TriangleCenter shatterStyle = TriangleCenter.centroid;

    public TestVoronoiScreen() {
        generateNewPoints(MathUtils.random(3, 16), false, false);
    }

    private void generateNewPoints(int numPoints, boolean regular, boolean addCentroid) {
        clear();
        float centerScreenX = Gdx.graphics.getWidth() * 0.5f;
        float centerScreenY = Gdx.graphics.getHeight() * 0.5f;
        boolean recenter = false;

        /*
        //CO-LINEAR POINTS!!!
        //todo: warning! co-linear points will break centering and triangulation. but makes pretty patterns in the voronoi render layer
        for (int i = 0; i < numPoints * 2; i += 2) {
            float dist = (float) Gdx.graphics.getWidth() / numPoints;
            float x = i * 0.5f * dist;
            float y = centerScreenY;
            points.add(x);
            points.add(y);
        }*/

        if (regular) {
            //shapes!
            //source: ShapeRenderer.circle();
            float x = centerScreenX;
            float y = centerScreenY;
            float size = Math.min(x, y);
            float angle = 2 * MathUtils.PI / numPoints;
            float cos = MathUtils.cos(angle);
            float sin = MathUtils.sin(angle);
            float cx = size, cy = 0;
            for (int i = 0; i < numPoints; i++) {
                points.add(x + cx, y + cy);
                float temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
            }
        } else {
            int pad = 200;//distance away from edge of screen
            for (int i = 0; i < numPoints * 2; i += 2) {
                float x = MathUtils.random(pad, Gdx.graphics.getWidth() - pad);
                float y = MathUtils.random(pad, Gdx.graphics.getHeight() - pad);
                //todo: reject duplicate points
                points.add(x);
                points.add(y);
            }
        }

        //calculate the convex hull of all the points
        //center around centroid in middle of screen
        hull = convex.computePolygon(points, false).toArray();//<--system.arraycopy()
        GeometryUtils.polygonCentroid(hull, 0, hull.length, centroid);
        //Gdx.app.log(getClass().getSimpleName(), "isCCW?" + GeometryUtils.isCCW(hull, 0, hull.length));
        if (addCentroid) {
            points.add(centroid.x, centroid.y);
        }
        if (recenter) {
            for (int i = 0; i < points.size; i += 2) {
                points.set(i, centroid.x - points.get(i) + centerScreenX);
                points.set(i + 1, centroid.y - points.get(i + 1) + centerScreenY);
            }
        }

        regenColor();

        //create cells out of points
        calculateDelaunay();
    }

    /**
     * Calculate triangulation on points.
     * Then create cells out of each triangle.
     * Calculate the convex hull based on outermost points.
     * Find and set neighbors for each cell.
     */
    private void calculateDelaunay() {
        //polygons must contain at least 3 points.
        if (points.size < 6) return;

        //todo: ArrayIndexOutOfBoundsException: Index -32768 out of bounds for length 32766
        // how did index become negative when we do not allow less than 6? see line above
        //
        // Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index -32768 out of bounds for length 32766
        // at com.badlogic.gdx.math.DelaunayTriangulator.computeTriangles(DelaunayTriangulator.java:140) -> i = p2 - end;
        // at com.badlogic.gdx.math.DelaunayTriangulator.computeTriangles(DelaunayTriangulator.java:43)
        // at com.spaceproject.screens.debug.TestVoronoiScreen.calculateDelaunay(TestVoronoiScreen.java:163)

        //computeTriangles() supports only up to 32767 IllegalArgumentException: count must be <= 32767
        if (points.size > maxVertices - 2) {
            Gdx.app.error(getClass().getSimpleName(), points.size + " too big. calculation ignored!");
            return;
        }

        //apply delaunay triangulation to points
        triangles = delaunay.computeTriangles(points, false);
        
        //create cells for each triangle
        dCells.clear();
        int discard = 1;
        for (int i = 0; i < triangles.size; i += 3) {
            //get point indexes
            int p1 = triangles.get(i) * 2;
            int p2 = triangles.get(i + 1) * 2;
            int p3 = triangles.get(i + 2) * 2;

            float p1x = points.get(p1), p1y = points.get(p1 + 1); // xy: 0, 1
            float p2x = points.get(p2), p2y = points.get(p2 + 1); // xy: 2, 3
            float p3x = points.get(p3), p3y = points.get(p3 + 1); // xy: 4, 5
            //discard duplicate points
            if     ((MathUtils.isEqual(p1x, p2x) && MathUtils.isEqual(p1y, p2y)) || // p1 == p2 or
                    (MathUtils.isEqual(p1x, p3x) && MathUtils.isEqual(p1y, p3y)) || // p1 == p3 or
                    (MathUtils.isEqual(p2x, p3x) && MathUtils.isEqual(p2y, p3y))) { // p2 == p3
                Gdx.app.error(getClass().getSimpleName(), "Duplicate point!: " + discard++);
                continue;
            }

            Vector2 a = new Vector2(p1x, p1y);
            Vector2 b = new Vector2(p2x, p2y);
            Vector2 c = new Vector2(p3x, p3y);
            DelaunayCell d = new DelaunayCell(a, b, c);//todo: pool cell
            //todo: either pool the vecs, or store index of points and retrive from points
            //DelaunayCell d = new DelaunayCell(points, p1, p2, p3);
            dCells.add(d);
        }
        
        //find surrounding cells for each cell
        DelaunayCell.findNeighbors(dCells);
        
        //calculate the convex hull of all the points
        hull = convex.computePolygon(points, false).toArray();// <- system.arraycopy()
        //computePolygon -> Returns convex hull in counter-clockwise order. Note: the last point in the returned list is the same as the first one.
        //todo: explore isCCW
        // Gdx.app.log(getClass().getSimpleName(), "isCCW?" + GeometryUtils.isCCW(hull, 0, hull.length));
        hullPoly.setVertices(hull);
        hullPoly.getCentroid(centroid); //-> GeometryUtils.polygonCentroid(hull, 0, hull.length, centroid);

        if (drawCentroidDelaunay) {
            calculateCentroidDelaunay();
        }
    }

    private void calculateCentroidDelaunay() {
        if (dCells.isEmpty()) return;
        //holy crap more graphs!!! but getting heavy to compute....
        centroidPoints.clear();
        for (DelaunayCell cell : dCells) {
            float px = 0, py = 0;
            switch (shatterStyle) {
                case centroid:
                    px = cell.centroid.x;
                    py = cell.centroid.y;
                    break;
                case circumcircle:
                    px = cell.circumCenter.x;
                    py = cell.circumCenter.y;
                    break;
                case incenter:
                    px = cell.incircle.x;
                    py = cell.incircle.y;
                    break;
                case orthocenter:
                    px = cell.orthocenter.x;
                    py = cell.orthocenter.y;
                    break;
            }
            centroidPoints.add(px, py);
        }
        centroidGraph = dualaunay.computeTriangles(centroidPoints, false);
    }

    /**
     * Check if a line from a to b intersects with the convex hull.
     * If so, the point of intersection is stored in the intersect vector.
     *
     * @param a         point one
     * @param b         point two
     * @param intersect point of intersection
     * @return true if intersect. Point of intersection stored in intersect
     */
    public boolean collideWithHull(Vector2 a, Vector2 b, Vector2 intersect) {
        //for each line segment between two vertices
        float[] vertices = hullPoly.getTransformedVertices();
        for (int v = 0; v < vertices.length - 2; v += 2) {
            float xA = vertices[v];
            float yA = vertices[v + 1];
            float xB = vertices[v + 2];
            float yB = vertices[v + 3];
            // convex hull line between A and B
            Vector2 edgeA = new Vector2(xA, yA); //todo: cache
            Vector2 edgeB = new Vector2(xB, yB); //todo: cache
            
            if (Intersector.intersectSegments(edgeA, edgeB, a, b, intersect)) {
                //the two lines intersect. point of intersection is set in variable intersect
                return true;
            }
            
        }
        return false;
    }
    
    private void drawCellEdge(DelaunayCell cellA, DelaunayCell cellB) {
        //check circle is within hull
        if (hullPoly.contains(cellA.circumCenter)) {
            if (cellB != null) {
                shape.setColor(Color.ORANGE);
                if (hullPoly.contains(cellB.circumCenter)) {
                    //if both circumcenters are within the convex hull, draw from circumcenter to circumcenter
                    shape.line(cellA.circumCenter, cellB.circumCenter);
                } else {
                    //if circumcenter is outside convex hull, draw from circumcenter to intersection
                    Vector2 intersect = new Vector2();//todo: cache
                    if (collideWithHull(cellA.circumCenter, cellB.circumCenter, intersect)) {
                        shape.line(cellA.circumCenter, intersect);
                        shape.circle(intersect.x, intersect.y, 8);//show point of intersection
                    }
                }
            } else {
                //if no neighbor, draw from midpoint to circumcenter
                //TODO: don't connect to mid points if already connected to voronoi point
                shape.setColor(Color.PINK);
                shape.line(cellA.circumCenter, cellA.midAB);
                shape.line(cellA.circumCenter, cellA.midBC);
                shape.line(cellA.circumCenter, cellA.midCA);
                shape.circle(cellA.midAB.x, cellA.midAB.y, 8);
                shape.circle(cellA.midBC.x, cellA.midBC.y, 8);
                shape.circle(cellA.midCA.x, cellA.midCA.y, 8);
            }
        } else {
            // check collision with convex hull, only draw within hull
            float[] vertices = hullPoly.getTransformedVertices();
            for (int v = 0; v < vertices.length - 2; v += 2) {
                float xA = vertices[v];
                float yA = vertices[v + 1];
                float xB = vertices[v + 2];
                float yB = vertices[v + 3];
                // convex hull line
                Vector2 edgeA = new Vector2(xA, yA); //todo: cache
                Vector2 edgeB = new Vector2(xB, yB); //todo: cache
                
                //TODO: only draw where voronoi points are not connected and ignore certain midpoints.
                //ignore where the circumcenter is outside of hull? and some other edge cases...
                //if (Intersector.isPointInTriangle(cellA.circumcenter, cellA.a, cellA.b, cellA.c)?
                //if (Intersector.pointLineSide(edgeA, edgeB, cellA.midXX) == 1)?
                //if circumcenter is same side as midpoint, opposite of obtuse angle, dont draw?
                drawIntersectingLines(cellA, cellA.midAB, edgeA, edgeB);
                drawIntersectingLines(cellA, cellA.midBC, edgeA, edgeB);
                drawIntersectingLines(cellA, cellA.midCA, edgeA, edgeB);
            }
        }
    }
    
    private void drawIntersectingLines(DelaunayCell cell, Vector2 mid, Vector2 edgeA, Vector2 edgeB) {
        Vector2 intersect = new Vector2();//todo: cache
        if (Intersector.intersectSegments(edgeA, edgeB, cell.circumCenter, mid, intersect)) {
            shape.setColor(Color.GREEN);
            shape.line(mid, intersect);
            shape.circle(intersect.x, intersect.y, 3);
        }
    }
    
    private void drawStuff() {
        //enable blending
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        //background distance based voronoi render
        if (voronoiRender && points.notEmpty()) {
            shape.begin(ShapeType.Line);
            //todo: this could be rendered to a texture, distance does not need to be calculated every frame.
            //  only needs to update when points are modified (add/remove/drag)
            for (int y = 0; y < Gdx.graphics.getHeight(); y++) {
                for (int x = 0; x < Gdx.graphics.getWidth(); x++) {
                    shape.setColor(closestPointColor(x, y));
                    shape.point(x, y, 0);
                }
            }
            shape.end();
        }

        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        cacheVec.set(mouseX, mouseY);
        //mouse grab point
        if (focusedPoint >= 0) {
            shape.begin(ShapeType.Filled);
            float focusX = points.get(focusedPoint);
            float focusY = points.get(focusedPoint + 1);
            shape.setColor(Color.GREEN);
            shape.circle(focusX, focusY, dragRadius);
            shape.end();
        }

        if (drawTriangleQuality) {
            //moveable layer? this could by under or over...
            shape.begin(ShapeType.Filled);
            for (DelaunayCell cell : dCells) {
                shape.setColor(0.3f, 0.3f, 0.3f, 1 - cell.quality);
                shape.triangle(cell.a.x, cell.a.y, cell.b.x, cell.b.y, cell.c.x, cell.c.y);
            }
            shape.end();
        }

        shape.begin(ShapeType.Line);

        //all points
        if (drawPoints) {
            shape.setColor(Color.BLACK);
            for (int i = 0; i < points.size; i += 2) {
                float x = points.get(i);
                float y = points.get(i + 1);
                shape.circle(x, y, pSize);
            }
        }

        //main centroid for whole poly
        if (drawCentroid) {
            shape.setColor(Color.RED);
            shape.circle(centroid.x, centroid.y, pSize);
        }
        
        for (DelaunayCell cell : dCells) {
            //draw circumcircle
            if (!hullPoly.contains(cell.circumCenter)) {
                shape.setColor(Color.MAGENTA);
            } else {
                shape.setColor(Color.GREEN);
            }
            if (drawCircumcircle) {
                //todo: failed: ArrayIndexOutOfBoundsException: Index 20003 out of bounds for length 20000
                //      Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 20003 out of bounds for length 20000
                //      at com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20.color(ImmediateModeRenderer20.java:121)
                //      at com.badlogic.gdx.graphics.glutils.ShapeRenderer.circle(ShapeRenderer.java:906)
                //      at com.badlogic.gdx.graphics.glutils.ShapeRenderer.circle(ShapeRenderer.java:892)
                //      at com.spaceproject.screens.debug.TestVoronoiScreen.drawStuff(TestVoronoiScreen.java:438)
                shape.circle(cell.circumCenter.x, cell.circumCenter.y, cell.circumRadius);
            }
            if (drawCircumcenter) {
                shape.circle(cell.circumCenter.x, cell.circumCenter.y, pSize);
            }

            if (drawInRadius) {
                shape.setColor(Color.CORAL);
                shape.circle(cell.incircle.x, cell.incircle.y, cell.inRadius);
            }
            if (drawInCenter) {
                shape.setColor(Color.CORAL);
                shape.circle(cell.incircle.x, cell.incircle.y, 3);
            }

            /*
            //differentiate points A, B, C
            if (drawPoints) {
                shape.setColor(Color.BLACK);
                shape.circle(cell.a.x, cell.a.y, 1);
                shape.circle(cell.b.x, cell.b.y, 2);
                shape.circle(cell.c.x, cell.c.y, 3);
            }*/

            //per delaunay cell centroid
            if (drawCentroid) {
                shape.setColor(Color.CYAN);
                shape.circle(cell.centroid.x, cell.centroid.y, pSize);
            }
            
            //draw delaunay triangles
            if (drawDelaunay) {
                shape.setColor(Color.BLACK);
                shape.triangle(cell.a.x, cell.a.y, // A
                        cell.b.x, cell.b.y, // B
                        cell.c.x, cell.c.y);// C
            }
            
            //draw midpoints
            if (drawMidpoints) {
                shape.setColor(Color.BLUE);
                shape.circle(cell.midAB.x, cell.midAB.y, 1);//Different radius to help determine edge
                shape.circle(cell.midCA.x, cell.midCA.y, 2);
                shape.circle(cell.midBC.x, cell.midBC.y, 3);
            }

            //draw ortho
            if (drawOrtho) {
                shape.setColor(Color.MAGENTA);
                shape.circle(cell.orthocenter.x, cell.orthocenter.y, 2);
            }

            //connect centroids: another dual-graph?
            if (drawCenteroidPointGraph) {
                shape.setColor(Color.PURPLE);
                shape.line(cell.centroid, cell.a);
                shape.line(cell.centroid, cell.b);
                shape.line(cell.centroid, cell.c);
                //todo: OBSERVATIONS of VertexToCentroid Dual Graph
                // all internal cells are only ever 4 vertex (quadrilateral) and always convex!
                // edges connect to nowhere
            }
            //NOTE: Delaunay and Voronoi are Convex polygons only.
            //a second dual-graph? (tri-graph?)
            //connect midpoints to delaunay centroid
            //midpoint also known as: semiperimeter: https://en.wikipedia.org/wiki/Semiperimeter
            if (drawMidGraph) {
                shape.setColor(Color.BLUE);
                shape.line(cell.centroid, cell.midAB);
                shape.line(cell.centroid, cell.midBC);
                shape.line(cell.centroid, cell.midCA);
                //todo: OBSERVATIONS of MidPointToCentroid Dual Graph
                // cells may be concave and convex!
            }

            if (drawInTriangle) {
                shape.setColor(Color.ROYAL);
                shape.triangle(cell.midAB.x, cell.midAB.y,
                        cell.midBC.x, cell.midBC.y,
                        cell.midCA.x, cell.midCA.y);

            }
            //are the other dual graphs? I think im beginning to see some shapes and patterns...
            //there's graphs everywhere!!!

            //draw voronoi cells
            if (drawVoronoi) {
				/*
				shape.setColor(Color.ORANGE);		
				shape.line(cell.circumcenter, cell.midAB);
				shape.line(cell.circumcenter, cell.midBC);
				shape.line(cell.circumcenter, cell.midCA);
				*/
				/*
				shape.setColor(Color.ORANGE);
				if (cell.nAB != null) shape.line(cell.circumcenter, cell.nAB.circumcenter);
				if (cell.nBC != null) shape.line(cell.circumcenter, cell.nBC.circumcenter);
				if (cell.nCA != null) shape.line(cell.circumcenter, cell.nCA.circumcenter);
				*/
                
                drawCellEdge(cell, cell.nAB);
                drawCellEdge(cell, cell.nBC);
                drawCellEdge(cell, cell.nBC);
                
				/*
				if (hullPoly.contains(cell.circumcenter)) {
					/*
					shape.setColor(Color.ORANGE);
					if (cell.nAB != null) shape.line(cell.circumcenter, cell.nAB.circumcenter);
					if (cell.nBC != null) shape.line(cell.circumcenter, cell.nBC.circumcenter);
					if (cell.nCA != null) shape.line(cell.circumcenter, cell.nCA.circumcenter);
					*
					/*
					drawCellEdge(cell, cell.nAB);
					drawCellEdge(cell, cell.nBC);
					drawCellEdge(cell, cell.nBC);*

				} else {
					//check collision with convex hull, only draw within hull
					float[] vertices = hullPoly.getTransformedVertices();
					for (int velocity = 0; velocity < vertices.length - 2; velocity += 2) {
						float x1 = vertices[velocity];
						float y1 = vertices[velocity + 1];
						float x2 = vertices[velocity + 2];
						float y2 = vertices[velocity + 3];
						// convex hull line
						Vector2 edgeA = new Vector2(x1, y1);
						Vector2 edgeB = new Vector2(x2, y2);

						drawIntersectingLines(cell, cell.midAB, edgeA, edgeB);
						drawIntersectingLines(cell, cell.midBC, edgeA, edgeB);
						drawIntersectingLines(cell, cell.midCA, edgeA, edgeB);

					}
				}*/
            }
        }


        shape.end();

/*
        if (drawTriangleQuality) {
            //moveable layer? this could by under or over...
            shape.begin(ShapeType.Filled);
            for (DelaunayCell cell : dCells) {
                shape.setColor(0.3f, 0.3f, 0.3f, 1 - cell.quality);
                shape.triangle(cell.a.x, cell.a.y, cell.b.x, cell.b.y, cell.c.x, cell.c.y);
            }
            shape.end();
        }*/

        //always at end to be on top
        shape.begin(ShapeType.Line);
        if (drawCentroidDelaunay && centroidPoints.notEmpty()) {
            int discard = 1;
            shape.setColor(Color.GREEN);
            for (int i = 0; i < centroidGraph.size; i += 3) {
                //get point indexes
                int p1 = centroidGraph.get(i) * 2;
                int p2 = centroidGraph.get(i + 1) * 2;
                int p3 = centroidGraph.get(i + 2) * 2;

                float ax = centroidPoints.get(p1), ay = centroidPoints.get(p1 + 1); // xy: 0, 1
                float bx = centroidPoints.get(p2), by = centroidPoints.get(p2 + 1); // xy: 2, 3
                float cx = centroidPoints.get(p3), cy = centroidPoints.get(p3 + 1); // xy: 4, 5
                //discard duplicate points
                if     ((MathUtils.isEqual(ax, bx) && MathUtils.isEqual(ay, by)) ||
                        (MathUtils.isEqual(ax, cx) && MathUtils.isEqual(ay, cy)) ||
                        (MathUtils.isEqual(bx, cx) && MathUtils.isEqual(by, cy))) {
                    Gdx.app.error(getClass().getSimpleName(), "Duplicate point!: " + discard++);
                    continue;
                }
                shape.triangle(ax, ay, bx, by, cx, cy);
            }
        }

        if (drawHull && hull != null) {
            shape.setColor(Color.RED);
            shape.polyline(hullPoly.getVertices());
        }

        if (drawTriangleInfo) {
            shape.setColor(Color.WHITE);
            for (DelaunayCell cell : dCells) {
                //only render mouse over triangle
                if (!Intersector.isPointInTriangle(cacheVec, cell.a, cell.b, cell.c)) {
                    continue;
                }
                shape.triangle(cell.a.x, cell.a.y, // A
                        cell.b.x, cell.b.y, // B
                        cell.c.x, cell.c.y);// C

                //could display each centroid type?
                float px = 0, py = 0;
                switch (shatterStyle) {
                    case centroid:
                        px = cell.centroid.x;
                        py = cell.centroid.y;
                        break;
                    case circumcircle:
                        px = cell.circumCenter.x;
                        py = cell.circumCenter.y;
                        break;
                    case incenter:
                        px = cell.incircle.x;
                        py = cell.incircle.y;
                        break;
                    case orthocenter:
                        px = cell.orthocenter.x;
                        py = cell.orthocenter.y;
                        break;
                }
                shape.circle(px, py, 2);
                //render all so you can see which triangle they belong too, as it is noisy with many dense points
                shape.circle(cell.centroid.x, cell.centroid.y, 1);
                shape.circle(cell.circumCenter.x, cell.circumCenter.y, 1);
                shape.circle(cell.incircle.x, cell.incircle.y, 1);
                shape.circle(cell.orthocenter.x, cell.orthocenter.y, 1);
            }
        }

        // debug draw points in order first to last color coded
        // this is to visualize the actual order of the points in the array
        if (debugPointOrder) {
            if (points.size > 2) {
                Color colorA = Color.RED;
                Color colorB = Color.BLUE;
                float pX = points.get(0), pY = points.get(1);
                for (int i = 0; i < points.size; i += 2) {
                    float x = points.get(i);
                    float y = points.get(i + 1);
                    if (i != 0) { //skip if no previous xy
                        float ratio = (float) i / points.size;
                        cacheColor.set(colorA).lerp(colorB, ratio);
                        shape.line(x, y, pX, pY, cacheColor, cacheColor);
                        pX = x;
                        pY = y;
                    }
                }
            }
        }

        if (snap != null) {
            shape.setColor(Color.GREEN);
            shape.circle(snap.x, snap.y, 3);
        }
        shape.end();


        batch.begin();
        boolean debugLocation = false;
        boolean debugCentroid = true;
        boolean debugQA = false; //qa = quality area
        if (drawTriangleInfo) {
            dataFont.setColor(Color.BLACK);
            for (DelaunayCell cell : dCells) {
                //only render mouse over triangle
                if (!Intersector.isPointInTriangle(cacheVec, cell.a, cell.b, cell.c)) {
                    continue;
                }
                //could display each centroid type?
                float px = 0, py = 0;
                switch (shatterStyle) {
                    case centroid:
                        px = cell.centroid.x;
                        py = cell.centroid.y;
                        break;
                    case circumcircle:
                        px = cell.circumCenter.x;
                        py = cell.circumCenter.y;
                        break;
                    case incenter:
                        px = cell.incircle.x;
                        py = cell.incircle.y;
                        break;
                    case orthocenter:
                        px = cell.orthocenter.x;
                        py = cell.orthocenter.y;
                        break;
                }
                //draw cendroid!
                layout.setText(dataFont, MyMath.round(px, 2) + "," + MyMath.round(py, 2), dataFont.getColor(), 0, Align.center, false);
                dataFont.draw(batch, layout, px, py);
                //a, b, c
                layout.setText(dataFont, MyMath.round(cell.a.x, 2) + "," + MyMath.round(cell.a.y, 2), dataFont.getColor(), 0, Align.center, false);
                dataFont.draw(batch, layout, cell.a.x, cell.a.y);
                layout.setText(dataFont, MyMath.round(cell.b.x, 2) + "," + MyMath.round(cell.b.y, 2), dataFont.getColor(), 0, Align.center, false);
                dataFont.draw(batch, layout, cell.b.x, cell.b.y);
                layout.setText(dataFont, MyMath.round(cell.c.x, 2) + "," + MyMath.round(cell.c.y, 2), dataFont.getColor(), 0, Align.center, false);
                dataFont.draw(batch, layout, cell.c.x, cell.c.y);

                if (debugQA) {
                    //area
                    layout.setText(dataFont, MyMath.round(cell.area, 2) + "", dataFont.getColor(), 0, Align.center, false);
                    dataFont.draw(batch, layout, cell.centroid.x, cell.centroid.y);
                    //quality
                    layout.setText(dataFont, cell.quality + "", dataFont.getColor(), 0, Align.center, false);
                    dataFont.draw(batch, layout, cell.centroid.x, cell.centroid.y - layout.height * 1.3f);
                }
                //show points [0,0,0,0,0,0,0,0,0,0] ...
                //show hull [0,0,0,0,0,0]
                //show triangles [0,0,0,0,0], [0,0,0,0,0], [0,0,0,0,0], [0,0,0,0,0]

                //or highlight?
                // show all points but highlight hull points [0,[#FF0000]0,0,0,0,0,0,0,0,0]
                // (may be faster to render once normally then just render hull on top? how to align characters tho...monospace?
            }

            //render location
            if (debugLocation) {
                for (int i = 0; i < points.size; i += 2) {
                    float x = points.get(i);
                    float y = points.get(i + 1);
                    layout.setText(dataFont, MyMath.round(x, 2) + ", " + MyMath.round(y, 2), dataFont.getColor(), 0, Align.center, false);
                    dataFont.draw(batch, layout, x, y);
                }
            }
        }

        //raw points
        if (debugCentroid) {
            dataFont.setColor(Color.BLACK);
            if (points.size <= 4) {
                for (int i = 0; i < points.size; i += 2) {
                    float x = points.get(i);
                    float y = points.get(i + 1);
                    layout.setText(dataFont, MyMath.formatPos(x, y, 1), dataFont.getColor(), 0, Align.center, false);
                    dataFont.draw(batch, layout, x, y);
                }
            }
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public Color closestPointColor(float x, float y) {
        cacheVec.set(x, y);
        int closeIndex = -1;//index?
        float closeDist = Float.MAX_VALUE;
        for (int i = 0; i < points.size; i += 2) {
            float xx = points.get(i);
            float yy = points.get(i + 1);
            float dX = Math.abs(x - xx);
            float dY = Math.abs(y - yy);
            float min = Math.min(dX, dY);
            float max = Math.max(dX, dY);
            float dist = 0;
            switch (renderStyle) {
                case euclidean:
                    dist = cacheVec.dst2(xx, yy); //squared for cpu
                    break;
                case manhattan: //add
                    //dist = Math.abs(x - xx) + Math.abs(y - yy);
                    dist = dX + dY;
                    break;
                case chess: //Chebyshev (max)
                    dist = max;
                    dist =  MyMath.chessDistance(x, y, xx, yy);
                    break;
                case antiChess: //anti-Chebyshev (min)
                    //dist = MyMath.antiChessDistance(x, y, xx, yy);
                    dist = min;
                    break;
                case multiplyXY:
                    dist = dX * dY;
                    break;
                case divXY:
                    dist = dX / dY;
                    break;
                case divYX:
                    dist = dY / dX;
                    break;
                    case minMin:
                    dist = min * min;
                    break;
                case minMax:
                    dist = min * max;
                    break;
                case divMinMax:
                    dist = min / max;
                    break;
                case divMaxMin:
                    dist = max / min;
                    break;
                /*case maxMax:
                    dist = max * max;//exact same as chess!
                    break;*/
                case subXY:
                    dist = dX - dY;//inverse? of manhattan, doesnt seem to be very interesting.
                    break;
                case subYX:
                    dist = dY - dX;
                    break;
            }
            if (dist < closeDist) {
                closeDist = dist;
                closeIndex = i;
            }
        }
        if (closeIndex != 0) {
            closeIndex /= 2;
        }
        return colorTest.get(closeIndex);
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        //clear screen
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        //render voronoi stuff
        drawStuff();

        //toggles, add/move points, reset
        updateControls();
        
        drawMenu();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(projectionMatrix);
        //todo: recentering may also flip the entire polygon? interesting bug...
        //todo: could put a timer to wait a small delay that gets reset on repid resizing so calculating large points is less on high point count
        boolean recenter = true;
        if (recenter) {
            //calculate the convex hull of all the points
            //center around centroid in middle of screen
            if (points.size < 3) {
                //if n = two, center is difference between the points
                //centroid = (p1 - p2) / 2
                //if n = 1, just center the single point...
                return;
            }
            hull = convex.computePolygon(points, false).toArray(); //sorted seems to have no impact on
            GeometryUtils.polygonCentroid(hull, 0, hull.length, centroid);
            for (int i = 0; i < points.size; i+= 2) {
                points.set(i, centroid.x - points.get(i) + width*0.5f);
                points.set(i + 1, centroid.y - points.get(i + 1) + height*0.5f);
            }
            calculateDelaunay();
        }
    }

    private void drawMenu() {
        batch.begin();
        int x = 10;
        int y = Gdx.graphics.getHeight() - x;
        float h = text.getLineHeight();
        int line = 0;
        int maxPoints = maxVertices / 2;
        layout.setText(text, "Points: " + (int) (points.size * 0.5f) + "/" + maxPoints + ", D-Cells:" + dCells.size() + ", V-Cells: ?", Color.WHITE, 0, Align.center, false);
        text.draw(batch, layout, Gdx.graphics.getWidth() * 0.5f, y);

        text.setColor(Color.BLACK);
        //controls
        text.draw(batch, "[ESC] Return to menu", x, y - h * line++);
        text.draw(batch, "[X] Clear", x, y - h * line++);//B makes no sense but Copy has to be C
        text.draw(batch, "[Spacebar] Generate Random + [ALT] add centroid", x, y - h * line++);
        text.draw(batch, "[SHIFT + Spacebar] Generate Regular + [ALT] add centroid", x, y - h * line++);
        text.draw(batch, "[L-Click] Drag point", x, y - h * line++);
        text.draw(batch, "[R-Click] Create new point", x, y - h  * line++);
        //text.draw(batch, "[Shift + R-Click] Create new point: locked", 10, y- h  * line++);//todo
        text.draw(batch, "[SHIFT + L-Click] Drag points", x, y - h * line++);
        text.draw(batch, "[S] Shatter -> " + shatterStyle.toString().toUpperCase(), x, y - h * line++);
        text.draw(batch, "[A] Cycle Shatter Center" , x, y - h * line++);
        text.draw(batch, "[CTRL + D] Save PNG", x, y - h * line++);
        text.draw(batch, "[CTRL + C] Copy points to clipboard", x, y - h * line++); //todo
        text.draw(batch, "[CTRL + V] Load points from clipboard", x, y - h * line++); //todo

        //todo: reorder in a more sensible grouping. maybe:
        //  points
        //      - centers and circles
        // graphs:
        //      - hull
        //      - dalaunay
        //      -

        //toggles
        text.setColor(debugPointOrder ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[F2] Debug Point Order", x, y - h  * line++);

        text.setColor(drawCircumcenter ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[1] CircumCenter", x, y - h  * line++);
        
        text.setColor(drawCircumcircle ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[2] CircumCircle", x, y - h * line++);
        
        text.setColor(drawPoints ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[3] Points", x, y - h * line++);
        
        text.setColor(drawMidpoints ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[4] Semiperimeter (midpoints)", x, y - h * line++);
        
        text.setColor(drawVoronoi ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[5] Voronoi Graph", x, y - h * line++);
        
        text.setColor(drawDelaunay ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[6] Delaunay Graph", x, y - h * line++);
        
        text.setColor(drawHull ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[7] Hull", x, y - h * line++);
        
        text.setColor(drawCentroid ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[8] Centroid", x, y - h * line++);

        text.setColor(drawTriangleQuality ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[9] Triangle Quality Graph", x, y - h * line++);

        text.setColor(drawTriangleInfo ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[0] Triangle Info", x, y - h * line++);

        text.setColor(drawCenteroidPointGraph ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[-] Centroid-Vertex Graph", x, y - h * line++);

        text.setColor(drawMidGraph ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[=] Centroid-Semiperimeter Graph", x, y - h * line++);

        text.setColor(voronoiRender ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[Q] Voronoi Render -> " + renderStyle.name().toUpperCase(), x, y - h * line++);
        text.draw(batch, "[W] Cycle Render Style", x, y - h * line++);
        text.draw(batch, "[E] Voronoi Render (Regenerate Colors)", x, y - h * line++);

        text.setColor(drawInTriangle ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[T] InTriangle", x, y - h * line++);

        text.setColor(drawInCenter ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[Y] InCircle: InCenter", x, y - h * line++);
        text.setColor(drawInRadius ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[U] InCircle: InRadius", x, y - h * line++);

        text.setColor(drawOrtho ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[I] OrthoCenter", x, y - h * line++);

        text.setColor(drawCentroidDelaunay ? Color.GREEN : Color.BLACK);
        text.draw(batch, "[O] Centroid Delaunay Graph -> " +  shatterStyle.name().toUpperCase(), x, y - h * line++);

        batch.end();
    }

    Vector2 snap = null;
    private void updateControls() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }

        // clear: full reset
        if (Gdx.input.isKeyJustPressed(Keys.X)) {
            clear();
        }

        //reset. random test points
        if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            generateNewPoints(MathUtils.random(3, 16), Gdx.input.isKeyPressed(Keys.SHIFT_LEFT), Gdx.input.isKeyPressed(Keys.CONTROL_LEFT));
        }

        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();

        if (Gdx.input.isKeyJustPressed(Keys.SHIFT_LEFT)) {
            float snapRadius = 20;
            for (DelaunayCell cell : dCells) {
                if (Vector2.dst(x, y, cell.centroid.x, cell.centroid.y) < snapRadius) {
                    //highlight cell, set x,y marker
                    snap = new Vector2(cell.centroid);
                }
            }
            //snap should highlight snap location on press, add on release
        }

        //create new point
        if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.RIGHT)) {
            //computeTriangles() supports only up to 32767 IllegalArgumentException: count must be <= 32767
            if (points.size > maxVertices) {
                Gdx.app.error(getClass().getSimpleName(), points.size + " > 32767. ignored!");
                return;
            }
            if (!isDuplicate(x, y)) {
                points.add(x);
                points.add(y);
                colorTest.add(new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 1));
                calculateDelaunay();
            }
        }

        if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
            if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) {
                //drag all points around
                if (!isDrag) {
                    isDrag = true;
                    dragStart.set(x, y);
                }
                float offsetX = dragStart.x - x;
                float offsetY = dragStart.y - y;
                for (int i = 0; i < points.size; i += 2) {
                    float px = points.get(i);
                    float py = points.get(i + 1);
                    points.set(i, px - offsetX);
                    points.set(i + 1, py - offsetY);
                }
                calculateDelaunay();
                dragStart.set(x, y);
            } else {
                //drag selected point around
                boolean mod = false;
                if (focusedPoint >= 0) {
                    points.set(focusedPoint, x);
                    points.set(focusedPoint + 1, y);
                    mod = true;
                }
                for (int i = 0; i < points.size && !mod; i += 2) {
                    //if (i == focusedPoint) continue; //skip self
                    float px = points.get(i);
                    float py = points.get(i + 1);
                    if (Vector2.dst(x, y, px, py) < dragRadius) {
                        focusedPoint = i;
                        mod = true;
                    }
                }
                if (mod) {
                    calculateDelaunay();
                }
            }
        } else {
            focusedPoint = -1;
            isDrag = false;
        }

        if (Gdx.input.isKeyJustPressed(Keys.A)) {
            shatterStyle = shatterStyle.next();
            if (drawCentroidDelaunay) {
                calculateCentroidDelaunay();
            }
        }

        //sub-shatter!
        if (Gdx.input.isKeyJustPressed(Keys.S)) {
            boolean added = false;
            for (DelaunayCell cell : dCells) {
                //computeTriangles() supports only up to 32767 IllegalArgumentException: count must be <= 32767
                if (points.size > maxVertices - 4) {
                    Gdx.app.error(getClass().getSimpleName(), points.size + " too many points. shatter aborted!");
                    return;
                }
                float px = 0, py = 0;
                switch (shatterStyle) {
                    case centroid:
                        px = cell.centroid.x;
                        py = cell.centroid.y;
                        break;
                    case circumcircle:
                        px = cell.circumCenter.x;
                        py = cell.circumCenter.y;
                        break;
                    case incenter:
                        px = cell.incircle.x;
                        py = cell.incircle.y;
                        break;
                    case orthocenter:
                        px = cell.orthocenter.x;
                        py = cell.orthocenter.y;
                        break;
                }
                //float
                if (!isDuplicate(px, py)) {
                    points.add(px);
                    points.add(py);
                    colorTest.add(new Color((float) Math.random() * 0.5f, (float) Math.random() * 0.5f, (float) Math.random() * 0.5f, 0.5f));
                    added = true;
                }
            }
            if (added) {
                calculateDelaunay();
            }
        }

        //Control + D -> Save to file
        if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Keys.D)) {
            renderToFile(!voronoiRender);
        }

        //copy paste!
        if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Keys.C)) {
            if (points.isEmpty()) return;
            //comma separated values
            stringBuilder.setLength(0);
            for (int i = 0; i < points.size; i += 2) {
                float px = points.get(i);
                float py = points.get(i + 1);
                stringBuilder.append(px).append(",").append(py);
                //last point
                if (i + 2 != points.size) {
                    stringBuilder.append(",");
                }
            }
            Gdx.app.getClipboard().setContents(stringBuilder.toString());
            Gdx.app.log(getClass().getSimpleName(), "copied vertices to clipboard: " + points.size);
        }
        if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Keys.V)) {
            if (!Gdx.app.getClipboard().hasContents()) return;
            stringBuilder.setLength(0);
            stringBuilder.append(Gdx.app.getClipboard().getContents());
            String[] csv = stringBuilder.toString().split(",");
            FloatArray newPoints = new FloatArray();
            try {
                for (String value : csv) {
                    float parsed = Float.parseFloat(value);
                    newPoints.add(parsed);
                }
            } catch (NumberFormatException ex) {
                Gdx.app.error(getClass().getSimpleName(),"Invalid input! Float could not parsed. This expects floats in XY pairs as CSV. Clipboard paste aborted.");
                return;
            }
            clear();
            points.addAll(newPoints);
            Gdx.app.log(getClass().getSimpleName(), "pasted vertices from clipboard: " + points.size);
            calculateDelaunay();
            regenColor();
        }

        //toggle drawings
        if (Gdx.input.isKeyJustPressed(Keys.F2)) {
            debugPointOrder = !debugPointOrder;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
            drawCircumcenter = !drawCircumcenter;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
            drawCircumcircle = !drawCircumcircle;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) {
            drawPoints = !drawPoints;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_4)) {
            drawMidpoints = !drawMidpoints;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_5)) {
            drawVoronoi = !drawVoronoi;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_6)) {
            drawDelaunay = !drawDelaunay;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_7)) {
            drawHull = !drawHull;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_8)) {
            drawCentroid = !drawCentroid;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_9)) {
            drawTriangleQuality = !drawTriangleQuality;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_0)) {
            drawTriangleInfo = !drawTriangleInfo;
        }
        if (Gdx.input.isKeyJustPressed(Keys.MINUS)) {
            drawCenteroidPointGraph = !drawCenteroidPointGraph;
        }
        if (Gdx.input.isKeyJustPressed(Keys.EQUALS)) {
            drawMidGraph = !drawMidGraph;
        }
        //todo: running out of numbers. need better controls
        if (Gdx.input.isKeyJustPressed(Keys.Q)) {
            voronoiRender = !voronoiRender;
        }
        if (Gdx.input.isKeyJustPressed(Keys.W)) {
            renderStyle = renderStyle.next();
        }
        if (Gdx.input.isKeyJustPressed(Keys.E)) {
            regenColor();
        }
        //todo: I am really just making things up now and need a new solution soon beyond simply qwertyuiop'ing my way down the keyboards...
        if (Gdx.input.isKeyJustPressed(Keys.T)) {
            drawInTriangle = !drawInTriangle;
        }
        if (Gdx.input.isKeyJustPressed(Keys.Y)) {
            drawInCenter = !drawInCenter;
        }
        if (Gdx.input.isKeyJustPressed(Keys.U)) {
            drawInRadius = !drawInRadius;
        }
        if (Gdx.input.isKeyJustPressed(Keys.I)) {
            drawOrtho = !drawOrtho;
        }
        if (Gdx.input.isKeyJustPressed(Keys.O)) {
            drawCentroidDelaunay = !drawCentroidDelaunay;
            //may have to call recalculate here:
            if (drawCentroidDelaunay) {
                calculateDelaunay();
            }
        }
    }

    private void regenColor() {
        colorTest.clear();
        for (int i = 0; i < points.size; i+= 2) {
            Color newColor = new Color();//todo: pool
            /*
            switch (pallete) {
                case rngColor:
                    newColor.set((float)Math.random() * 0.5f, (float)Math.random() * 0.5f, (float)Math.random() * 0.5f, 1);
                    break;
                case grayscale:
                    float v = (float) Math.random() * 0.5f;
                    newColor.set(v, v, v, 0.5f);
                    break;
            }*/
            //colorTest.add(new Color((float)Math.random() * 0.5f, (float)Math.random() * 0.5f, (float)Math.random() * 0.5f, 1));
            //black n white! or grayscale. is it gray or grey?
            float v = (float) Math.random() * 0.5f;
            newColor.set(v, v, v, 0.5f);
            colorTest.add(newColor);
        }
    }

    private boolean isDuplicate(float x1, float y1) {
        float tolerance = 0.1f;
        boolean duplicate = false;
        for (int i = 0; i < points.size && !duplicate; i += 2) {
            if (MathUtils.isEqual(x1, points.get(i), tolerance) && MathUtils.isEqual(y1, points.get(i+1), tolerance)) {
                duplicate = true;
            }
        }
        return duplicate;
    }

    /** Save render to PNG.
     * Note: transparency causes redraw! expensive!
     */
    private void renderToFile(boolean transparent) {
        if (points.size < 6) return;
        long startTime = System.currentTimeMillis();

        if (transparent) {
            //manually clear with clear color for transparent background
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            //and force re-render
            drawStuff();
        }

        //crop to hull bounds
        Rectangle bounds = hullPoly.getBoundingRectangle();
        int padding = 10;
        Pixmap cropImage = Pixmap.createFromFrameBuffer((int) bounds.x - padding, (int) bounds.y - padding, (int) bounds.getWidth() + padding * 2, (int) bounds.getHeight() + padding * 2);

        //fullscreen capture
        Pixmap fullscreenImage = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        String mode = (voronoiRender ? renderStyle.toString().toLowerCase() : "") + (transparent ? "_transparent" : "");
        String msaa = (((MyScreenAdapter)game.getScreen()).isMSAAEnabled() ? "_MSAA_" : "_");

        //save fullscreen framebuffer to file
        FileHandle fullscreenHandle = Gdx.files.local("assets/capture/" + Gdx.graphics.getFrameId() + mode + msaa + fullscreenImage.getWidth() + "x" + fullscreenImage.getHeight() + ".png");
        Gdx.app.log(getClass().getSimpleName(), "writing to: " + fullscreenHandle.path());
        PixmapIO.writePNG(fullscreenHandle, fullscreenImage, -1, true);

        //save cropped frambuffer to file
        FileHandle cropHandle = Gdx.files.local("assets/capture/" + Gdx.graphics.getFrameId() + mode + "_crop" + msaa + cropImage.getWidth() + "x" + cropImage.getHeight() + ".png");
        Gdx.app.log(getClass().getSimpleName(), "writing to: " + cropHandle.path());
        PixmapIO.writePNG(cropHandle, cropImage, -1, true);

        long end = System.currentTimeMillis() - startTime;
        Gdx.app.log(getClass().getSimpleName(), "finished: " + end);
    }

    private void renderBlendToFile() {
        if (points.size < 6) return;
        long startTime = System.currentTimeMillis();

        int padding = 10;
        Rectangle bounds = hullPoly.getBoundingRectangle(); //crop to hull bounds

        // ----- first pass render no MSAA -----
        ((MyScreenAdapter)game.getScreen()).disableMSAA();
        //manually clear with clear color for transparent background
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        //and force re-render
        drawStuff();
        //capture first pass frambuffer to file
        Pixmap rawRender = Pixmap.createFromFrameBuffer((int) bounds.x - padding, (int) bounds.y - padding, (int) bounds.getWidth() + padding * 2, (int) bounds.getHeight() + padding * 2);
        FileHandle rawFile = Gdx.files.local("assets/capture/" + Gdx.graphics.getFrameId() + "_RAW_" + rawRender.getWidth() + "x" + rawRender.getHeight() + ".png");
        Gdx.app.log(getClass().getSimpleName(), "writing to: " + rawFile.path());
        PixmapIO.writePNG(rawFile, rawRender, -1, true);

        // ----- second pass render with MSAA enabled -----
        ((MyScreenAdapter)game.getScreen()).enableMSAA();
        //manually clear with clear color for transparent background
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        //and force re-render
        drawStuff();
        Pixmap msaaRender = Pixmap.createFromFrameBuffer((int) bounds.x - padding, (int) bounds.y - padding, (int) bounds.getWidth() + padding * 2, (int) bounds.getHeight() + padding * 2);
        //capture msaa render
        FileHandle msaaFile = Gdx.files.local("assets/capture/" + Gdx.graphics.getFrameId() + "_MSAA_" + msaaRender.getWidth() + "x" + msaaRender.getHeight() + ".png");
        Gdx.app.log(getClass().getSimpleName(), "writing to: " + msaaFile.path());
        PixmapIO.writePNG(msaaFile, msaaRender, -1, true);


        //blend pixmaps
        msaaRender.setBlending(Pixmap.Blending.SourceOver);
        rawRender.setBlending(Pixmap.Blending.SourceOver);
        rawRender.drawPixmap(msaaRender, rawRender.getWidth(), rawRender.getHeight());
        //msaaRender.drawPixmap(rawRender, msaaRender.getWidth(), msaaRender.getHeight());
        //capture blended render
        FileHandle blendFile = Gdx.files.local("assets/capture/" + Gdx.graphics.getFrameId() + "_BLEND_" + rawRender.getWidth() + "x" + rawRender.getHeight() + ".png");
        Gdx.app.log(getClass().getSimpleName(), "writing to: " + blendFile.path());
        PixmapIO.writePNG(blendFile, rawRender, -1, true);

        long end = System.currentTimeMillis() - startTime;
        Gdx.app.log(getClass().getSimpleName(), "finished: " + end);
    }

    private void clear() {
        points.clear();
        dCells.clear();
        hull = null;
        colorTest.clear();
        centroidPoints.clear();
    }

}
