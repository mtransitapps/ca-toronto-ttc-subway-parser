package org.mtransit.parser.ca_toronto_ttc_subway;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

// https://open.toronto.ca/dataset/ttc-routes-and-schedules/
// OLD: http://opendata.toronto.ca/TTC/routes/OpenData_TTC_Schedules.zip
// http://opendata.toronto.ca/toronto.transit.commission/ttc-routes-and-schedules/OpenData_TTC_Schedules.zip
public class TorontoTTCSubwayAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-toronto-ttc-subway-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new TorontoTTCSubwayAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating TTC subway data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating TTC subway data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		return super.excludeRoute(gRoute);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_SUBWAY;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
	}

	private static final Pattern EXTRACT_RLN = Pattern.compile("(line [0-9] \\(([^)]*)\\))", Pattern.CASE_INSENSITIVE);
	private static final String EXTRACT_RLN_REPLACEMENT = "$2";

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		routeLongName = EXTRACT_RLN.matcher(routeLongName).replaceAll(EXTRACT_RLN_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "B80000"; // RED (AGENCY WEB SITE CSS)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_FFC41E = "FFC41E"; // 1 - Yellow (web site CSS)
	private static final String COLOR_2B720A = "2B720A"; // 2 - Green (web site CSS)
	private static final String COLOR_0A6797 = "0A6797"; // 3 - Blue (web site CSS)
	private static final String COLOR_8B1962 = "8B1962"; // 4 - Violet (web site CSS)

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		int rsn = Integer.parseInt(gRoute.getRouteShortName());
		switch (rsn) {
		// @formatter:off
		case 1: return COLOR_FFC41E;
		case 2: return COLOR_2B720A;
		case 3: return COLOR_0A6797;
		case 4: return COLOR_8B1962;
		// @formatter:on
		default:
			throw new MTLog.Fatal("Unexpected route color %s!", gRoute);
		}
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("%s: Using direction finder to merge %s and %s!", mTrip.getRouteId(), mTrip, mTripToMerge);
	}

	private static final Pattern STARTS_WITH_TOWARDS_ = Pattern.compile("((^|^.* )towards )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = STARTS_WITH_TOWARDS_.matcher(tripHeadsign).replaceAll(Constants.EMPTY);
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = STATION.matcher(tripHeadsign).replaceAll(STATION_REPLACEMENT);
		tripHeadsign = CleanUtils.fixMcXCase(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern PLATFORM = Pattern.compile("(^|\\s)(platform)($|\\s)", Pattern.CASE_INSENSITIVE);
	private static final String PLATFORM_REPLACEMENT = " ";

	private static final Pattern STATION = Pattern.compile("(^|\\s)(station)($|\\s)", Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = " ";

	private static final Pattern SUBWAY = Pattern.compile("(^|\\s)(subway)($|\\s)", Pattern.CASE_INSENSITIVE);
	private static final String SUBWAY_REPLACEMENT = " ";

	private static final Pattern BOUND = Pattern.compile("(^|\\s)(eastbound|eb|westbound|wb|northbound|nb|southbound|sb)($|\\s)",
			Pattern.CASE_INSENSITIVE);
	private static final String BOUND_REPLACEMENT = " ";

	private static final Pattern CENTER = Pattern.compile("(cent(er|re))", Pattern.CASE_INSENSITIVE);
	private static final String CENTER_REPLACEMENT = "Ctr";

	private static final Pattern ENDS_WITH_DASH_ = Pattern.compile("(( )?-( )?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_TOWARDS_ = Pattern.compile("( towards .*$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = ENDS_WITH_TOWARDS_.matcher(gStopName).replaceAll(Constants.EMPTY);
		if (Utils.isUppercaseOnly(gStopName, true, true)) {
			gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		}
		gStopName = BOUND.matcher(gStopName).replaceAll(BOUND_REPLACEMENT);
		gStopName = CENTER.matcher(gStopName).replaceAll(CENTER_REPLACEMENT);
		gStopName = PLATFORM.matcher(gStopName).replaceAll(PLATFORM_REPLACEMENT);
		gStopName = STATION.matcher(gStopName).replaceAll(STATION_REPLACEMENT);
		gStopName = SUBWAY.matcher(gStopName).replaceAll(SUBWAY_REPLACEMENT);
		gStopName = ENDS_WITH_DASH_.matcher(gStopName).replaceAll(Constants.EMPTY);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.fixMcXCase(gStopName);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
