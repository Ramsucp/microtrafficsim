package microtrafficsim.core.logic.vehicles.driver;

import microtrafficsim.core.logic.routes.Route;
import microtrafficsim.core.logic.streets.DirectedEdge;
import microtrafficsim.core.logic.vehicles.machines.Vehicle;
import microtrafficsim.math.random.distributions.impl.Random;
import microtrafficsim.utils.logging.EasyMarkableLogger;
import microtrafficsim.utils.strings.builder.LevelStringBuilder;
import org.slf4j.Logger;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Basic implementation of {@code Driver}.
 *
 * @author Dominic Parga Cacheiro
 */
public class BasicDriver implements Driver {
    public static final Logger logger = new EasyMarkableLogger(BasicDriver.class);


    /* general */
    private final ReentrantLock lock_priorityCounter;
    private final Random        random;

    /* variable information */
    private Route route;

    /* dynamic information */
    private int travellingTime;
    private int priorityCounter;    // for crossing logic
    /* Hulk */
    private final int maxAnger;
    private       int anger;
    private       int totalAnger;

    /* fix information */
    private Vehicle vehicle;
    private float dawdleFactor;
    private float laneChangeFactor;

    /**
     * seed         seed for {@link Random}, e.g. used for dawdling
     * dawdleFactor probability to dawdle (after Nagel-Schreckenberg-model)
     * spawnDelay   after this number of simulation steps, this driver starts travelling
     */
    public static class InitSetup {
        public final long seed;
        public int spawnDelay = 0;
        public float dawdleFactor = 0.2f;
        public float laneChangeFactor = 0.8f;

        public InitSetup(long seed) {
            this.seed = seed;
        }
    }

    public BasicDriver(InitSetup setup) {
        /* general */
        lock_priorityCounter = new ReentrantLock(true);
        random               = new Random(setup.seed);

        /* variable information */
        route = null;

        /* dynamic information */
        this.travellingTime = -setup.spawnDelay;
        resetPriorityCounter();
        maxAnger   = Integer.MAX_VALUE;
        anger      = 0;
        totalAnger = 0;

        /* fix information */
        vehicle         = null;
        setDawdleFactor(setup.dawdleFactor);
        setLaneChangeFactor(setup.laneChangeFactor);
    }

    @Override
    public String toString() {
        LevelStringBuilder strBuilder = new LevelStringBuilder()
                .setDefaultLevelSeparator()
                .setDefaultLevelSubString();
        strBuilder.appendln("<" + getClass().getSimpleName() + ">").incLevel(); {
            strBuilder.appendln("seed  = " + random.getSeed());
            strBuilder.appendln("priority counter = " + priorityCounter);
            strBuilder.appendln(route);
        } strBuilder.decLevel().appendln("</" + getClass().getSimpleName() + ">");

        return strBuilder.toString();
    }

    /*
    |============|
    | (i) Driver |
    |============|
    */
    @Override
    public int accelerate(int tmpV) {
        return tmpV + 1;
    }

    @Override
    public boolean tendToChangeLane() {
        return random.nextFloat() < laneChangeFactor;
    }

    @Override
    public int dawdle(int tmpV) {
        if (tmpV < 1)
            return 0;
        // Dawdling only 5km/h => return tmpV - 5
        if (random.nextFloat() < dawdleFactor)
            return tmpV - 1;
        return tmpV;
    }

    @Override
    public int getMaxVelocity() {
        DirectedEdge.Lane lane = vehicle.getLane();
        if (lane == null)
            return Integer.MAX_VALUE;
        else
            return lane.getMaxVelocity();
    }

    @Override
    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public Route getRoute() {
        return route;
    }

    @Override
    public void setRoute(Route route) {
        this.route = route;
    }

    @Override
    public float getLaneChangeFactor() {
        return laneChangeFactor;
    }

