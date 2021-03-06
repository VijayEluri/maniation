package de.kleppmann.maniation.dynamics;

import java.util.List;
import java.util.Map;

import de.kleppmann.maniation.maths.Matrix;
import de.kleppmann.maniation.maths.MatrixImpl;
import de.kleppmann.maniation.maths.Vector;
import de.kleppmann.maniation.maths.Vector3D;
import de.kleppmann.maniation.maths.VectorImpl;

public class VertexFaceCollision implements InequalityConstraint {
    
    private final Body vertexBody, faceBody;
    private Body.State vertexBodyState, faceBodyState;
    private final Vector3D vertexWorld, facePoint, faceNormal;
    private Vector3D vertexLocal;
    private final double elasticity;
    private double a1, a2, a3, b1, b2, b3, ad1, ad2, ad3, bd1, bd2, bd3, w1, w2, w3, p1, p2, p3,
        t1, t2, t3, n1, n2, n3;
    private Map<GeneralizedBody, Matrix> jacMap, jacDotMap;

    // All vectors are in world coordinates!
    public VertexFaceCollision(Body vertexBody, Vector3D vertex,
            Body faceBody, Vector3D facePoint, Vector3D faceNormal, double elasticity) {
        this.vertexBody = vertexBody;
        this.vertexWorld = vertex;
        this.faceBody = faceBody;
        this.facePoint = facePoint;
        this.faceNormal = faceNormal.normalize();
        this.elasticity = elasticity;
    }
    
    public void setStateMapping(Map<GeneralizedBody, GeneralizedBody.State> states) {
        jacMap = jacDotMap = null;
        try {
            vertexBodyState = (Body.State) states.get(vertexBody);
            faceBodyState   = (Body.State) states.get(faceBody);
        } catch (ClassCastException e) {
            throw new IllegalStateException(e);
        }
        vertexLocal = vertexWorld.subtract(vertexBodyState.getCoMPosition());
        a1 = faceBodyState.getCoMPosition().getComponent(0);
        a2 = faceBodyState.getCoMPosition().getComponent(1);
        a3 = faceBodyState.getCoMPosition().getComponent(2);
        b1 = vertexBodyState.getCoMPosition().getComponent(0);
        b2 = vertexBodyState.getCoMPosition().getComponent(1);
        b3 = vertexBodyState.getCoMPosition().getComponent(2);
        ad1 = faceBodyState.getCoMVelocity().getComponent(0);
        ad2 = faceBodyState.getCoMVelocity().getComponent(1);
        ad3 = faceBodyState.getCoMVelocity().getComponent(2);
        bd1 = vertexBodyState.getCoMVelocity().getComponent(0);
        bd2 = vertexBodyState.getCoMVelocity().getComponent(1);
        bd3 = vertexBodyState.getCoMVelocity().getComponent(2);
        w1 = faceBodyState.getAngularVelocity().getComponent(0);
        w2 = faceBodyState.getAngularVelocity().getComponent(1);
        w3 = faceBodyState.getAngularVelocity().getComponent(2);
        p1 = vertexBodyState.getAngularVelocity().getComponent(0);
        p2 = vertexBodyState.getAngularVelocity().getComponent(1);
        p3 = vertexBodyState.getAngularVelocity().getComponent(2);
        t1 = vertexLocal.getComponent(0);
        t2 = vertexLocal.getComponent(1);
        t3 = vertexLocal.getComponent(2);
        n1 = faceNormal.getComponent(0);
        n2 = faceNormal.getComponent(1);
        n3 = faceNormal.getComponent(2);
    }

    public Vector getPenalty() {
        double[] v = {vertexWorld.subtract(facePoint).mult(faceNormal)};
        return new VectorImpl(v);
    }

    public Vector getPenaltyDot() {
        double[] v = {n1*(bd1 - ad1 + p2*t3 - p3*t2) + n2*(bd2 - ad2 + p3*t1 - p1*t3) +
                n3*(bd3 - ad3 + p1*t2 - p2*t1) + (a1 - b1 - t1)*(n2*w3 - n3*w2) +
                (a2 - b2 - t2)*(n3*w1 - n1*w3) + (a3 - b3 - t3)*(n1*w2 - n2*w1) };
        return new VectorImpl(v);
    }

    public Map<GeneralizedBody, Matrix> getJacobian() {
        if (jacMap != null) return jacMap;
        double x1 = a1 - b1 - t1, x2 = a2 - b2 - t2, x3 = a3 - b3 - t3;
        double[][] jf = {{ -n1, -n2, -n3, x2*n3 - x3*n2, x3*n1 - x1*n3, x1*n2 - x2*n1 }};
        double[][] jv = {{  n1,  n2,  n3, t2*n3 - t3*n2, t3*n1 - t1*n3, t1*n2 - t2*n1 }};
        jacMap = new java.util.HashMap<GeneralizedBody, Matrix>();
        jacMap.put(faceBody, new MatrixImpl(jf));
        jacMap.put(vertexBody, new MatrixImpl(jv));
        return jacMap;
    }

    public Map<GeneralizedBody, Matrix> getJacobianDot() {
        if (jacDotMap != null) return jacDotMap;
        double t6 = ad2-bd2-p3*t1+p1*t3;
        double t11 = ad3-bd3-p1*t2+p2*t1;
        double t14 = a2-b2-t2;
        double t17 = w1*n2-w2*n1;
        double t19 = a3-b3-t3;
        double t22 = -w3*n1+w1*n3;
        double t26 = ad1-bd1-p2*t3+p3*t2;
        double t31 = a1-b1-t1;
        double t36 = w2*n3-w3*n2;
        double[][] jf = {{ 0, 0, 0,
            2.0*t6*n3-2.0*t11*n2+t14*t17+t19*t22,
            -2.0*t26*n3+2.0*t11*n1-t31*t17+t19*t36,
            2.0*t26*n2-2.0*t6*n1-t31*t22-t14*t36 }};
        double t7 = p1*t2-p2*t1;
        double t10 = -p3*t1+p1*t3;
        double t16 = p2*t3-p3*t2;
        double[][] jv = {{ 0, 0, 0, -t7*n2-n3*t10, t7*n1-n3*t16, n1*t10+n2*t16 }};
        jacDotMap = new java.util.HashMap<GeneralizedBody, Matrix>();
        jacDotMap.put(faceBody, new MatrixImpl(jf));
        jacDotMap.put(vertexBody, new MatrixImpl(jv));
        return jacDotMap;
    }

    public Map<Body, Vector3D> setToZero() {
        double value = 0.5*getPenalty().getComponent(0);
        Vector3D shift = faceNormal.mult(value);
        Map<Body, Vector3D> result = new java.util.HashMap<Body, Vector3D>();
        result.put(vertexBody, vertexBodyState.getCoMPosition().subtract(shift));
        result.put(faceBody, faceBodyState.getCoMPosition().add(shift));
        return result;
    }

    public boolean isInequality() {
        return true;
    }

    public int getDimension() {
        return 1;
    }

    public List<SimulationObject> getObjects() {
        List<SimulationObject> result = new java.util.ArrayList<SimulationObject>();
        result.add(faceBody);
        result.add(vertexBody);
        return result;
    }

    public double getElasticity() {
        return elasticity;
    }
}
