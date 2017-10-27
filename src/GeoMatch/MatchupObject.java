package GeoMatch;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class MatchupObject {
	String filename;
	// Global file attributes
	  String DPR_Version;
	  String DPR_ScanType;
	  String GV_UF_Z_field;
	  String GV_UF_ZDR_field;
	  String GV_UF_KDP_field;
	  String GV_UF_RHOHV_field;
	  String GV_UF_RC_field;
	  String GV_UF_RP_field;
	  String GV_UF_RR_field;
	  String GV_UF_HID_field;
	  String GV_UF_D0_field;
	  String GV_UF_NW_field;
	  String GV_UF_DM_field;
	  String GV_UF_N2_field;
	  String DPR_2ADPR_file;
	  String DPR_2AKU_file;
	  String DPR_2AKA_file;
	  String DPR_2BCMB_file;
	  String GR_file;
	  
	  // dimensions
	  
	  int fpdim;
	  int elevationAngleDim;
	  int xydim;
	  int hidim;
	  int len_atime_ID;
	  int len_site_ID;

	  Variable elevationAngle;
	  Variable numScans;
	  Variable numRays;
	  Variable rangeThreshold;
	  Variable DPR_dBZ_min;
	  Variable GR_dBZ_min;
	  Variable rain_min;
	  Variable DPR_decluttered;
	  Variable have_GR_Z;
	  Variable have_GR_Zdr;
	  Variable have_GR_Kdp;
	  Variable have_GR_RHOhv;
	  Variable have_GR_RC_rainrate;
	  Variable have_GR_RP_rainrate;
	  Variable have_GR_RR_rainrate;
	  Variable have_GR_HID;
	  Variable have_GR_Dzero;
	  Variable have_GR_Nw;
	  Variable have_GR_Dm;
	  Variable have_GR_N2;
	  Variable have_GR_blockage;
	  Variable have_ZFactorMeasured;
	  Variable have_ZFactorCorrected;
	  Variable have_piaFinal;
	  Variable have_paramDSD;
	  Variable have_PrecipRate;
	  Variable have_Epsilon;
	  Variable have_LandSurfaceType;
	  Variable have_PrecipRateSurface;
	  Variable have_SurfPrecipTotRate;
	  Variable have_heightStormTop;
	  Variable have_BBheight;
	  Variable have_BBstatus;
	  Variable have_qualityData;
	  Variable have_FlagPrecip;
	  Variable have_TypePrecip;
	  Variable have_clutterStatus;
	  Variable latitude;
	  Variable longitude;
	  Variable xCorners;
	  Variable yCorners;
	  Variable topHeight;
	  Variable bottomHeight;
	  Variable GR_Z;
	  Variable GR_Z_StdDev;
	  Variable GR_Z_Max;
	  Variable GR_Zdr;
	  Variable GR_Zdr_StdDev;
	  Variable GR_Zdr_Max;
	  Variable GR_Kdp;
	  Variable GR_Kdp_StdDev;
	  Variable GR_Kdp_Max;
	  Variable GR_RHOhv;
	  Variable GR_RHOhv_StdDev;
	  Variable GR_RHOhv_Max;
	  Variable GR_RC_rainrate;
	  Variable GR_RC_rainrate_StdDev;
	  Variable GR_RC_rainrate_Max;
	  Variable GR_RP_rainrate;
	  Variable GR_RP_rainrate_StdDev;
	  Variable GR_RP_rainrate_Max;
	  Variable GR_RR_rainrate;
	  Variable GR_RR_rainrate_StdDev;
	  Variable GR_RR_rainrate_Max;
	  Variable GR_HID;
	  Variable GR_Dzero;
	  Variable GR_Dzero_StdDev;
	  Variable GR_Dzero_Max;
	  Variable GR_Nw;
	  Variable GR_Nw_StdDev;
	  Variable GR_Nw_Max;
	  Variable GR_Dm;
	  Variable GR_Dm_StdDev;
	  Variable GR_Dm_Max;
	  Variable GR_N2;
	  Variable GR_N2_StdDev;
	  Variable GR_N2_Max;
	  Variable GR_blockage;
	  Variable ZFactorMeasured;
	  Variable ZFactorCorrected;
	  Variable PrecipRate;
	  Variable Dm;
	  Variable Nw;
	  Variable Epsilon;
	  Variable clutterStatus;
	  Variable n_gr_z_rejected;
	  Variable n_gr_zdr_rejected;
	  Variable n_gr_kdp_rejected;
	  Variable n_gr_rhohv_rejected;
	  Variable n_gr_rc_rejected;
	  Variable n_gr_rp_rejected;
	  Variable n_gr_rr_rejected;
	  Variable n_gr_hid_rejected;
	  Variable n_gr_dzero_rejected;
	  Variable n_gr_nw_rejected;
	  Variable n_gr_dm_rejected;
	  Variable n_gr_n2_rejected;
	  Variable n_gr_expected;
	  Variable n_dpr_meas_z_rejected;
	  Variable n_dpr_corr_z_rejected;
	  Variable n_dpr_corr_r_rejected;
	  Variable n_dpr_dm_rejected;
	  Variable n_dpr_nw_rejected;
	  Variable n_dpr_epsilon_rejected;
	  Variable n_dpr_expected;
	  Variable DPRlatitude;
	  Variable DPRlongitude;
	  Variable piaFinal;
	  Variable LandSurfaceType;
	  Variable PrecipRateSurface;
	  Variable SurfPrecipTotRate;
	  Variable heightStormTop;
	  Variable BBheight;
	  Variable BBstatus;
	  Variable qualityData;
	  Variable FlagPrecip;
	  Variable TypePrecip;
	  Variable scanNum;
	  Variable rayNum;
	  Variable timeNearestApproach;
	  Variable atimeNearestApproach;
	  Variable timeSweepStart;
	  Variable atimeSweepStart;
	  Variable site_ID;
	  Variable site_lat;
	  Variable site_lon;
	  Variable site_elev;
	  Variable version;
/*
	mKey = fptr.findGlobalAttribute(mGroupName+"_Key").getNumericValue().intValue();
	mType = fptr.findGlobalAttribute(mGroupName+"_Type").getNumericValue().intValue();
//	mUnits = fptr.findGlobalAttribute(mGroupName+"_Units").getNumericValue().intValue();
	mUnits = fptr.findGlobalAttribute(mGroupName+"_Units").getStringValue();
	mNumPix = fptr.findGlobalAttribute(mGroupName+"_NumPix").getNumericValue().intValue();
	mNumLines = fptr.findGlobalAttribute(mGroupName+"_NumLines").getNumericValue().intValue();
	mChannelWaveLength = fptr.findGlobalAttribute(mGroupName+"_ChannelWaveLength").getNumericValue().floatValue();
	mSpatialResolutionX = fptr.findGlobalAttribute(mGroupName+"_SpatialResolutionX").getNumericValue().floatValue();
	mSpatialResolutionY = fptr.findGlobalAttribute(mGroupName+"_SpatialResolutionY").getNumericValue().floatValue();
	mDataByteSize = fptr.findGlobalAttribute(mGroupName+"_DataByteSize").getNumericValue().intValue();
//	mMaxValue = fptr.findGlobalAttribute(mGroupName+"_MaxValue").getNumericValue().intValue();
//	mMinValue = fptr.findGlobalAttribute(mGroupName+"_MinValue").getNumericValue().intValue();
	mMaxValue = fptr.findGlobalAttribute(mGroupName+"_MaxValue").getNumericValue().floatValue();
	mMinValue = fptr.findGlobalAttribute(mGroupName+"_MinValue").getNumericValue().floatValue();
	mChannelLabel = fptr.findGlobalAttribute(mGroupName+"_ChannelLabel").getStringValue();
	mChannelComment = fptr.findGlobalAttribute(mGroupName+"_ChannelComment").getStringValue();
*/
	public MatchupObject(String filename) {
		this.filename=filename;
	}
	public void openReadOnly()
	{
		
	}
	public void readMetadata()
	// read global attributes and dimensions
	{

	}
	public void addGlobalAttribute()
	// add global attributes
	{

	}
	public void readGlobalAttribute()
	// read global attributes
	{

	}
	public void addVariable()
	// read global attributes and dimensions
	{

	}
	public void readVariable()
	// read global attributes and dimensions
	{

	}
	public void saveAs(String filename)
	{
		
	}

}