    @Override
    public void setLaneChangeFactor(float laneChangeFactor) {
        String errorMsg = null;
        if (laneChangeFactor> 1) {
            this.laneChangeFactor = 1;
            errorMsg = "It must hold: 0 <= laneChangeFactor <= 1\n" +
                    "Current: " + laneChangeFactor + "\n" +
                    "Value set to 1.";
        } else if (laneChangeFactor < 0) {
            this.laneChangeFactor = 0;
            errorMsg = "It must hold: 0 <= laneChangeFactor <= 1\n" +
                    "Current: " + laneChangeFactor + "\n" +
                    "Value set to 0.";
        } else
            this.laneChangeFactor = laneChangeFactor;

        if (errorMsg != null)
            logger.error(errorMsg);
    }

    @Override
    public float getDawdleFactor() {
        return dawdleFactor;
    }

    @Override
    public void setDawdleFactor(float dawdleFactor) {
        String errorMsg = null;
        if (dawdleFactor > 1) {
            this.dawdleFactor = 1;
            errorMsg = "It must hold: 0 <= dawdleFactor <= 1\n" +
                    "Current: " + dawdleFactor + "\n" +
                    "Value set to 1.";
        } else if (dawdleFactor < 0) {
            this.dawdleFactor = 0;
            errorMsg = "It must hold: 0 <= dawdleFactor <= 1\n" +
                    "Current: " + dawdleFactor + "\n" +
                    "Value set to 0.";
        } else
            this.dawdleFactor = dawdleFactor;

        if (errorMsg != null)
            logger.error(errorMsg);
    }

    @Override
    public int getTravellingTime() {
        return travellingTime;
    }

    @Override
    public void incTravellingTime() {
        travellingTime++;
    }

    @Override
    public int getPriorityCounter() {
        lock_priorityCounter.lock();
        int tmp = priorityCounter;
        lock_priorityCounter.unlock();

        return tmp;
    }

    @Override
    public void resetPriorityCounter() {
        lock_priorityCounter.lock();

        priorityCounter = 0;

        lock_priorityCounter.unlock();
    }

    @Override
    public void incPriorityCounter() {
        lock_priorityCounter.lock();

        int old = priorityCounter;
        priorityCounter++;
        if (old > priorityCounter) {
            try {
                throw new Exception(getClass().getSimpleName() + ".incPriorityCounter() - int overflow");
            } catch (Exception e) { e.printStackTrace(); }
        }

        lock_priorityCounter.unlock();
    }

    @Override
    public void decPriorityCounter() {
        lock_priorityCounter.lock();

        int old = priorityCounter;
        priorityCounter--;
        if (old < priorityCounter) {
            try {
                throw new Exception(getClass().getSimpleName() + ".incPriorityCounter() - int underflow");
            } catch (Exception e) { e.printStackTrace(); }
        }

        lock_priorityCounter.unlock();
    }


    /*
    |==========|
    | (i) Hulk |
    |==========|
    */
    /**
     * Increases the anger by 1 up to a maximum of {@link #getMaxAnger()}
     */
    @Override
    public void becomeMoreAngry() {
        if (anger < maxAnger)
            anger++;
        totalAnger += 1;
    }

    /**
     * Decreases the anger by 1 down to a minimum of 0.
     */
    @Override
    public void calmDown() {
        if (anger > 0)
            anger--;
    }

    @Override
    public int getAnger() {
        return anger;
    }

    @Override
    public int getTotalAnger() {
        return totalAnger;
    }

    @Override
    public int getMaxAnger() {
        return maxAnger;
    }


    /*
    |================|
    | (i) Resettable |
    |================|
    */
    @Override
    public void reset() {
        /* general */
        random.reset();

        /* variable information */
        route = null;

        /* dynamic information */
        this.travellingTime = 0;
        resetPriorityCounter();
        anger      = 0;
        totalAnger = 0;

        /* fix information */
        vehicle         = null;
    }
}
