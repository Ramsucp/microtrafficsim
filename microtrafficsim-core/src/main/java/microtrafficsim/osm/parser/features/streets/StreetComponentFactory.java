package microtrafficsim.osm.parser.features.streets;

import microtrafficsim.core.map.features.info.LaneInfo;
import microtrafficsim.core.map.features.info.MaxspeedInfo;
import microtrafficsim.core.map.features.info.OnewayInfo;
import microtrafficsim.core.map.features.info.StreetType;
import microtrafficsim.osm.features.info.LaneInfoParser;
import microtrafficsim.osm.features.info.MaxspeedInfoParser;
import microtrafficsim.osm.features.info.OnewayInfoParser;
import microtrafficsim.osm.features.info.StreetTypeParser;
import microtrafficsim.osm.parser.ecs.ComponentFactory;
import microtrafficsim.osm.parser.ecs.Entity;
import microtrafficsim.osm.parser.ecs.entities.WayEntity;
import microtrafficsim.osm.parser.features.FeatureDefinition;
import microtrafficsim.osm.primitives.Way;

import java.util.Map;
import java.util.Set;


/**
* A {@code ComponentFactory} for {@code StreetComponent}s.
*
* @author Maximilian Luz
*/
public class StreetComponentFactory implements ComponentFactory<StreetComponent, Way> {

	/**
	 * Creates a component from the specified source-element and its set of
	 * matching {@code FeatureDefinition}s.
	 *
	 * <p>
	 * Note: a StreetComponent can only be created for an entity of type WayEntity.
	 * </p>
	 *
	 * @param entity	the entity to which the created {@code Component}
	 * 					belongs.
	 * @param source	the source-element from which the {@code Component}
	 * 					should be created.
	 * @param features	the set of {@code FeatureDefinition}s for the
	 * 					source-element.
	 * @return a Component created from the specified source-element and its
	 * {@code FeatureDefinition}s.
	 */
	@Override
	public StreetComponent create(Entity entity, Way source, Set<FeatureDefinition> features) {
		OnewayInfo oneway = OnewayInfoParser.parse(source.tags);
		LaneInfo lanes = LaneInfoParser.parse(source.tags);
		MaxspeedInfo maxspeed = MaxspeedInfoParser.parse(source.tags);
		StreetType streettype = StreetTypeParser.parse(source.tags);
		float layer = parseLayer(source.tags);

		return new StreetComponent((WayEntity) entity, streettype, lanes, maxspeed, oneway, layer);
	}

	private static float parseLayer(Map<String, String> tags) {
		Float layer = tags.get("layer") != null ? Float.parseFloat(tags.get("layer")) : null;

		if (layer == null) {
			if (tags.get("bridge") != null
					&& !(tags.get("bridge").equals("0")
					|| !tags.get("bridge").equals("false")
					|| !tags.get("bridge").equals("no"))) {
				layer = 1.f;
			} else if (tags.get("tunnel") != null
					&& !(tags.get("tunnel").equals("0")
					|| !tags.get("tunnel").equals("false")
					|| !tags.get("tunnel").equals("no"))) {
				layer = -1.f;
			} else {
				layer = 0.f;
			}
		}

		return layer;
	}
}