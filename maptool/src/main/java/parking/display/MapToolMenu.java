package parking.display;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

class MapToolMenu {

	private MapTool mapTool;
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenu viewMenu;
	private JMenuItem fileOpen;
	private JMenuItem fileSave;
	private JMenuItem view;
		
	private FileMenuListener fileMenuListener;
	private ViewMenuListener viewMenuListener;
	
	public MapToolMenu(MapTool mapTool) {

		this.mapTool = mapTool;

		menuBar = new JMenuBar();
		mapTool.setJMenuBar(menuBar);

		fileMenu = new JMenu("File");
		viewMenu = new JMenu("View");

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		
		fileMenuListener = new FileMenuListener();
		viewMenuListener = new ViewMenuListener();

		fileOpen = new JMenuItem("Open");
		fileOpen.setActionCommand("Open");
		fileOpen.addActionListener(fileMenuListener);
		fileMenu.add(fileOpen);

		fileSave = new JMenuItem("Save");
		fileSave.setActionCommand("Save");
		fileSave.addActionListener(fileMenuListener);
		fileMenu.add(fileSave);

		view = new JMenuItem("View");
		view.setActionCommand("View");
		view.addActionListener(viewMenuListener);
		viewMenu.add(view);

	}

	class FileMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("Open")){
         		mapTool.open();
         	}
         	else if (cmd.equals("Save")) {
         		mapTool.save();
         	}
         }
	}


	class ViewMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("View")){
       			mapTool.view();
         	}
         }
	}
}