package com.mcjty.rftools.blocks.teleporter;

import com.mcjty.entity.GenericEnergyHandlerTileEntity;
import com.mcjty.rftools.network.Argument;
import com.mcjty.varia.Coordinate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DialingDeviceTileEntity extends GenericEnergyHandlerTileEntity {

    public static final int MAXENERGY = 50000;
    public static final int RECEIVEPERTICK = 100;

    public static final String CMD_TELEPORT = "tp";

    // For client.
    private List<TeleportDestination> receivers = null;
    private List<TransmitterInfo> transmitters = null;

    public DialingDeviceTileEntity() {
        super(MAXENERGY, RECEIVEPERTICK);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
    }

    public List<TeleportDestination> searchReceivers() {
        TeleportDestinations destinations = TeleportDestinations.getDestinations(worldObj);
        return destinations.getValidDestinations("");
    }

    public void storeReceiversForClient(List<TeleportDestination> receivers) {
        this.receivers = new ArrayList<TeleportDestination>(receivers);
    }

    public List<TeleportDestination> getReceivers() {
        return receivers;
    }

    public List<TransmitterInfo> searchTransmitters() {
        int x = xCoord;
        int y = yCoord;
        int z = zCoord;
        List<TransmitterInfo> transmitters = new ArrayList<TransmitterInfo>();
        for (int dy = -3 ; dy <= 3 ; dy++) {
            int yy = y + dy;
            if (yy >= 0 && yy < worldObj.getActualHeight()) {
                for (int dz = -10 ; dz <= 10 ; dz++) {
                    int zz = z + dz;
                    for (int dx = -10 ; dx <= 10 ; dx++) {
                        int xx = x + dx;
                        if (dx != 0 || dy != 0 || dz != 0) {
                            Coordinate c = new Coordinate(xx, yy, zz);
                            TileEntity tileEntity = worldObj.getTileEntity(xx, yy, zz);
                            if (tileEntity != null) {
                                if (tileEntity instanceof MatterTransmitterTileEntity) {
                                    MatterTransmitterTileEntity matterTransmitterTileEntity = (MatterTransmitterTileEntity) tileEntity;
                                    transmitters.add(new TransmitterInfo(c, matterTransmitterTileEntity.getName()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return transmitters;
    }

    public void storeTransmittersForClient(List<TransmitterInfo> transmitters) {
        this.transmitters = new ArrayList<TransmitterInfo>(transmitters);
    }

    public List<TransmitterInfo> getTransmitters() {
        return transmitters;
    }

    @Override
    public boolean execute(String command, Map<String,Argument> args) {
        boolean rc = super.execute(command, args);
        if (rc) {
            return true;
        }
        if (CMD_TELEPORT.equals(command)) {
            Argument playerName = args.get("player");
            Argument dest = args.get("dest");
            Argument dim = args.get("dim");
            EntityPlayer player = worldObj.getPlayerEntityByName(playerName.getString());
            player.closeScreen();
            player.addChatComponentMessage(new ChatComponentText("Begin to teleport player " + playerName.getString() + " to " + dest.getCoordinate()));
            Coordinate c = dest.getCoordinate();
            player.setPositionAndUpdate(c.getX(), c.getY(), c.getZ());
            return true;
        }
        return false;
    }
}
