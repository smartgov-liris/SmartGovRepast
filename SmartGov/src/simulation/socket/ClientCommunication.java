package simulation.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Enable communication with a server launch on a specific port. 
 * @author Simon
 *
 */
public class ClientCommunication {

	private static Socket socket;
	public static int port = 15554;

	/**
	 * Send a message to a specific server and waits for an answer.
	 * @param message to be sent to the server.
	 * @return the answer from the server.
	 */
	public static String communicationWithServer(String message) {
		try {
			String host = "localhost";
			InetAddress address = InetAddress.getByName(host);
			socket = new Socket(address, port);
			sentMessage(message);
			return receiveMessage();
		} catch (IOException e) {
			return "-1";
		}
	}

	private static void sentMessage(String sendMessage) {
		try {
			java.io.OutputStream os = socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);

			bw.write(sendMessage);
			bw.flush();
		} catch (IOException e) {

		}
	}

	private static String receiveMessage() {
		try {
			InputStream is = socket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			return br.readLine();
		} catch (IOException e) {
			return null;
		}
	}

}
