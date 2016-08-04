package parking.display;

import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JSlider;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class SpinSlider extends JFrame {
	
	private JSpinner spinner;
	private JSlider slider;
	private JPanel panel;
	private SignTool signTool;

	public SpinSlider(SignTool signTool) {

		this.signTool = signTool;

		setTitle("Spin Slider");
		setBackground( Color.gray );
		setLayout(new FlowLayout());

		spinner = new JSpinner();
		slider = new JSlider();
		panel = new JPanel();
		panel.setLayout(new FlowLayout());
		add(panel);

		slider.addChangeListener(new ChangeListener() {
			@Override
            public void stateChanged(ChangeEvent e) {
                JSlider s = (JSlider) e.getSource();
                spinner.setValue(s.getValue());
                signTool.invoke(s.getValue());
            }
		});

		panel.add(slider);

		spinner.setModel(new SpinnerNumberModel(50, 0, 100, 1));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0'%'"));
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSpinner s = (JSpinner) e.getSource();
                int value = (Integer) s.getValue();
                slider.setValue(value);
                signTool.invoke(value);
            }
        });

        panel.add(spinner);		
	}

	public void setSpinner(int value, int min, int max, int step) {
		spinner.setModel(new SpinnerNumberModel(value, min, max, step));
	}
}