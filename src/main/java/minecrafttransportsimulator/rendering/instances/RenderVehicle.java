package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

/**Main render class for all vehicles.  Renders the vehicle, along with all parts.
 * As entities don't render above 255 well due to the new chunk visibility system, 
 * this code is called both from the regular render loop and manually from
 * the event-based last pass.  This pass is -1, and should allow both the regular
 * and blending operations to run.
 *
 * @author don_bruce
 */
public class RenderVehicle extends ARenderEntityDefinable<EntityVehicleF_Physics>{	
	
	@Override
	public void renderBoundingBoxes(EntityVehicleF_Physics vehicle, TransformationMatrix transform){
		super.renderBoundingBoxes(vehicle, transform);
		for(BoundingBox box : vehicle.groundDeviceCollective.getGroundBounds()){
			box.renderWireframe(vehicle, transform, null, ColorRGB.BLUE);
		}
	}
}
