package microtrafficsim.core.simulation.scenarios.impl;

import microtrafficsim.core.logic.nodes.Node;
import microtrafficsim.core.logic.streetgraph.Graph;
import microtrafficsim.core.logic.streets.DirectedEdge;
import microtrafficsim.core.shortestpath.ShortestPathAlgorithm;
import microtrafficsim.core.shortestpath.astar.BidirectionalAStars;
import microtrafficsim.core.simulation.builder.ScenarioBuilder;
import microtrafficsim.core.simulation.configs.SimulationConfig;
import microtrafficsim.core.simulation.core.Simulation;
import microtrafficsim.core.simulation.scenarios.Scenario;
import microtrafficsim.core.simulation.scenarios.containers.VehicleContainer;
import microtrafficsim.core.simulation.scenarios.containers.impl.ConcurrentVehicleContainer;
import microtrafficsim.core.simulation.utils.ODMatrix;
import microtrafficsim.core.simulation.utils.RouteContainer;
import microtrafficsim.core.simulation.utils.SparseODMatrix;
import microtrafficsim.core.simulation.utils.UnmodifiableODMatrix;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * This scenario defines different scenarios in a queue, which can be executed after each other. The scenarios are
 * getting prepared/calculated on the fly, so this class is made only for small scenarios due to runtime.
 *
 * @author Dominic Parga Cacheiro
 */
public abstract class OldQueueScenarioSmall implements Scenario {

    /* general */
    private final SimulationConfig config;
    private final Graph graph;
    private final VehicleContainer vehicleContainer;
    private ShortestPathAlgorithm<Node, DirectedEdge> scout;
    private boolean isPrepared;

    /* scenario definition */
    private ArrayList<RouteContainer> routes;
    private int curIdx;
    private boolean isLooping;

    /* scenario building */
    private ScenarioBuilder scenarioBuilder;

    /**
     * Default constructor. After calling super(...) you should call {@link #setScenarioBuilder(ScenarioBuilder)}
     *
     * @param config this config is used for internal settings and should be set already
     * @param graph used for route definitions etc.
     * @param vehicleContainer stores and manages vehicles running in this scenario
     */
    protected OldQueueScenarioSmall(SimulationConfig config, Graph graph, VehicleContainer vehicleContainer) {
        /* general */
        this.config           = config;
        this.graph            = graph;
        this.vehicleContainer = vehicleContainer;
        scout                 = BidirectionalAStars.shortestPathAStar(config.metersPerCell);

        /* scenario definition */
        routes = new ArrayList<>();
        curIdx             = -1;
        isLooping          = false;
    }

    /**
     * Just calls {@code QueueScenario(config, graph, new ConcurrentVehicleContainer())}.
     *
     * @see ConcurrentVehicleContainer
     * @see OldQueueScenarioSmall#OldQueueScenarioSmall(SimulationConfig, Graph, VehicleContainer)
     */
    protected OldQueueScenarioSmall(SimulationConfig config, Graph graph) {
        this(config, graph, new ConcurrentVehicleContainer());
    }

    protected void setScenarioBuilder(ScenarioBuilder scenarioBuilder) {
        this.scenarioBuilder = scenarioBuilder;
    }

    public static SimulationConfig setupConfig(SimulationConfig config) {

        config.metersPerCell           = 7.5f;
        config.seed                    = 1455374755807L;
        config.multiThreading.nThreads = 1;

        config.speedup                                 = 5;
        config.crossingLogic.drivingOnTheRight         = true;
        config.crossingLogic.edgePriorityEnabled       = true;
        config.crossingLogic.priorityToTheRightEnabled = true;
        config.crossingLogic.onlyOneVehicleEnabled     = false;

        return config;
    }

    public void setLooping(boolean isLooping) {
        this.isLooping = isLooping;
    }

    public boolean isLooping() {
        return isLooping;
    }

    /*
    |==============|
    | matrix setup |
    |==============|
    */
    public final void addSubScenario(RouteContainer routes) {
        this.routes.add(routes);
    }

    public final void prepare() {
        curIdx = curIdx + 1;
        if (!isLooping && curIdx == routes.size()) {
            curIdx = -1;
            return;
        }
        curIdx %= routes.size();

        try {
            scenarioBuilder.prepare(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Peeks the first spawn-delay-matrix of the queue.
     */
    public final UnmodifiableODMatrix getSpawnDelayMatrix() {
        return new UnmodifiableODMatrix(spawnDelayMatrices.get(curIdx));
    }

    /*
    |==================|
    | (i) StepListener |
    |==================|
    */
    @Override
    public void didOneStep(Simulation simulation) {
        if (getVehicleContainer().getVehicleCount() == 0) {
            boolean isPaused = simulation.isPaused();
            simulation.cancel();
            setPrepared(false);
            prepare();
            if (isPrepared()) {
                simulation.setAndInitPreparedScenario(this);

                if (!isPaused)
                    simulation.run();
            }
        }
    }

    /*
    |==============|
    | (i) Scenario |
    |==============|
    */
    @Override
    public final SimulationConfig getConfig() {
        return config;
    }

    @Override
    public final Graph getGraph() {
        return graph;
    }

    @Override
    public final VehicleContainer getVehicleContainer() {
        return vehicleContainer;
    }

    @Override
    public final void setPrepared(boolean isPrepared) {
        this.isPrepared = isPrepared;
    }

    @Override
    public final boolean isPrepared() {
        return isPrepared;
    }

    /**
     * @return Peeks the first origin-destination-matrix of the queue.
     */
    @Override
    public final UnmodifiableODMatrix getODMatrix() {
        return new UnmodifiableODMatrix(routes.get(curIdx));
    }

    @Override
    public final Supplier<ShortestPathAlgorithm<Node, DirectedEdge>> getScoutFactory() {
        return () -> scout;
    }
}