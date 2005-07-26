/**
 * Crust2Profile.java
 *
 * @author Philip Crotwell
 */

package edu.sc.seis.receiverFunction.crust2;

import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.TauP.VelocityLayer;
import edu.sc.seis.receiverFunction.compare.StationResult;

public class Crust2Profile {

    public Crust2Profile(String code, String name, VelocityLayer[] layers) {
        if (layers.length != 8) {
            throw new IllegalArgumentException("profiles in crust2.0 have exactly 8 layers");
        }
        this.layers = layers;
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public VelocityLayer getLayer(int i) {
        return layers[i];
    }

    /** Calculated for vertical incidence P wave, assumes layers are constant velocity
     * and ignores the last layer (halfspace). */
    public double getPWaveAvgVelocity() {
        double travelTime = 0;
        for (int i = 0; i < layers.length-1; i++) {
            travelTime += layers[i].getBotPVelocity()*(layers[i].getBotDepth()-layers[i].getTopDepth());
        }
        return travelTime/layers[layers.length-1].getTopDepth();
    }

    /** Calculated for vertical incidence S wave, assumes layers are constant velocity
     * and ignores the last layer (halfspace). */
    public double getSWaveAvgVelocity() {
        double travelTime = 0;
        for (int i = 0; i < layers.length-1; i++) {
            travelTime += layers[i].getBotSVelocity()*(layers[i].getBotDepth()-layers[i].getTopDepth());
        }
        return travelTime/layers[layers.length-1].getTopDepth();
    }
    
    public QuantityImpl getCrustThickness() {
        return new QuantityImpl(layers[7].getTopDepth(), UnitImpl.KILOMETER);
    }

    String name;
    String code;
    VelocityLayer[] layers;

}

