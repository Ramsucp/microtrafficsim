package microtrafficsim.core.simulation.controller;

import microtrafficsim.core.frameworks.vehicle.ILogicVehicle;
import microtrafficsim.core.frameworks.vehicle.IVisualizationVehicle;
import microtrafficsim.core.frameworks.vehicle.VehicleEntity;
import microtrafficsim.core.logic.StreetGraph;
import microtrafficsim.core.logic.vehicles.AbstractVehicle;
import microtrafficsim.core.simulation.controller.configs.SimulationConfig;
import microtrafficsim.core.simulation.controller.manager.SimulationManager;
import microtrafficsim.core.simulation.controller.manager.VehicleManager;
import microtrafficsim.core.simulation.controller.manager.impl.MultiThreadedSimulationManager;
import microtrafficsim.core.simulation.controller.manager.impl.MultiThreadedVehicleManager;
import microtrafficsim.core.simulation.controller.manager.impl.SingleThreadedSimulationManager;
import microtrafficsim.core.simulation.controller.manager.impl.SingleThreadedVehicleManager;
import microtrafficsim.core.vis.opengl.utils.Color;
import microtrafficsim.core.vis.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

/**
 * <p>
 *     This class manages the simulation. It serves methods for starting and pausing the simulation, but you have to
 *     use it by extending it (class name: e.g. scenarios). The extension should include a static class extending
 *     {@link SimulationConfig}. In this config class, you can also set the number of threads. The
 *     {@link AbstractSimulation} handles alone whether the simulation steps can be executed sequentially or parallel.
 * </p>
 * <p>
 *     Logging can be disabled by setting {@link SimulationConfig#logger#enabled}.
 * </p>
 *
 * @author Jan-Oliver Schmidt, Dominic Parga Cacheiro
 */
public abstract class AbstractSimulation implements Simulation {

	protected final SimulationConfig config;
	protected final StreetGraph graph;
	// simulation steps
	private boolean prepared;
	private boolean paused;
	private Timer timer;
	private int age;
	// manager
	private final VehicleManager vehicleManager;
    private final SimulationManager simManager;
    // logging
    private long time;

	/**
	 * Default constructor.
	 */
	public AbstractSimulation(SimulationConfig config,
							  StreetGraph graph,
							  Supplier<IVisualizationVehicle> vehicleFactory) {
		this.config = config;
		this.graph = graph;
		// simulation
		prepared = false;
		paused = true;
		timer = new Timer();
		age = 0;
		if (this.config.multiThreading.nThreads > 1) {
            vehicleManager = new MultiThreadedVehicleManager(vehicleFactory);
            simManager = new MultiThreadedSimulationManager(config);
        } else {
            vehicleManager = new SingleThreadedVehicleManager(vehicleFactory);
            simManager = new SingleThreadedSimulationManager();
        }
	}

	/*
	|===========|
	| prepare() |
	|===========|
	*/
    /**
	 * This method should be called before the simulation starts. E.g. it can be
	 * used to set start nodes, that are used in the {@link AbstractSimulation}.
	 * {@link #createAndAddVehicles()}.
	 */
	protected abstract void prepareScenario();

	/**
	 * This method should fill not spawned vehicles using {@link Simulation}.{@link #addVehicle(AbstractVehicle)} and
	 * {@link AbstractSimulation}.{@link #getSpawnedVehicles()}. The spawning and other work
	 * will be done automatically.
	 */
	protected abstract void createAndAddVehicles();

    /**
     * This method could be called to add the given vehicle to the simulation. For visualization, this method creates an
     * instance of {@link IVisualizationVehicle} and connects it to the given vehicle using a {@link VehicleEntity}.
     *
     * @param vehicle An instance of {@link AbstractVehicle}
     */
    protected final void createAndAddVehicle(AbstractVehicle vehicle) {

        IVisualizationVehicle visCar = createVisVehicle();
        VehicleEntity entity = new VehicleEntity(config, vehicle, visCar);
        vehicle.setEntity(entity);
        visCar.setEntity(entity);
        addVehicle(vehicle);
    }

    /**
     * This method could be called to add the given vehicle to the simulation. For visualization, this method creates an
     * instance of {@link IVisualizationVehicle} and connects it to the given vehicle using a {@link VehicleEntity}.
     *
     * @param vehicle An instance of {@link AbstractVehicle}
     * @param color The color of the vehicle
     */
    protected final void createAndAddVehicle(AbstractVehicle vehicle, Color color) {

        IVisualizationVehicle visCar = createVisVehicle();
        visCar.setBaseColor(color);
        VehicleEntity entity = new VehicleEntity(config, vehicle, visCar);
        vehicle.setEntity(entity);
        visCar.setEntity(entity);
        addVehicle(vehicle);
    }

