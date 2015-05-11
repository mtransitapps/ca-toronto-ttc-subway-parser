package org.mtransit.parser.ca_toronto_ttc_subway;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www1.toronto.ca/wps/portal/contentonly?vgnextoid=96f236899e02b210VgnVCM1000003dd60f89RCRD
// http://opendata.toronto.ca/TTC/routes/OpenData_TTC_Schedules.zip
public class TorontoTTCSubwayAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-toronto-ttc-subway-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new TorontoTTCSubwayAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("Generating TTC subway data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating TTC subway data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_SUBWAY;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.route_short_name); // using route short name as route ID
	}

	private static final String SHEPPARD = "Sheppard";
	private static final String SCARBOROUGH = "Scarborough";
	private static final String BLOOR_DANFORTH = "Bloor-Danforth";
	private static final String YONGE_UNIVERSITY = "Yonge-University";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		int rsn = Integer.parseInt(gRoute.route_short_name);
		switch (rsn) {
		// @formatter:off
		case 1: return YONGE_UNIVERSITY;
		case 2: return BLOOR_DANFORTH;
		case 3: return SCARBOROUGH;
		case 4: return SHEPPARD;
		// @formatter:on
		default:
			System.out.println("Unexpected route long name " + gRoute);
			System.exit(-1);
			return null;
		}
	}

	private static final String AGENCY_COLOR = "B80000"; // RED (AGENCY WEB SITE CSS)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_FFC41E = "FFC41E"; // 1 - Yellow (web site CSS)
	private static final String COLOR_2B720A = "2B720A"; // 2 - Green (web site CSS)
	private static final String COLOR_0A6797 = "0A6797"; // 3 - Blue (web site CSS)
	private static final String COLOR_8B1962 = "8B1962"; // 4 - Violet (web site CSS)

	@Override
	public String getRouteColor(GRoute gRoute) {
		int rsn = Integer.parseInt(gRoute.route_short_name);
		switch (rsn) {
		// @formatter:off
		case 1: return COLOR_FFC41E;
		case 2: return COLOR_2B720A;
		case 3: return COLOR_0A6797;
		case 4: return COLOR_8B1962;
		// @formatter:on
		default:
			System.out.println("Unexpected route color " + gRoute);
			System.exit(-1);
			return null;
		}
	}

	private static final String SHEPPARD_YONGE = "Sheppard-Yonge";
	private static final String DON_MILLS = "Don Mills";
	private static final String KIPLING = "Kipling";
	private static final String UNION = "Finch";
	private static final String DOWNSVIEW = "Downsview";
	private static final String MC_COWAN = "McCowan";
	private static final String KENNEDY = "Kennedy";

	@Override
	public void setTripHeadsign(MRoute route, MTrip mTrip, GTrip gTrip) {
		if (route.id == 1l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(DOWNSVIEW, 0);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(FINCH, 1);
				return;
			}
		} else if (route.id == 2l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(KENNEDY, 0);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(KIPLING, 1);
				return;
			}
		} else if (route.id == 3l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(KENNEDY, 0);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(MC_COWAN, 1);
				return;
			}
		} else if (route.id == 4l) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(DON_MILLS, 0);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(SHEPPARD_YONGE, 1);
				return;
			}
		}
		int directionId = gTrip.direction_id;
		String stationName = cleanTripHeadsign(gTrip.trip_headsign);
		mTrip.setHeadsignString(stationName, directionId);
		System.out.println("Unexpected trip " + gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return MSpec.cleanLabel(tripHeadsign);
	}

	private static final Pattern AT = Pattern.compile("( at )", Pattern.CASE_INSENSITIVE);
	private static final String AT_REPLACEMENT = " / ";

	private static final Pattern PLATFORM = Pattern.compile("(^|\\s){1}(platform)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String PLATFORM_REPLACEMENT = " ";

	private static final Pattern STATION = Pattern.compile("(^|\\s){1}(station)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = " ";

	private static final Pattern SUBWAY = Pattern.compile("(^|\\s){1}(subway)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String SUBWAY_REPLACEMENT = " ";

	private static final Pattern BOUND = Pattern.compile("(^|\\s){1}(eastbound|westbound|northbound|southbound)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String BOUND_REPLACEMENT = " ";

	private static final Pattern CENTER = Pattern.compile("(cent(er|re))", Pattern.CASE_INSENSITIVE);
	private static final String CENTER_REPLACEMENT = "Ctr";

	private static final String DASH = "-";

	private static final Pattern TOWARDS = Pattern.compile("(- towards .*)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = AT.matcher(gStopName).replaceAll(AT_REPLACEMENT);
		gStopName = BOUND.matcher(gStopName).replaceAll(BOUND_REPLACEMENT);
		gStopName = CENTER.matcher(gStopName).replaceAll(CENTER_REPLACEMENT);
		gStopName = PLATFORM.matcher(gStopName).replaceAll(PLATFORM_REPLACEMENT);
		gStopName = STATION.matcher(gStopName).replaceAll(STATION_REPLACEMENT);
		gStopName = SUBWAY.matcher(gStopName).replaceAll(SUBWAY_REPLACEMENT);
		gStopName = TOWARDS.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		if (gStopName.trim().endsWith(DASH)) {
			gStopName = gStopName.substring(0, gStopName.trim().length() - 1);
		}
		gStopName = MSpec.cleanStreetTypes(gStopName);
		gStopName = MSpec.cleanNumbers(gStopName);
		return MSpec.cleanLabel(gStopName);
	}
}
