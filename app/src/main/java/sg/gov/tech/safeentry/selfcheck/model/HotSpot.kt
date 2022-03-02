package sg.gov.tech.safeentry.selfcheck.model

class HotSpot(val timeWindow: TimeWindow, val location: MatchLocation, val matchId: String?)
class TimeWindow(val start: Long, val end: Long)
