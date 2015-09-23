package vsa;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import vsa.Analysis.AnalysisState;
import access.mypackage.offdebug.Debug;

/**
 * Listener of buttons implements action performed after pressing each button
 *
 */
public class ButtonListener {
	
	private BtnPRPlayListener btnPRPlayListener;
	private BtnPRPauseListener btnPRPauseListener;
	private BtnPRStopListener btnPRStopListener;
	private BtnPRRecordListener btnPRRecordListener;
	
	private BtnOpenFileListener btnOpenFileListener;  
	private BtnSaveFileListener btnSaveFileListener;
	
	private BtnStartAnalysisListener btnStartAnalysisListener;
	private BtnStopAnalysisListener btnStopAnalysisListener;
	
	private MenuItemMixerChooserListener menuItemMixerChooserListener;
	private MenuItemAnalysisDataListener menuItemAnalysisDataListener;
	
	/**
	 * Class controlling file choosing
	 */
	private FileChooser fileChooser;
	
	/**
	 * AudioPanelButtonsAndFields auto-reference for
	 * changing the availability of buttons. 
	 */
	private ButtonsAndFields buttons;
	
	/**
	 * Play or record an audio.
	 */
	private Player player;
	private Analysis analyzer;
	private AnalysisDataWindow analysisDataWindow;
	
	private DisplayPanel displayPanelWaveform,
						displayPanelAnalysis;
	
	private Thread threadPaintWaveform = null;
	private Thread threadAnalysis = null;
	
	private JFrame frame;
	
	private float[] audioFloatArray;
	
	private Object audioLock = new Object();
	
	private String fileName;
	
	
	
	/******************************************************************************************
	 * 
	 * Klasy bêd¹ce odpowiednimi Listenerami
	 * 
	 * ****************************************************************************************
	 */
	
	
	/**
	 * Action Listener for open file button.
	 * It acts differently whether the file opening finish with success or not.  
	 */
	class BtnOpenFileListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Debug.debug("open");
				
			fileChooser.choosePathToOpenFile();

