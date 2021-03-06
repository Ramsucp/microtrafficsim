package microtrafficsim.core.map.layers;

import microtrafficsim.core.map.Bounds;
import microtrafficsim.core.map.tiles.TileId;
import microtrafficsim.core.map.tiles.TilingScheme;
import microtrafficsim.math.Rect2d;


/**
 * Generic source for a (tiled) layer.
 *
 * @author Maximilian Luz
 */
public interface TileLayerSource {

    /**
     * Returns the type of this source.
     *
     * @return the type of this source.
     */
    Class<? extends TileLayerSource> getType();

    /**
     * Checks if this source is available.
     *
     * @return {@code true} if this source is available.
     */
    boolean isAvailable();

    /**
     * Returns the (un-projected) bounds of the data this layer provides.
     *
     * @return the (un-projected) bounds of the data this layer provides.
     */
    Bounds getBounds();

    /**
     * Returns the (projected) bounds of the data this layer provides.
     *
     * @return the (projected) bounds of the data this layer provides.
     */
    Rect2d getProjectedBounds();

    /**
     * Returns the tiling-scheme used by this source.
     *
     * @return the tiling-scheme used by this source.
     */
    TilingScheme getTilingScheme();

    /**
     * Adds the given change-listener to this source.
     *
     * @param listener the listener to add.
     * @return {@code true} if the underlying collection of listeners changed with this call.
     */
    boolean addLayerSourceChangeListener(TileLayerSourceChangeListener listener);

    /**
     * Removes the given change-listener from this source.
     *
     * @param listener the listener to remove.
     * @return {@code true} if the underlying collection of listeners changed with this call.
     */
    boolean removeLayerSourceChangeListener(TileLayerSourceChangeListener listener);

    /**
     * Checks if the given change-listener is present on this source.
     *
     * @param listener the listener to check for.
     * @return {@code true} if this source contains the given listener.
     */
    boolean hasLayerSourceChangeListener(TileLayerSourceChangeListener listener);


    /**
     * Change-listener for layer sources.
     */
    interface TileLayerSourceChangeListener {

        /**
         * Notified when a layer-source changed.
         *
         * @param source the changed source.
         */
        void sourceChanged(TileLayerSource source);

        /**
         * Notified when a single tile of a layer-source changed.
         *
         * @param source the source on which the tile changed.
         * @param tile   the tile that changed.
         */
        void sourceChanged(TileLayerSource source, TileId tile);
    }
}
