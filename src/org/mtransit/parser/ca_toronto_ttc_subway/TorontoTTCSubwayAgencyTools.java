package org.mtransit.parser.ca_toronto_ttc_subway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTripStop;
import org.mtransit.parser.CleanUtils;
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
		System.out.printf("\nGenerating TTC subway data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating TTC subway data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
		return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
	}

	private static final Pattern EXTRACT_RLN = Pattern.compile("(line [0-9]{1} \\(([^\\)]*)\\))", Pattern.CASE_INSENSITIVE);
	private static final String EXTRACT_RLN_REPLACEMENT = "$2";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		routeLongName = EXTRACT_RLN.matcher(routeLongName).replaceAll(EXTRACT_RLN_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
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
		int rsn = Integer.parseInt(gRoute.getRouteShortName());
		switch (rsn) {
		// @formatter:off
		case 1: return COLOR_FFC41E;
		case 2: return COLOR_2B720A;
		case 3: return COLOR_0A6797;
		case 4: return COLOR_8B1962;
		// @formatter:on
		default:
			System.out.printf("\nUnexpected route color %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
	}

	private static final String SHEPPARD_YONGE = "Sheppard-Yonge";
	private static final String DON_MILLS = "Don Mills";
	private static final String KIPLING = "Kipling";
	private static final String FINCH = "Finch";
	private static final String DOWNSVIEW = "Downsview";
	private static final String MC_COWAN = "McCowan";
	private static final String KENNEDY = "Kennedy";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1l, new RouteTripSpec(1l, //
				0, MTrip.HEADSIGN_TYPE_STRING, DOWNSVIEW, //
				1, MTrip.HEADSIGN_TYPE_STRING, FINCH) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"14404", // FINCH STATION - SOUTHBOUND PLATFORM
								"14409", // EGLINTON STATION - SOUTHBOUND PLATFORM
								"14410", // !=
								"13143", // !=
								"14411", // ==
								"14420", // UNION STATION - NORTHBOUND PLATFORM towards DOWNSVIEW"
								"14429", // ==
								"13768", // != "ST. CLAIR W POCKET"
								"14430", // !=
								"14434", // WILSON STATION - NORTHBOUND PLATFORM
								"14435" // DOWNSVIEW STATION - SUBWAY PLATFORM
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"14436", // DOWNSVIEW STATION - SOUTHBOUND PLATFORM
								"14437", // WILSON STATION - SOUTHBOUND PLATFORM
								"14440", // GLENCAIRN STATION - SOUTHBOUND PLATFORM
								"14441", // !=
								"13768", // != "ST. CLAIR W POCKET"
								"14442", // == ST CLAIR WEST STATION - SOUTHBOUND PLATFORM
								"14445", // ST GEORGE STATION - SOUTHBOUND PLATFORM
								"14448", // ST PATRICK STATION - SOUTHBOUND PLATFORM
								"14451", // UNION STATION - NORTHBOUND PLATFORM towards FINCH
								"14454", // DUNDAS STATION - NORTHBOUND PLATFORM
								"14457", // BLOOR STATION - NORTHBOUND PLATFORM
								"14460", // != ST CLAIR STATION - NORTHBOUND PLATFORM
								"13143", // != DAVIS BUILD-UP
								"14461", // ==
								"15144", // YORK MILLS POCKET
								"14464", // YORK MILLS STATION - NORTHBOUND PLATFORM
								"14467" // FINCH STATION - SUBWAY PLATFORM
						})) //
				.compileBothTripSort());
		map2.put(
				2l,
				new RouteTripSpec(2l, //
						0, MTrip.HEADSIGN_TYPE_STRING, KENNEDY, //
						1, MTrip.HEADSIGN_TYPE_STRING, KIPLING) //
						.addTripSort(0, //
								Arrays.asList(new String[] { "14468", "14491", "14492", "14498" })) //
						.addTripSort(
								1, //
								Arrays.asList(new String[] { "14499", "14503", "14506", "14509", "14512", "14514", "14517", "14518", "14520", "14522", "14525",
										"14526", "14529" })) //
						.compileBothTripSort());
		map2.put(3l, new RouteTripSpec(3l, //
				0, MTrip.HEADSIGN_TYPE_STRING, KENNEDY, //
				1, MTrip.HEADSIGN_TYPE_STRING, MC_COWAN) //
				.addTripSort(0, //
						Arrays.asList(new String[] { "14541", "14543", "14546" })) //
				.addTripSort(1, //
						Arrays.asList(new String[] { "14547", "14549", "14552" })) //
				.compileBothTripSort());
		map2.put(4l, new RouteTripSpec(4l, //
				0, MTrip.HEADSIGN_TYPE_STRING, DON_MILLS, //
				1, MTrip.HEADSIGN_TYPE_STRING, SHEPPARD_YONGE) //
				.addTripSort(0, //
						Arrays.asList(new String[] { "14530", "14531", "14532", "14533", "14534" })) //
				.addTripSort(1, //
						Arrays.asList(new String[] { "14535", "14537", "14539" })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		System.out.printf("\nUnexpected trip %s!\n", gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

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
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = BOUND.matcher(gStopName).replaceAll(BOUND_REPLACEMENT);
		gStopName = CENTER.matcher(gStopName).replaceAll(CENTER_REPLACEMENT);
		gStopName = PLATFORM.matcher(gStopName).replaceAll(PLATFORM_REPLACEMENT);
		gStopName = STATION.matcher(gStopName).replaceAll(STATION_REPLACEMENT);
		gStopName = SUBWAY.matcher(gStopName).replaceAll(SUBWAY_REPLACEMENT);
		gStopName = TOWARDS.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		if (gStopName.trim().endsWith(DASH)) {
			gStopName = gStopName.substring(0, gStopName.trim().length() - 1);
		}
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
