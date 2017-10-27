package Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
	private int mYear, mMonth, mDay, mHour, mMinute, mSecond;
//	private Calendar mCal=null;
	public DateUtil (Date date)
	{
		setTime(date);
	}
	public DateUtil (int date)
	{
		setTime(date);
	}
	public DateUtil (Calendar cal)
	{
		setTime(cal);
	}
	public DateUtil (String textTimestamp) 
	{
		setTime(textTimestamp);
	}
	public int getYear() {
		return mYear;
	}

	public void setYear(int Year) {
		this.mYear = Year;
	}

	public int getMonth() {
		return mMonth;
	}

	public void setMonth(int Month) {
		this.mMonth = Month;
	}

	public int getDay() {
		return mDay;
	}

	public void setDay(int Day) {
		this.mDay = Day;
	}

	public int getHour() {
		return mHour;
	}

	public void setHour(int Hour) {
		this.mHour = Hour;
	}

	public int getMinute() {
		return mMinute;
	}

	public void setMinute(int Minute) {
		this.mMinute = Minute;
	}

	public int getSecond() {
		return mSecond;
	}

	public void setSecond(int Second) {
		this.mSecond = Second;
	}
	public void setTime(Date date)
	{
		Calendar cal=Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.setTime(date);		
		setTime(cal);
		
	}
	public void setTime(Calendar cal)
	{			
		mYear  = cal.get(Calendar.YEAR);
		mMonth = cal.get(Calendar.MONTH)+1;
		mDay   = cal.get(Calendar.DAY_OF_MONTH);
		mHour   = cal.get(Calendar.HOUR_OF_DAY);
		mMinute   = cal.get(Calendar.MINUTE);
		mSecond   = cal.get(Calendar.SECOND);
	}
    
    public void setTime(String textDate)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        Date date=null;
        
		try {
			date = sdf.parse(textDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 
        setTime(date);
    }
    public void setTime(int dateInt)
    {
    	String dateStr = Integer.toString(dateInt);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        Date date=null;
        
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 
        setTime(date);
    }
    public void setTimeHr(int dateInt)
    {
    	String dateStr = Integer.toString(dateInt);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhh");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        Date date=null;
        
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 
        setTime(date);
    }
	public String toTextDate() {
		return formatTextDate(mYear,mMonth,mDay,mHour,mMinute,mSecond);
	}
	public String toPackageDate() {
		return String.format("%04d%02d%02d", mYear, mMonth, mDay);

	}
    public Calendar toCalendarDate()
    {
    		 
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
       
		cal.set(Calendar.YEAR, mYear);
		cal.set(Calendar.MONTH, mMonth-1);
		cal.set(Calendar.DAY_OF_MONTH, mDay);
		cal.set(Calendar.HOUR_OF_DAY, mHour);
		cal.set(Calendar.MINUTE, mMinute);
		cal.set(Calendar.SECOND, mSecond);
        
//        System.out.println("Text date: "  + toTextDate());
//        System.out.println("calendar date: " + cal.toString());
        
        return cal;
    }

    
    
	public static String formatTextDate(int year, int month, int day, int hour, int minute, int second) {
//		return String.format("%04d", year) + "-" + String.format("%02d", month) + "-" + String.format("%02d", day) + "T" + String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second) + "Z";
		return String.format("%04d", year) + "-" + String.format("%02d", month) + "-" + String.format("%02d", day) + " " + String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second);

	}
	public static String toTextDate(Calendar cal)
	{
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		String sDate = formatTextDate(year, month, day, hour, minute, second);
		
//		System.out.println("date: " + sDate);
		return sDate;
		
	}
    public static Calendar toCalendarDate(String textDate)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        Date date=null;
        
		try {
			date = sdf.parse(textDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        
//        System.out.println("Text date: "  + textDate);
//        System.out.println("calendar date: " + calendar.toString());
        
        return calendar;
    }

    
}
