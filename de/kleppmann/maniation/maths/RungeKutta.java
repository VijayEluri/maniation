package de.kleppmann.maniation.maths;

public class RungeKutta implements ODESolver {
    
    private ODE ode;
    private double time = 0.0;
    private double h, hmin = 1e-10, eps = 1e-6;
    private Vector status;

    public RungeKutta(ODE ode, double timeStepGuess) {
        this.ode = ode;
        this.h = timeStepGuess;
        this.status = ode.getInitial();
    }
    
    public void setMinTimeStep(double minTimeStep) {
        hmin = minTimeStep;
    }
    
    public void setAccuracy(double requiredAccuracy) {
        eps = requiredAccuracy;
    }

    public void solve(double startTime, double finishTime) {
        while (time + hmin < finishTime) {
            if (time + h > finishTime) h = finishTime - time;
            Vector k1 = ode.derivative(time,         status                  ).mult(h);
            Vector k2 = ode.derivative(time + 0.5*h, status.add(k1.mult(0.5))).mult(h);
            Vector k3 = ode.derivative(time + 0.5*h, status.add(k2.mult(0.5))).mult(h);
            Vector k4 = ode.derivative(time +     h, status.add(k3          )).mult(h);
            status = status.add(k1.mult(1/6.0).add(
                                k2.mult(1/3.0).add(
                                k3.mult(1/3.0).add(
                                k4.mult(1/6.0)))));
            ode.timeStepCompleted(time, status);
            time += h;
        }
    }
}
