package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.rendering.instances.RenderPart;

/**This class is the base for all parts and should be extended for any entity-compatible parts.
 * Use {@link AEntityF_Multipart#addPart(APart, boolean)} to add parts 
 * and {@link AEntityF_Multipart#removePart(APart, Iterator)} to remove them.
 * You may extend {@link AEntityF_Multipart} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart extends AEntityF_Multipart<JSONPart>{
	private static RenderPart renderer;
	
	//JSON properties.
	public JSONPartDefinition placementDefinition;
	public final int placementSlot;
	
	//Instance properties.
	/**The entity this part has been placed on.  May be a vehicle or a part.*/
	public final AEntityF_Multipart<?> entityOn;
	/**The vehicle this part has been placed on.  This recurses to the vehicle itself if this part was placed on a part.
	 * Will be null, however, if this part isn't on a vehicle (say if it's on a decor).*/
	public final EntityVehicleF_Physics vehicleOn;
	/**The part this part is on, or null if it's on a base entity.*/
    public final APart partOn;
	
	public boolean isInvisible = false;
	public boolean isActive = true;
	public final boolean turnsWithSteer;
	public final boolean isSpare;
	/**The local offset from this part, to the master entity.  This may not be the offset from the part to the entity it is
	 * on if the entity is a part itself.*/
	public final Point3D localOffset;
	public final RotationMatrix localOrientation;
	public final RotationMatrix zeroReferenceOrientation;
	public final RotationMatrix prevZeroReferenceOrientation;
	private AnimationSwitchbox placementActiveSwitchbox;
	private AnimationSwitchbox internalActiveSwitchbox;
	private AnimationSwitchbox placementMovementSwitchbox;
	private AnimationSwitchbox internalMovementSwitchbox;
		
	public APart(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data){
		super(entityOn.world, placingPlayer, data);
		this.entityOn = entityOn;
		this.vehicleOn = entityOn instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) entityOn : null;
		this.partOn = entityOn instanceof APart ? (APart) entityOn : null;
		this.placementDefinition = placementDefinition;
		this.placementSlot = entityOn.definition.parts.indexOf(placementDefinition);
		
		this.localOffset = placementDefinition.pos.copy();
		this.localOrientation = new RotationMatrix();
		this.zeroReferenceOrientation = new RotationMatrix();
		this.prevZeroReferenceOrientation = new RotationMatrix();
		
		this.turnsWithSteer = placementDefinition.turnsWithSteer || (partOn != null && partOn.turnsWithSteer);
		this.isSpare = placementDefinition.isSpare || (partOn != null && partOn.isSpare);
		
		//Set initial position and rotation.  This ensures part doesn't "warp" the first tick.
		//Note that this isn't exact, as we can't calculate the exact locals until after the first tick
		//when we init all our animations.
		position.set(localOffset).add(entityOn.position);
		prevPosition.set(position);
		orientation.set(entityOn.orientation);
		prevOrientation.set(orientation);
	}
	
	@Override
	protected void initializeDefinition(){
		super.initializeDefinition();
		if(placementDefinition.animations != null || placementDefinition.applyAfter != null){
			List<JSONAnimationDefinition> animations = new ArrayList<JSONAnimationDefinition>();
			if(placementDefinition.animations != null){
				animations.addAll(placementDefinition.animations);
			}
			placementMovementSwitchbox = new AnimationSwitchbox(entityOn, animations, placementDefinition.applyAfter);
		}
		if(definition.generic.movementAnimations != null){
			internalMovementSwitchbox = new AnimationSwitchbox(this, definition.generic.movementAnimations, null);
		}
		if(placementDefinition.activeAnimations != null){
			placementActiveSwitchbox = new AnimationSwitchbox(entityOn, placementDefinition.activeAnimations, null);
		}
		if(definition.generic.activeAnimations != null){
			internalActiveSwitchbox = new AnimationSwitchbox(this, definition.generic.activeAnimations, null);
		}
	}
	
	@Override
	public void update(){
		super.update();
		//Update active state.
		isActive = partOn != null ? partOn.isActive : true;
		if(isActive && placementActiveSwitchbox != null){
			isActive = placementActiveSwitchbox.runSwitchbox(0, false);
		}
		if(isActive && internalActiveSwitchbox != null){
			isActive = internalActiveSwitchbox.runSwitchbox(0, false);
		}
		
		//Set initial offsets.
		motion.set(entityOn.motion);
		position.set(entityOn.position);
		orientation.set(entityOn.orientation);
		localOffset.set(placementDefinition.pos);
		
		//Update zero-reference.
		prevZeroReferenceOrientation.set(zeroReferenceOrientation);
		zeroReferenceOrientation.set(entityOn.orientation);
		if(placementDefinition.rot != null){
			zeroReferenceOrientation.multiply(placementDefinition.rot);
		}
		
		//Update local position, orientation, scale, and enabled state.
		isInvisible = false;
		scale.set(entityOn.scale);
		localOrientation.setToZero();
		
		//Placement movement uses the coords of the thing we are on.
		if(placementMovementSwitchbox != null){
			isInvisible = !placementMovementSwitchbox.runSwitchbox(0, false);
			//Offset needs to move according to full transform.
			//This is because these coords are from what we are on.
			//Orientation just needs to update according to new rotation.
			localOffset.transform(placementMovementSwitchbox.netMatrix);
			localOrientation.multiply(placementMovementSwitchbox.rotation);
		}
		//Offset now needs to be multiplied by the scale, as that's the scale of what we are on.
		//This ensures that we're offset relative the proper amount.
		localOffset.multiply(scale);
		
		//Internal movement uses local coords.
		//First rotate orientation to face rotated state.
		if(placementDefinition.rot != null){
			localOrientation.multiply(placementDefinition.rot);
		}
		//Also apply part scale, so everything stays local.
		//We will still have to multiply the translation by this scale though.
		if(placementDefinition.partScale != null){
			scale.multiply(placementDefinition.partScale);
		}
		if(internalMovementSwitchbox != null){
			isInvisible = !internalMovementSwitchbox.runSwitchbox(0, false) || isInvisible;
			//Offset here is local and just needs translation, as it's
			//assuming that we are the origin.
			localOffset.add(internalMovementSwitchbox.translation.multiply(scale));
			localOrientation.multiply(internalMovementSwitchbox.rotation);
		}
		
		//Now that locals are set, set globals to reflect them.
		Point3D localPositionDelta = new Point3D().set(localOffset).rotate(orientation);
		position.add(localPositionDelta);
		orientation.multiply(localOrientation);
		
		//Update bounding box, as scale changes width/height.
		boundingBox.widthRadius = getWidth()/2D*scale.x;
		boundingBox.heightRadius = getHeight()/2D*scale.y;
		boundingBox.depthRadius = getWidth()/2D*scale.z;
	}
	
	@Override
	protected void updateCollisionBoxes(){
		//Add collision if we aren't a fake part.
		if(!isFake()){
			super.updateCollisionBoxes();
			interactionBoxes.add(boundingBox);
		}
	}
	
	@Override
    protected void updateBoxLists(){
	    super.updateBoxLists();
	    
	    //Don't add our collision boxes to the box list if we aren't active and on the client.
	    //Servers need all of these since we might be active for some players and not others.
        if(world.isClient() && areVariablesBlocking(placementDefinition, InterfaceManager.clientInterface.getClientPlayer())){
            allInteractionBoxes.clear();
            return;
        }
        
        //If we are holding a wrench, and the part has children, don't add it.  We can't wrench those parts.
        //The only exception are parts that have permanent-default parts on them.  These can be wrenched.
        //Again, this only applies on clients for that client player.
        if(world.isClient() && InterfaceManager.clientInterface.getClientPlayer().isHoldingItemType(ItemComponentType.WRENCH)){
            for(APart childPart : parts){
                if(!childPart.placementDefinition.isPermanent){
                    allInteractionBoxes.clear();
                    return;
                }
            }
        }
	    
    }
	
	@Override
	public void attack(Damage damage){
		//Check if we can be removed by this attack.
		if(!placementDefinition.isPermanent && definition.generic.canBeRemovedByHand && damage.isHand){
			//Attacked a removable part, remove us to the player's inventory.
			//If the inventory can't fit us, don't remove us.
			IWrapperPlayer player = (IWrapperPlayer) damage.entityResponsible;
			if(entityOn.locked){
				player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_LOCKED));
			}else{
				if(player.getInventory().addStack(getItem().getNewStack(save(InterfaceManager.coreInterface.getNewNBTWrapper())))){
					entityOn.removePart(this, null);
				}
			}
		}else{
			//Not a removable part, or is an actual attack.
			super.attack(damage);
		}
	}
	
	@Override
    public void addPart(APart part, boolean sendPacket){
	    super.addPart(part, sendPacket);
	    this.onPartChange();
	}
	
	@Override
	public PlayerOwnerState getOwnerState(IWrapperPlayer player){
		return entityOn.getOwnerState(player);
	}
	
	@Override
	public double getMass(){
		return definition.generic.mass;
	}
	
	@Override
	public boolean shouldSavePosition(){
		return false;
	}

	/**
	 * Updates the tone of the part to its appropriate type.
	 * If the part can't match the tone of this vehicle, then it is not modified.
	 */
	public void updateTone(boolean recursive){
		if(placementDefinition.toneIndex != 0){
			List<String> partTones = null;
			for(JSONSubDefinition subDefinition : entityOn.definition.definitions){
				if(subDefinition.subName.equals(entityOn.subName)){
					partTones = subDefinition.partTones;
				}
			}
			if(partTones != null && partTones.size() >= placementDefinition.toneIndex){
				String partTone = partTones.get(placementDefinition.toneIndex - 1);
				for(JSONSubDefinition subDefinition : definition.definitions){
					if(subDefinition.subName.equals(partTone)){
						subName = partTone;
						return;
					}
				}
			}
		}
		
		if(recursive && !parts.isEmpty()){
			for(APart part : parts) {
				part.updateTone(true);
			}
		}
	}
	
	/**
     * Called whenever a part is added or removed from the entity this part is on.
     * At the time of call, the part that was added will already be added, and the part
     * that was removed will already me removed.  The part removed will never be this part,
     * but this part may be the part added.  There also may not be any part addition or removal,
     * as is the case when a part reports a change because one of its subparts was changed.
     * No updates are performed prior to calling this method in the latter case, so do not 
     * reference any animation blocks in this method.
     */
    public void onPartChange(){
        parts.forEach(part -> part.onPartChange());
    }
	
	/**
     * Adds all linked parts to the passed-in list.  This method is semi-recursive.  If a part is
     * in a linked slot, and it matches the class, then that part is added and the next slot is checked.
     * If the part doesn't match, then all child parts of that part are checked to see if they match.
     * This is done irrespective of the slot match on the sub-part, but will respect the part class.
     * This is done because wheels and other parts will frequently be attached to other parts in specific
     * slots, such as custom axles or gun mounting hardpoints.  This method will also check in reverse, in
     * that if a part is linked to the slot of this part, then it will act as if this part had a linking to
     * the slot of the other part, provided the class matches the passed-in class.  Note that for all cases,
     * the JSON values are 1-indexed, whereas the map is 0-indexed.
     */
    public <PartClass extends APart> void addLinkedPartsToList(List<PartClass> partList, Class<PartClass> partClass){
        //Check for parts we are linked to.
        if(placementDefinition.linkedParts != null) {
            for(int partIndex : placementDefinition.linkedParts) {
                APart partAtIndex = entityOn.partsInSlots.get(partIndex-1);
                if(partClass.isInstance(partAtIndex)){
                    partList.add(partClass.cast(partAtIndex));
                }else if(partAtIndex != null) {
                    partAtIndex.addMatchingPartsToList(partList, partClass);
                }
            }
        }
        
        //Now check for parts linked to us.
        for(APart part : entityOn.parts) {
            if(part != this && part.placementDefinition.linkedParts != null) {
                for(int partIndex : part.placementDefinition.linkedParts) {
                    if(partIndex-1 == this.placementSlot){
                        if(partClass.isInstance(part)) {
                            partList.add(partClass.cast(part));
                        }else {
                            part.addMatchingPartsToList(partList, partClass);
                        }
                    }
                }
            }
        }
    }
    
    public <PartClass extends APart> void addMatchingPartsToList(List<PartClass> partList, Class<PartClass> partClass){
        for(APart part : parts) {
            if(partClass.isInstance(part)){
                partList.add(partClass.cast(part));
            }else if(part != null) {
                part.addMatchingPartsToList(partList, partClass);
            }
        }
    }
	
	/**
	 * Returns true if this part is in liquid.
	 */
	public boolean isInLiquid(){
		return world.isBlockLiquid(position);
	}
	
	/**
	 * Checks if this part can be removed with a wrench.  If so, then null is returned.
	 * If not, a {@link LanguageEntry} is returned with the message of why it cannot be.
	 * 
	 */
	public LanguageEntry checkForRemoval(){
		return null;
	}

	
	/**
	 * This is called during part save/load calls.  Fakes parts are
	 * added to entities, but they aren't saved with the NBT.  Rather, 
	 * they should be re-created in the constructor of the part that added
	 * them in the first place.
	 */
	public boolean isFake(){
		return false;
	}
	
	public double getWidth(){
		return definition.generic.width != 0 ? definition.generic.width : 0.75F;
	}
	
	public double getHeight(){
		return definition.generic.height != 0 ? definition.generic.height : 0.75F;
	}

	
	//--------------------START OF SOUND AND ANIMATION CODE--------------------
	@Override
	public float getLightProvided(){
		return entityOn.getLightProvided();
	}
	
	@Override
	public boolean shouldRenderBeams(){
		return entityOn.shouldRenderBeams();
	}
	
	@Override
	public String getTexture(){
		return definition.generic.useVehicleTexture ? entityOn.getTexture() : super.getTexture();
	}
	
	@Override
	public boolean renderTextLit(){
		return entityOn.renderTextLit();
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		//If the variable is prefixed with "parent_", then we need to get our parent's value.
		if(variable.startsWith("parent_")){
			return entityOn.getRawVariableValue(variable.substring("parent_".length()), partialTicks);
		}else if(definition.parts != null){
			//Check sub-parts for the part with the specified index.
			int partNumber = getVariableNumber(variable);
			if(partNumber != -1){
				return getSpecificPartAnimation(variable, partNumber, partialTicks);
			}
		}
		
		//Check for generic part variables.
		switch(variable){
			case("part_present"): return 1;
			case("part_ismirrored"): return placementDefinition.isMirrored ? 1 : 0;
		}
		
		//No variables, check super variables before doing generic forwarding.
		//We need this here for position-specific values, as some of the
		//super variables care about position, so we can't forward those.
		double value = super.getRawVariableValue(variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}
		
		//If we are down here, we must have not found a part variable.
		//This means we might be requesting a variable on the entity this part is placed on.
		//Try to get the parent variable, and return whatever we get, NaN or otherwise.
		return entityOn.getRawVariableValue(variable, partialTicks);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderPart getRenderer(){
		if(renderer == null){
			renderer = new RenderPart();
		}
		return renderer;
	}
}
