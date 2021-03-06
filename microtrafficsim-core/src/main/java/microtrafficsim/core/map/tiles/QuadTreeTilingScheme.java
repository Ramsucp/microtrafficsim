package microtrafficsim.core.map.tiles;

import microtrafficsim.core.vis.map.projections.Projection;
import microtrafficsim.math.MathUtils;
import microtrafficsim.math.Rect2d;
import microtrafficsim.math.Vec2d;
import microtrafficsim.math.Vec2i;
import microtrafficsim.utils.hashing.FNVHashBuilder;


/**
 * Tiling-scheme using a quad-tree structure for tiling.
 *
 * @author Maximilian Luz
 */
public class QuadTreeTilingScheme implements TilingScheme {

    private Projection projection;
    private int        minlevel;
    private int        maxlevel;

    /**
     * Calls {@link #QuadTreeTilingScheme(Projection, int, int) this(projection, 0, 19)}
     */
    public QuadTreeTilingScheme(Projection projection) {
        this(projection, 0, 19);
    }

    /**
     * Constructs a new {@code QuadTreeTilingScheme} using the given projection.
     *
     * @param projection the projection to be used for/with this tiling-scheme.
     * @param minlevel   the minimum zoom-level of the tiling scheme.
     * @param maxlevel   the maximum zoom-level of the tiling scheme.
     */
    public QuadTreeTilingScheme(Projection projection, int minlevel, int maxlevel) {
        this.projection = projection;
        this.minlevel   = minlevel;
        this.maxlevel   = maxlevel;
    }

    @Override
    public TileId getTile(Vec2d xy, double zoom) {
        Rect2d max = projection.getProjectedMaximumBounds();

        int level = MathUtils.clamp((int) Math.ceil(zoom), minlevel, maxlevel);
        int tiles = 1 << level;

        return new TileId((int) (((xy.x - max.xmin) / (max.xmax - max.xmin)) * tiles),
                          (int) ((1.0 - (xy.y - max.ymin) / (max.ymax - max.ymin)) * tiles),
                          level);
    }

    @Override
    public TileId getTile(Rect2d r) {
        for (int z = maxlevel; z >= minlevel; z--) {
            TileRect bounds = getTiles(r, z);
            if (bounds.xmin == bounds.xmax && bounds.ymin == bounds.ymax)
                return new TileId(bounds.xmin, bounds.ymin, bounds.zoom);
        }

        return null;
    }

    @Override
    public TileRect getTiles(TileRect b, double zoom) {
        int level = MathUtils.clamp((int) Math.ceil(zoom), minlevel, maxlevel);

        if (level > b.zoom) {    // child tiles
            int diff = level - b.zoom;
            return new TileRect(
                    b.xmin << diff,
                    b.ymin << diff,
                    (b.xmax << diff) + (1 << diff) - 1,
                    (b.ymax << diff) + (1 << diff) - 1,
                    level
            );

        } else if (level < b.zoom) {    // parent tiles
            int ptiles = 1 << level;
            int ctiles = 1 << b.zoom;

            int xmin = (int) ((b.xmin / (double) ctiles) * ptiles);
            int ymin = (int) ((b.ymin / (double) ctiles) * ptiles);
            int xmax = (int) ((b.xmax / (double) ctiles) * ptiles);
            int ymax = (int) ((b.ymax / (double) ctiles) * ptiles);

            return new TileRect(
                    MathUtils.clamp(xmin, 0, ptiles - 1),
                    MathUtils.clamp(ymin, 0, ptiles - 1),
                    MathUtils.clamp(xmax, 0, ptiles - 1),
                    MathUtils.clamp(ymax, 0, ptiles - 1),
                    level
            );

        } else {
            return new TileRect(b);
        }
    }

    @Override
    public TileRect getTiles(int tx, int ty, int tz, double zoom) {
        int level = MathUtils.clamp((int) Math.ceil(zoom), minlevel, maxlevel);

        if (level > tz) {    // child tiles
            int diff = level - tz;
            return new TileRect(
                    tx << diff, ty << diff, (tx << diff) + (1 << diff) - 1, (ty << diff) + (1 << diff) - 1, level);

        } else if (level < tz) {    // parent tile
            int ptiles = 1 << level;
            int ctiles = 1 << tz;
            int x      = MathUtils.clamp((int) ((tx / (double) ctiles) * ptiles), 0, ptiles - 1);
            int y      = MathUtils.clamp((int) ((ty / (double) ctiles) * ptiles), 0, ptiles - 1);
            return new TileRect(x, y, x, y, level);

        } else {
            return new TileRect(tx, ty, tx, ty, level);
        }
    }

