package minecrafttransportsimulator.mcinterface;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemBase;

/**Interface to the core MC system.  This class has methods for registrations
 * file locations, and other core things that are common to clients and servers.
 * Client-specific things go into {@link IInterfaceManager.clientInterface}, rendering goes into
 * {@link IInterfaceManager.renderingInterface}.
 *
 * @author don_bruce
 */
public interface IInterfaceCore {

    /**
     *  Returns the game version for this current instance.
     */
    public String getGameVersion();

    /**
     *  Returns true if the mod with the passed-in modID is present.
     */
    public boolean isModPresent(String modID);

    /**
     *  Returns true if there is a fluid registered with the passed-in name, false otherwise.
     */
    public boolean isFluidValid(String fluidID);

    /**
     *  Returns the text-based name for the passed-in mod.
     */
    public String getModName(String modID);

    /**
     *  Returns a new NBT IWrapper instance with no data.
     */
    public IWrapperNBT getNewNBTWrapper();

    /**
     *  Returns a new stack for the passed-in item.  Note that this is only valid for items
     *  that have {@link AItemBase#autoGenerate()} as true.
     */
    public IWrapperItemStack getAutoGeneratedStack(AItemBase item, IWrapperNBT data);

    /**
     *  Returns a new stack for the item properties.  Or an empty stack if the name is invalid.
     */
    public IWrapperItemStack getStackForProperties(String name, int meta, int qty);

    /**
     *  Returns the registry name for the passed-in stack.  Can be used in conjunction with
     *  {@link #getStackForProperties(String, int, int)} to get a new stack later.
     */
    public String getStackItemName(IWrapperItemStack stack);

    /**
     *  Returns true if both stacks are Oredict compatible.
     */
    public boolean isOredictMatch(IWrapperItemStack stackA, IWrapperItemStack stackB);

    /**
     *  Returns all possible stacks that could be used for the passed-in OreDict name.
     */
    public List<IWrapperItemStack> getOredictMaterials(String oreName);

    /**
     *  Logs an error to the logging system.  Used when things don't work right.
     */
    public void logError(String message);

    /**
     * Called to send queued logs to the logger.  This is required as the logger
     * gets created during pre-init, but logs can be generated during construction.
     */
    public void flushLogQueue();
}
