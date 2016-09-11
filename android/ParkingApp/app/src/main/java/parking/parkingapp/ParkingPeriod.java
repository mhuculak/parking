package parking.parkingapp;

public class ParkingPeriod {

    private CalendarSelector from;
    private CalendarSelector until;

    public ParkingPeriod(CalendarSelector from, CalendarSelector until) {
        this.from = from;
        this.until = until;
    }

    public CalendarSelector getFrom() {
        return from;
    }

    public CalendarSelector getUntil() {
        return until;
    }
}