    @Override
    public TileRect getTiles(Rect2d b, double zoom) {
        if (b == null) return null;
        Rect2d max = projection.getProjectedMaximumBounds();

        int level = MathUtils.clamp((int) Math.ceil(zoom), minlevel, maxlevel);
        int tiles = 1 << level;

        int xmin = (int) (((b.xmin - max.xmin) / (max.xmax - max.xmin)) * tiles);
        int ymin = (int) ((1.0 - (b.ymax - max.ymin) / (max.ymax - max.ymin)) * tiles);
        int xmax = (int) (((b.xmax - max.xmin) / (max.xmax - max.xmin)) * tiles);
        int ymax = (int) ((1.0 - (b.ymin - max.ymin) / (max.ymax - max.ymin)) * tiles);

        return new TileRect(
                MathUtils.clamp(xmin, 0, tiles - 1),
                MathUtils.clamp(ymin, 0, tiles - 1),
                MathUtils.clamp(xmax, 0, tiles - 1),
                MathUtils.clamp(ymax, 0, tiles - 1),
                level
        );
    }


    /**
     * Returns the leaf-tiles for the given tile.
     *
     * @param tile the tile to return the leaf tiles for.
     * @return the leaf-tiles of the given tile as rectangle.
     */
    public TileRect getLeafTiles(TileId tile) {
        return getLeafTiles(tile.x, tile.y, tile.z);
    }

    /**
     * Returns the leaf-tiles for the given tile.
     *
     * @param x the x-id of the tile.
     * @param y the y-id of the tile.
     * @param z the z-id of the tile.
     * @return the leaf-tiles of the given tile as rectangle.
     */
    public TileRect getLeafTiles(int x, int y, int z) {
        int diff = maxlevel - z;

        return new TileRect(
                x << diff,
                y << diff,
                (x << diff) + (1 << diff) - 1,
                (y << diff) + (1 << diff) - 1,
                maxlevel
        );
    }

    /**
     * Returns the leaf-tiles for the given tile-bounds.
     *
     * @param bounds the bounds for which the leaf-tiles should be returned.
     * @return the leaf-tiles of the given tile as rectangle.
     */
    public TileRect getLeafTiles(TileRect bounds) {
        int diff = maxlevel - bounds.zoom;

        return new TileRect(
                bounds.xmin << diff,
                bounds.ymin << diff,
                (bounds.xmax << diff) + (1 << diff) - 1,
                (bounds.ymax << diff) + (1 << diff) - 1,
                maxlevel
        );
    }

    /**
     * Returns the position of the top-left tile vertex.
     *
     * @param x the x-id of the tile.
     * @param y the y-id of the tile.
     * @param z the z-id of the tile.
     * @return the position of the top-left vertex of the specified tile.
     */
    @Override
    public Vec2d getPosition(int x, int y, int z) {
        Rect2d max   = projection.getProjectedMaximumBounds();
        int    tiles = 1 << z;

        return new Vec2d(max.xmin + (x / (double) tiles) * (max.xmax - max.xmin),
                         max.ymin + (1 - y / (double) tiles) * (max.ymax - max.ymin));
    }

    @Override
    public Rect2d getBounds(int x, int y, int z) {
        Rect2d max   = projection.getProjectedMaximumBounds();
        int    tiles = 1 << z;

        return new Rect2d(
                max.xmin + (x / (double) tiles) * (max.xmax - max.xmin),
                max.ymin + (1 - (y + 1) / (double) tiles) * (max.ymax - max.ymin),
                max.xmin + ((x + 1) / (double) tiles) * (max.xmax - max.xmin),
                max.ymin + (1 - y / (double) tiles) * (max.ymax - max.ymin)
        );
    }

    @Override
    public Rect2d getBounds(TileRect tiles) {
        Rect2d max = projection.getProjectedMaximumBounds();
        int    n   = 1 << tiles.zoom;

        return new Rect2d(
                max.xmin + (tiles.xmin / (double) n) * (max.xmax - max.xmin),
                max.ymin + (1 - (tiles.ymax + 1) / (double) n) * (max.ymax - max.ymin),
                max.xmin + ((tiles.xmax + 1) / (double) n) * (max.xmax - max.xmin),
                max.ymin + (1 - tiles.ymin / (double) n) * (max.ymax - max.ymin)
        );
    }

    /**
     * Returns the maximum zoom level of this tiling-scheme.
     *
     * @return the maximum zoom level of this tiling-scheme.
     */
    public int getMaximumZoomLevel() {
        return maxlevel;
    }

    /**
     * Returns the minimum zoom level of this tiling-scheme.
     *
     * @return the minimum zoom level of this tiling-scheme.
     */
    public int getMinimumZoomLevel() {
        return minlevel;
    }

    @Override
    public Vec2i getTileSize() {
        Rect2d max = projection.getProjectedMaximumBounds();
        return new Vec2i((int) (max.xmax - max.xmin), (int) (max.ymax - max.ymin));
    }

    @Override
    public Projection getProjection() {
        return projection;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof QuadTreeTilingScheme)) return false;

        QuadTreeTilingScheme other = (QuadTreeTilingScheme) obj;
        return this.projection.equals(other.getProjection());
    }

    @Override
    public int hashCode() {
        return new FNVHashBuilder().add(projection).getHash();
    }
}
