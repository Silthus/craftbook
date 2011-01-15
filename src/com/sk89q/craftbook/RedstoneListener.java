package com.sk89q.craftbook;
// $Id$

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.sk89q.craftbook.access.Configuration;
import com.sk89q.craftbook.access.PlayerInterface;
import com.sk89q.craftbook.access.ServerInterface;
import com.sk89q.craftbook.access.WorldInterface;
import com.sk89q.craftbook.exception.InsufficientArgumentsException;
import com.sk89q.craftbook.exception.LocalWorldEditBridgeException;
import com.sk89q.craftbook.mech.ic.*;
import com.sk89q.craftbook.mech.ic.custom.*;
import com.sk89q.craftbook.mech.ic.logic.*;
import com.sk89q.craftbook.mech.ic.world.*;
import com.sk89q.craftbook.mech.ic.plc.*;
import com.sk89q.craftbook.mech.ic.plc.types.*;
import com.sk89q.craftbook.util.BlockVector;
import com.sk89q.craftbook.util.CraftBookUtil;
import com.sk89q.craftbook.util.MinecraftUtil;
import com.sk89q.craftbook.util.SignText;
import com.sk89q.craftbook.util.Tuple2;
import com.sk89q.craftbook.util.Vector;


/**
 * Event listener for redstone enhancements such as redstone pumpkins and
 * integrated circuits. Redstone hooks for mechanisms are in
 * MechanismListener.
 * 
 * @author sk89q
 * @author Lymia
 */
