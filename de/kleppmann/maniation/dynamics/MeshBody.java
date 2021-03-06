package de.kleppmann.maniation.dynamics;

import java.util.List;

import de.kleppmann.maniation.geometry.AnimateMesh;
import de.kleppmann.maniation.geometry.Collision;
import de.kleppmann.maniation.geometry.MeshTriangle;
import de.kleppmann.maniation.geometry.MeshVertex;
import de.kleppmann.maniation.maths.Matrix33;
import de.kleppmann.maniation.maths.Quaternion;
import de.kleppmann.maniation.maths.Vector3D;

public class MeshBody extends RigidBody implements Collideable {
    
    private final World world;
    private final AnimateMesh mesh;
    private final MeshInfo info;
    private final Vector3D initialLocation;
    private final Quaternion initialOrientation;
    private final Vector3D nail1, nail2, nail3;
    //private final Quaternion toPrincipalAxes;
    //private final double radial, axial;
    private final Matrix33 inertia, invInertia;
    
    public MeshBody(World world, AnimateMesh mesh) {
        this.world = world;
        this.mesh = mesh;
        this.mesh.setDynamicBody(this);
        this.info = new MeshInfo(mesh, mesh.getTriangles());
        this.initialLocation = mesh.getCurrentLocation();
        this.initialOrientation = mesh.getCurrentOrientation();
        Vector3D restLocation = mesh.getRestLocation();
        Quaternion restOrientation = mesh.getRestOrientation();
        if (info.com != null) {
            info.com = restOrientation.getInverse().transform(info.com.subtract(restLocation));
        } else {
            info.com = new Vector3D();
        }
        nail1 = getInitialPosition();
        nail2 = restOrientation.transform(new Vector3D(1,0,0)).add(nail1);
        nail3 = restOrientation.transform(new Vector3D(0,1,0)).add(nail1);

        /*this.toPrincipalAxes = Quaternion.fromDirectionRoll(info.axis, new Vector3D(0,0,1), 0.0);
        this.radial = (info.length*info.length + 3.0*info.radius*info.radius)*info.mass/12.0;
        this.axial = 0.5*info.mass*info.radius*info.radius;*/
        
        // Hard-coded moment of inertia for a 3x3x3 cube
        double inert = info.mass*1.5;
        this.inertia    = new Matrix33(new Vector3D(    inert,     inert,     inert));
        this.invInertia = new Matrix33(new Vector3D(1.0/inert, 1.0/inert, 1.0/inert));
    }
    
    @Override
    protected double getMass() {
        return info.mass;
    }

    @Override
    protected Matrix33 getInertia(Body.State state) {
        // Hard-coded moment of inertia for a 3x3x3 cube
        return inertia;
        /*Matrix33 i = new Matrix33(new Vector3D(radial, radial, axial));
        Matrix33 rot = toPrincipalAxes.mult(state.getOrientation().getInverse()).toMatrix();
        return rot.transpose().mult33(i).mult33(rot);*/
    }

    @Override
    protected Matrix33 getInvInertia(Body.State state) {
        // Hard-coded moment of inertia for a 3x3x3 cube
        return invInertia;
        /*Matrix33 i = new Matrix33(new Vector3D(1.0/radial, 1.0/radial, 1.0/axial));
        Matrix33 rot = toPrincipalAxes.mult(state.getOrientation().getInverse()).toMatrix();
        return rot.transpose().mult33(i).mult33(rot);*/
    }

    @Override
    protected Vector3D getInitialPosition() {
        return initialLocation.add(initialOrientation.transform(info.com));
    }

    @Override
    protected Quaternion getInitialOrientation() {
        return initialOrientation;
    }

    @Override
    protected Vector3D getInitialLinearMomentum() {
        return new Vector3D();
    }

    @Override
    protected Vector3D getInitialAngularMomentum() {
        return new Vector3D();
    }
    
    public Vector3D getCentreOfMass() {
        return info.com;
    }
    
    @Override
    public void interaction(SimulationObject.State ownState, SimulationObject.State partnerState,
            InteractionList result, boolean allowReverse) {
        try {
            Body.State me = (Body.State) ownState;
            if (me.getOwner() != this) throw new IllegalArgumentException();
            // If interaction partner supports collision detection, check for collision.
            if (partnerState.getOwner() instanceof Collideable) {
                mesh.setDynamicState(me, info.com);
                Collideable partner = (Collideable) partnerState.getOwner();
                partner.collide((GeneralizedBody.State) partnerState, mesh, result);
            } else super.interaction(me, partnerState, result, allowReverse);
            // If this body is immobile, also nail it to the world
            if ((mesh.getSceneBody() != null) && !mesh.getSceneBody().isMobile() &&
                    (partnerState.getOwner() == world)) {
                result.addInteraction(new NailConstraint(world, me.getOwner(), new Vector3D(0,0,0), nail1));
                result.addInteraction(new NailConstraint(world, me.getOwner(), new Vector3D(1,0,0), nail2));
                result.addInteraction(new NailConstraint(world, me.getOwner(), new Vector3D(0,1,0), nail3));
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void collide(GeneralizedBody.State ownState, AnimateMesh partner, InteractionList result) {
        if (ownState.getOwner() != this) throw new IllegalArgumentException();
        mesh.setDynamicState(ownState, info.com);
        Collision collision = new Collision(mesh, partner);
        mesh.getCollisionVolume().intersect(partner.getCollisionVolume(), collision);
        collision.process(result);
    }
    
    @Override
    protected boolean allowPositionChange() {
        if (mesh.getSceneBody() == null) return false;
        return mesh.getSceneBody().isMobile();
    }

    public AnimateMesh getMesh() {
        return mesh;
    }
    
    public String toString() {
        return mesh.getSceneBody().getName();
    }

    
    private static class MeshInfo {

        double radius, length, mass, density;
        Vector3D axis, com;
        
        MeshInfo(AnimateMesh mesh, MeshTriangle triangles[]) {
            if (triangles.length == 0) {
                mass = 0.01; axis = new Vector3D(); com = null;
                return;
            }
            if (mesh.getSceneBody() != null) {
                de.kleppmann.maniation.scene.Vector vaxis = mesh.getSceneBody().getAxis();
                axis = new Vector3D(vaxis.getX(), vaxis.getY(), vaxis.getZ());
                axis = axis.normalize();
                density = mesh.getSceneBody().getMesh().getMaterial().getDensity();
            } else {
                axis = new Vector3D(1,0,0);
                density = 1.0;
            }
            List<Vector3D> points = new java.util.ArrayList<Vector3D>();
            // Approximate location of the centre of mass by averaging all vertex positions
            com = new Vector3D();
            for (MeshTriangle tri : triangles) {
                for (MeshVertex v : tri.getVertices()) {
                    Vector3D pos = v.getPosition();
                    points.add(pos);
                    com = com.add(pos);
                }
            }
            com = com.mult(1.0/points.size());
            // Radius is the maximum distance from the axis (straight line through CoM).
            // Length is the difference between the maximum and minimum positions
            // projected onto the axis.
            radius = 0.0;
            double lmin = 1e20, lmax = -1e20;
            for (Vector3D v : points) {
                Vector3D vr = v.subtract(com);
                double r = vr.cross(axis).magnitude();
                if (r > radius) radius = r;
                double l = vr.mult(axis);
                if (l > lmax) lmax = l;
                if (l < lmin) lmin = l;
            }
            length = lmax - lmin;
            // Calculate mass based on volume and density
            mass = density*Math.PI*radius*radius*length;
        }
    }
}
