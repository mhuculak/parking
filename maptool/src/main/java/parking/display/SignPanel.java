package parking.display;

import javax.swing.JPanel;
import javax.swing.JButton;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import parking.map.Sign;

public class SignPanel extends JPanel {

	private Sign sign;
	private BufferedImage signImage;
	private JButton actionButton;
	private String action;
	private final int panelWidth = 200;

	public SignPanel(int preferredHeight) {
		setPreferredSize(new Dimension(panelWidth, preferredHeight));
		action = "Edit";
		actionButton = new JButton(action);
		add(actionButton);
		actionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (action.equals("Edit")) {            		
            		action = "Save";
            	}
            	else {
            		action = "Edit";
            	}
            	actionButton.setText(action);        	
            }              
        });
        setVisible(false);
	}

	public int getWidth() {
		return panelWidth;
	}

	public void setSign(Sign sign) {
		this.sign = sign;
		setVisible(true);
	}
}