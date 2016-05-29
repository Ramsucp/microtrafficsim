package microtrafficsim.math;

public class MathUtils {

    public static double clamp(double val, double min, double max) {
        return val < min ? min : val > max ? max : val;
    }
}
