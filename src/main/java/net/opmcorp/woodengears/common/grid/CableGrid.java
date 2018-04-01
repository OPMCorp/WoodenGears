package net.opmcorp.woodengears.common.grid;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;

@EqualsAndHashCode
@ToString
@Getter
public abstract class CableGrid
{
    private final int identifier;

    private final HashSet<ITileNode<?>> cables;

    public CableGrid(final int identifier)
    {
        this.identifier = identifier;

        this.cables = new HashSet<>();
    }

    public void tick()
    {

    }

    public abstract CableGrid copy(final int identifier);

    public boolean canMerge(final CableGrid grid)
    {
        return grid.getIdentifier() != this.getIdentifier();
    }

    /**
     * Called on the destination grid after the merging has occurred.
     *
     * @param grid the source grid
     */
    public void onMerge(final CableGrid grid)
    {

    }

    /**
     * Called after a grid splitting has occurred, each new fragment will
     * receive this event.
     *
     * @param grid the grid source grid before splitting.
     */
    public void onSplit(final CableGrid grid)
    {

    }

    public void addCable(@Nonnull final ITileNode<?> cable)
    {
        this.cables.add(cable);
    }

    public void addCables(final Collection<ITileNode<?>> cables)
    {
        cables.forEach(this::addCable);
    }

    public boolean removeCable(final ITileNode<?> cable)
    {
        if (this.cables.remove(cable))
        {
            if (this.cables.isEmpty())
                GridManager.getInstance().removeGrid(this.identifier);
            return true;
        }
        return false;
    }

    public void removeCables(final Collection<ITileNode<?>> cables)
    {
        cables.forEach(this::removeCable);
    }

    public boolean hasCable(final ITileNode<?> cable)
    {
        return this.cables.contains(cable);
    }
}
