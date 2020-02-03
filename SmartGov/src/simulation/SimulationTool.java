package simulation;

import repast.simphony.engine.environment.RunEnvironment;

public class SimulationTool {
	
	public static long BEGIN_SIMULATION_TIME;
	public static long END_SIMULATION_TIME;

	public static void STOP(){
		SimulationTool.END_SIMULATION_TIME = System.currentTimeMillis();
		long millis = END_SIMULATION_TIME - BEGIN_SIMULATION_TIME;
		long second = (millis / 1000) % 60;
		long minute = (millis / (1000 * 60)) % 60;
		long hour = (millis / (1000 * 60 * 60)) % 24;
		millis = millis % 1000;
		System.out.println("Stopping the system after " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount() + " ticks in " + String.format("%02d:%02d:%02d:%d", hour, minute, second, millis)  + " milliseconds.");
		RunEnvironment.getInstance().getCurrentSchedule().executeEndActions();
		RunEnvironment.getInstance().endRun();
	}
	
	public static String showTick(){
		return "[" + RunEnvironment.getInstance().getCurrentSchedule().getTickCount() + "]";
	}
	
}
