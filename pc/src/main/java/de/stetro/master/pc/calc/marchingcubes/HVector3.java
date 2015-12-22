package de.stetro.master.pc.calc.marchingcubes;

import org.rajawali3d.math.vector.Vector3;


public class HVector3 extends Vector3 {
    public HVector3(double x, double y, double z) {
        super(x,y,z);
    }

    @Override
    public int hashCode() {
        String s1 = String.valueOf(Long.valueOf(Double.doubleToLongBits(x)).hashCode());
        String s2 = String.valueOf(Long.valueOf(Double.doubleToLongBits(y)).hashCode());
        String s3 = String.valueOf(Long.valueOf(Double.doubleToLongBits(z)).hashCode());
        return (s1 + s2 + s3).hashCode();
    }
}
