package Util;

public class ProjUtil {

	public static double GreatDistance(double slatDeg, double slonDeg, double elatDeg, double elonDeg) {
		
       /*************************************************************************
        * Compute using law of cosines
        *************************************************************************/
        // great circle distance in radians
		double slat = Math.toRadians(slatDeg);
		double slon = Math.toRadians(slonDeg);
		double elat = Math.toRadians(elatDeg);
		double elon = Math.toRadians(elonDeg);
        double angle = Math.acos(Math.sin(slon) * Math.sin(elon)
                      + Math.cos(slon * Math.cos(elon) * Math.cos(slat - elat)));

        // convert back to degrees
        angle = Math.toDegrees(angle);

        // each degree on a great circle of Earth is 60 nautical miles
        double distance = 60 * angle;
        distance = distance * 1.852; // nautical miles to kilometer
        return distance;
	}
}
