package vsa;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JTextField;

/**
 * Different kinds of buttons and fields used to open, play and record audio
 * files. It is necessary to construct the object of AudioPanelButtonsAndFields
 * class before constructing any field of this class.
 */
public class ButtonsAndFields {

	/**
	 * Buttons actions listener.
	 */
	private ButtonListener buttonListener;

	// Buttons and text field
	Button saveFile = new Button("Save as...", false), openFile = new Button("Open File", true),
			playFile = new Button("Play", false), pauseFile = new Button("Pause", false),
			stopFile = new Button("Stop", false), recordFile = new Button("Record", true),
			startAnalysis = new Button("Start Analysis", false), stopAnalysis = new Button("Stop Analysis", false);

	TextFieldFileName textFieldFileName = new TextFieldFileName();
	TextFieldAnalysis textFieldAverageFrequency = new TextFieldAnalysis();
	TextFieldAnalysis textFieldDecision = new TextFieldAnalysis();

	JMenuItem menuItemMixerChooser = new JMenuItem("Audio input and output devices...");
	JMenuItem menuItemAnalysisData = new JMenuItem("Results of analyses");

	/**
	 * Add listeners to all buttons
	 */
	void addButtonsListeners() {
		saveFile.addActionListener(buttonListener.getBtnSaveFileListener());
		openFile.addActionListener(buttonListener.getBtnOpenFileListener());
		playFile.addActionListener(buttonListener.getBtnPRPlayListener());
		pauseFile.addActionListener(buttonListener.getBtnPRPauseListener());
		stopFile.addActionListener(buttonListener.getBtnPRStopListener());
		recordFile.addActionListener(buttonListener.getBtnPRRecordListener());
		startAnalysis.addActionListener(buttonListener.getBtnStartAnalysisListener());
		stopAnalysis.addActionListener(buttonListener.getBtnStopAnalysisListener());
		menuItemMixerChooser.addActionListener(buttonListener.getMenuItemMixerChooserListener());
		menuItemAnalysisData.addActionListener(buttonListener.getMenuItemAnalysisDataListener());
	}

	@SuppressWarnings("serial")
	class TextFieldAnalysis extends JTextField {

		Color color;

		public TextFieldAnalysis() {
			setText("----");
			setEditable(false);
			setColumns(10);
			color = getBackground();
		}

		public void setDefaultColor() {
			setBackground(color);
		}
	}

	@SuppressWarnings("serial")
	class TextFieldFileName extends JTextField {

		public TextFieldFileName() {
			setText("");
			setEditable(false);
			setColumns(10);
		}
	}

	/**
	 * Class that extends JButton. Consists of field and methods to control the
	 * state of button.
	 */
	@SuppressWarnings("serial")
	class Button extends JButton {
		/**
		 * State of a button describes whether the button is enabled or not.
		 */
		private boolean state;

		/**
		 * Constructor with two arguments to specified the primary parameters of
		 * button.
		 * 
		 * @param name
		 *            name of a button
		 * @param state
		 *            state of a button
		 */
		Button(String name, boolean state) {
			super(name);
			this.state = state;
			setEnabled(state);
		}

		/**
		 * Default constructor, sets the state as a false, and name as a null.
		 */
		Button() {
			super();
			state = false;
			setEnabled(false);
		}

		/**
		 * Returns the state of a button
		 * 
		 * @return State of a button in the type of boolean. True if button is
		 *         enabled, false if it is disabled.
		 */
		boolean isAvaliable() {
			return state;
		}

		/**
		 * Change the state of a button into opposite state.
		 */
		void changeState() {
			state = !state;
			setEnabled(state);
		}
	}

	/**
	 * Initialize AudioPanelButtonsAndFields. It constructs ButtonListener and
	 * sending auto-reference to this object.
	 * 
	 * @param frame
	 *            top frame of the application necessary for creating
	 *            ButtonListener.
	 */
	ButtonsAndFields(JFrame frame) {
		buttonListener = new ButtonListener(frame);
		// send an auto-reference
		buttonListener.setButtonsAndFieldsAutoref(this);
	}

	ButtonListener getButtonListener() {
		return buttonListener;
	}

	void systemExit() {
		buttonListener.systemExit();
	}
}
