package de.kleppmann.maniation.dynamics;

import java.util.Map;
import de.kleppmann.maniation.maths.Matrix;
import de.kleppmann.maniation.maths.Vector;

public interface Constraint extends Interaction {
    void setStateMapping(Map<GeneralizedBody, GeneralizedBody.State> states);
    Vector getPenalty();
    Vector getPenaltyDot();
    Map<GeneralizedBody, Matrix> getJacobian();
    Map<GeneralizedBody, Matrix> getJacobianDot();
    int getDimension();
}