			if(fileChooser.getFileState() == FileChooser.FileState.FILE_OPEN) {
				
				//Set stream to play obtained from chosen file
				player.setByteArrayToPlay(AudioFileOperations.getByteArrayWithAudio());
				
				synchronized(audioLock) {
					audioFloatArray = AudioFileOperations.byteArrayIntoFloatArray(AudioFileOperations.getByteArrayWithAudio());
				}
				
				buttonsStateSuccessfulOpening();
				fieldsAnalysisDefaultState();
				fileName = fileChooser.getFilePathToOpen();
				setNameInTextField(fileName);
				
				threadPaintWaveform = new Thread(new PaintWaveformThread(displayPanelWaveform, audioFloatArray));
				threadPaintWaveform.start();
						
			}
			else if(fileChooser.getFileState() == FileChooser.FileState.NO_FILE){
				buttonsStateErrorOpening();
				buttons.textFieldFileName.setText("");
			}
		}
	}
	
	
	
	/**
	 * Action Listener for save file button. If file name were selected then
	 * save audio bytes array into chose directory. Open that file and set text field with its name. 
	 */
	class BtnSaveFileListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Debug.debug("save");
			fileChooser.choosePathToSaveFile();
			if (fileChooser.getFileState() == FileChooser.FileState.FILE_SAVE) {
				fileChooser.saveFile(player.getAudioBytes());
				if (fileChooser.getFileState() == FileChooser.FileState.FILE_OPEN) {
					fileName = fileChooser.getFilePathToOpen();
					//buttons.textFieldFileName.setText(name.substring(name.lastIndexOf('\\') + 1));
					setNameInTextField(fileName);
				}
			}
		}
	}
	
	/**
	 * Action Listener for play file button.
	 */
	class BtnPRPlayListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Debug.debug("play");
			
			buttonsStatePlayFile();
			player.playback.play();
			
		}
	}
	
	/**
	 * Action Listener for pause file button.
	 */
	class BtnPRPauseListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Debug.debug("pause");
			if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
			player.playback.pause();
			if (!buttons.playFile.isAvaliable()) buttons.playFile.changeState();
		}
	}
	
	/**
	 * Action Listener for stop file button.
	 */
	class BtnPRStopListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Debug.debug("stop");
			if (player.getPlayState() == PlayState.RECORD) {
				player.capture.stop();
				buttons.textFieldFileName.setText("Recording");
				fileName = "Recording";
				
				synchronized(audioLock) {
					audioFloatArray = AudioFileOperations.byteArrayIntoFloatArray(player.getAudioBytes());
				}
				
				threadPaintWaveform = new Thread(new PaintWaveformThread(displayPanelWaveform, audioFloatArray));
				threadPaintWaveform.start();
			}
			else {
				player.playback.stop();
			}
			buttonsStateStopFile();
		}
	}
	
	/**
	 * Action Listener for record file button.
	 */
	class BtnPRRecordListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Debug.debug("rec");
			fieldsAnalysisDefaultState();
			displayPanelWaveform.reset();
			player.capture.start();
			buttonsStateRecordFile();
		}
	}
	
	class BtnStartAnalysisListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			buttonsStateStartAnalysis();
			fieldsAnalysisDefaultState();
			threadAnalysis = new Thread(new StartAnalysisThread(audioFloatArray));
			threadAnalysis.start();
		}
		
	}
	
	class BtnStopAnalysisListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (analyzer.getAnalysisState() == AnalysisState.ANALYSING) {
				analyzer.setAnalysisState(AnalysisState.INTERRUPTING);
			}
		}
		
	}
	
	class MenuItemMixerChooserListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			new MixerChooserWindow(frame);
		}
		
	}
	
	class MenuItemAnalysisDataListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			analysisDataWindow.setVisible(true);
		}
		
	}
	
	
	
	/****************************************************************************
	 * 
	 * Klasy implementuj¹ce interfejs Runnable
	 * 
	 * **************************************************************************
	 */
	
	
	
	class StartAnalysisThread implements Runnable {
		
		private float[] samplesToAnalysis;
		
		StartAnalysisThread(float[] samples) {
			synchronized(audioLock) {
				samplesToAnalysis = samples.clone();
			}
		}
		
		@Override
		public void run() {
			analyzer = new Analysis();
			MicrotremorFunction microtremorFunction = null;
			try {
				microtremorFunction = analyzer.startAnalysis(samplesToAnalysis);
				new Thread(new PaintWaveformThread(displayPanelAnalysis, microtremorFunction.samples)).start();
				setFrequencyInTheTextField(microtremorFunction.freq);
				setDecisionInTheTextField(microtremorFunction.getDecision());
				analysisDataWindow.addAnalysisData(fileName, getFrequencyFromTheTextField(), getDecisionFromTheTextField());
			} catch(AnalysisInterruptedException e) {
				microtremorFunction = null;
			} finally {
				analyzer.setAnalysisState(AnalysisState.INACTIVITY);
				analyzer = null;
			}
			buttonsStateStopAnalysis();
		}
		
	}
	
	
	
	/**
	 * Klasa implementujaca interfejs Runnable s³u¿¹cy do narysowania funkcji
	 */
	class PaintWaveformThread implements Runnable {

		float[] functionToDraw;
		DisplayPanel displayPanel;
		
		PaintWaveformThread(DisplayPanel displayPanel, float[] function) {
			this.displayPanel = displayPanel;
			synchronized (audioLock) {
				functionToDraw = function.clone();
			}
		}
		
		@Override
		public void run() {
			displayPanel.displayFunction(functionToDraw);
			functionToDraw = null;
		}
		
	}
	
	/*****************************************************************************
	 * 
	 * Zmiany stanów przycisków i pól
	 * 
	 * ***************************************************************************
	 */
	
	/**
	 * Set name of a file in JTextField placed in the audio panel.
	 * @param name name of a file
	 */
	private void setNameInTextField(String name) {
		int i = name.lastIndexOf('\\');
		if ((i > 0) && (i < name.length() - 1))
			buttons.textFieldFileName.setText(name.substring(i+1));
		else
			buttons.textFieldFileName.setText(name);
	}
	
	private void setFrequencyInTheTextField(float frequency) {
		String text = Float.toString(frequency);
		int i;
		text = (((i = text.indexOf('.'))!= -1)? text.substring(0, (i + 3 < text.length()? i+3: i+2)) + " Hz": text + " HZ");
		buttons.textFieldAverageFrequency.setText(text);
	}
	
	private String getFrequencyFromTheTextField() {
		return buttons.textFieldAverageFrequency.getText();
	}
	
	private String getDecisionFromTheTextField() {
		return buttons.textFieldDecision.getText();
	}
	
	private void setDecisionInTheTextField(boolean stressDetected) {
		if(stressDetected == true) {
			buttons.textFieldDecision.setText("Stress detected");
			buttons.textFieldDecision.setBackground(Color.RED);
		}
		else {
			buttons.textFieldDecision.setText("Stress undetected");
			buttons.textFieldDecision.setBackground(Color.GREEN);
			
		}
	}
	
	void buttonsStatePlayFile() {
		synchronized (this) {
			if (buttons.playFile.isAvaliable()) buttons.playFile.changeState();
			if(buttons.openFile.isAvaliable()) buttons.openFile.changeState();
			if(buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
			if (!buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
			if (!buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
			if (buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
			buttons.menuItemMixerChooser.setEnabled(false);
		}
	}
	
	
	/**
	 * Buttons state after pressing record button.   
	 */
	void buttonsStateRecordFile() {
		synchronized (this) {
			if (buttons.playFile.isAvaliable()) buttons.playFile.changeState();
			if(buttons.openFile.isAvaliable()) buttons.openFile.changeState();
			if(buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
			if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
			if (!buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
			if (buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
			if (buttons.startAnalysis.isAvaliable()) buttons.startAnalysis.changeState();
			if (buttons.stopAnalysis.isAvaliable()) buttons.stopAnalysis.changeState();
			buttons.menuItemMixerChooser.setEnabled(false);
		}
	}
	
	void buttonsStateStopFile() {
		synchronized (this) {
			if(analyzer == null) {
				if (!buttons.playFile.isAvaliable()) buttons.playFile.changeState();
				if(!buttons.openFile.isAvaliable()) buttons.openFile.changeState();
				if(!buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
				if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
				if (buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
				if (!buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
				if (!buttons.startAnalysis.isAvaliable()) buttons.startAnalysis.changeState();
				buttons.menuItemMixerChooser.setEnabled(true);
			}
			else if(analyzer.getAnalysisState() == AnalysisState.INACTIVITY) {
				if (!buttons.playFile.isAvaliable()) buttons.playFile.changeState();
				if(!buttons.openFile.isAvaliable()) buttons.openFile.changeState();
				if(!buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
				if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
				if (buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
				if (!buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
				if (!buttons.startAnalysis.isAvaliable()) buttons.startAnalysis.changeState();
				buttons.menuItemMixerChooser.setEnabled(true);
			}
			else {
					if (!buttons.playFile.isAvaliable())buttons.playFile.changeState();
					if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
					if (buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
			}
		}
	}
	
	
	/**
	 * Buttons state after successfully opened file   
	 */
	void buttonsStateSuccessfulOpening() {
		synchronized (this) {
			if (!buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
			if (!buttons.playFile.isAvaliable()) buttons.playFile.changeState();
			if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
			if (buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
			if (!buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
			if (!buttons.startAnalysis.isAvaliable()) buttons.startAnalysis.changeState();
			if (buttons.stopAnalysis.isAvaliable()) buttons.stopAnalysis.changeState();
			buttons.menuItemMixerChooser.setEnabled(true);
		}
	}
	
	/**
	 * Buttons state after unsuccessfully opened file   
	 */
	void buttonsStateErrorOpening() {
		synchronized (this) {
			if (buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
			if (buttons.playFile.isAvaliable()) buttons.playFile.changeState();
			if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
			if (buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
			if (!buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
			buttons.menuItemMixerChooser.setEnabled(true);
		}
	}
	
	void buttonsStateStartAnalysis() {
		if (buttons.openFile.isAvaliable()) buttons.openFile.changeState();
		if (buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
		if (buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
		if (!buttons.stopAnalysis.isAvaliable()) buttons.stopAnalysis.changeState();
		if (buttons.startAnalysis.isAvaliable()) buttons.startAnalysis.changeState();
		buttons.menuItemMixerChooser.setEnabled(false);
	}
	
	void buttonsStateStopAnalysis() {
		if (player.getPlayState() == PlayState.STOP) {
			if (!buttons.playFile.isAvaliable()) buttons.playFile.changeState();
			if(!buttons.openFile.isAvaliable()) buttons.openFile.changeState();
			if(!buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
			if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
			if (buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
			if (!buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
			buttons.menuItemMixerChooser.setEnabled(true);
		}
		else {
			if(buttons.openFile.isAvaliable()) buttons.openFile.changeState();
			if(buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
			if (!buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
			if (!buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
			if (buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
			buttons.menuItemMixerChooser.setEnabled(false);
		}
		
		if (!buttons.startAnalysis.isAvaliable()) buttons.startAnalysis.changeState();
		if (buttons.stopAnalysis.isAvaliable()) buttons.stopAnalysis.changeState();
	}
	
	void setDefaultState() {
		if (fileChooser.getFileState() == FileChooser.FileState.FILE_OPEN) {
			if (!buttons.playFile.isAvaliable()) buttons.playFile.changeState();
			if(!buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
			if(!buttons.startAnalysis.isAvaliable()) buttons.startAnalysis.changeState();
		}
		else {
			if (buttons.playFile.isAvaliable()) buttons.playFile.changeState();
			if(buttons.saveFile.isAvaliable()) buttons.saveFile.changeState();
			if(buttons.startAnalysis.isAvaliable()) buttons.startAnalysis.changeState();
		}
		
		if(!buttons.openFile.isAvaliable()) buttons.openFile.changeState();
		if (buttons.pauseFile.isAvaliable()) buttons.pauseFile.changeState();
		if (buttons.stopFile.isAvaliable()) buttons.stopFile.changeState();
		if (!buttons.recordFile.isAvaliable()) buttons.recordFile.changeState();
		if(buttons.stopAnalysis.isAvaliable()) buttons.stopAnalysis.changeState();
		
		buttons.menuItemMixerChooser.setEnabled(true);
	}
	
	void fieldsAnalysisDefaultState(){
		buttons.textFieldDecision.setText("----");
		buttons.textFieldDecision.setDefaultColor();
		buttons.textFieldAverageFrequency.setText("----");
		displayPanelAnalysis.reset();
	}
	/****************************************************************************************
	 * 
	 * Gettery i settery
	 * 
	 * **************************************************************************************
	 */
	
	/**
	 * Returns listener to button which is used for opening a file.
	 */
	BtnOpenFileListener getBtnOpenFileListener() {
		return btnOpenFileListener;
	}
	
	/**
	 * Returns listener to button which is used for saving a file.
	 */
	BtnSaveFileListener getBtnSaveFileListener() {
		return btnSaveFileListener;
	}
	
	/**
	 * Returns listener to button which is used for playing a file.
	 */
	BtnPRPlayListener getBtnPRPlayListener() {
		return btnPRPlayListener;
	}
	
	/**
	 * Returns listener to button which pauses a file.
	 */
	BtnPRPauseListener getBtnPRPauseListener() {
		return btnPRPauseListener;
	}
	
	/**
	 * Returns listener to button which stops a file.
	 */
	BtnPRStopListener getBtnPRStopListener() {
		return btnPRStopListener;
	}
	
	/**
	 * Returns listener to button which is used for recording a file.
	 */
	BtnPRRecordListener getBtnPRRecordListener() {
		return btnPRRecordListener;
	}
	
	BtnStartAnalysisListener getBtnStartAnalysisListener() {
		return btnStartAnalysisListener;
	}
	
	BtnStopAnalysisListener getBtnStopAnalysisListener() {
		return btnStopAnalysisListener;
	}
	
	MenuItemMixerChooserListener getMenuItemMixerChooserListener() {
		return menuItemMixerChooserListener;
	}
	
	MenuItemAnalysisDataListener getMenuItemAnalysisDataListener() {
		return menuItemAnalysisDataListener;
	}
	
	
	/************************************************************
	 * 
	 * Konstruktory
	 * 
	 * **********************************************************
	 */
	
	
	
	/**
	 * Initialize Button Listener.
	 * @param frame  JFrame on which Button Listeners listens out actions
	 */
	public ButtonListener(JFrame frame) {
		this.frame = frame;
		btnPRPlayListener = new BtnPRPlayListener();
		btnPRPauseListener = new BtnPRPauseListener();
		btnPRStopListener = new BtnPRStopListener();
		btnPRRecordListener = new BtnPRRecordListener();
		
		fileChooser = new FileChooser();
		btnOpenFileListener = new BtnOpenFileListener();
		btnSaveFileListener = new BtnSaveFileListener();
		
		btnStartAnalysisListener = new BtnStartAnalysisListener();
		btnStopAnalysisListener = new BtnStopAnalysisListener();
		
		menuItemMixerChooserListener = new MenuItemMixerChooserListener();
		menuItemAnalysisDataListener = new MenuItemAnalysisDataListener();
		
		player = new Player();
		analysisDataWindow = new AnalysisDataWindow();
		player.setButtonListenerAutoref(this);
	}
	
	/************************************************************************
	 * 
	 * Autoreferencje
	 * 
	 * **********************************************************************
	 */
	
	/**
	 * Set an auto-reference to AudioPanelButtonsAndFields
	 */
	void setButtonsAndFieldsAutoref(ButtonsAndFields buttons){
		this.buttons = buttons;
	}
	
	void setDisplayPanelWaveformAutoref(DisplayPanel displayPanel) {
		displayPanelWaveform = displayPanel;
	}
	
	void setDisplayPanelAnalysisAutoref(DisplayPanel displayPanel) {
		displayPanelAnalysis = displayPanel;
	}
	
	
	/*****************************************************************
	 * 
	 * Koñczenie pracy programu
	 * 
	 * ***************************************************************
	 */
	
	/**
	 * System will exit after saving a file. It will wait for end of that operation.
	 */
	@SuppressWarnings("static-access")
	void systemExit(){
		synchronized(this) {
			try{
				while(fileChooser.getFileState() == FileChooser.FileState.FILE_SAVE)
				wait();
			} catch( InterruptedException e) {
				Debug.debug(e.toString());
			}
		}
		
		if(analyzer != null) {
			if (analyzer.getAnalysisState() == AnalysisState.ANALYSING)
				analyzer.setAnalysisState(AnalysisState.INTERRUPTING);
			while(analyzer.getAnalysisState() != AnalysisState.INACTIVITY)
				Thread.currentThread().yield();
		}
		player.systemExit();
	}
}
