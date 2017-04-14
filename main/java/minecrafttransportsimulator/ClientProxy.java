package minecrafttransportsimulator;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.guis.GUIInstrumentsPlane;
import minecrafttransportsimulator.guis.GUIPropellerBench;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

/**Class responsible for performing client-only updates and operations.
 * Any version-updatable, client-based method should be put in here.
 * 
 * @author don_bruce
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy{	
	
	@Override
	public void preInit(FMLPreInitializationEvent event){
		super.preInit(event);
		ConfigSystem.initClient();
		MTSRegistryClient.preInit();
	}
	
	@Override
	public void init(FMLInitializationEvent event){
		super.init(event);
		MTSRegistryClient.init();
		ControlSystem.init();
		MinecraftForge.EVENT_BUS.register(ClientEventSystem.instance);
		FMLCommonHandler.instance().bus().register(ClientEventSystem.instance);
	}
	
	@Override
	public void openGUI(Object clicked, EntityPlayer clicker){
		if(clicked instanceof EntityPlane){
			FMLCommonHandler.instance().showGuiScreen(new GUIInstrumentsPlane((EntityPlane) clicked, clicker));
		}else if(clicked instanceof TileEntityPropellerBench){
			FMLCommonHandler.instance().showGuiScreen(new GUIPropellerBench((TileEntityPropellerBench) clicked, clicker));
		}
	}
	
	@Override
	public void playSound(Entity noisyEntity, String soundName, float volume, float pitch){
		SFXSystem.playSound(noisyEntity, soundName, volume, pitch);
	}
	
	@Override
	public void updateSFXEntity(SFXEntity entity, World world){
		SFXSystem.doSFX(entity, world);
	}
}
