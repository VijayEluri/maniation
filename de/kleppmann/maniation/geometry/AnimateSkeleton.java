package de.kleppmann.maniation.geometry;

import javax.media.j3d.Appearance;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.LineArray;
import javax.media.j3d.Shape3D;

import de.kleppmann.maniation.maths.Quaternion;
import de.kleppmann.maniation.maths.Vector;
import de.kleppmann.maniation.scene.Bone;
import de.kleppmann.maniation.scene.Deform;
import de.kleppmann.maniation.scene.Skeleton;
import de.kleppmann.maniation.scene.Vertex;
import de.realityinabox.util.Pair;

public class AnimateSkeleton implements AnimateObject, GeometryUpdater {
    
    public static final boolean DRAW_SKELETON = false;
    
    private int frame = 0;
    private Skeleton skeleton;
    private double[] coordinates;
    private LineArray lines;
    private Shape3D shape;
    private java.util.Map<Bone,Pair<Vector,Quaternion>> skeletonRest, skeletonCurrent;
    private java.util.Map<Bone,Vector> boneEnds;

    public AnimateSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
        if (DRAW_SKELETON) buildJava3D();
    }

    public Shape3D getShape3D() {
        return shape;
    }
    
    private void buildJava3D() {
        coordinates = new double[12*skeleton.getBones().size()];
        updateSkeleton();
        lines = new LineArray(4*skeleton.getBones().size(),
                LineArray.COORDINATES | LineArray.COLOR_3 | LineArray.BY_REFERENCE);
        lines.setCapability(LineArray.ALLOW_REF_DATA_READ);
        lines.setCapability(LineArray.ALLOW_REF_DATA_WRITE);
        lines.setCapability(LineArray.ALLOW_COUNT_READ);
        lines.setCoordRefDouble(coordinates);
        float[] colours = new float[12*skeleton.getBones().size()];
        lines.setColorRefFloat(colours);
        for (int i=0; i<skeleton.getBones().size(); i++) {
            colours[12*i +  0] = 1.0f; colours[12*i +  1] = 1.0f; colours[12*i +  2] = 1.0f;
            colours[12*i +  3] = 1.0f; colours[12*i +  4] = 1.0f; colours[12*i +  5] = 1.0f;
            colours[12*i +  6] = 1.0f; colours[12*i +  7] = 0.0f; colours[12*i +  8] = 0.0f;
            colours[12*i +  9] = 1.0f; colours[12*i + 10] = 0.0f; colours[12*i + 11] = 0.0f;
        }
        shape = new Shape3D(lines, new Appearance());
    }

    private void updateSkeleton() {
        updateBones();
        for (int i=0; i<skeleton.getBones().size(); i++) {
            Bone b = skeleton.getBones().get(i);
            Pair<Vector,Quaternion> transform = skeletonCurrent.get(b);
            Vector base = transform.getLeft();
            Quaternion orient = transform.getRight();
            Vector end = orient.transform(boneEnds.get(b)).add(base);
            Vector xaxis = orient.transform(new Vector(0.1, 0, 0)).add(base);
            coordinates[12*i +  0] = base.getElement(0);
            coordinates[12*i +  1] = base.getElement(1);
            coordinates[12*i +  2] = base.getElement(2);
            coordinates[12*i +  3] = end.getElement(0);
            coordinates[12*i +  4] = end.getElement(1);
            coordinates[12*i +  5] = end.getElement(2);
            coordinates[12*i +  6] = base.getElement(0);
            coordinates[12*i +  7] = base.getElement(1);
            coordinates[12*i +  8] = base.getElement(2);
            coordinates[12*i +  9] = xaxis.getElement(0);
            coordinates[12*i + 10] = xaxis.getElement(1);
            coordinates[12*i + 11] = xaxis.getElement(2);
        }
    }
    
    public Vector worldToBone(Vector x, Bone bone) {
        Pair<Vector,Quaternion> transform = skeletonRest.get(bone);
        Vector boneBase = transform.getLeft();
        Quaternion boneOrient = transform.getRight();
        return boneOrient.getInverse().transform(x.subtract(boneBase));        
    }

    public Vector boneToWorld(Vector x, Bone bone) {
        Pair<Vector,Quaternion> transform = skeletonCurrent.get(bone);
        Vector boneBase = transform.getLeft();
        Quaternion boneOrient = transform.getRight();
        return boneOrient.transform(x).add(boneBase);
    }

    public Vector currentVertexPosition(Vertex vert) {
        Vector pos = new Vector(
                vert.getPosition().getX(),
                vert.getPosition().getY(),
                vert.getPosition().getZ());
        Vector deformed = new Vector(0.0, 0.0, 0.0);
        for (Deform deform : vert.getDeforms()) {
            Vector local = worldToBone(pos, deform.getBone());
            Vector world = boneToWorld(local, deform.getBone());
            deformed = deformed.add(world.mult(deform.getWeight()));
        }
        return deformed;
    }

    private void updateBones() {
        skeletonRest = new java.util.HashMap<Bone,Pair<Vector,Quaternion>>();
        skeletonCurrent = new java.util.HashMap<Bone,Pair<Vector,Quaternion>>();
        boneEnds = new java.util.HashMap<Bone,Vector>();
        for (Bone b : skeleton.getBones()) updateBone(b);
    }

    private void updateBone(Bone b) {
        if (skeletonRest.get(b) != null) return;
        Vector baseRest = new Vector(0, 0, 0);
        Quaternion orientRest = new Quaternion(1, 0, 0, 0);
        Vector baseCurrent = new Vector(0, 0, 0);
        Quaternion orientCurrent = new Quaternion(1, 0, 0, 0);
        if (b.getParentBone() != null) {
            updateBone(b.getParentBone());
            Pair<Vector,Quaternion> parentRest = skeletonRest.get(b.getParentBone());
            Pair<Vector,Quaternion> parentCurrent = skeletonCurrent.get(b.getParentBone());
            baseRest = parentRest.getLeft(); orientRest = parentRest.getRight();
            baseCurrent = parentCurrent.getLeft(); orientCurrent = parentCurrent.getRight();
        }
        Vector local = new Vector(b.getBase().getX(), b.getBase().getY(), b.getBase().getZ());
        baseRest = baseRest.add(orientRest.transform(local));
        baseCurrent = baseCurrent.add(orientCurrent.transform(local));
        orientRest = b.getOrientation().getValue().mult(orientRest);
        orientCurrent = b.getRotationAt(frame/30.0).getInverse().mult(b.getOrientation().getValue().mult(orientCurrent));
        skeletonRest.put(b, new Pair<Vector,Quaternion>(baseRest, orientRest));
        skeletonCurrent.put(b, new Pair<Vector,Quaternion>(baseCurrent, orientCurrent));
        boneEnds.put(b.getParentBone(), local);
        boneEnds.put(b, new Vector(0, 0.1, 0));
    }
    
    public void processStimulus() {
        lines.updateData(this);
    }

    public void updateData(Geometry geometry) {
        frame++;
        updateBones();
        if (DRAW_SKELETON) updateSkeleton();
    }
}