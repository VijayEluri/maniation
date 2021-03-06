package de.kleppmann.maniation.geometry;

import java.util.Map;
import java.util.Set;

import javax.media.j3d.Appearance;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;

import de.kleppmann.maniation.dynamics.GeneralizedBody;
import de.kleppmann.maniation.maths.Quaternion;
import de.kleppmann.maniation.maths.Vector3D;
import de.kleppmann.maniation.scene.Face;
import de.kleppmann.maniation.scene.Mesh;
import de.kleppmann.maniation.scene.Vertex;
import de.realityinabox.util.Pair;

public class AnimateMesh implements AnimateObject {

    de.kleppmann.maniation.scene.Body sceneBody;
    double[] coordinates;
    float[] normals;
    MeshTriangle[] triangles;
    Map<Vertex, MeshVertex> vertexMap;
    IndexedTriangleArray geometry = null;
    private Shape3D shape;
    private MyUpdater myUpdater = new MyUpdater();
    private CollisionVolume volume;
    private de.kleppmann.maniation.dynamics.Body dynamicBody;
    private de.kleppmann.maniation.dynamics.Body.State dynamicState;

    public AnimateMesh(de.kleppmann.maniation.scene.Body sceneBody) {
        this.sceneBody = sceneBody;
        // need to call setDynamicBody before object is operational!
    }
    
    public de.kleppmann.maniation.scene.Body getSceneBody() {
        return sceneBody;
    }
    
    public GeneralizedBody getDynamicBody() {
        return dynamicBody;
    }
    
    public void setDynamicBody(GeneralizedBody dynamicBody) {
        if (dynamicBody != null) {
            if (!(dynamicBody instanceof de.kleppmann.maniation.dynamics.Body))
                throw new IllegalArgumentException();
            this.dynamicBody = (de.kleppmann.maniation.dynamics.Body) dynamicBody;
        }
        buildArrays();
        buildJava3D();
        volume = new CollisionVolume(triangles);
    }
    
    public GeneralizedBody.State getDynamicState() {
        return dynamicState;
    }
    
    public void setDynamicState(GeneralizedBody.State state, Vector3D com) {
        if (!(state instanceof de.kleppmann.maniation.dynamics.Body.State))
            throw new IllegalArgumentException();
        this.dynamicState = (de.kleppmann.maniation.dynamics.Body.State) state;
        Vector3D location = dynamicState.getCoMPosition();
        location = location.subtract(dynamicState.getOrientation().transform(com));
        sceneBody.getLocation().setX(location.getComponent(0));
        sceneBody.getLocation().setY(location.getComponent(1));
        sceneBody.getLocation().setZ(location.getComponent(2));
        sceneBody.getOrientation().setW(dynamicState.getOrientation().getW());
        sceneBody.getOrientation().setX(dynamicState.getOrientation().getX());
        sceneBody.getOrientation().setY(dynamicState.getOrientation().getY());
        sceneBody.getOrientation().setZ(dynamicState.getOrientation().getZ());
        processStimulus();
    }
    
    public Vector3D getRestLocation() {
        return new Vector3D();
    }
    
    public Quaternion getRestOrientation() {
        return new Quaternion();
    }
    
    public Vector3D getCurrentLocation() {
        return new Vector3D(sceneBody.getLocation().getX(), 
                sceneBody.getLocation().getY(), sceneBody.getLocation().getZ());
    }
    
    public Quaternion getCurrentOrientation() {
        return new Quaternion(sceneBody.getOrientation().getW(), sceneBody.getOrientation().getX(), 
                sceneBody.getOrientation().getY(), sceneBody.getOrientation().getZ());
    }
    
    public CollisionVolume getCollisionVolume() {
        return volume;
    }
    
    public MeshTriangle[] getTriangles() {
        return triangles;
    }
    
    public boolean isInsideVolume(Vector3D point) {
        Vector3D outside = new Vector3D(volume.getBBox().minx - volume.size(),
                volume.getBBox().miny - volume.size(), volume.getBBox().minz - volume.size());
        return (volume.intersections(point, outside) % 2) == 1;
    }
    
    private void updateVertex(Vertex vert, int offset, Quaternion orient, Vector3D loc) {
        Vector3D pos = new Vector3D(vert.getPosition().getX(),
                vert.getPosition().getY(), vert.getPosition().getZ());
        Vector3D norm = new Vector3D(vert.getNormal().getX(),
                vert.getNormal().getY(), vert.getNormal().getZ());
        pos = orient.transform(pos).add(loc);
        norm = orient.transform(norm.normalize());
        pos.toDoubleArray(coordinates, offset);
        for (int i=0; i<3; i++) normals[offset+i] = (float) norm.getComponent(i);
    }
    
