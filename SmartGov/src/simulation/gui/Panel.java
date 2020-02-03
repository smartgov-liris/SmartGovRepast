package simulation.gui;

import javax.swing.JPanel;
import javax.swing.JTextField;

public class Panel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JTextField clockDisplay;
	
	public Panel(){
		this.clockDisplay = new JTextField();
	}
	
	public JTextField getClockDisplay() {
		return clockDisplay;
	}
	
}
