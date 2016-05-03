package microtrafficsim.core.simulation.scenarios;

import microtrafficsim.core.frameworks.shortestpath.ShortestPathAlgorithm;
import microtrafficsim.core.frameworks.vehicle.IVisualizationVehicle;
import microtrafficsim.core.logic.DirectedEdge;
import microtrafficsim.core.logic.Node;
import microtrafficsim.core.logic.Route;
import microtrafficsim.core.logic.StreetGraph;
import microtrafficsim.core.logic.vehicles.impl.Car;
import microtrafficsim.core.map.polygon.Polygon;
import microtrafficsim.core.simulation.controller.AbstractSimulation;
import microtrafficsim.core.simulation.controller.configs.SimulationConfig;
import microtrafficsim.math.Distribution;
import microtrafficsim.math.random.WheelOfFortune;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * <p>
 *     This class extends the {@link AbstractSimulation} by implementing a scenario using a set of start and end nodes
 *     for defining the vehicles routes. For standard, it uses a radius for a circle in the map's center containing
 *     start nodes (in percent!) and a circle to define all nodes outside of this circle as end nodes. By implementing
 *     {@link #createScoutFactory()} you can define the route calculation on your own by extending
 *     {@link ShortestPathAlgorithm}.
 * </p>
 * <p>
 *     Overriding {@link #prepareScenario()} and {@link #createAndAddVehicles()} lets define you routes
 * </p>
 *
 * @author Dominic Parga Cacheiro
 */
public abstract class AbstractStartEndScenario extends AbstractSimulation {

    public static class Config extends SimulationConfig {

        public int ageForPause = -1;
    }

    protected final Config config;
    private final Supplier<ShortestPathAlgorithm> scoutFactory;
    // used for multithreaded vehicle creation
    private int orderIdx;
    // used for creating vehicles and routes etc.
    private final HashMap<Polygon, ArrayList<Node>> startFields, endFields;
    private final WheelOfFortune startWheel, endWheel;
    private final Random random;
    // used for printing vehicle creation process
    private int lastPercentage;
    private final Integer percentageDelta; // > 0 !!!

    /**
     * Default constructor.
     *
     * @param config The used config file for this scenarios.
     * @param graph The streetgraph used for this scenarios.
     * @param vehicleFactory This creates vehicles.
     */
    public AbstractStartEndScenario(Config config,
                                    StreetGraph graph,
                                    Supplier<IVisualizationVehicle> vehicleFactory) {
        super(config,
                graph,
                vehicleFactory);
        this.config = config;
        scoutFactory = createScoutFactory();
        // used for creating vehicles and routes etc.
        startFields = new HashMap<>();
        endFields = new HashMap<>();
        startWheel = new WheelOfFortune(config.seed);
        endWheel = new WheelOfFortune(config.seed);
        random = new Random(config.seed);
        // used for printing vehicle creation process
        lastPercentage = 0;
        percentageDelta = 5; // > 0 !!!
    }

    /**
     * TODO
     * randomness depending on {@link WheelOfFortune} and {@link Random}
     *
     * @return
     */
    protected final Node getRandomStartNode() {
        ArrayList<Node> nodeField = startFields.get(startWheel.nextObject());
        return nodeField.get(random.nextInt(nodeField.size()));
    }

    /**
     * TODO
     * randomness depending on {@link WheelOfFortune} and {@link Random}
     *
     * @return
     */
    protected final Node getRandomEndNode() {
        ArrayList<Node> nodeField = endFields.get(endWheel.nextObject());
        return nodeField.get(random.nextInt(nodeField.size()));
    }

    /**
     * TODO<br>
     * <br>
     * random depends on {@link Random#nextInt(int)}
     *
     * @param field
     * @return
     */
    protected final Node getRandomStartNode(Polygon field) {
        ArrayList<Node> nodeField = startFields.get(field);
        return nodeField.get(random.nextInt(nodeField.size()));
    }

    /**
     * TODO<br>
     * <br>
     * random depends on {@link Random#nextInt(int)}
     *
     * @param polygon
     * @return
     */
    protected final Node getRandomEndNode(Polygon polygon) {
        ArrayList<Node> nodeField = endFields.get(polygon);
        return nodeField.get(random.nextInt(nodeField.size()));
    }

    /*
    |=====================================|
    | create and find routes for vehicles |
    |=====================================|
    */
    protected abstract Supplier<ShortestPathAlgorithm> createScoutFactory();

    /**
     * TODO
     *
     * @return Node[] containing exactly 2 nodes
     */
    protected abstract Node[] findRouteNodes();

    /*
    |=========================|
    | create and add vehicles |
    |=========================|
    */
    private void singleThreadedVehicleCreation() {

        // calculate routes and create vehicles
        int successfullyAdded = 0;
        while (successfullyAdded < config.maxVehicleCount) {
            Node[] bla = findRouteNodes();
            Node start = bla[0];
            Node end = bla[1];
            // create route
            @SuppressWarnings("unchecked")
            Route route = new Route(start, end,
                    (Queue<DirectedEdge>) scoutFactory.get().findShortestPath(start, end));
            // create and add vehicle
            // has permission to create vehicle
            if (!route.isEmpty()) {
                // add route to vehicle and vehicle to graph
                createAndAddVehicle(new Car(config.longIDGenerator, this, route));
                successfullyAdded++;
            }

            logProgress(successfullyAdded, config.maxVehicleCount);
        }
    }

    private void multiThreadedVehicleCreation() {

        ExecutorService pool = Executors.newFixedThreadPool(config.multiThreading.nThreads);
        ArrayList<Callable<Object>> todo = new ArrayList<>(config.multiThreading.nThreads);

        // deterministic/pseudo-random route + vehicle generation needs
        // variables for synchronization:
        orderIdx = 0;
        final Object lock_random = new Object();
        final ReentrantLock lock = new ReentrantLock(true);
        final boolean[] permission = new boolean[config.multiThreading.nThreads];
        Condition[] conditions = new Condition[config.multiThreading.nThreads];
        for (int i = 0; i < conditions.length; i++) {
            conditions[i] = lock.newCondition();
            permission[i] = i == 0;
        }
        // distribute vehicle generation uniformly over all threads
        Iterator<Integer> bucketCounts = Distribution.uniformly(config.maxVehicleCount, config.multiThreading.nThreads);
        while (bucketCounts.hasNext()) {
            int bucketCount = bucketCounts.next();

            todo.add(Executors.callable(() -> {
                // calculate routes and create vehicles
                int successfullyAdded = 0;
                while (successfullyAdded < bucketCount) {
                    Node start, end;
                    int idx;
                    synchronized (lock_random) {
                        idx = orderIdx % config.multiThreading.nThreads;
                        orderIdx++;
                        Node[] bla = findRouteNodes();
                        start = bla[0];
                        end = bla[1];
                    }
                    // create route
                    @SuppressWarnings("unchecked")
                    Route route = new Route(start, end,
                            (Queue<DirectedEdge>) scoutFactory.get().findShortestPath(start, end));
                    // create and add vehicle
                    lock.lock();
                    // wait for permission
                    if (!permission[idx]) {
                        try {
                            conditions[idx].await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // has permission to create vehicle
                    if (!route.isEmpty()) {
                        // add route to vehicle and vehicle to graph
                        createAndAddVehicle(new Car(config.longIDGenerator, this, route));
                        successfullyAdded++;
                    }
                    // let next thread finish its work
                    permission[idx] = false;
                    int nextIdx = (idx + 1) % config.multiThreading.nThreads;
                    if (lock.hasWaiters(conditions[nextIdx])) {
                        conditions[nextIdx].signal();
                    } else {
                        permission[nextIdx] = true;
                    }

                    lock.unlock();

                    // nice output
                    logProgress(successfullyAdded, bucketCount);
                }
            }));
        }
        try {
            pool.invokeAll(todo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logProgress(int finished, int total) {
        int percentage = (100 * finished) / total;
        synchronized (percentageDelta) {
            if (percentage - lastPercentage >= percentageDelta) {
                config.logger.info(percentage + "% vehicles created.");
                lastPercentage += percentageDelta;
            }
        }
    }

    /*
    |============================|
    | create and add node fields |
    |============================|
    */
    /**
     * TODO
     */
    protected abstract void createNodeFields();

    /**
     * TODO
     *
     * multiple calls doesn't matter
     *
     * @param polygon start field
     * @param probabilitySize given to {@link WheelOfFortune#addField(Object, int)}
     */
    protected final void addStartField(Polygon polygon, int probabilitySize) {
        startFields.put(polygon, new ArrayList<>());
        startWheel.addField(polygon, probabilitySize);
    }

    /**
     * TODO
     *
     * multiple calls doesn't matter
     *
     * @param polygon end field
     * @param probabilitySize given to {@link WheelOfFortune#addField(Object, int)}
     */
    protected final void addEndField(Polygon polygon, int probabilitySize) {
        endFields.put(polygon, new ArrayList<>());
        endWheel.addField(polygon, probabilitySize);
    }

    /*
    |================|
    | (i) Simulation |
    |================|
    */
    /**
     * This implementation calls {@link AbstractSimulation#cancel()}, if the simulation age reaches
     * {@link AbstractStartEndScenario.Config#ageForPause}.
     */
    @Override
    public void didRunOneStep() {
        super.didRunOneStep();
        if (getAge() == config.ageForPause) {
            cancel();
        }
    }

    /**
     * This implementation creates the start and end nodes depending on the radius percentages defined in the
     * {@link AbstractStartEndScenario.Config}.
     *
     * <p>
     *     This method is calling two subtasks:<br>
     *     &bull creating start and end fields<br>
     *     &bull filling data structures for nodes being in the start and end fields<br>
     *     You should extend {@link AbstractStartEndScenario#createNodeFields()} to define node fields for start/end
     *     using {@link AbstractStartEndScenario#addStartField(Polygon, int)}} and
     *     {@link AbstractStartEndScenario#addEndField(Polygon, int)}.
     * </p>
     */
    @Override
    protected final void prepareScenario() {

        createNodeFields();

        HashSet<Polygon> emptyStartFields = new HashSet<>(startFields.keySet());
        HashSet<Polygon> emptyEndFields = new HashSet<>(endFields.keySet());

        Iterator<Node> nodes = graph.getNodeIterator();
        while (nodes.hasNext()) {
            Node node = nodes.next();

            startFields.keySet().stream().filter(
                    polygon -> polygon.contains(node)
            ).forEach(
                    polygon -> {
                        emptyStartFields.remove(polygon);
                        startFields.get(polygon).add(node);
                    }
            );

            endFields.keySet().stream().filter(
                    polygon -> polygon.contains(node)
            ).forEach(
                    polygon -> {
                        emptyEndFields.remove(polygon);
                        endFields.get(polygon).add(node);
                    }
            );
        }

        if (emptyStartFields.size() > 0) {
            try {
                throw new Exception("At least one selected start field is empty!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (emptyEndFields.size() > 0) {
            try {
                throw new Exception("At least one selected end field is empty!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        emptyStartFields.forEach(polygon -> {
//            startFields.remove(polygon);
//            startWheel.remove(polygon);
//        });
//        emptyEndFields.forEach(polygon -> {
//            endFields.remove(polygon);
//            endWheel.remove(polygon);
//        });
    }

    @Override
    protected final void createAndAddVehicles() {

        if (startFields.size() <= 0 || endFields.size() <= 0) {
            if (startFields.size() <= 0)
                config.logger.info("You are using no or only empty start fields!");
            if (endFields.size() <= 0)
                config.logger.info("You are using no or only empty end fields!");
        } else {
            config.logger.info("CREATING VEHICLES started");
            long time = System.nanoTime();

            if (config.multiThreading.nThreads > 1) {
                multiThreadedVehicleCreation();
            } else {
                singleThreadedVehicleCreation();
            }

            config.logger.infoNanoseconds("CREATING VEHICLES finished after ", System.nanoTime() - time);
        }
    }
}