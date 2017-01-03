package microtrafficsim.core.simulation.scenarios.impl;

import microtrafficsim.core.entities.vehicle.VisualizationVehicleEntity;
import microtrafficsim.core.logic.StreetGraph;
import microtrafficsim.core.simulation.configs.SimulationConfig;
import microtrafficsim.core.simulation.scenarios.Scenario;
import microtrafficsim.core.simulation.scenarios.containers.VehicleContainer;
import microtrafficsim.core.simulation.scenarios.containers.impl.ConcurrentVehicleContainer;
import microtrafficsim.core.simulation.utils.ODMatrix;
import microtrafficsim.core.simulation.utils.SparseODMatrix;
import microtrafficsim.core.simulation.utils.UnmodifiableODMatrix;

import java.util.function.Supplier;

/**
 * This class should only implement the basic stuff for children classes.
 *
 * @author Dominic Parga Cacheiro
 */
public abstract class BasicScenario implements Scenario {

    private final SimulationConfig config;
    private final StreetGraph graph;
    private final VehicleContainer vehicleContainer;
    private boolean isPrepared;
    protected ODMatrix odMatrix;

    /**
     * Default constructor
     *
     * @param config this config is used for internal settings and should be set already
     * @param graph used for route definitions etc.
     * @param vehicleContainer stores and manages vehicles running in this scenario
     */
    protected BasicScenario(SimulationConfig config,
                            StreetGraph graph,
                            VehicleContainer vehicleContainer) {
        this.config = config;
        this.graph = graph;
        this.vehicleContainer = vehicleContainer;

        this.odMatrix = new SparseODMatrix();
    }

    protected BasicScenario(SimulationConfig config,
                            StreetGraph graph) {
        this(config, graph, new ConcurrentVehicleContainer());
    }

    @Override
    public SimulationConfig getConfig() {
        return config;
    }

    @Override
    public StreetGraph getGraph() {
        return graph;
    }

    @Override
    public VehicleContainer getVehicleContainer() {
        return vehicleContainer;
    }

    @Override
    public void setPrepared(boolean isPrepared) {
        this.isPrepared = isPrepared;
    }

    @Override
    public boolean isPrepared() {
        return isPrepared;
    }

    @Override
    public void setODMatrix(ODMatrix odMatrix) {
        this.odMatrix = odMatrix;
    }

    @Override
    public UnmodifiableODMatrix getODMatrix() {
        return new UnmodifiableODMatrix(odMatrix);
    }
}