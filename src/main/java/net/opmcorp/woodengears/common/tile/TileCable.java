package net.opmcorp.woodengears.common.tile;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.opmcorp.woodengears.client.render.WGOBJState;
import net.opmcorp.woodengears.common.TickHandler;
import net.opmcorp.woodengears.common.grid.*;

import java.util.*;

public class TileCable extends WGTileBase implements ITileRail, ILoadable
{
    protected final EnumSet<EnumFacing>                       renderConnections;
    @Getter
    protected final EnumMap<EnumFacing, ITileCable<RailGrid>> connectionsMap;
    protected final EnumMap<EnumFacing, IRailConnectable>     adjacentHandler;
    @Getter
    @Setter
    protected       int                                       grid;

    public TileCable()
    {
        this.connectionsMap = new EnumMap<>(EnumFacing.class);
        this.adjacentHandler = new EnumMap<>(EnumFacing.class);
        this.grid = -1;

        this.renderConnections = EnumSet.noneOf(EnumFacing.class);
    }

    public Collection<IRailConnectable> getConnectedHandlers()
    {
        return this.adjacentHandler.values();
    }

    @Override
    public void disconnect(EnumFacing facing)
    {
        this.connectionsMap.remove(facing);
        this.updateState();
    }

    public void connectHandler(EnumFacing facing, IRailConnectable to, TileEntity tile)
    {
        this.adjacentHandler.put(facing, to);

        if (tile instanceof IConnectionAware)
            ((IConnectionAware) tile).connectTrigger(facing.getOpposite(), this.getGridObject());
        this.updateState();
    }

    public void disconnectHandler(EnumFacing facing, TileEntity tile)
    {
        this.adjacentHandler.remove(facing);

        if (tile instanceof IConnectionAware)
            ((IConnectionAware) tile).disconnectTrigger(facing.getOpposite(), this.getGridObject());
        this.updateState();
    }

    public void disconnectItself()
    {
        GridManager.getInstance().disconnectCable(this);
    }

    @Override
    public void onChunkUnload()
    {
        this.disconnectItself();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (!this.world.isRemote && this.getGrid() == -1)
            TickHandler.loadables.add(this);
        else if (this.isClient())
        {
            this.forceSync();
        }
    }

    @Override
    public void load()
    {
        GridManager.getInstance().connectCable(this);
        for (final EnumFacing facing : EnumFacing.VALUES)
            this.scanHandlers(this.pos.offset(facing));
    }

    @Override
    public void readFromNBT(final NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);

        final int previousConnections = this.renderConnections.size();

        if (this.isClient())
        {
            this.renderConnections.clear();
            for (final EnumFacing facing : EnumFacing.VALUES)
            {
                if (tagCompound.hasKey("connected" + facing.ordinal()))
                    this.renderConnections.add(facing);
            }
            if (this.renderConnections.size() != previousConnections)
                this.updateState();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);

