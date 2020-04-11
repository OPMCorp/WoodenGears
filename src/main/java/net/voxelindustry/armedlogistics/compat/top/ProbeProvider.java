package net.voxelindustry.armedlogistics.compat.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.voxelindustry.armedlogistics.ArmedLogistics;
import net.voxelindustry.steamlayer.tile.ITileInfoProvider;

public class ProbeProvider implements IProbeInfoProvider
{
    @Override
    public String getID()
    {
        return ArmedLogistics.MODID;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
                             IBlockState blockState, IProbeHitData data)
    {
        TileEntity tile = world.getTileEntity(data.getPos());
        if (tile instanceof ITileInfoProvider)
        {
            net.voxelindustry.armedlogistics.compat.top.TileInfoListImpl list = new net.voxelindustry.armedlogistics.compat.top.TileInfoListImpl(probeInfo);
            ((ITileInfoProvider) tile).addInfo(list);
        }
    }
}
