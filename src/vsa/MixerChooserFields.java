package vsa;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class MixerChooserFields {
	
	MixerChooserWindow mixerChooserWindow;
	private SoundTest soundTest;
	
	static int selectedOutputMixer = 0;
	static int selectedInputMixer = 0;
	
	JLabel lblInputMixerLabel;
	JComboBox<String> comboBoxInputMixer;
	JLabel lblOutputMixerLabel;
	JComboBox<String> comboBoxOutputMixer;
	JButton btnPlaybackOutputTest;
	JButton btnMixerAccepted;
	JButton btnPlaybackInputTest;
	JButton btnCaptureInputTest;
	
	private ComboBoxInputMixerListener comboBoxInputMixerListener;
	private ComboBoxInputMixerCreated comboBoxInputMixerCreated;
	private ComboBoxOutputMixerListener comboBoxOutputMixerListener;
	private ComboBoxOutputMixerCreated comboBoxOutputMixerCreated;
	private BtnPlaybackInputTestListener btnPlaybackInputTestListener;
	private BtnCaptureInputTestListener btnCaptureInputTestListener;
	private BtnPlaybackOutputTestListener btnPlaybackOutputTestListener;
	private BtnMixerAcceptedListener btnMixerAcceptedListener;
	
	
	void addActionListeners() {
		comboBoxInputMixer.addActionListener(comboBoxInputMixerListener);
		comboBoxInputMixer.addAncestorListener(comboBoxInputMixerCreated);
		comboBoxOutputMixer.addActionListener(comboBoxOutputMixerListener);
		comboBoxOutputMixer.addAncestorListener(comboBoxOutputMixerCreated);
		btnPlaybackInputTest.addActionListener(btnPlaybackInputTestListener);
		btnCaptureInputTest.addActionListener(btnCaptureInputTestListener);
		btnPlaybackOutputTest.addActionListener(btnPlaybackOutputTestListener);
		btnMixerAccepted.addActionListener(btnMixerAcceptedListener);
	}
	
	class BtnPlaybackOutputTestListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (btnPlaybackOutputTest.getText().startsWith("Play")) {
				
				buttonsStateStartPlaybackOrRecording();
				btnPlaybackInputTest.setEnabled(false);
				btnCaptureInputTest.setEnabled(false);
				
				soundTest.outputSoundTest.playback.start();
				btnPlaybackOutputTest.setText("Stop");
			}
			else {
				
				buttonsStateStopPlaybackOrRecording();
				soundTest.outputSoundTest.playback.stop();
			}
		}
		
	}
	
	
	
	class BtnPlaybackInputTestListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (btnPlaybackInputTest.getText().startsWith("Play")) {
				
				buttonsStateStartPlaybackOrRecording();
				btnPlaybackOutputTest.setEnabled(false);
				
				soundTest.inputSoundTest.playback.start();
				btnCaptureInputTest.setEnabled(false);
				btnPlaybackInputTest.setText("Stop");
			} else {
				
				buttonsStateStopPlaybackOrRecording();
				btnPlaybackOutputTest.setEnabled(true);
				
				soundTest.inputSoundTest.playback.stop();
				btnCaptureInputTest.setEnabled(true);
				btnPlaybackInputTest.setText("Play");
				
				
			}
		}
		
	}
	
	class BtnCaptureInputTestListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (btnCaptureInputTest.getText().startsWith("Record")) {
				
				buttonsStateStartPlaybackOrRecording();
				btnPlaybackOutputTest.setEnabled(false);
				
				soundTest.inputSoundTest.capture.start();
				btnPlaybackInputTest.setEnabled(false);
				btnCaptureInputTest.setText("Stop");
			} else {
				
				buttonsStateStopPlaybackOrRecording();
				btnPlaybackOutputTest.setEnabled(true);
				
				soundTest.inputSoundTest.capture.stop();
				btnPlaybackInputTest.setEnabled(true);
				btnCaptureInputTest.setText("Record");
			}
		}
		
	}
	
	/**
	 * Je¿eli mixery zosta³y zaakceptowane wy³¹cz okno wybroru
	 */
	class BtnMixerAcceptedListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			mixerChooserWindow.getFrame().setVisible(false);
			mixerChooserWindow.getFrame().dispose();
			mixerChooserWindow.getParentFrame().setEnabled(true);
			mixerChooserWindow.getParentFrame().setVisible(true);
		}
		
	}
	
	class ComboBoxInputMixerCreated implements AncestorListener  {

		@SuppressWarnings("unchecked")
		@Override
		public void ancestorAdded(AncestorEvent event) {
			((JComboBox<String>)event.getSource()).setSelectedIndex(selectedInputMixer);
		}
		@Override
		public void ancestorRemoved(AncestorEvent event) {}

		@Override
		public void ancestorMoved(AncestorEvent event) {}
		
	}
	
	class ComboBoxOutputMixerCreated implements AncestorListener {
		
		@SuppressWarnings("unchecked")
		@Override
		public void ancestorAdded(AncestorEvent event) {
			((JComboBox<String>)event.getSource()).setSelectedIndex(selectedOutputMixer);
		}

		@Override
		public void ancestorRemoved(AncestorEvent event) {}
		@Override
		public void ancestorMoved(AncestorEvent event) {}
	}
	
	
	/**
	 * Ustaw mixer wejœciowy taki jaki zosta³ wybrany w JComboBox
	 */
	class ComboBoxInputMixerListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			@SuppressWarnings("unchecked")
			JComboBox<String> box = (JComboBox<String>)e.getSource();
			selectedInputMixer = box.getSelectedIndex();
			if (box.getSelectedIndex() == 0)
				MixerChooser.setDefaultInputMixer();
			else
				MixerChooser.setInputMixer((String)box.getSelectedItem());
		}
		
	}
	
	/**
	 * Ustaw mixer wyjœciowy taki jaki zosta³ wybrany w JComboBox
	 */
	class ComboBoxOutputMixerListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			@SuppressWarnings("unchecked")
			JComboBox<String> box = (JComboBox<String>)e.getSource();
			selectedOutputMixer = box.getSelectedIndex();
			if (box.getSelectedIndex() == 0)
				MixerChooser.setDefaultOutputMixer();
			else
				MixerChooser.setOutputMixer((String)box.getSelectedItem());
		}
		
	}
	
	public void buttonsStateStartPlaybackOrRecording() {
		comboBoxInputMixer.setEnabled(false);
		comboBoxOutputMixer.setEnabled(false);
		btnMixerAccepted.setEnabled(false);
	}
	
	public void buttonsStateStopPlaybackOrRecording() {
		comboBoxInputMixer.setEnabled(true);
		comboBoxOutputMixer.setEnabled(true);
		btnMixerAccepted.setEnabled(true);
	}
	
	public void setDefaultState() {
		shutDown();
		comboBoxInputMixer.setEnabled(true);
		comboBoxOutputMixer.setEnabled(true);
		btnMixerAccepted.setEnabled(true);
		btnPlaybackInputTest.setText("Play");
		if (soundTest.inputSoundTest.isRecordedStramToPlay())
			btnPlaybackInputTest.setEnabled(true);
		else
			btnPlaybackInputTest.setEnabled(false);
		btnCaptureInputTest.setText("Record");
		btnCaptureInputTest.setEnabled(true);
		btnPlaybackOutputTest.setText("Play");
		btnPlaybackOutputTest.setEnabled(true);
	}
	
	
	public void shutDown() {
		soundTest.inputSoundTest.playback.stop();
		soundTest.inputSoundTest.capture.stop();
		soundTest.outputSoundTest.playback.stop();
	}
	
	
	public MixerChooserFields(MixerChooserWindow mixerChooserWindow) {
		this.mixerChooserWindow = mixerChooserWindow;
		comboBoxInputMixerListener = new ComboBoxInputMixerListener();
		comboBoxInputMixerCreated = new ComboBoxInputMixerCreated();
		comboBoxOutputMixerListener = new ComboBoxOutputMixerListener();
		comboBoxOutputMixerCreated = new ComboBoxOutputMixerCreated();
		btnPlaybackInputTestListener = new BtnPlaybackInputTestListener();
		btnCaptureInputTestListener = new BtnCaptureInputTestListener();
		btnPlaybackOutputTestListener = new BtnPlaybackOutputTestListener();
		btnMixerAcceptedListener = new BtnMixerAcceptedListener();
		soundTest = new SoundTest();
		soundTest.setMixerChooserFieldsAutoref(this);
	}
	
}
