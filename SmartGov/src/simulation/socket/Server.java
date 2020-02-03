package simulation.socket;

import java.io.IOException;

/**
 * SmartGov includes a server to manage discussion with external programs. For example, the machine learning part of SmartGov in built in python from extsrc and some classes
 * exploit the result of the learning to manage elements of the simulation.
 * @author Simon
 *
 */
public class Server {

	/**
	 * The server is launched in a terminal in order to display logs. Currently the method has some problems of compatibility with OS like mac and is dependant of one's
	 * configuration.
	 * A bash script is provided in Unix systems to display logs in a terminal rather than in a background process. Is is possible to launch a shell script for windows when 
	 * "server_debug" is set to 1 in the config file.
	 * @param file to launch from the terminal. It is possible to specify parameters.
	 * @param command to use in a terminal.
	 */
	public static void startServer(String file, String command) {
		
		String OS = System.getProperty("os.name").toLowerCase();
		
		try {
			if(OS.indexOf("win")>=0) {
				Runtime.getRuntime().exec("cmd.exe /c start " + command + " " + file);
			} else if(OS.indexOf("mac") >= 0) {
				
			} else if(OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
				Runtime.getRuntime().exec("sh extsrc/server.sh " + String.valueOf(ClientCommunication.port));
			} else {
				System.out.println("Your OS is not supported.");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