        if (this.isServer())
        {
            for (final Map.Entry<EnumFacing, ITileCable<RailGrid>> entry : this.connectionsMap.entrySet())
                tagCompound.setBoolean("connected" + entry.getKey().ordinal(), true);
            for (final Map.Entry<EnumFacing, IRailConnectable> entry : this.adjacentHandler.entrySet())
                tagCompound.setBoolean("connected" + entry.getKey().ordinal(), true);
        }
        return tagCompound;
    }

    public void scanHandlers(BlockPos posNeighbor)
    {
        TileEntity tile = this.world.getTileEntity(posNeighbor);

        BlockPos substracted = posNeighbor.subtract(this.pos);
        EnumFacing facing = EnumFacing.getFacingFromVector(substracted.getX(), substracted.getY(), substracted.getZ())
                .getOpposite();

        if (this.adjacentHandler.containsKey(facing.getOpposite()))
        {
            if (!(tile instanceof IRailConnectable))
            {
                if (this.hasGrid() && this.adjacentHandler.get(facing.getOpposite()) instanceof TileArmReservoir)
                    this.getGridObject().removeConnectedReservoir(this,
                            (TileArmReservoir) this.adjacentHandler.get(facing.getOpposite()));
                this.disconnectHandler(facing.getOpposite(), tile);
            }
        }
        else
        {
            if (tile instanceof IRailConnectable)
            {
                this.connectHandler(facing.getOpposite(), (IRailConnectable) tile, tile);

                if (this.hasGrid() &&
                        this.adjacentHandler.get(facing.getOpposite()) instanceof TileArmReservoir)
                    this.getGridObject().addConnectedReservoir(this,
                            (TileArmReservoir) this.adjacentHandler.get(facing.getOpposite()));
            }
        }
    }

    @Override
    public BlockPos getBlockPos()
    {
        return this.getPos();
    }

    @Override
    public boolean canConnect(ITileNode<?> to)
    {
        return to instanceof TileCable;
    }

    @Override
    public World getBlockWorld()
    {
        return this.getWorld();
    }

    @Override
    public RailGrid createGrid(int nextID)
    {
        return new RailGrid(nextID);
    }

    ////////////
    // RENDER //
    ////////////

    private static final HashMap<String, WGOBJState> variants = new HashMap<>();

    public WGOBJState getVisibilityState()
    {
        String key = this.getVariantKey();

        if (!variants.containsKey(key))
            variants.put(key, buildVisibilityState());
        return variants.get(key);
    }

    private String getVariantKey()
    {
        StringBuilder rtn = new StringBuilder(12);

        if (this.isConnected(EnumFacing.EAST))
            rtn.append("x+");
        if (this.isConnected(EnumFacing.WEST))
            rtn.append("x-");
        if (this.isConnected(EnumFacing.SOUTH))
            rtn.append("z+");
        if (this.isConnected(EnumFacing.NORTH))
            rtn.append("z-");
        return rtn.toString();
    }

    private WGOBJState buildVisibilityState()
    {
        List<String> parts = new ArrayList<>();

        parts.add("middleX");
        parts.add("middleZ");
        parts.add("middleBack");


        if (!this.isConnected(EnumFacing.WEST))
        {
            parts.add("barZPosNeg");
            parts.add("backZPosNeg");
            parts.add("barZNegNeg");
            parts.add("backZNegNeg");
        }
        if (!this.isConnected(EnumFacing.EAST))
        {
            parts.add("barZPosPos");
            parts.add("backZPosPos");
            parts.add("barZNegPos");
            parts.add("backZNegPos");
        }

        if (!this.isConnected(EnumFacing.NORTH))
        {
            parts.add("barXPosNeg");
            parts.add("backXPosNeg");
            parts.add("barXNegNeg");
            parts.add("backXNegNeg");
        }
        if (!this.isConnected(EnumFacing.SOUTH))
        {
            parts.add("barXPosPos");
            parts.add("backXPosPos");
            parts.add("barXNegPos");
            parts.add("backXNegPos");
        }

        if (this.renderConnections.size() == 1)
        {
            if (this.isConnected(EnumFacing.EAST))
            {
                parts.add("backXPos");
                parts.add("barXPos");
            }
            else if (this.isConnected(EnumFacing.WEST))
            {
                parts.add("backXNeg");
                parts.add("barXNeg");
            }
            else if (this.isConnected(EnumFacing.NORTH))
            {
                parts.add("backZNeg");
                parts.add("barZNeg");
            }
            else if (this.isConnected(EnumFacing.SOUTH))
            {
                parts.add("backZPos");
                parts.add("barZPos");
            }
        }

        if (this.isStraight())
        {
            if (this.isConnected(EnumFacing.NORTH) || this.isConnected(EnumFacing.SOUTH))
            {
                parts.add("backZNeg");
                parts.add("barZNeg");
                parts.add("backZPos");
                parts.add("barZPos");
            }
            else if (this.isConnected(EnumFacing.EAST) || this.isConnected(EnumFacing.WEST))
            {
                parts.add("backXNeg");
                parts.add("barXNeg");
                parts.add("backXPos");
                parts.add("barXPos");
            }
        }
        return new WGOBJState(parts, false);
    }

    public void updateState()
    {
        if (this.isServer())
        {
            this.sync();
            return;
        }

        this.world.markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
    }

    public boolean isStraight()
    {
        if (this.renderConnections.size() == 2)
            return this.isConnected(EnumFacing.NORTH) && this.isConnected(EnumFacing.SOUTH)
                    || this.isConnected(EnumFacing.WEST) && this.isConnected(EnumFacing.EAST)
                    || this.isConnected(EnumFacing.UP) && this.isConnected(EnumFacing.DOWN);
        return false;
    }

    public boolean isConnected(final EnumFacing facing)
    {
        return this.renderConnections.contains(facing);
    }

    public NBTTagCompound writeRenderConnections(NBTTagCompound tag)
    {
        for (Map.Entry<EnumFacing, ITileCable<RailGrid>> entry : this.connectionsMap.entrySet())
            tag.setBoolean("connected" + entry.getKey().ordinal(), true);
        for (Map.Entry<EnumFacing, IRailConnectable> entry : this.adjacentHandler.entrySet())
            tag.setBoolean("connected" + entry.getKey().ordinal(), true);
        return tag;
    }

    public void readRenderConnections(NBTTagCompound tag)
    {
        this.renderConnections.clear();
        for (final EnumFacing facing : EnumFacing.VALUES)
        {
            if (tag.hasKey("connected" + facing.ordinal()))
                this.renderConnections.add(facing);
        }
    }

    @Override
    public void adjacentConnect()
    {
        List<TileCable> adjacents = new ArrayList<>(6);
        for (EnumFacing facing : EnumFacing.HORIZONTALS)
        {
            final TileEntity adjacent = this.getBlockWorld().getTileEntity(this.getAdjacentPos(facing));
            if (adjacent instanceof TileCable && this.canConnect((ITileCable<?>) adjacent)
                    && ((ITileCable<?>) adjacent).canConnect(this))
            {
                this.connect(facing, (TileCable) adjacent);
                ((TileCable) adjacent).connect(facing.getOpposite(), this);
                adjacents.add((TileCable) adjacent);
            }
        }
    }
}
