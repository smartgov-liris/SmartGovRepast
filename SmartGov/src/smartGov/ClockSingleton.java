package smartGov;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import repast.simphony.engine.schedule.ScheduledMethod;
import simulation.gui.UserPanel;

/**
 * Repast Simphony works with internal ticks of simulation. The clock is built on top of this and allows for specific time management. 
 * Only one clock is available at any time with getInstance() method. It is possible to specify how the clock behaves in regard to internal tick of simulation.
 * Agents uses clock time in their behaviors.
 * @author Simon
 *
 */
public final class ClockSingleton {

	private int timeTick = 1; //Increment of the clock per tick in second
	private static ClockSingleton instance = null;
	private int localTime = 0;
	private DateTime dateTime;
	Locale locale = Locale.FRANCE;
	DateTimeFormatter formatterOutput = DateTimeFormat.forPattern("E").withLocale(locale);

	Map<DateTime, Double> agentsMoving = new LinkedHashMap <DateTime, Double>();

	private ClockSingleton(){
		this.dateTime = new DateTime("2016-01-04T00:00:01.000");
		this.localTime = getLocalTime();
	}

	private ClockSingleton(DateTime dateTime){
		this.dateTime = dateTime;
		dateTime2LocalTime();
	}

	public static synchronized ClockSingleton getInstance(){
		if(instance == null){
			instance = new ClockSingleton();
		}
		return instance;
	}

	/**
	 * This should only be called by specific purpose agent
	 * @param dateTime specific start of the simulation
	 * @return
	 */
	public static synchronized ClockSingleton getInstance(DateTime dateTime){
		if(instance == null){
			instance = new ClockSingleton(dateTime);
		}
		return instance;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void live(){
		ClockSingleton.getInstance().setDateTime(ClockSingleton.getInstance().getDateTime().plusSeconds(timeTick));
		ClockSingleton.getInstance().setLocalTime(timeTick);
		UserPanel.day.setText(ClockSingleton.getInstance().displayDay());
		UserPanel.clockDisplay.setText(ClockSingleton.getInstance().displayTime());
	}

	private void setLocalTime(int timeIncrement) {
		this.localTime += timeIncrement;
	}

	public DateTime getDateTime() {
		return dateTime;
	}

	public String displayTime(){
		return new String(this.dateTime.getHourOfDay() + "h "+ this.dateTime.getMinuteOfHour() + "min " + this.dateTime.getSecondOfMinute() +"s.");
	}

	public String displayDay(){
		return formatterOutput.print(ClockSingleton.getInstance().getDateTime());
	}

	public void dateTime2LocalTime(){
		this.localTime = ClockSingleton.getInstance().getDateTime().getHourOfDay()*60 + 
				ClockSingleton.getInstance().getDateTime().getMinuteOfHour();
	}

	public int getLocalTime() {
		return this.localTime;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	public void setTimeTick(int timeTick) {
		this.timeTick = timeTick;
	}

	public static void resetSingleton(){
		if(instance != null){
			instance = null;
		}
	}

	public void setDateTime(DateTime dateTime){
		this.dateTime = dateTime;
	}

}
