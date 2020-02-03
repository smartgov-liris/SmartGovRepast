package simulation.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import microagent.properties.ParkProperties;
import repast.simphony.userpanel.ui.UserPanelCreator;
import simulation.FilePath;
import smartGov.ClockSingleton;

public class UserPanel extends JPanel implements UserPanelCreator, ActionListener {

	private static final long serialVersionUID = 1L;
	
	public static JMenuBar menuBar;
	public static JMenu menu;
	public static JMenuItem menuItem;
	
	public static JTextField clockDisplay;
	public static JTextField timeStep;
	public static JTextField day;
	public static JButton timeButton;
	
	private static JPanel clockPanel;
	private static JPanel dayPanel;
	private static JPanel timePanel;
	
	private static JPanel tab1;
	
	public static JPanel behaviorPanel;
	public static JPanel behaviorElement;
	public static JComboBox<String> behaviorList;
	public static JPanel behaviorButtonPanel;
	public static JButton behaviorSave;
	public static JButton behaviorAdd;
	
	public static JTabbedPane tabbedPane;
	
	public static final String TIME_CHANGE = "timeChange";
	
	@Override
	public JPanel createPanel() {
		
		menuBar = new JMenuBar();
		menu = new JMenu("A menu");
		menu.setMnemonic(KeyEvent.VK_A);
		menu.getAccessibleContext().setAccessibleDescription("bla");
		menuBar.add(menu);
		
		menuItem = new JMenuItem("test");
		menu.add(menuItem);
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel simulationTick = createPanelWithBorderName("SimulationTick");
		
		tabbedPane = new JTabbedPane();
		tab1 = new JPanel();
		tab1.setLayout(new BoxLayout(tab1, BoxLayout.Y_AXIS));
		
		dayPanel = new JPanel();
		createDayPanel();
		clockPanel=new JPanel();
		createClockPanel();
		timePanel= new JPanel();
		createTimePanel();
		
		simulationTick.add(dayPanel);
		simulationTick.add(Box.createRigidArea(new Dimension(0,20)));
		simulationTick.add(clockPanel);
		simulationTick.add(Box.createRigidArea(new Dimension(0,20)));
		simulationTick.add(timePanel);
		tab1.add(simulationTick);
		
		behaviorPanel = new JPanel();
		behaviorPanel.setLayout(new BoxLayout(behaviorPanel, BoxLayout.Y_AXIS));
		behaviorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		TitledBorder behaviorTitle = BorderFactory.createTitledBorder("Behavior");
		behaviorPanel.setBorder(behaviorTitle);
		
		behaviorElement = new JPanel();
		behaviorElement.setLayout(new BoxLayout(behaviorElement, BoxLayout.Y_AXIS));
		behaviorElement.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		File folder = new File(FilePath.behaviorFolder);
		File[] listOfFiles = folder.listFiles();
		String[] elements = new String[listOfFiles.length];
		for(int i = 0; i < listOfFiles.length; i++){
			elements[i] = listOfFiles[i].getName().replaceFirst("[.][^.]+$", "");
		}
		
		behaviorList = new JComboBox<>(elements);
		behaviorList.setPreferredSize(new Dimension(100,20));
		behaviorList.setMaximumSize(new Dimension(200,20));
		behaviorList.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				behaviorElement.removeAll();
				String name = behaviorList.getItemAt(behaviorList.getSelectedIndex());
				
				Map<String, Double> elements = ParkProperties.parseBehaviorFile("input\\behavior\\"+name+".txt");
				for(Entry<String, Double> element : elements.entrySet()){
					JPanel temp = new JPanel();
					temp.setLayout(new BoxLayout(temp, BoxLayout.X_AXIS));
					temp.setAlignmentX(Component.LEFT_ALIGNMENT);
					
					JLabel labelTemp = new JLabel(element.getKey()+": ");
					temp.add(labelTemp);
					JTextField fieldTemp = new JTextField(String.valueOf(element.getValue()));
					fieldTemp.setMaximumSize(new Dimension(200,20));
					temp.add(fieldTemp);	   		   
					behaviorElement.add(temp);
					behaviorElement.add(Box.createRigidArea(new Dimension(0,10)));
				}
				behaviorElement.revalidate();
				behaviorAdd.setEnabled(true);
				behaviorSave.setEnabled(true);
			}
		});
		
		behaviorButtonPanel = new JPanel();
		behaviorButtonPanel.setLayout(new BoxLayout(behaviorButtonPanel, BoxLayout.X_AXIS));
		behaviorButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		behaviorSave = new JButton("Save");
		behaviorSave.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Map<String, Double> elements = new HashMap<>();
				String[] key = null;
				double value = 0.0;
				Component[] components = behaviorElement.getComponents();
				for(Component component : components){
					//System.out.println(component);
					Component[] jpanelComponents = ((JPanel)component).getComponents();
					for(int i = 0; i < jpanelComponents.length; i++){
						if(jpanelComponents[i] instanceof JLabel){
							key = ((JLabel)jpanelComponents[i]).getText().split(":");
						} else if(jpanelComponents[i] instanceof JTextField){
							value = Double.valueOf(((JTextField)jpanelComponents[i]).getText());
						}
					}
					elements.put(key[0], value);
				}
				String name = behaviorList.getItemAt(behaviorList.getSelectedIndex());
				String filename = "input\\behavior\\"+name+".txt";
				ParkProperties.updateFile(filename, elements);
			}
		});
		behaviorSave.setEnabled(false);
		behaviorAdd = new JButton("Add");
		behaviorAdd.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
								
			}
		});
		behaviorAdd.setEnabled(false);
		//JButton behaviorDelete = new JButton("Delete");
		behaviorButtonPanel.add(behaviorSave);
		behaviorButtonPanel.add(behaviorAdd);
		
		behaviorPanel.add(behaviorList);
		behaviorPanel.add(behaviorElement);
		behaviorPanel.add(behaviorButtonPanel);
		
		//this.add(behaviorPanel);
		tab1.add(behaviorPanel);
		
		tabbedPane.addTab("Simulation", null, tab1, null);
		tabbedPane.addTab("Editor", null, createPanelWithBorderName("Editor"), null);
		this.add(tabbedPane);

		return this;
	}
	
	private void createDayPanel() {
		dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.X_AXIS));
		dayPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel dayOfWeek = new JLabel("Day: ");
		day = new JTextField();
		day.setPreferredSize(new Dimension(100,20));
		day.setMaximumSize(new Dimension(200,20));
		dayPanel.add(dayOfWeek);
		dayPanel.add(day);
	}

	private void createClockPanel() {
		clockPanel.setLayout(new BoxLayout(clockPanel, BoxLayout.X_AXIS));
		clockPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel clock = new JLabel("Clock: ");
		clockDisplay = new JTextField();
		clockDisplay.setPreferredSize(new Dimension(100,20));
		clockDisplay.setMaximumSize(new Dimension(200,20));
		clockPanel.add(clock);
		clockPanel.add(clockDisplay);
	}

	
	private void createTimePanel() {
		timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.X_AXIS));
		timePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel timeStamp = new JLabel("Time: ");
		timeStep = new JTextField(10);
		timeStep.setPreferredSize(new Dimension(100,20));
		timeStep.setMaximumSize(new Dimension(200,20));
		timeButton = new JButton("Change");
		timePanel.add(timeStamp);
		timePanel.add(timeStep);
		timePanel.add(timeButton);
		
		UserPanel.timeButton.setActionCommand(TIME_CHANGE);
		UserPanel.timeButton.addActionListener(this);
	}
	
	private JPanel createPanelWithBorderName(String name){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		TitledBorder panelBorder = BorderFactory.createTitledBorder(name);
		panel.setBorder(panelBorder);
		return panel;
	}
	
	
	public void actionPerformed(ActionEvent e){
		if(TIME_CHANGE.equals(e.getActionCommand())){
			try{
				int timeTick = Integer.parseInt(UserPanel.timeStep.getText());
				ClockSingleton.getInstance().setTimeTick(timeTick);
			} catch (Exception error){
				System.out.println("Error with the input. Make sure to enter a integer");
			}
		}
	}

}
