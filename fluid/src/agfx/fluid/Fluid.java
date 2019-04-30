package agfx.fluid;

public class Fluid {

    public final double molarMass;
    public final double restDensity;
    public final double restPressure;
    public final double viscosity;
    public final double speedOfSound;
    public final double cohesion;

    public Fluid(double kgPerMol, double kgPerM3, double pa, double paS, double mPerS, double sigma) {
        molarMass = kgPerMol;
        restDensity = kgPerM3;
        restPressure = pa;
        viscosity = paS;
        speedOfSound = mPerS;
        cohesion = sigma;
    }
}
