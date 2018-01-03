package GeoMatch;

import java.io.File;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.LatLngTool.Bearing;
import com.javadocmd.simplelatlng.util.LengthUnit;
public class SiteRadiusCalculator
{
	
public static void main(String[] args) {
		// TODO Auto-generated method stub

		
//		if (args.length<3) {
//			System.out.println("Usage:  java -jar SiteRadiusCalculator site_lat site_lon km_radius");
//			System.out.println("  computes upper_lat, lower_lat, left_lon, right_long bounds for given radius");
//			System.exit(-1);
//		}
//		double site_lat = Double.parseDouble(args[0]);
//		double site_lon = Double.parseDouble(args[1]);
//		double radius_km = Double.parseDouble(args[2]);

	double site_lat = -21.1905;
	double site_lon = 55.5735;
	double radius_km = 100.0;
		
		LatLng latlon = new LatLng((double)site_lat,(double)site_lon);
		
		LatLng up = LatLngTool.travel(latlon, Bearing.NORTH, radius_km,LengthUnit.KILOMETER);
		LatLng down = LatLngTool.travel(latlon, Bearing.SOUTH, radius_km,LengthUnit.KILOMETER);
		LatLng right = LatLngTool.travel(latlon, Bearing.EAST, radius_km,LengthUnit.KILOMETER);
		LatLng left = LatLngTool.travel(latlon, Bearing.WEST, radius_km,LengthUnit.KILOMETER);

		System.out.println("up    (N): lat " + up.getLatitude()+ " lon "+ up.getLongitude());
		System.out.println("down  (S): lat " + down.getLatitude()+ " lon "+ down.getLongitude());
		System.out.println("left  (W): lat " + left.getLatitude()+ " lon "+ left.getLongitude());
		System.out.println("right (E): lat " + right.getLatitude()+ " lon "+ right.getLongitude());
	}
}
