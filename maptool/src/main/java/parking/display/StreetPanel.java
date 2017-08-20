package parking.display;

import parking.map.Address;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;

import java.awt.FlowLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class StreetPanel extends JPanel {

	private final JLabel panelLabel = new JLabel("Street:");;
	private final JLabel streetText  = new JLabel();
	private final JButton actionButton  = new JButton();
    private final JButton cancelButton  = new JButton("Cancel");
    private MapTool mapTool;
	private String action;
    private String mode;
    private Address streetAddress;

	public StreetPanel(MapTool mapTool) {
        this.mapTool = mapTool;
		setLayout(new FlowLayout(FlowLayout.LEFT));
        action = "Select";
        setModeSelecting();
        actionButton.setText(action);
        add(panelLabel);
        add(streetText);
        add(actionButton);
        add(cancelButton);
        setVisible(true);
        
        actionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (action.equals("Select")) {            		
            		action = "Save";
            	}
            	else if (action.equals("Save")) {
            		action = "Select";
                    mapTool.saveChanges();
            	}
            	actionButton.setText(action);        	
            }              
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                action = "Select";                
                actionButton.setText(action);
                streetText.setText("");
                mapTool.revertChanges(); 
            }
        });
	}

    public boolean getModeSelecting() {
        return mode.equals("Selecting");
    }

    public void setModeSelecting() {
        mode = "Selecting";
    }

    public void setModeWorking() {
        mode = "Working";
    }

    public void setStreetAddress(Address address) {
        streetAddress = address;
        System.out.println("change street name to "+streetAddress.getStreetName());
        streetText.setText(streetAddress.getStreetName());
        action = "Save";
        setModeWorking();
        actionButton.setText(action);
    }
}