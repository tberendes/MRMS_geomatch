package GeoMatch;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.io.Files;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import Util.TempFile;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayString;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

public class MrmsGeoMatch {

	static int [] HISTOGRAM_INDICES = {-1,0,1,2,3,4,6,7,91,96,10};
	static String [] HISTOGRAM_CODES = {"missing","no precip", "warm stratiform rain"
			,"warm stratiform rain at ground, radar data in or above melting layer"
			,"snow", "snow at ground, radar data 1.5km or higher above ground"
			,"convective", "hail", "tropical/stratiform rain mix"
			,"tropical/convecive rain mix", "cool stratiform rain"};
	
	Map<Integer, String> maskLabels = new HashMap<>();
	double DPR_FOOTPRINT = 5.2;
	String vnInputDir;
	String vnOutputDir;
	String mrmsPath;
	public MrmsGeoMatch(String InputDir, String OutputDir, String mrmsPath)
	{
		this.mrmsPath = mrmsPath;
		this.vnInputDir = InputDir;
		this.vnOutputDir = OutputDir;
    	// initialize histogram labels
    	for (int ind1=0;ind1<HISTOGRAM_INDICES.length;ind1++)
    		maskLabels.put(HISTOGRAM_INDICES[ind1], HISTOGRAM_CODES[ind1]);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		if (args.length<3) {
			System.out.println("Usage:  java -jar MrmsGeoMatch input_VN_directory output_VN_directory MRMS_root_directory ");
			System.out.println("  Matches MRMS data with GPM VN matchup files and creates new netCDF files");
			System.exit(-1);
		}
		String vnInputDir = args[0];
		String vnOutputDir = args[1];
		String mrmsPath = args[2];
		
//		String vnInputDir = "C:\\Users\\tberendes\\gpmgv\\input_dir";
//		String vnOutputDir = "C:\\Users\\tberendes\\gpmgv\\output_dir";
//		String mrmsPath = "C:\\Users\\tberendes\\gpmgv\\MRMS\\level2";

		if (vnInputDir.equals(vnOutputDir)) {
			System.out.println("Error: output directory must be different from input directory");
			System.exit(-1);
		}
		
		File outDir = new File (vnOutputDir);
		if (outDir.exists()&& outDir.isDirectory()) {
			System.out.println("Output directory " + vnOutputDir + " exists");
			
		}
		else if (outDir.isFile()){
			System.out.println("Error:  Output directory path " + vnOutputDir + " is a file not a directory");
			System.exit(-1);
		}
		else {
			System.out.println("Creating output directory " + vnOutputDir);		
			outDir.mkdirs();
		}
		if (!outDir.exists()) {
			System.out.println("Error: could not create output directory " + vnOutputDir);
			System.exit(-1);
		}
		
		MrmsGeoMatch mrmsGeo = new MrmsGeoMatch(vnInputDir,vnOutputDir,mrmsPath);
		

		mrmsGeo.processDirectory();
		
	}
	void processDirectory()
	{
//		String vnInputFilename = "C:\\Users\\tberendes\\gpmgv\\GPM_matchups\\GRtoDPR.KHTX.160626.13215.ITE114.DPR.NS.1_21.15dbzGRDPR_newDm.nc.gz";
//		String vnOutputFilename = "C:\\Users\\tberendes\\gpmgv\\GPM_matchups\\GRtoDPR.KHTX.160626.13215.ITE114.DPR.NS.1_21.15dbzGRDPR_newDm_mrms.nc";
//		processFile(vnInputFilename, vnOutputFilename);
		
		File dsFile = new File (vnInputDir);
    	File [] dirListing = dsFile.listFiles();
    	if (dirListing==null || dirListing.length==0) {  // no dates are processed
    		try {
				System.out.println("no files to process in " + dsFile.getCanonicalPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		return;
    	}
		// loop over files in given directory
    	for (int ind1=0;ind1<dirListing.length;ind1++) {
    		// only process files, skip directories
    		if (dirListing[ind1].exists()&&dirListing[ind1].isFile()) {
        		String filename = dirListing[ind1].getName();
//        		System.out.println("filename "+ filename);
        		String [ ] arrStr = filename.split("\\.");
        		        		
        		// check for each file type
    			if (filename.endsWith(".nc.gz") || filename.endsWith(".nc")) {
    				String vnInputFilename = vnInputDir + File.separator + filename;
    				String vnOutputFilename = vnOutputDir + File.separator + filename;
    				processFile(vnInputFilename, vnOutputFilename);
    			}
    		}
    	}
	
	}
	
	void processFile(String vnInputFilename, String vnOutputFilename)
	{
		NetcdfFileWriter mFptr=null;
		String gpmTime=null;
		float siteLat=0.0f;
		float siteLon=0.0f;
		String filename;
		TempFile temp=null;
		Array dprLatitude=null, dprLongitude=null;
		Dimension fpdim=null;
		Mrms mrms=null;
		try {
			temp = new TempFile(vnInputFilename);
			filename = temp.getTempFilename();
			if (vnInputFilename.endsWith(".gz")) {
//				System.out.println("temporary file: "+ filename);
				temp.unzip();
			}
			else {
				temp.copy();
			}
			
			mFptr = NetcdfFileWriter.openExisting(filename);
			Variable var = mFptr.findVariable("atimeNearestApproach");
			if (var==null) {
				System.err.println("cannot find variable atimeNearestApproach");
				throw new IOException();			
			}
			int[] shape = var.getShape(); // array dimensions
			gpmTime = var.readScalarString();
			System.out.println("gpmTime: " + gpmTime);
//			Array arr = var.read();
			//ByteBuffer buff = arr.getDataAsByteBuffer();

			fpdim = mFptr.getNetcdfFile().findDimension("fpdim");
			var = mFptr.findVariable("site_lat");
			if (var==null) {
				System.err.println("cannot find variable site_lat");
				throw new IOException();			
			}
			siteLat = var.readScalarFloat();
			System.out.println("site_lat: " + siteLat);
			var = mFptr.findVariable("site_lon");
			if (var==null) {
				System.err.println("cannot find variable site_lon");
				throw new IOException();			
			}
			siteLon = var.readScalarFloat();
			System.out.println("site_lat: " + siteLon);

			var = mFptr.findVariable("DPRlatitude");
			if (var==null) {
				System.err.println("cannot find variable DPRlatitude");
				throw new IOException();			
			}
			dprLatitude = var.read();
			long numFp = dprLatitude.getSize();
			var = mFptr.findVariable("DPRlongitude");
			if (var==null) {
				System.err.println("cannot find variable DPRlongitude");
				throw new IOException();			
			}
			dprLongitude = var.read();

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			mrms = new Mrms(gpmTime, mrmsPath, siteLat, siteLon);
	    	
	    	// enter define mode 
			mFptr.setRedefineMode(true);
			mFptr.flush();

	    	// add new dimension and variables to netcdf file
			Dimension mrmsMaskDim = mFptr.addDimension(null, "mrms_mask", 11);

//			Variable maskLabels = mFptr.addVariable(null, "MRMSMaskLabels", DataType.STRING, "mrms_mask");
//			ArrayString labels = new ArrayString.D1(mrmsMaskDim.getLength());
//			Index index=labels.getIndex();
//			int ind1=0;
//			for (String str:HISTOGRAM_CODES) {
//				labels.set(index.set(ind1++), str);
//			}
//			int[] origin = new int[2];
//			// write out variable
//			mFptr.write( maskLabels, origin, labels);
			
			String MRMSmaskStr="";
			for (int ind:HISTOGRAM_INDICES) {
				MRMSmaskStr = MRMSmaskStr + ind + "-" + maskLabels.get(ind) + "; ";
			}
			mFptr.addGroupAttribute(null, new Attribute("MRMS Mask categories", MRMSmaskStr));
			 
			// add new variables to the temporary file
			Variable precipMeanLowVar = mFptr.addVariable(null, "PrecipMeanLow", DataType.FLOAT, "fpdim");
			precipMeanLowVar.addAttribute(new Attribute("units", "mm/hr"));
			precipMeanLowVar.addAttribute(new Attribute("description", "MRMS average precip rate with RQI >= .5"));
			Variable precipMeanMedVar = mFptr.addVariable(null, "PrecipMeanMed", DataType.FLOAT, "fpdim");
			precipMeanMedVar.addAttribute(new Attribute("units", "mm/hr"));
			precipMeanMedVar.addAttribute(new Attribute("description", "MRMS average precip rate with RQI >= .75"));
			Variable precipMeanHighVar = mFptr.addVariable(null, "PrecipMeanHigh", DataType.FLOAT, "fpdim");
			precipMeanHighVar.addAttribute(new Attribute("units", "mm/hr"));
			precipMeanHighVar.addAttribute(new Attribute("description", "MRMS average precip rate with RQI >= .9"));
			Variable precipMeanVeryHighVar = mFptr.addVariable(null, "PrecipMeanVeryHigh", DataType.FLOAT, "fpdim");
			precipMeanVeryHighVar.addAttribute(new Attribute("units", "mm/hr"));
			precipMeanVeryHighVar.addAttribute(new Attribute("description", "MRMS average precip rate with RQI >= .99"));

			Variable guageRatioMeanLowVar = mFptr.addVariable(null, "GuageRatioMeanLow", DataType.FLOAT, "fpdim");
			guageRatioMeanLowVar.addAttribute(new Attribute("units", "mm/hr"));
			guageRatioMeanLowVar.addAttribute(new Attribute("description", "MRMS average guageRatio rate with RQI >= .5"));
			Variable guageRatioMeanMedVar = mFptr.addVariable(null, "GuageRatioMeanMed", DataType.FLOAT, "fpdim");
			guageRatioMeanMedVar.addAttribute(new Attribute("units", "mm/hr"));
			guageRatioMeanMedVar.addAttribute(new Attribute("description", "MRMS average guageRatio rate with RQI >= .75"));
			Variable guageRatioMeanHighVar = mFptr.addVariable(null, "GuageRatioMeanHigh", DataType.FLOAT, "fpdim");
			guageRatioMeanHighVar.addAttribute(new Attribute("units", "mm/hr"));
			guageRatioMeanHighVar.addAttribute(new Attribute("description", "MRMS average guageRatio rate with with RQI >= .9"));
			Variable guageRatioMeanVeryHighVar = mFptr.addVariable(null, "GuageRatioMeanVeryHigh", DataType.FLOAT, "fpdim");
			guageRatioMeanVeryHighVar.addAttribute(new Attribute("units", "mm/hr"));
			guageRatioMeanVeryHighVar.addAttribute(new Attribute("description", "MRMS average guageRatio rate with RQI >= .99"));

		    List<Dimension> dims = new ArrayList<Dimension>();
		    dims.add(fpdim);
		    dims.add(mrmsMaskDim);

		    Variable maskLowVar = mFptr.addVariable(null, "MaskLow", DataType.FLOAT, dims);
			maskLowVar.addAttribute(new Attribute("units", "none"));
			maskLowVar.addAttribute(new Attribute("description", "MRMS categories with RQI >= .5"));
			Variable maskMedVar = mFptr.addVariable(null, "MaskMed", DataType.FLOAT, dims);
			maskMedVar.addAttribute(new Attribute("units", "none"));
			maskMedVar.addAttribute(new Attribute("description", "MRMS categories with RQI >= .75"));
			Variable maskHighVar = mFptr.addVariable(null, "MaskHigh", DataType.FLOAT, dims);
			maskHighVar.addAttribute(new Attribute("units", "none"));
			maskHighVar.addAttribute(new Attribute("description", "MRMS categories RQI with RQI >= .9"));
			Variable maskVeryHighVar = mFptr.addVariable(null, "MaskVeryHigh", DataType.FLOAT, dims);
			maskVeryHighVar.addAttribute(new Attribute("units", "none"));
			maskVeryHighVar.addAttribute(new Attribute("description", "MRMS categories with RQI >= .99"));

		
	    	// leave define mode 
			mFptr.flush();
			mFptr.setRedefineMode(false);
			mFptr.flush();
		
			// allocate arrays for variables
			ArrayFloat precipMeanLowArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat precipMeanMedArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat precipMeanHighArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat precipMeanVeryHighArr = new ArrayFloat.D1(fpdim.getLength());
			
			ArrayFloat guageRatioMeanLowArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat guageRatioMeanMedArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat guageRatioMeanHighArr = new ArrayFloat.D1(fpdim.getLength());
			ArrayFloat guageRatioMeanVeryHighArr = new ArrayFloat.D1(fpdim.getLength());
			
			int [] shape = maskLowVar.getShape();
			ArrayInt maskLowArr = new ArrayInt.D2(shape[0], shape[1]);
			ArrayInt maskMedArr = new ArrayInt.D2(shape[0], shape[1]);
			ArrayInt maskHighArr = new ArrayInt.D2(shape[0], shape[1]);
			ArrayInt maskVeryHighArr = new ArrayInt.D2(shape[0], shape[1]);
			
			// loop through DPR footprints
			for (int ind1=0;ind1<fpdim.getLength();ind1++) {
				LatLng latlon = new LatLng(dprLatitude.getDouble(ind1),dprLongitude.getDouble(ind1));
				// assume all MRMS data files are at same resolution and same area coverage
				ArrayList<Float> RQIArray = mrms.getRQIdata().getSiteRadiusValuesFloat(latlon, DPR_FOOTPRINT/2.0);
				ArrayList<Float> PRECIPArray = mrms.getPRECIPRATEdata().getSiteRadiusValuesFloat(latlon, DPR_FOOTPRINT/2.0);
//				System.out.println("precipArray " + PRECIPArray.size() + " " + PRECIPArray);
				ArrayList<Float> HCFArray = mrms.getHCFdata().getSiteRadiusValuesFloat(latlon, DPR_FOOTPRINT/2.0);
				
				// include negative values in MASK
				ArrayList<Integer> MASKArray = mrms.getMASKdata().getSiteRadiusValuesInt(latlon, DPR_FOOTPRINT/2.0);
			// define four RQI bins 0-.5, .51-.75, .76-.99, 1.0
				
				DescriptiveStatistics precipLow=filteredStats(PRECIPArray,RQIArray,0.5,100.0);
				DescriptiveStatistics precipMed=filteredStats(PRECIPArray,RQIArray,0.75,100.0);
				DescriptiveStatistics precipHigh=filteredStats(PRECIPArray,RQIArray,0.9,100.0);
				DescriptiveStatistics precipVeryHigh=filteredStats(PRECIPArray,RQIArray,0.99,100);
				
				// compute mean, precip rate, HCF (gauge ratios)
				DescriptiveStatistics guageRatioLow=filteredStats(HCFArray,RQIArray,0.5,100.0);
				DescriptiveStatistics guageRatioMed=filteredStats(HCFArray,RQIArray,0.75,100.0);
				DescriptiveStatistics guageRatioHigh=filteredStats(HCFArray,RQIArray,0.9,100.0);
				DescriptiveStatistics guageRatioVeryHigh=filteredStats(HCFArray,RQIArray,0.99,100.0);
				
				// compute histogram of mask values
				Map<Integer, Integer> maskMapLow = maskHistogram(MASKArray,RQIArray,0.5,100.0);
				Map<Integer, Integer> maskMapMed=maskHistogram(MASKArray,RQIArray,0.75,100.0);
				Map<Integer, Integer> maskMapHigh=maskHistogram(MASKArray,RQIArray,0.9,100.0);
				Map<Integer, Integer> maskMapVeryHigh=maskHistogram(MASKArray,RQIArray,0.99,100.0);
				
				Set<Integer> keyset = maskMapLow.keySet();
				int ind2=0;
				Index index;

				for (Integer key:keyset){
					index=maskLowArr.getIndex();
					maskLowArr.setInt(index.set(ind1,ind2), maskMapLow.get(key));
					maskMedArr.setInt(index.set(ind1,ind2), maskMapMed.get(key));
					maskHighArr.setInt(index.set(ind1,ind2), maskMapHigh.get(key));
					maskVeryHighArr.setInt(index.set(ind1,ind2), maskMapVeryHigh.get(key));
					ind2++;
				}
				
				index=precipMeanLowArr.getIndex();

				precipMeanLowArr.setFloat(index.set(ind1), precipLow==null?-999.0f:(float)precipLow.getMean());
				precipMeanMedArr.setFloat(index.set(ind1), precipMed==null?-999.0f:(float)precipMed.getMean());
				precipMeanHighArr.setFloat(index.set(ind1), precipHigh==null?-999.0f:(float)precipHigh.getMean());
				precipMeanVeryHighArr.setFloat(index.set(ind1), precipVeryHigh==null?-999.0f:(float)precipVeryHigh.getMean());
								
				guageRatioMeanLowArr.setFloat(index.set(ind1), guageRatioLow==null?-999.0f:(float)guageRatioLow.getMean());
				guageRatioMeanMedArr.setFloat(index.set(ind1), guageRatioMed==null?-999.0f:(float)guageRatioMed.getMean());
				guageRatioMeanHighArr.setFloat(index.set(ind1), guageRatioHigh==null?-999.0f:(float)guageRatioHigh.getMean());
				guageRatioMeanVeryHighArr.setFloat(index.set(ind1), guageRatioVeryHigh==null?-999.0f:(float)guageRatioVeryHigh.getMean());
			
				
			}
			// write vars
			mFptr.write(precipMeanLowVar, precipMeanLowArr);
			mFptr.write(precipMeanMedVar, precipMeanMedArr);
			mFptr.write(precipMeanHighVar, precipMeanHighArr);
			mFptr.write(precipMeanVeryHighVar, precipMeanVeryHighArr);
			
			mFptr.write(guageRatioMeanLowVar, guageRatioMeanLowArr);
			mFptr.write(guageRatioMeanMedVar, guageRatioMeanMedArr);
			mFptr.write(guageRatioMeanHighVar, guageRatioMeanHighArr);
			mFptr.write(guageRatioMeanVeryHighVar, guageRatioMeanVeryHighArr);

			mFptr.write(maskLowVar, maskLowArr);
			mFptr.write(maskMedVar, maskMedArr);
			mFptr.write(maskHighVar, maskHighArr);
			mFptr.write(maskVeryHighVar, maskVeryHighArr);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (mrms==null) {
				System.out.println("No matching MRMS data, skipping file " + vnInputFilename);
			}
			else
				e.printStackTrace();
		} finally {
			try {
				mFptr.close();
				
				if (mrms!=null) {
					// need to save modified temp files to output path 
		//			java.nio.file.Files.copy(java.nio.file.Paths.get(temp.getTempFilename()), java.nio.file.Paths.get(vnOutputFilename),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					//or compress..
					//temp.zip(temp.getTempFilename(),vnOutputFilename);
					// remove temp files
		//			temp.deleteTemp();
	
					if (vnOutputFilename.endsWith(".gz")) {
						temp.zip(temp.getTempFilename(),vnOutputFilename);
					}
					else {
						java.nio.file.Files.copy(java.nio.file.Paths.get(temp.getTempFilename()), java.nio.file.Paths.get(vnOutputFilename),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
					System.out.println("Finished file " + vnInputFilename);
				}
				
				temp.deleteTemp();				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		
	}
	Map<Integer, Integer> maskHistogram(ArrayList<Integer> value, ArrayList<Float> filter, double min, double max) {
		
		Map<Integer, Integer> maskMap = new HashMap<>();
		// initialze histogram bins to zero
		for (int ind1=0;ind1<HISTOGRAM_INDICES.length;ind1++) {
			maskMap.put(HISTOGRAM_INDICES[ind1], 0);
		}
		for (int ind1=0;ind1<value.size();ind1++){
//			if (filter.get(ind1)>=min&&filter.get(ind1)<max&&value.get(ind1)>=0) {
			if (filter.get(ind1)>=min&&filter.get(ind1)<max) {
				int val = value.get(ind1);
				if (val<0) val=-1;
				Integer cnt=maskMap.get(val);
				cnt++;
				maskMap.put(val,cnt);
			}
//			else {
//				System.out.println("filter val " + filter.get(ind1));
//			}
		}
		return maskMap;
	}
	DescriptiveStatistics filteredStats(ArrayList<Float> value, ArrayList<Float> filter, double min, double max) 
	{
		if (value.size()==0) {
//			System.out.println("empty arraylist");
			return null;
		}
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int ind1=0;ind1<value.size();ind1++){
			if (filter.get(ind1)>=min&&filter.get(ind1)<max&&value.get(ind1)>=0) {
				stats.addValue(value.get(ind1));
	//			System.out.println("filter " + filter.get(ind1));
			}
//			else {
//				System.out.println("filter val " + filter.get(ind1) + " value " + value.get(ind1));
//			}
		}
		if (stats.getN()==0)
			return null;
		else
			return stats;
		
		// Compute some statistics
//		double mean = stats.getMean();
//		double std = stats.getStandardDeviation();
//		double median = stats.getPercentile(50);
//		int count = (int) stats.getN();
		
	}
}
