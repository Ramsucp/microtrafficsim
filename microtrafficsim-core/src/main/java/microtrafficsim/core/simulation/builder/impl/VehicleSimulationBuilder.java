package microtrafficsim.core.simulation.builder.impl;

import microtrafficsim.core.entities.vehicle.VehicleEntity;
import microtrafficsim.core.entities.vehicle.VisualizationVehicleEntity;
import microtrafficsim.core.logic.Node;
import microtrafficsim.core.logic.Route;
import microtrafficsim.core.logic.vehicles.AbstractVehicle;
import microtrafficsim.core.logic.vehicles.impl.Car;
import microtrafficsim.core.map.area.Area;
import microtrafficsim.core.simulation.builder.Builder;
import microtrafficsim.core.simulation.configs.SimulationConfig;
import microtrafficsim.core.simulation.scenarios.Scenario;
import microtrafficsim.core.simulation.scenarios.containers.VehicleContainer;
import microtrafficsim.core.simulation.utils.ODMatrix;
import microtrafficsim.core.simulation.utils.SparseODMatrix;
import microtrafficsim.interesting.progressable.ProgressListener;
import microtrafficsim.utils.StringUtils;
import microtrafficsim.utils.collections.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Supplier;

/**
 * This class is an implementation of {@link Builder} for {@link AbstractVehicle}s.
 *
 * @author Dominic Parga Cacheiro
 */
public class VehicleSimulationBuilder implements Builder {

    private Logger logger = LoggerFactory.getLogger(VehicleSimulationBuilder.class);

    // used for printing vehicle creation process
    private int lastPercentage;
    private final Integer percentageDelta; // > 0 !!!
    // used for vehicle creation
    private final Supplier<VisualizationVehicleEntity> vehicleFactory;

    /**
     * Default constructor
     *
     * @param vehicleFactory is used for creating the visualized component of a vehicle
     */
    public VehicleSimulationBuilder(Supplier<VisualizationVehicleEntity> vehicleFactory) {

        lastPercentage  = 0;
        percentageDelta = 5;

        this.vehicleFactory = vehicleFactory;
    }

    /**
     * Addition to superclass-doc: If the ODMatrix should be created using origin/destination fields, all nodes are
     * collected in one set, so there are no duplicates and the nodes are chosen distributed uniformly at random.
     */
    @Override
    public Scenario prepare(final Scenario scenario, final ProgressListener listener) {
        logger.info("PREPARING SCENARIO started");
        long time_preparation = System.nanoTime();

        // ---------- ---------- ---------- ---------- --
        // reset all
        // ---------- ---------- ---------- ---------- --
        logger.info("RESETTING SCENARIO started");
        scenario.setPrepared(false);
        scenario.getVehicleContainer().clearAll();
        scenario.getGraph().reset();
        logger.info("RESETTING SCENARIO finished");

        // ---------- ---------- ---------- ---------- --
        // check if odmatrix has to be built
        // ---------- ---------- ---------- ---------- --
        if (!scenario.isODMatrixBuilt()) {
            logger.info("BUILDING ODMatrix started");

            ArrayList<Node>
                    origins = new ArrayList<>(),
                    destinations = new ArrayList<>();

            // ---------- ---------- ---------- ---------- --
            // for each graph node, check its location relative to the origin/destination fields
            // ---------- ---------- ---------- ---------- --
            Iterator<Node> nodes = scenario.getGraph().getNodeIterator();
            while (nodes.hasNext()) {
                Node node = nodes.next();

                // for each node being in an origin field => add it
                for (Area area : scenario.getOriginFields())
                    if (area.contains(node)) {
                        origins.add(node);
                        break;
                    }

                // for each node being in a destination field => add it
                for (Area area : scenario.getDestinationFields())
                    if (area.contains(node)) {
                        destinations.add(node);
                        break;
                    }
            }

            // ---------- ---------- ---------- ---------- --
            // build matrix
            // ---------- ---------- ---------- ---------- --
            ODMatrix odmatrix = new SparseODMatrix();
            Random random = scenario.getConfig().rndGenGenerator.next();
            for (int i = 0; i < scenario.getConfig().maxVehicleCount; i++) {
                int rdmOrig = random.nextInt(origins.size());
                int rdmDest = random.nextInt(destinations.size());
                odmatrix.inc(origins.get(rdmOrig), destinations.get(rdmDest));
            }

            // ---------- ---------- ---------- ---------- --
            // finish creating ODMatrix
            // ---------- ---------- ---------- ---------- --
            scenario.setODMatrix(odmatrix);
            scenario.setODMatrixBuilt(true);
            logger.info("BUILDING ODMatrix finished");
        }

        // ---------- ---------- ---------- ---------- --
        // create vehicle routes
        // ---------- ---------- ---------- ---------- --
        ODMatrix odmatrix = scenario.getODMatrix();
        logger.info("CREATING VEHICLES started");
        long time_routes = System.nanoTime();

        if (scenario.getConfig().multiThreading.nThreads > 1)
            multiThreadedVehicleCreation(scenario, listener);
        else
            singleThreadedVehicleCreation(scenario, listener);

        time_routes = System.nanoTime() - time_routes;
        logger.info(StringUtils.buildTimeString(
                "CREATING VEHICLES finished after ",
                time_routes,
                "ns"
        ).toString());

        // ---------- ---------- ---------- ---------- --
        // finish building scenario
        // ---------- ---------- ---------- ---------- --
        scenario.setPrepared(true);
        time_preparation = System.nanoTime() - time_preparation;
        logger.info(StringUtils.buildTimeString(
                "PREPARING SCENARIOS finished after ",
                time_preparation,
                "ns"
        ).toString());
        return scenario;
    }

    private void multiThreadedVehicleCreation(final Scenario scenario, final ProgressListener listener) {
        // TODO
    }

    private void singleThreadedVehicleCreation(final Scenario scenario, final ProgressListener listener) {

        lastPercentage = 0;

        int vehicleCount = 0;
        for (Triple<Node, Node, Integer> triple : scenario.getODMatrix()) {
            Node start = triple.obj0;
            Node end = triple.obj1;
            int routeCount = triple.obj2;

            for (int i = 0; i < routeCount; i++) {
                Route<Node> route = new Route<>(start, end);
                scenario.getScoutFactory().get().findShortestPath(start, end, route);
                createAndAddVehicle(scenario, route);
                logProgress(vehicleCount, scenario.getConfig().maxVehicleCount, listener);
            }
        }
    }

    private void createAndAddVehicle(Scenario scenario, Route<Node> route) {

        // init stuff
        SimulationConfig config = scenario.getConfig();
        VehicleContainer vehicleContainer = scenario.getVehicleContainer();

        // create vehicle components
        AbstractVehicle vehicle = new Car(config, vehicleContainer, route);
        VisualizationVehicleEntity visCar;
        synchronized (vehicleFactory) {
            visCar = vehicleFactory.get();
        }

        // create vehicle entity
        VehicleEntity entity = new VehicleEntity(vehicle, visCar);
        vehicle.setEntity(entity);
        visCar.setEntity(entity);

        // add to graph
        if (vehicle.registerInGraph())
            vehicleContainer.addVehicle(vehicle);
    }

    private void logProgress(int finished, int total, ProgressListener listener) {
        int percentage = (100 * finished) / total;
        synchronized (percentageDelta) {
            if (percentage - lastPercentage >= percentageDelta) {
                logger.info(percentage + "% vehicles created.");
                if (listener != null) listener.didProgress(percentage);
                lastPercentage += percentageDelta;
            }
        }
    }
}
