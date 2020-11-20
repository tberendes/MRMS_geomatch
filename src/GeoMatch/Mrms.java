package GeoMatch;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import Util.DateUtil;

public class Mrms {
	
	static int MAX_2MIN_TIME_INTERVAL = 4*60*1000;  //miliseconds - 4 mins
	static int MAX_1HR_TIME_INTERVAL = 2*3600*1000;  //miliseconds - 2 HRS
	// define conus lat/lon bounds
	static float MIN_LAT=20.005f;
	static float MAX_LAT=55.005f;
	static float MIN_LON=-129.995f;
	static float MAX_LON=-59.995f;
	static float CELL_SIZE=0.01f;
	
	String rootDirectory=null;
	String RQIfile=null;
	DateUtil RQIdate=null;
	String HCFfile=null;
	DateUtil HCFdate=null;
	String MASKfile=null;
	DateUtil MASKdate=null;
	String PRECIPRATEfile=null;
	DateUtil PRECIPRATEdate=null;
	
	String timeStr;
	DateUtil date=null;
	MrmsData RQIdata=null;
	MrmsData HCFdata=null;
	MrmsData MASKdata=null;
	MrmsData PRECIPRATEdata=null;
	
	public static float getMIN_LAT() {
		return MIN_LAT;
	}
	public static float getMAX_LAT() {
		return MAX_LAT;
	}
	public static float getMIN_LON() {
		return MIN_LON;
	}
	public static float getMAX_LON() {
		return MAX_LON;
	}
	public String getRootDirectory() {
		return rootDirectory;
	}
	public String getDateTimeStr() {
		return timeStr;
	}
	public DateUtil getDateTime() {
		return date;
	}
	public MrmsData getRQIdata() {
		return RQIdata;
	}
	public MrmsData getHCFdata() {
		return HCFdata;
	}
	public MrmsData getMASKdata() {
		return MASKdata;
	}
	public MrmsData getPRECIPRATEdata() {
		return PRECIPRATEdata;
	}
	private DateUtil dateSetTime(String tempStr, DateUtil fileDate)
	{
		int hr = Integer.parseInt(tempStr.substring(0, 2));
		int min = Integer.parseInt(tempStr.substring(2, 4));
		int sec = Integer.parseInt(tempStr.substring(4, 6));
		String dateStr = DateUtil.formatTextDate(fileDate.getYear(), fileDate.getMonth(), fileDate.getDay(), hr, min, sec);
//		System.out.println("dateSetTime "+dateStr);
		return new DateUtil(dateStr);

	}
	public Mrms(String time, String rootDirectory, float sitelat, float sitelon, String tmpDir) throws Exception
	{
		if (sitelat<MIN_LAT || sitelat>MAX_LAT || sitelon<MIN_LON || sitelon>MAX_LON) {
			// set error condition and return
			throw new Exception("Notice: lat/lon not within CONUS");
		}
		if (rootDirectory==null) {
 			throw new Exception("Error: null root directory");	
		}
		this.rootDirectory = rootDirectory;
		if (time==null) {
 			throw new Exception("Error: null time");	
		}
		timeStr=time;
		DateUtil date=new DateUtil(timeStr);
		
		// find sub directory with yyyyMM
		String yyyyMM=String.format("%04d%02d", date.getYear(), date.getMonth());
		String hhmmss=String.format("%02d%02d%02d", date.getHour(), date.getMinute(), date.getSecond());
		
		File dsFile = new File (rootDirectory + File.separator + yyyyMM);
    	File [] dirListing = dsFile.listFiles();
    	if (dirListing==null || dirListing.length==0) {  // no dates are processed
    		System.out.println("no MRMS files to process in " + dsFile.getCanonicalPath());
    		throw new Exception("no MRMS files to process in " + dsFile.getCanonicalPath());
    	}
    	
		// loop over Mrms files for given date and find files with the closest time to GPM
    	for (int ind1=0;ind1<dirListing.length;ind1++) {
    		// only process files, skip directories
    		if (dirListing[ind1].exists()&&dirListing[ind1].isFile()) {
        		String filename = dirListing[ind1].getName();
//        		System.out.println("filename "+ filename);
        		String [ ] arrStr = filename.split("\\.");
        		      
        		// check for each file type
    			if (filename.contains("1HCF")) {
    				int fileDay = Integer.parseInt(arrStr[1].substring(6, 8));
    				if (date.getDay()!=fileDay)
    					continue;
//    				System.out.println("time "+arrStr[2]);
        			// extract time from filename
            		DateUtil fileDate = dateSetTime(arrStr[2], date);
 
//            		System.out.println(fileDate.toTextDate());
            		if (HCFfile==null) { // first file, set to closest
    					HCFdate = fileDate;
    					HCFfile=filename;
    				}
    				else {
//    					System.out.println("date " + date.toTextDate());
//    					System.out.println("file date " + fileDate.toTextDate());
        				long fileDif = Math.abs(date.toCalendarDate().getTimeInMillis() - fileDate.toCalendarDate().getTimeInMillis());
                		// see if file date is closer than current file date
    					if (fileDif < Math.abs(date.toCalendarDate().getTimeInMillis() - HCFdate.toCalendarDate().getTimeInMillis())
    							&& (date.toCalendarDate().before(fileDate.toCalendarDate())||(date.toCalendarDate().equals(fileDate.toCalendarDate())))) {
        					HCFdate = fileDate;
        					HCFfile=filename;   						
    					}
    				}
    			}
    			else if (filename.contains("MASK")) {
    				int fileDay = Integer.parseInt(arrStr[1].substring(6, 8));
    				if (date.getDay()!=fileDay)
    					continue;
        			// extract time from filename
            		DateUtil fileDate = dateSetTime(arrStr[2], date);
    				if (MASKfile==null) { // first file, set to closest
    					MASKdate = fileDate;
    					MASKfile=filename;
    				}
    				else {
        				long fileDif = Math.abs(date.toCalendarDate().getTimeInMillis() - fileDate.toCalendarDate().getTimeInMillis());
        				// see if file date is closer than current file date
    					if (fileDif < Math.abs(date.toCalendarDate().getTimeInMillis() - MASKdate.toCalendarDate().getTimeInMillis())) {
        					MASKdate = fileDate;
        					MASKfile=filename;   						
    					}
    				}
   				
    			}
    			else if (filename.contains("PRECIPRATE")) {
    				int fileDay = Integer.parseInt(arrStr[2].substring(6, 8));
    				if (date.getDay()!=fileDay)
    					continue;
        			// extract time from filename
            		DateUtil fileDate = dateSetTime(arrStr[3], date);
    				if (PRECIPRATEfile==null) { // first file, set to closest
    					PRECIPRATEdate = fileDate;
    					PRECIPRATEfile=filename;
    				}
    				else {
        				long fileDif = Math.abs(date.toCalendarDate().getTimeInMillis() - fileDate.toCalendarDate().getTimeInMillis());
    					// see if file date is closer than current file date
    					if (fileDif < Math.abs(date.toCalendarDate().getTimeInMillis() - PRECIPRATEdate.toCalendarDate().getTimeInMillis())) {
        					PRECIPRATEdate = fileDate;
        					PRECIPRATEfile=filename;   						
    					}
    				}
    				
    			}
    			else if (filename.contains("RQI")) {
    				int fileDay = Integer.parseInt(arrStr[1].substring(6, 8));
    				if (date.getDay()!=fileDay)
    					continue;
        			// extract time from filename
            		DateUtil fileDate = dateSetTime(arrStr[2], date);
    				if (RQIfile==null) { // first file, set to closest
    					RQIdate = fileDate;
    					RQIfile=filename;
    				}
    				else {
        				long fileDif = Math.abs(date.toCalendarDate().getTimeInMillis() - fileDate.toCalendarDate().getTimeInMillis());
    					// see if file date is closer than current file date
    					if (fileDif < Math.abs(date.toCalendarDate().getTimeInMillis() - RQIdate.toCalendarDate().getTimeInMillis())) {
        					RQIdate = fileDate;
        					RQIfile=filename;   						
    					}
    				}
    				
    			}
    			else { // skip
    				
    			}
    		}
    	}
    	// check for out of range values
    	if (HCFdate !=null && (Math.abs(date.toCalendarDate().getTimeInMillis() - HCFdate.toCalendarDate().getTimeInMillis()) > MAX_1HR_TIME_INTERVAL)) {
    		HCFdate=null;
    		HCFfile=null;
    	}
    	if (RQIdate !=null && (Math.abs(date.toCalendarDate().getTimeInMillis() - RQIdate.toCalendarDate().getTimeInMillis()) > MAX_2MIN_TIME_INTERVAL)) {
    		RQIdate=null;
    		RQIfile=null;
    	}
    	if (PRECIPRATEdate !=null && (Math.abs(date.toCalendarDate().getTimeInMillis() - PRECIPRATEdate.toCalendarDate().getTimeInMillis()) > MAX_2MIN_TIME_INTERVAL)) {
    		PRECIPRATEdate=null;
    		PRECIPRATEfile=null;
    	}
    	if (MASKdate !=null && (Math.abs(date.toCalendarDate().getTimeInMillis() - MASKdate.toCalendarDate().getTimeInMillis()) > MAX_2MIN_TIME_INTERVAL)) {
    		MASKdate=null;
    		MASKfile=null;
    	}

    	if (RQIfile!=null) {
    		//System.out.println("Reading RQI file " + RQIfile);
    		RQIdata=new MrmsData(rootDirectory + File.separator + yyyyMM + File.separator + RQIfile,MrmsData.FLOAT_TYPE, MIN_LAT,MIN_LON, CELL_SIZE, tmpDir);
    	}
    	else {
 			throw new Exception("Missing RQI file");	
		}
    	if (HCFfile!=null) {
    		//System.out.println("Reading HCF file " + HCFfile);
    		HCFdata=new MrmsData(rootDirectory + File.separator + yyyyMM + File.separator + HCFfile,MrmsData.FLOAT_TYPE, MIN_LAT,MIN_LON, CELL_SIZE, tmpDir);
    	}
    	else {
 			throw new Exception("Missing HCF file");	
		}
    	if (PRECIPRATEfile!=null) {
    		//System.out.println("Reading PRECIPRATE file " + PRECIPRATEfile);
    		PRECIPRATEdata=new MrmsData(rootDirectory + File.separator + yyyyMM + File.separator + PRECIPRATEfile,MrmsData.FLOAT_TYPE, MIN_LAT,MIN_LON, CELL_SIZE, tmpDir);
    	}
    	else {
 			throw new Exception("Missing PRECIPRATE file");	
		}
    	if (MASKfile!=null) {
    		//System.out.println("Reading MASK file " + MASKfile);
    		MASKdata=new MrmsData(rootDirectory + File.separator + yyyyMM + File.separator + MASKfile,MrmsData.INT_TYPE, MIN_LAT,MIN_LON, CELL_SIZE, tmpDir);
    	}
    	else {
 			throw new Exception("Missing MASK file");	
		}
	}
//	public void readFile(String filename)
//	{
//		BufferedReader br = null;
//		String currentLine = null;
//		try
//		{
//			br = new BufferedReader(
//			new FileReader(filename));
//			// read header lines, split on whitespace
//			currentLine = br.readLine();
//			int ncols = Integer.parseInt(currentLine.split("\\s+")[1]);
//			currentLine = br.readLine();
//			int nrows = Integer.parseInt(currentLine.split("\\s+")[1]);
//			
//			while ((currentLine = br.readLine()) != null)
//			{
//				
//				System.out.println(currentLine);
//			}
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
//	public static class MrmsStats
//	{
//		private double meanPRECIPRATE=0;
//		private double meanHCF=0;
//		private double RQIThreshold=1.0;
//		private long count=0;
//		private Map<Integer, Integer> MASKMap = new HashMap<>();
//		
//		public double getMeanPR() {
//			return meanPRECIPRATE;
//		}
//		public double getMeanGuageRatio() {
//			return meanHCF;
//		}
//		public double getRQIThreshold() {
//			return RQIThreshold;
//		}
//		public long getCount() {
//			return count;
//		}
//		
//		private Map<Integer, Integer> getMASKHistogram()
//		{
//			return MASKMap;
//		}
//		
//	}
}