    private void buildArrays() {
        Mesh mesh = sceneBody.getMesh();
        coordinates = new double[3*mesh.getVertices().size()];
        normals = new float[3*mesh.getVertices().size()];
        int i = 0;
        Map<VertexPosition, Pair<MeshVertex,Set<Integer>>> uniqueVertices =
                new java.util.HashMap<VertexPosition, Pair<MeshVertex,Set<Integer>>>();
        vertexMap = new java.util.HashMap<Vertex, MeshVertex>();
        for (Vertex v : mesh.getVertices()) {
            coordinates[3*i+0] = v.getPosition().getX();
            coordinates[3*i+1] = v.getPosition().getY();
            coordinates[3*i+2] = v.getPosition().getZ();
            normals[3*i+0] = (float) v.getNormal().getX();
            normals[3*i+1] = (float) v.getNormal().getY();
            normals[3*i+2] = (float) v.getNormal().getZ();
            VertexPosition vp = new VertexPosition(v);
            Pair<MeshVertex,Set<Integer>> pair = uniqueVertices.get(vp);
            if (pair != null) {
                vertexMap.put(v, pair.getLeft());
                pair.getRight().add(i);
            } else {
                Set<Integer> indices = new java.util.HashSet<Integer>();
                indices.add(i);
                MeshVertex mv = new MeshVertex(coordinates, normals, indices);
                vertexMap.put(v, mv);
                uniqueVertices.put(vp, new Pair<MeshVertex,Set<Integer>>(mv, indices));
            }
            i++;
        }
        triangles = new MeshTriangle[mesh.getFaces().size()];
        i = 0;
        for (Face face : mesh.getFaces()) {
            triangles[i] = new MeshTriangle(dynamicBody,
                    vertexMap.get(face.getVertices().get(0)),
                    vertexMap.get(face.getVertices().get(1)),
                    vertexMap.get(face.getVertices().get(2)));
            i++;
        }
    }
    
    private void buildJava3D() {
        geometry = new IndexedTriangleArray(sceneBody.getMesh().getVertices().size(),
                IndexedTriangleArray.COORDINATES |
                IndexedTriangleArray.NORMALS |
                IndexedTriangleArray.BY_REFERENCE |
                IndexedTriangleArray.USE_COORD_INDEX_ONLY,
                3*sceneBody.getMesh().getFaces().size());
        geometry.setCapability(IndexedTriangleArray.ALLOW_REF_DATA_READ);
        geometry.setCapability(IndexedTriangleArray.ALLOW_REF_DATA_WRITE);
        geometry.setCapability(IndexedTriangleArray.ALLOW_COUNT_READ);
        geometry.setCoordRefDouble(coordinates);
        geometry.setNormalRefFloat(normals);
        for (int i=0; i<triangles.length; i++) {
            for (int j=0; j<3; j++)
                for (Integer k : triangles[i].vertices[j].getIndices())
                    geometry.setCoordinateIndex(3*i+j, k);
        }
        Appearance appearance = new Appearance();
        appearance.setMaterial(sceneBody.getMesh().getMaterial().getJava3D());
        shape = new Shape3D(geometry, appearance);
    }

    public Node getJava3D() {
        return shape;
    }

    public void processStimulus() {
        if (geometry != null) geometry.updateData(myUpdater); else myUpdater.updateData(null);
    }

    
    private class MyUpdater implements GeometryUpdater {
        public void updateData(Geometry geometry) {
            Quaternion orient = getCurrentOrientation(); Vector3D loc = getCurrentLocation();
            int coordIndex = 0;
            for (Vertex vert : sceneBody.getMesh().getVertices()) {
                updateVertex(vert, coordIndex, orient, loc);
                coordIndex += 3;
            }
            volume.updateBBox();
        }
    }
    
    
    private static class VertexPosition extends InexactPoint {
        private Vertex v;
        
        VertexPosition(Vertex v) {
            super(new Vector3D(v.getPosition().getX(), v.getPosition().getY(), v.getPosition().getZ()));
            this.v = v;
        }
        
        public Vertex getVertex() {
            return v;
        }
    }
}
