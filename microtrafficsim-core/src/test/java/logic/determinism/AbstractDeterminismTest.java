package logic.determinism;

import microtrafficsim.core.convenience.parser.DefaultParserConfig;
import microtrafficsim.core.logic.routes.Route;
import microtrafficsim.core.logic.streetgraph.Graph;
import microtrafficsim.core.logic.streets.DirectedEdge;
import microtrafficsim.core.logic.vehicles.machines.Vehicle;
import microtrafficsim.core.map.MapProperties;
import microtrafficsim.core.parser.OSMParser;
import microtrafficsim.core.simulation.builder.impl.VehicleScenarioBuilder;
import microtrafficsim.core.simulation.configs.SimulationConfig;
import microtrafficsim.core.simulation.core.Simulation;
import microtrafficsim.core.simulation.core.VehicleSimulation;
import microtrafficsim.core.simulation.scenarios.Scenario;
import microtrafficsim.math.MathUtils;
import microtrafficsim.math.random.distributions.impl.Random;
import microtrafficsim.utils.logging.EasyMarkableLogger;
import microtrafficsim.utils.logging.LoggingLevel;
import microtrafficsim.utils.resources.PackagedResource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import testhelper.ResourceClassLinks;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * <p>
 * Tests a given scenario for deterministic execution. Parsing and creating a new scenario several times are tested
 * for determinism as well as resetting the old graph and re-preparing the old scenario.
 *
 * <p>
 * The only difference between above mentioned tests is the number of simulation steps per simulation run. The
 * <b>parsing/creating-test</b> is executing the initial step and compares them once, the <b>refurbish-test</b>
 * executes multiple scenario steps and compares several states. The refurbish-test is described more detailed below.
 *
 * <p>
 * Implementation details, where <b>{@code r}</b>, <b>{@code s}</b>, <b>{@code c}</b> are defined by subclasses: <br>
 * The simulation runs <b>{@code r}</b> times in addition to the initial run (for reference). So a simulation is tested
 * for deterministic execution <b>{@code r}</b> times. <b>{@code s}</b> is the number of steps per simulation
 * run. So one simulation executes <b>{@code s}</b> steps. <b>{@code c}</b> is the number of state checks. Using
 * {@link MathUtils#createSigmoidSequence(int, int, int) MathUtils.createSigmoidSequence(1, s, c - 2)}, the checks
 * are concentrated in the beginning and the end of a simulation run. Concentration in the beginning saves runtime if
 * the test fails early. Concentration in the end is a compromise: The longer the simulation executes, the higher is
 * the test precision, but more vehicles are disappeared as well. <br>
 * A state check compares for a certain simulation step:<br>
 * &bull; for every vehicle: {@code (vehicle's id, vehicle's street, vehicle's cell position)} is identical<br>
 * &bull; correct number of vehicles
 *
 * @author Dominic Parga Cacheiro
 */
public abstract class AbstractDeterminismTest {
    private static Logger logger = new EasyMarkableLogger(AbstractDeterminismTest.class);

    /* (part of) tested parameters */
    private final SimulationConfig config;
    private Graph graph;
    private final Simulation simulation;
    private int expectedAge;

    /* memory */
    // simulation age -> (vehicle id -> vehicle stamp)
    private HashMap<Integer, HashMap<Long, VehicleStamp>> stamps;


    public AbstractDeterminismTest() {

        /* (part of) tested parameters */
        config      = createConfig();
        logger.info("config created with seed = " + config.seed);
        simulation  = new VehicleSimulation();
        expectedAge = 0;

        /* memory */
        stamps = new HashMap<>();
    }

    protected abstract int getChecks();
    protected abstract int getMaxStep();
    protected abstract int getSimulationRuns();

    protected SimulationConfig createConfig() {

        SimulationConfig config = new SimulationConfig();

        // general
        config.speedup = Integer.MAX_VALUE;
        config.seed    = new Random().getSeed();
        // crossing logic
        config.crossingLogic.drivingOnTheRight            = true;
        config.crossingLogic.edgePriorityEnabled          = true;
        config.crossingLogic.priorityToTheRightEnabled    = true;
        config.crossingLogic.friendlyStandingInJamEnabled = true;
        config.crossingLogic.onlyOneVehicleEnabled        = false;
        // vehicles
        config.maxVehicleCount = 4000;
        // multithreading
        config.multiThreading.nThreads = 42;

        return config;
    }

    protected Graph createGraph(SimulationConfig config) {
        Graph graph;
        try {
            File file = new PackagedResource(
                    AbstractDeterminismTest.class,
                    ResourceClassLinks.BACKNANG_MAP_PATH).asTemporaryFile();
            OSMParser parser = DefaultParserConfig.get(config).build();
            graph = parser.parse(file, new MapProperties(config.crossingLogic.drivingOnTheRight)).streetgraph;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        logger.debug("\n" + graph);

        return graph;
    }

    protected abstract Scenario createScenario(SimulationConfig config, Graph graph);

    protected Scenario prepareScenario(SimulationConfig config, Scenario scenario) {
        VehicleScenarioBuilder scenarioBuilder = new VehicleScenarioBuilder(config.seed);

        try {
            scenarioBuilder.prepare(scenario);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return scenario;
    }


    /*
    |===============|
    | testing utils |
    |===============|
    */
    private void storeStateFor(int simulationAge) {
        /* add stamps for given simulation age */
        HashMap<Long, VehicleStamp> vehicles = stamps.get(simulationAge);
        if (vehicles == null) {
            vehicles = new HashMap<>();
            stamps.put(simulationAge, vehicles);
        }

        getCurrentState(vehicles);
    }

    private void getCurrentState(HashMap<Long, VehicleStamp> stamps) {
        /* add stamp per vehicle */
        for (Vehicle vehicle : simulation.getScenario().getVehicleContainer()) {
            VehicleStamp stamp = new VehicleStamp(
                    vehicle.getId(),
                    vehicle.getLane(),
                    vehicle.getCellPosition());
            stamps.put(vehicle.getId(), stamp);
        }
    }

    private void simulate(int steps) {
        for (int i = 0; i < steps; i++)
            simulation.runOneStep();
        expectedAge += steps;
        assertAge();
    }

    private void compareStates(int simulationAge) {
        HashMap<Long, VehicleStamp> expected = stamps.get(simulationAge);
        HashMap<Long, VehicleStamp> actual = new HashMap<>();
        getCurrentState(actual);
        String msg = "There are too ";
        msg += expected.size() > actual.size() ? "less" : "many";
        msg += " vehicles.";
        assertEquals(msg, expected.size(), actual.size());

        int correctCount = 0;
        for (long id : expected.keySet()) {
            assertEquals(
                    "Unequal vehicle stamps; correct until now: " + correctCount,
                    expected.get(id),
                    actual.get(id));
            correctCount++;
        }
    }

    private void assertAge() {
        assertEquals("Wrong age", expectedAge, simulation.getAge());
    }

    private void assertVehiclesExist() {
        assertFalse("Scenario has no vehicles.", simulation.getScenario().getVehicleContainer().isEmpty());
    }

    private void assertRoutesAreNotEmpty() {
        boolean allRoutesAreEmpty = true;
        for (Vehicle vehicle : simulation.getScenario().getVehicleContainer()) {
            Route route = vehicle.getDriver().getRoute();
            allRoutesAreEmpty &= route.isEmpty();
        }
        assertFalse("All routes are empty", allRoutesAreEmpty);
    }


    /*
    |================|
    | test case impl |
    |================|
    */
    private void executeAndRememberFirstRun() {
        Iterator<Integer> simulationAges = MathUtils.createSigmoidSequence(1, getMaxStep(), getChecks() - 2);
        int currentCheck = 1;
        int lastSimulationAge = 0;
        while (simulationAges.hasNext()) {

            // get next sigmoid number
            int simulationAge = simulationAges.next();
            logger.info("Remember for check #" + currentCheck++ + " after " + simulationAge + " steps (= age).");
            // simulate delta steps between last step and next step
            simulate(simulationAge - lastSimulationAge);
            lastSimulationAge = simulationAge;

            // remember current state (which is the expected state later)
            storeStateFor(simulationAge);
        }
    }

    private void setupNewTest() {
        graph = createGraph(config);
        setupScenario(createScenario(config, graph));
    }

    private void setupNewSimulationRun() {
        graph.reset();
        setupScenario(simulation.getScenario());
    }

    private void setupScenario(Scenario scenario) {
        prepareScenario(config, scenario);
        simulation.setAndInitPreparedScenario(scenario);

        expectedAge = 0;
        assertAge();
        assertVehiclesExist();
        assertRoutesAreNotEmpty();
    }

    private void executeSimulationRun() {
        Iterator<Integer> simulationAges = MathUtils.createSigmoidSequence(1, getMaxStep(), getChecks() - 2);
        int currentCheck = 1;
        int lastSimulationAge = 0;
        while (simulationAges.hasNext()) {

            // get next sigmoid number
            int simulationAge = simulationAges.next();
            logger.info("Check #" + currentCheck++ + " after " + simulationAge + " steps (= age).");
            // simulate delta steps between last step and next step
            simulate(simulationAge - lastSimulationAge);
            lastSimulationAge = simulationAge;

            // compare current state with expected state
            compareStates(simulationAge);
        }
    }


    /*
    |============|
    | test cases |
    |============|
    */
    /**
     * @see AbstractDeterminismTest
     */
    @Test
    public void testDeterministicSimulation() throws Exception {
        setupNewTest();
        executeAndRememberFirstRun();
        for (int run = 0; run < getSimulationRuns(); run++) {
            setupNewSimulationRun();
            executeSimulationRun();
        }
    }

    /**
     * @see AbstractDeterminismTest
     */
    @Test
    public void testDeterministicParsing() {
        logger.info("Parse and remember vehicle states after parsing.");
        setupNewTest();
        simulate(1);
        storeStateFor(1);
        for (int run = 1; run <= getSimulationRuns(); run++) {
            logger.info("Checking determinism for simulation run #" + run + " after new parsing.");
            setupNewTest();
            simulate(1);
            compareStates(1);
        }
    }

    /*
    |=======|
    | utils |
    |=======|
    */
    @BeforeClass
    public static void buildSetup() {
        LoggingLevel.setEnabledGlobally(false, false, true, true, true);
//        LoggingLevel.setEnabledGlobally(false, false, false, false, false);
    }
}
