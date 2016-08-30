package microtrafficsim.core.vis.simulation;

import microtrafficsim.core.entities.vehicle.VisualizationVehicleEntity;
import microtrafficsim.core.simulation.Simulation;
import microtrafficsim.core.vis.Overlay;

import java.util.function.Supplier;


/**
 * @author Dominic Parga Cacheiro
 */
public interface VehicleOverlay extends Overlay {
    void setSimulation(Simulation simulation);
    Supplier<VisualizationVehicleEntity> getVehicleFactory();
}
