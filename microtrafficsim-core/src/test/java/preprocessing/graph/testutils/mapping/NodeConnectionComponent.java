package preprocessing.graph.testutils.mapping;

import microtrafficsim.osm.parser.ecs.Component;
import microtrafficsim.osm.parser.ecs.Entity;
import microtrafficsim.osm.parser.ecs.entities.WayEntity;

import java.util.HashSet;


/**
 * A Component for {@code NodeEntities} to store all referenced ways.
 * Used to generate the {@code WaySliceMappings}.
 *
 * @author Maximilian Luz
 */
public class NodeConnectionComponent extends Component {

    public HashSet<WayEntity> ways;

    public NodeConnectionComponent(Entity entity) {
        super(entity);
        this.ways = new HashSet<>();
    }

    @Override
    public Component clone(Entity e) {
        // not required
        return null;
    }
}