public class RedstoneListener extends CraftBookDelegateListener
        implements CustomICAccepter{
    
    /**
     * Currently registered ICs
     */
    private Map<String,RegisteredIC> icList = 
            new HashMap<String,RegisteredIC>(32);

    private Set<BlockVector> instantICs = 
            new HashSet<BlockVector>(32);

    private HashMap<String,PlcLang> plcLanguageList = 
            new HashMap<String,PlcLang>();
    
    private boolean checkCreatePermissions = false;
    private boolean redstonePumpkins = true;
    private boolean redstoneNetherstone = false;
    private boolean redstoneICs = true;
    private boolean redstonePLCs = true;
    private boolean redstonePLCsRequirePermission = true;
    private boolean listICs = true;
    private boolean listUnusuableICs = true;
    
    private boolean enableSelfTriggeredICs = true;
    private boolean restrictSelfTriggeredICs = false;
    
    /**
     * Construct the object.
     * 
     * @param craftBook
     * @param listener
     */
    public RedstoneListener(CraftBookCore craftBook, ServerInterface server) {
        super(craftBook, server);
        registerLang("perlstone_v1.0",new Perlstone_1_0());
        registerLang("perlstone32_v1",new Perlstone32_1());
    }

    /**
     * Loads relevant configuration.
     */
    @Override
    public void loadConfiguration() {
        Configuration c = server.getConfiguration();
        
        checkCreatePermissions = c.getBoolean(
                "check-create-permissions", false);

        redstonePumpkins = c.getBoolean("redstone-pumpkins", true);
        redstoneNetherstone = c.getBoolean("redstone-netherstone", false);
        redstoneICs = c.getBoolean("redstone-ics", true);
        redstonePLCs = c.getBoolean("redstone-plcs", true);
        redstonePLCsRequirePermission = c.getBoolean(
                "redstone-plcs-require-permission", false);
        
        listICs = c.getBoolean("enable-ic-list",true);
        listUnusuableICs = c.getBoolean("ic-list-show-unusuable",true);
        
        enableSelfTriggeredICs = c.getBoolean("enable-self-triggered-ics",true);
        restrictSelfTriggeredICs = c.getBoolean("self-triggered-ics-require-premission",false);

        icList.clear();
        
        // Load custom ICs
        if (c.getBoolean("custom-ics", true)) {
            try {
                CustomICLoader.load("custom-ics.txt", this, plcLanguageList);
                logger.info("Custom ICs for CraftBook loaded");
            } catch (CustomICException e) {
                Throwable cause = e.getCause();
                
                if (cause != null && !(cause instanceof FileNotFoundException)) {
                    logger.log(Level.WARNING,
                            "Failed to load custom IC file: " + e.getMessage(),cause);
                }
            }
        }
        
        addDefaultICs();
        
        try {
            for (WorldInterface w:server.getWorlds())
                for(Tuple2<Integer,Integer> chunkCoord:w.getLoadedChunks()) {
                    int xs = (chunkCoord.a+1)<<4;
                    int ys = (chunkCoord.b+1)<<4;
                    for(int x=chunkCoord.a<<4;x<xs;x++) 
                        for(int y=0;y<128;y++) 
                            for(int z=chunkCoord.b<<4;z<ys;z++) 
                                if(w.getId(x, y, z)==BlockType.WALL_SIGN)
                                    onSignAdded(w,x,y,z);
                }
        } catch(Throwable t) {
            System.err.println("Chunk finder failed: "+t.getClass());
            t.printStackTrace();
        }
    }
    
    /**
     * Populate the IC list with the default ICs.
     */
    private void addDefaultICs() {
        if (enableSelfTriggeredICs) {
            internalRegisterIC("MC0020", new MC0020(), ICType.ZISO);
            internalRegisterIC("MC0111", new MC1111(), ICType.ZISO);
            internalRegisterIC("MC0230", new MC1230(), ICType.ZISO);
            internalRegisterIC("MC0420", new MC1420(), ICType.ZISO);
            internalRegisterIC("MC0500", new MC1500(), ICType.ZISO);
            internalRegisterIC("MC0260", new MC1260(false), ICType.ZISO);
            internalRegisterIC("MC0261", new MC1261(false), ICType.ZISO);
            internalRegisterIC("MC0262", new MC1262(false), ICType.ZISO);
        }
        
        internalRegisterIC("MC1000", new MC1000(), ICType.SISO);
        internalRegisterIC("MC1001", new MC1001(), ICType.SISO);
        internalRegisterIC("MC1017", new MC1017(), ICType.SISO);
        internalRegisterIC("MC1018", new MC1018(), ICType.SISO);
        internalRegisterIC("MC1020", new MC1020(), ICType.SISO);
        internalRegisterIC("MC1025", new MC1025(), ICType.SISO);
        internalRegisterIC("MC1110", new MC1110(), ICType.SISO);
        internalRegisterIC("MC1111", new MC1111(), ICType.SISO);
        internalRegisterIC("MC1200", new MC1200(), ICType.SISO);
        internalRegisterIC("MC1201", new MC1201(), ICType.SISO);
        internalRegisterIC("MC1202", new MC1202(), ICType.SISO);
        internalRegisterIC("MC1205", new MC1205(), ICType.SISO);
        internalRegisterIC("MC1206", new MC1206(), ICType.SISO);
        internalRegisterIC("MC1230", new MC1230(), ICType.SISO);
        internalRegisterIC("MC1231", new MC1231(), ICType.SISO);
        internalRegisterIC("MC1240", new MC1240(), ICType.SISO);
        internalRegisterIC("MC1241", new MC1241(), ICType.SISO);
        internalRegisterIC("MC1250", new MC1250(), ICType.SISO);
        internalRegisterIC("MC1260", new MC1260(true), ICType.SISO);
        internalRegisterIC("MC1261", new MC1261(true), ICType.SISO);
        internalRegisterIC("MC1262", new MC1262(true), ICType.SISO);
        internalRegisterIC("MC1420", new MC1420(), ICType.SISO);
        internalRegisterIC("MC1500", new MC1500(), ICType.SISO);
        internalRegisterIC("MC1510", new MC1510(), ICType.SISO);
        internalRegisterIC("MC1511", new MC1511(), ICType.SISO);
        internalRegisterIC("MC1512", new MC1512(), ICType.SISO);

        internalRegisterIC("MC2020", new MC2020(), ICType.SI3O);
        
        internalRegisterIC("MC3020", new MC3020(), ICType._3ISO);
        internalRegisterIC("MC3002", new MC3002(), ICType._3ISO);
        internalRegisterIC("MC3003", new MC3003(), ICType._3ISO);
        internalRegisterIC("MC3021", new MC3021(), ICType._3ISO);
        internalRegisterIC("MC3030", new MC3030(), ICType._3ISO);
        internalRegisterIC("MC3031", new MC3031(), ICType._3ISO);
        internalRegisterIC("MC3032", new MC3032(), ICType._3ISO);
        internalRegisterIC("MC3033", new MC3033(), ICType._3ISO);
        internalRegisterIC("MC3034", new MC3034(), ICType._3ISO);
        internalRegisterIC("MC3036", new MC3036(), ICType._3ISO);
        internalRegisterIC("MC3040", new MC3040(), ICType._3ISO);
        internalRegisterIC("MC3101", new MC3101(), ICType._3ISO);
        internalRegisterIC("MC3231", new MC3231(), ICType._3ISO);
        internalRegisterIC("MC4000", new MC4000(), ICType._3I3O);
        internalRegisterIC("MC4010", new MC4010(), ICType._3I3O);
        internalRegisterIC("MC4100", new MC4100(), ICType._3I3O);
        internalRegisterIC("MC4110", new MC4110(), ICType._3I3O);
        internalRegisterIC("MC4200", new MC4200(), ICType._3I3O);

        internalRegisterPLC("MC5000", "perlstone_v1.0", ICType.VIVO);
        internalRegisterPLC("MC5001", "perlstone_v1.0", ICType._3I3O);
        
        internalRegisterPLC("MC5032", "perlstone32_v1", ICType.VIVO);
        internalRegisterPLC("MC5033", "perlstone32_v1", ICType._3I3O);
    }

    /**
     * Called when a sign is updated.
     * @param player
     * @param cblock
     * @return
     */
    public boolean onSignChange(PlayerInterface player, WorldInterface world, Vector v, SignText s) {
        int type = world.getId(v);
        
        String line2 = s.getLine2();
        int len = line2.length();

        // ICs
        if (line2.length() > 4
                && line2.substring(0, 3).equalsIgnoreCase("[MC") &&
                line2.charAt(len - 1) == ']') {

            // Check to see if the player can even create ICs
            if (checkCreatePermissions
                    && !player.canUseCommand("/makeic")) {
                player.sendMessage(Colors.RED
                        + "You don't have permission to make ICs.");
                MinecraftUtil.dropSign(world, v.getBlockX(), v.getBlockY(), v.getBlockZ());
                return true;
            }

            String id = line2.substring(1, len - 1).toUpperCase();
            RegisteredIC ic = icList.get(id);

            if (ic != null) {
                if (ic.isPlc) {
                    if (!redstonePLCs) {
                        player.sendMessage(Colors.RED + "PLCs are not enabled.");
                        MinecraftUtil.dropSign(world, v.getBlockX(), v.getBlockY(), v.getBlockZ());
                        return false;
                    }
                }
                
                if (!canCreateIC(player, id, ic)) {
                    player.sendMessage(Colors.Rose
                            + "You don't have permission to make " + id + ".");
                    MinecraftUtil.dropSign(v.getBlockX(), v.getBlockY(), v.getBlockZ());
                    return true;
                } else {
                    // To check the environment
                    Vector pos = new Vector(v.getBlockX(), v.getBlockY(), v.getBlockZ());
                    SignText signText = new SignText(
                        sign.getText(0), sign.getText(1), sign.getText(2),
                        sign.getText(3));

                    // Maybe the IC is setup incorrectly
                    String envError = ic.ic.validateEnvironment(pos, signText);

                    if (signText.isChanged()) {
                        sign.setText(0, signText.getLine1());
                        sign.setText(1, signText.getLine2());
                        sign.setText(2, signText.getLine3());
                        sign.setText(3, signText.getLine4());
                    }

                    if (envError != null) {
                        player.sendMessage(Colors.Rose
                                + "Error: " + envError);
                        MinecraftUtil.dropSign(v.getBlockX(), v.getBlockY(), v.getBlockZ());
                        return true;
                    } else {
                        sign.setText(0, ic.ic.getTitle());
                        sign.setText(1, "[" + id + "]");
                    }
                    
                    if(enableSelfTriggeredICs && ic.type.isSelfTriggered) {
                        instantICs.add(pos.toBlockVector());
                    }
                    
                    sign.update();
                }
                
                if (ic.isPlc && !redstonePLCs && redstoneICs) {
                    player.sendMessage(Colors.Rose + "Warning: PLCs are disabled.");
                }
            } else {
                sign.setText(1, Colors.Red + line2);
                player.sendMessage(Colors.Rose + "Unrecognized IC: " + id);
            }

            if (!redstoneICs) {
                player.sendMessage(Colors.Rose + "Warning: ICs are disabled.");
            } else if (type == BlockType.SIGN_POST) {
                player.sendMessage(Colors.Rose + "Warning: IC signs must be on a wall.");
            }

            return false;
        }
        
        return false;
    }

    /**
     * Handles the wire input at a block in the case when the wire is
     * directly connected to the block in question only.
     *
     * @param x
     * @param y
     * @param z
     * @param isOn
     */
    public void onDirectWireInput(final Vector pt, boolean isOn, final Vector changed) {
        int type = CraftBookCore.getBlockID(pt);
        
        // Redstone pumpkins
        if (redstonePumpkins
                && (type == BlockType.PUMPKIN || type == BlockType.JACKOLANTERN)) {
            Boolean useOn = RedstoneUtil.testAnyInput(pt);

            if (useOn != null && useOn) {
                CraftBookCore.setBlockID(pt, BlockType.JACKOLANTERN);
            } else if (useOn != null) {
                CraftBookCore.setBlockID(pt, BlockType.PUMPKIN);
            }
        // Redstone netherstone
        } else if (redstoneNetherstone
                && (type == BlockType.NETHERSTONE)) {
            Boolean useOn = RedstoneUtil.testAnyInput(pt);
            Vector above = pt.add(0, 1, 0);

            if (useOn != null && useOn && CraftBookCore.getBlockID(above) == 0) {
                CraftBookCore.setBlockID(above, BlockType.FIRE);
            } else if (useOn != null && CraftBookCore.getBlockID(above) == BlockType.FIRE) {
                CraftBookCore.setBlockID(above, 0);
            }
        } else if (type == BlockType.WALL_SIGN
                || type == BlockType.SIGN_POST) {
            ComplexBlock cblock = etc.getServer().getComplexBlock(
                    pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());

            if (!(cblock instanceof Sign)) {
                return;
            }

            final Sign sign = (Sign)cblock;
            String line2 = sign.getText(1);
            int len = line2.length();
            
            // ICs
            if (redstoneICs && type == BlockType.WALL_SIGN
                    && line2.length() > 4
                    && line2.substring(0, 3).equalsIgnoreCase("[MC")
                    && line2.charAt(len - 1) == ']') {

                String id = line2.substring(1, len - 1).toUpperCase();

                final SignText signText = new SignText(sign.getText(0),
                        sign.getText(1), sign.getText(2), sign.getText(3));

                final RegisteredIC ic = icList.get(id);
                
                if (ic == null) {
                    sign.setText(1, Colors.Red + line2);
                    sign.update();
                    return;
                }

                if (ic.type.isSelfTriggered) {
                    return;
                }

                craftBook.getDelay().delayAction(
                        new TickDelayer.Action(pt.toBlockVector(), 2) {
                    @Override
                    public void run() {
                        ic.think(pt, changed, signText, sign, craftBook.getDelay());

                        if (signText.isChanged()) {
                            sign.setText(0, signText.getLine1());
                            sign.setText(1, signText.getLine2());
                            sign.setText(2, signText.getLine3());
                            sign.setText(3, signText.getLine4());
                            
                            if (signText.shouldUpdate()) {
                                sign.update();
                            }
                        }
                    }
                });
            }
        }
    }

    public void onTick() {
        if(!enableSelfTriggeredICs) return;
        
        //XXX HACK: Do this in a more proper way later.
        if(etc.getServer().getTime()%2!=0) return;
        
        BlockVector[] bv = this.instantICs.toArray(new BlockVector[0]);
        for(BlockVector pt:bv) {
            Sign sign = (Sign)etc.getServer().getComplexBlock(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
            if(sign==null) {
                this.instantICs.remove(pt);
                continue;
            }
            String line2 = sign.getText(1);
            if(!line2.startsWith("[MC")) {
                this.instantICs.remove(pt);
                continue;
            }
            
            String id = line2.substring(1, line2.length() - 1).toUpperCase();
            RegisteredIC ic = icList.get(id);
            if (ic == null) {
                sign.setText(1, Colors.Red + line2);
                sign.update();
                this.instantICs.remove(pt);
                continue;
            }

            if(!ic.type.isSelfTriggered) {
                this.instantICs.remove(pt);
                continue;
            }

            SignText signText = new SignText(sign.getText(0),
                    sign.getText(1), sign.getText(2), sign.getText(3));
            
            ic.think(pt, signText, sign);
            
            if (signText.isChanged()) {
                sign.setText(0, signText.getLine1());
                sign.setText(1, signText.getLine2());
                sign.setText(2, signText.getLine3());
                sign.setText(3, signText.getLine4());
                
                if (signText.shouldUpdate()) {
                    sign.update();
                }
            }
        }
    }
    public void onSignAdded(int x, int y, int z) {
        if(!enableSelfTriggeredICs) return;
            
        Sign sign = (Sign)etc.getServer().getComplexBlock(x,y,z);
        String line2 = sign.getText(1);
        if(!line2.startsWith("[MC")) return;
        
        String id = line2.substring(1, line2.length() - 1).toUpperCase();
        RegisteredIC ic = icList.get(id);
        if (ic == null) {
            sign.setText(1, Colors.Red + line2);
            sign.update();
            return;
        }

        if(!ic.type.isSelfTriggered) return;

        instantICs.add(new BlockVector(x,y,z));
    }
    
    /**
     * Called when a command is run
     *
     * @param player
     * @param split
     * @return whether the command was processed
     */
    @Override
    public boolean onCheckedCommand(Player player, String[] split)
            throws InsufficientArgumentsException,
            LocalWorldEditBridgeException {
        
        if (listICs && split[0].equalsIgnoreCase("/listics")
                && Util.canUse(player, "/listics")) {
            String[] lines = generateICText(player);
            int pages = ((lines.length - 1) / 10) + 1;
            int accessedPage;
            
            try {
                accessedPage = split.length == 1 ? 0 : Integer
                        .parseInt(split[1]) - 1;
                if (accessedPage < 0 || accessedPage >= pages) {
                    player.sendMessage(Colors.Rose + "Invalid page \""
                            + split[1] + "\"");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Colors.Rose + "Invalid page \"" + split[1]
                        + "\"");
                return true;
            }

            player.sendMessage(Colors.Blue + "CraftBook ICs (Page "
                    + (accessedPage + 1) + " of " + pages + "):");
            
            for (int i = accessedPage * 10; i < lines.length
                    && i < (accessedPage + 1) * 10; i++) {
                player.sendMessage(lines[i]);
            }

            return true;
        }

        return false;
    }

    /**
     * Used for the /listics command.
     * 
     * @param p
     * @return
     */
    private String[] generateICText(Player p) {
        ArrayList<String> icNameList = new ArrayList<String>();
        icNameList.addAll(icList.keySet());

        Collections.sort(icNameList);

        ArrayList<String> strings = new ArrayList<String>();
        for (String ic : icNameList) {
            RegisteredIC ric = icList.get(ic);
            boolean canUse = canCreateIC(p, ic, ric);
            boolean auto = ric.type.isSelfTriggered;
            if (listUnusuableICs) {
                strings.add(Colors.Rose + ic + " (" + ric.type.name + ")"
                        + (auto ? " (SELF-TRIGGERED)" : "") + ": "
                        + ric.ic.getTitle() + (canUse ? "" : " (RESTRICTED)"));
            } else if (canUse) {
                strings.add(Colors.Rose + ic + " (" + ric.type.name + ")"
                        + (auto ? " (SELF-TRIGGERED)" : "") + ": "
                        + ric.ic.getTitle());
            }
        }
        
        return strings.toArray(new String[0]);
    }

    /**
     * Checks if the player can create an IC.
     * 
     * @param player
     * @param id
     * @param ic
     */
    private boolean canCreateIC(Player player, String id, RegisteredIC ic) {
        return (!ic.ic.requiresPermission()
                && !(ic.isPlc && redstonePLCsRequirePermission)
                && !(ic.type.isSelfTriggered && restrictSelfTriggeredICs))
                || player.canUseCommand("/allic")
                || player.canUseCommand("/" + id.toLowerCase());
    }

    /**
     * Register a new IC. Defined by the interface CustomICAccepter.
     * 
     * @param name
     * @param ic
     * @param type
     */
    public void registerIC(String name, IC ic, String type)
            throws CustomICException {
        if (icList.containsKey(name)) {
            throw new CustomICException("IC already defined");
        }
        ICType icType = getICType(type);
        if(!enableSelfTriggeredICs && icType.isSelfTriggered) return;
        
        registerIC(name, ic, icType, false);
    }

    /**
     * Get an IC type from its type name.
     * 
     * @param type
     * @return
     * @throws CustomICException thrown if the type does not exist
     */
    private ICType getICType(String type) throws CustomICException {
        ICType typeObject = ICType.forName(type);
        
        if (typeObject == null) {
            throw new CustomICException("Invalid IC type: " + type);
        }
        
        return typeObject;
    }

    /**
     * Registers an non-PLC IC.
     * 
     * @param name
     * @param ic
     * @param type
     */
    private void internalRegisterIC(String name, IC ic, ICType type) {
        if (!icList.containsKey(name)) {
            registerIC(name, ic, type, false);
        }
    }

    /**
     * Registers a PLC
     * 
     * @param name
     * @param ic
     * @param type
     * @param isPlc
     */
    private void internalRegisterPLC(String name, String plclang, ICType type) {
        if (!icList.containsKey(name)) {
            registerIC(name, new DefaultPLC(plcLanguageList.get(plclang)), type, true);
        }
    }

    /**
     * Registers a new non-PLC IC.
     * 
     * @param name
     * @param ic
     * @param type
     */
    public void registerIC(String name, IC ic, ICType type) {
        registerIC(name, ic, type, false);
    }

    /**
     * Registers a new IC.
     * 
     * @param name
     * @param ic
     * @param isPlc
     */
    public void registerIC(String name, IC ic, ICType type, boolean isPlc) {
        icList.put(name, new RegisteredIC(ic, type, isPlc));
    }

    public void registerLang(String name, PlcLang language) {
        plcLanguageList.put(name, language);
        craftBook.getStateManager().addStateHolder(name, language);
    }
    
    public void run() {onTick();}

    /**
     * Storage class for registered ICs.
     */
    private static class RegisteredIC {
        final ICType type;
        final IC ic;
        final boolean isPlc;

        /**
         * Construct the object.
         * 
         * @param ic
         * @param type
         * @param isPlc
         */
        public RegisteredIC(IC ic, ICType type, boolean isPlc) {
            this.type = type;
            this.ic = ic;
            this.isPlc = isPlc;
        }

        /**
         * Think.
         * 
         * @param pt
         * @param changedRedstoneInput
         * @param signText
         * @param sign
         * @param r
         */
        void think(Vector pt, Vector changedRedstoneInput, SignText signText,
                Sign sign, TickDelayer r) {
            type.think(pt, changedRedstoneInput, signText, sign, ic, r);
        }

        /**
         * Think.
         * 
         * @param pt
         * @param changedRedstoneInput
         * @param signText
         * @param sign
         */
        void think(Vector pt, SignText signText, Sign sign) {
            type.think(pt, signText, sign, ic);
        }
    }
    
}