	/*
	|==================|
	| simulation steps |
	|==================|
	*/
	/**
	 * This method should be called in a loop to calculate one simulation step.
	 * A simulation step contains acceleration, dashing, braking, dawdling,
	 * moving and right before braking, all nodes are updating their traffic
	 * logic.
	 */
	private void doSimulationStep() {


		if (config.logger.enabled) {
			long time = System.nanoTime();
            simManager.willMoveAll(vehicleManager.iteratorSpawned());
			config.logger.debugNanoseconds("time brake() etc. = ", System.nanoTime() - time);

			time = System.nanoTime();
            simManager.moveAll(vehicleManager.iteratorSpawned());
			config.logger.debugNanoseconds("time move() = ", System.nanoTime() - time);

			time = System.nanoTime();
            simManager.didMoveAll(vehicleManager.iteratorSpawned());
			config.logger.debugNanoseconds("time didMove() = ", System.nanoTime() - time);

			time = System.nanoTime();
			simManager.spawnAll(vehicleManager.iteratorNotSpawned());
			config.logger.debugNanoseconds("time spawn() = ", System.nanoTime() - time);

			time = System.nanoTime();
			simManager.updateNodes(graph.getNodeIterator());
			config.logger.debugNanoseconds("time updateNodes() = ", System.nanoTime() - time);
		} else {
            simManager.willMoveAll(vehicleManager.iteratorSpawned());
            simManager.moveAll(vehicleManager.iteratorSpawned());
            simManager.didMoveAll(vehicleManager.iteratorSpawned());
            simManager.spawnAll(vehicleManager.iteratorNotSpawned());
			simManager.updateNodes(graph.getNodeIterator());
		}
		age++;
	}

	// |================|
	// | (i) Simulation |
	// |================|
	@Override
	public boolean isPrepared() {
		return prepared;
	}

	@Override
	public final void prepare() {
		prepared = false;
		vehicleManager.clearAll();
		prepareScenario();
		createAndAddVehicles();
		simManager.updateNodes(graph.getNodeIterator());
		prepared = true;
	}
	
	@Override
	public int getAge() {
		return age;
	}
	
	@Override
	public final synchronized void run() {

		if (paused) {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					doRunOneStep();
				}
			}, 0, config.msPerTimeStep);
			paused = false;
		}
	}

	@Override
	public void willRunOneStep() {
        if (config.logger.enabled) {
            if (isPrepared()) {
                config.logger.debug("########## ########## ########## ########## ##");
                config.logger.debug("NEW SIMULATION STEP");
                config.logger.debug("simulation age before execution = " + getAge());
                time = System.nanoTime();
            } else {
                config.logger.debug("########## ########## ########## ########## ##");
                config.logger.debug("NEW SIMULATION STEP (NOT PREPARED)");
            }
        }
	}

	@Override
	public final synchronized void runOneStep() {
		if (paused)
            doRunOneStep();
	}

    @Override
    public final void doRunOneStep() {
        willRunOneStep();
        if (prepared) {
            doSimulationStep();
        }
        didRunOneStep();
    }

    @Override
    public void didRunOneStep() {
        if (config.logger.enabled) {
            if (isPrepared())
                config.logger.debugNanoseconds("time for this step = ", System.nanoTime() - time);
            config.logger.debug("number of vehicles after run = " + getVehiclesCount());
        }
    }

    @Override
	public final synchronized void cancel() {
		timer.cancel();
		paused = true;
	}
	
	public final synchronized boolean isPaused() {
		return paused;
	}

    @Override
	public final ArrayList<? extends AbstractVehicle> getSpawnedVehicles() {
		return new ArrayList<>(vehicleManager.getSpawnedVehicles());
	}

    @Override
    public final ArrayList<? extends AbstractVehicle> getVehicles() {
        return new ArrayList<>(vehicleManager.getVehicles());
    }

	@Override
	public int getSpawnedVehiclesCount() {
		return vehicleManager.getSpawnedCount();
	}
	
	@Override
	public int getVehiclesCount() {
		return vehicleManager.getVehicleCount();
	}
	
	@Override
	public final IVisualizationVehicle createVisVehicle() {
		IVisualizationVehicle v = vehicleManager.getVehicleFactory().get();
		vehicleManager.unlockVehicleFactory();
		return v;
	}
	
	@Override
	public final boolean addVehicle(AbstractVehicle vehicle) {
		if (graph.addVehicle(vehicle)) {
			vehicleManager.addVehicle(vehicle);
			return true;
		}
		return false;
	}

    @Override
    public void stateChanged(AbstractVehicle vehicle) {
        vehicleManager.stateChanged(vehicle);
    }
}