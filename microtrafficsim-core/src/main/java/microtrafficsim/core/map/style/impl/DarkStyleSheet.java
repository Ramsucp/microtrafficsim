package microtrafficsim.core.map.style.impl;

import microtrafficsim.core.logic.vehicles.machines.Vehicle;
import microtrafficsim.core.map.style.BasicStyleSheet;
import microtrafficsim.core.map.style.VehicleColorSchemes;
import microtrafficsim.core.vis.opengl.utils.Color;
import microtrafficsim.math.MathUtils;
import microtrafficsim.utils.logging.EasyMarkableLogger;


/**
 * A dark style-sheet for the MapViewer. It uses the colors defined in {@link LightStyleSheet} inverted.
 *
 * @author Dominic Parga Cacheiro, Maximilian Luz
 */
public class DarkStyleSheet extends BasicStyleSheet {
    private static final EasyMarkableLogger logger = new EasyMarkableLogger(DarkStyleSheet.class);


    @Override
    public Color getBackgroundColor() {
        return Color.inverseFromRGB(0xFFFFFF);
    }

    @Override
    protected boolean isStreetInlineActive(String streetType, int zoom) {
        switch (streetType) {
            case "motorway":
            case "trunk":
            case "primary":
            case "secondary":
            case "tertiary":
            case "unclassified":
            case "residential":
            case "living_street":
                return zoom >= 16;
            case "road":
                return zoom >= 17;
            default: // should be never reached
                logger.info("It is not defined whether " + streetType + " has an active inline.");
                return false;
        }
    }

    @Override
    protected Color getStreetOutlineColor(String streetType) {
        switch (streetType) {
            case "motorway":
                return Color.inverseFromRGB(0xFF6F69);
            case "trunk":
                return Color.inverseFromRGB(0x659118);
            case "primary":
                return Color.inverseFromRGB(0xDB9E36);
            case "secondary":
                return Color.inverseFromRGB(0x105B63);
            case "tertiary":
                return Color.inverseFrom(177, 98, 134);
            case "unclassified":
                return Color.inverseFrom(104, 157, 106);
            case "residential":
                return Color.inverseFrom(124, 111, 100);
            case "road":
                return Color.inverseFrom(146, 131, 116);
            case "living_street":
                return Color.inverseFrom(146, 131, 116);
            default: // should be never reached
                logger.info("The outline color of " + streetType + " is not defined.");
                return getBackgroundColor();
        }
    }

    @Override
    protected Color getStreetInlineColor(String streetType) {
        return Color.inverseFromRGB(0xFDFDFD);
    }

    @Override
    protected Color getStreetCenterLineColor(String streetType) {
        return getStreetOutlineColor(streetType);
    }

    @Override
    protected Color getStreetLaneLineColor(String streetType) {
        return getStreetOutlineColor(streetType);
    }


    @Override
    public Color getColor(Vehicle vehicle) {
        Color[] colors = VehicleColorSchemes.RED_TO_GREEN;
        int v = MathUtils.clamp(vehicle.getVelocity(), 0, colors.length - 1);
        return colors[v];
    }
}