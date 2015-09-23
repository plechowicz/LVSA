package vsa;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;

/**
 * Application that implements stress detection procedure used in Voice Stress
 * Analysis
 * 
 * @author Piotr Lechowicz
 *
 */

public class MainWindow {
	/**
	 * Top frame of the application.
	 */
	private JFrame frame;

	/**
	 * Size of the application top frame.
	 */
	private static final Dimension applicationSize = new Dimension(1000, 600);

	/**
	 * Audio panel used for playing or recording sound.
	 */
	private AudioPanel audioPanel;

	/**
	 * Analysis panel used for decide whether the micro-tremors exist.
	 */
	private AnalysisPanel analysisPanel;

	// auxiliary panels
	private JPanel panelLeftOutter;
	private JPanel panelLeftInner;
	private JPanel panelRightOutter;
	private JPanel panelRightInner;
	private JPanel menuBarPanel1;
	private JPanel menuBarPanel2;
	private JMenuBar menuBar;

	/**
	 * Object which contains different kinds of buttons and panels. Its
	 * constructor takes as an argument frame of the application
	 */
	private ButtonsAndFields buttonsAndFields;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 *            array of Strings
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		frame = new JFrame("Voice Stress Analysis");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				systemExit();
			}
		});
		frame.getContentPane().setLayout(new GridLayout(0, 2, 0, 0));

		setInTheCenter(applicationSize);

		createButtonsAndFields();

		addMenuBar();

		addPanelsForAudioAndAnalysisPanels();

		addAudioPanel();

		addAnalysisPanel();

		buttonsAndFields.addButtonsListeners();

		frame.setResizable(false);

	}

	/**
	 * Set the application in the center of the screen.
	 * 
	 * @param applicationSize
	 *            size of the application
	 */
	private void setInTheCenter(Dimension applicationSize) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		// checking if the size of the application isn't bigger than screen size
		if (screenSize.width < applicationSize.width)
			applicationSize.width = screenSize.width;
		if (screenSize.height < applicationSize.height)
			applicationSize.height = screenSize.height;

		frame.setBounds(new Rectangle((screenSize.width - applicationSize.width) / 2,
				(screenSize.height - applicationSize.height) / 2, applicationSize.width, applicationSize.height));
	}

	/**
	 * Add panels for the audio panel and the analysis panel to the application.
	 */
	private void addPanelsForAudioAndAnalysisPanels() {
		// adding left panel (audio panel)
		panelLeftOutter = new JPanel();
		panelLeftOutter.setLayout(new BorderLayout());
		panelLeftOutter.setBorder(new EmptyBorder(5, 5, 5, 5));

		panelLeftInner = new JPanel();
		SoftBevelBorder softBevelBorderLeft = new SoftBevelBorder(SoftBevelBorder.LOWERED);

		panelLeftInner.setLayout(new BorderLayout());
		panelLeftInner.setBorder(softBevelBorderLeft);

		panelLeftOutter.add(panelLeftInner);
		frame.getContentPane().add(panelLeftOutter);

		// adding right panel (analysis panel)

		panelRightOutter = new JPanel();
		panelRightOutter.setLayout(new BorderLayout());
		panelRightOutter.setBorder(new EmptyBorder(5, 5, 5, 5));

		panelRightInner = new JPanel();
		SoftBevelBorder softBevelBorderRight = new SoftBevelBorder(SoftBevelBorder.LOWERED);
		panelRightInner.setLayout(new BorderLayout());
		panelRightInner.setBorder(softBevelBorderRight);

		panelRightOutter.add(panelRightInner);
		frame.getContentPane().add(panelRightOutter);
	}

	private void addMenuBar() {
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		menuBarPanel1 = new JPanel();
		menuBar.add(menuBarPanel1);
		menuBarPanel1.setLayout(new BorderLayout(0, 0));
		buttonsAndFields.menuItemMixerChooser
				.setBorder(new SoftBevelBorder(SoftBevelBorder.RAISED, Color.GRAY, Color.GRAY));
		menuBarPanel1.add(buttonsAndFields.menuItemMixerChooser, BorderLayout.WEST);

		menuBarPanel2 = new JPanel();
		menuBarPanel2.setLayout(new BorderLayout(0, 0));
		buttonsAndFields.menuItemAnalysisData
				.setBorder(new SoftBevelBorder(SoftBevelBorder.RAISED, Color.GRAY, Color.GRAY));
		menuBarPanel2.add(buttonsAndFields.menuItemAnalysisData, BorderLayout.WEST);

		menuBarPanel1.add(menuBarPanel2, BorderLayout.CENTER);
	}

	/**
	 * Add an audio panel in the left panel of the application.
	 */
	private void addAudioPanel() {
		audioPanel = new AudioPanel();
		panelLeftInner.add(audioPanel);
	}

	/**
	 * Add an analysis panel in the right panel of the application.
	 */
	private void addAnalysisPanel() {
		analysisPanel = new AnalysisPanel();
		panelRightInner.add(analysisPanel);
	}

	/**
	 * Create buttons and fields that they can be later added to panels
	 */
	private void createButtonsAndFields() {
		// Create buttons and fields for audio panel
		buttonsAndFields = new ButtonsAndFields(frame);

	}

	/**
	 * Safely exit the application
	 */
	private void systemExit() {
		int reply = JOptionPane.showConfirmDialog(frame, "Are you sure you want to exit?", "Exit",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		switch (reply) {
		case JOptionPane.NO_OPTION:
		case JOptionPane.CLOSED_OPTION:
			break;
		case JOptionPane.YES_OPTION:
			buttonsAndFields.systemExit();
			frame.dispose();
			System.exit(0);
			break;
		default:
		}

	}

	/*--------------------------------------------------------------*
	 *																* 
	 * Audio Panel class											*
	 * 																*
	 *--------------------------------------------------------------*/

	/**
	 * Class which inherits from JPanel, divided to different sections
	 * associated with choosing, playing or recording sound.
	 */

	@SuppressWarnings("serial")
	public class AudioPanel extends JPanel {

		/**
		 * A panel which is intended for choosing a file.
		 */
		private JPanel panelFileChooser;

		/**
		 * A panel which is intended for control playing or recording audio
		 * data.
		 */
		private JPanel panelPlayAndRecord;

		/**
		 * A panel which is intended for showing waveform of audio file.
		 */
		private JPanel panelInputWaveform;
		private DisplayPanel displayPanelWaveform;

		/**
		 * Create the panel.
		 */
		public AudioPanel() {
			initialize();
		}

		/**
		 * Initialize the contents of the panel.
		 */
		private void initialize() {

			// Set the top frame for dialogs
			Dialogs.setFrame((JFrame) SwingUtilities.getWindowAncestor(this));

			setBackground(Color.WHITE);
			setLayout(new BorderLayout(0, 0));

			addFileChooserPanel();
			addFileChooserPanelComponents();

			addInputWaveformPanel();

			addPlayAndRecordPanel();
			addPlayAndRecordPanelComponents();

		}

		/**
		 * Add a panel which is intended for choosing a file.
		 */
		private void addFileChooserPanel() {
			panelFileChooser = new JPanel();
			panelFileChooser.setBackground(Color.WHITE);
			panelFileChooser.setBorder(new TitledBorder("Choose File"));
			((TitledBorder) panelFileChooser.getBorder()).setTitleColor(Color.BLACK);
			add(panelFileChooser, BorderLayout.NORTH);
			GridBagLayout gbl_panelFileChooser = new GridBagLayout();
			gbl_panelFileChooser.columnWidths = new int[] { 0, 0, 0 };
			gbl_panelFileChooser.rowHeights = new int[] { 0, 0, 0 };
			gbl_panelFileChooser.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
			gbl_panelFileChooser.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
			panelFileChooser.setLayout(gbl_panelFileChooser);
		}

		/**
		 * Add buttons and text field used to open or save a file and inform
		 * about file name.
		 */
		private void addFileChooserPanelComponents() {
			addButtonToOpenFile();
			addTextFieldWithFilePath();
			addButtonToSaveFile();
		}

		/**
		 * Add a panel which is intended for showing waveform of audio file.
		 */
		private void addInputWaveformPanel() {
			panelInputWaveform = new JPanel();
			panelInputWaveform.setBackground(Color.WHITE);
			panelInputWaveform.setBorder(new TitledBorder("Waveform"));
			panelInputWaveform.setLayout(new BoxLayout(panelInputWaveform, BoxLayout.X_AXIS));
			((TitledBorder) panelInputWaveform.getBorder()).setTitleColor(Color.BLACK);
			displayPanelWaveform = new DisplayPanel();
			panelInputWaveform.add(displayPanelWaveform);

			add(panelInputWaveform, BorderLayout.CENTER);

			buttonsAndFields.getButtonListener().setDisplayPanelWaveformAutoref(displayPanelWaveform);

		}

		/**
		 * Add a panel which is intended for control playing or recording audio
		 * data.
		 */
		private void addPlayAndRecordPanel() {
			panelPlayAndRecord = new JPanel();
			panelPlayAndRecord.setBackground(Color.WHITE);
			panelPlayAndRecord.setBorder(new TitledBorder("Play/Record"));
			((TitledBorder) panelPlayAndRecord.getBorder()).setTitleColor(Color.BLACK);
			add(panelPlayAndRecord, BorderLayout.SOUTH);
			GridBagLayout gbl_panelPlayAndRecord = new GridBagLayout();
			gbl_panelPlayAndRecord.columnWidths = new int[] { 0, 0, 0, 0, 0, 0 };
			gbl_panelPlayAndRecord.rowHeights = new int[] { 0, 0 };
			gbl_panelPlayAndRecord.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
			gbl_panelPlayAndRecord.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
			panelPlayAndRecord.setLayout(gbl_panelPlayAndRecord);
		}

		/**
		 * Add buttons used to play or record a file
		 */
		private void addPlayAndRecordPanelComponents() {
			addPlayButton();
			addPauseButton();
			addStopButton();
			addRecordButton();
		}

		/**
		 * Add a button used to open a file.
		 */
		private void addButtonToOpenFile() {
			GridBagConstraints gbc_btnOpenFile = new GridBagConstraints();
			gbc_btnOpenFile.insets = new Insets(0, 0, 5, 5);
			gbc_btnOpenFile.gridx = 0;
			gbc_btnOpenFile.gridy = 0;
			panelFileChooser.add(buttonsAndFields.openFile, gbc_btnOpenFile);
		}

		/**
		 * Add a button used to save a file.
		 */
		private void addButtonToSaveFile() {
			GridBagConstraints gbc_btnSaveFile = new GridBagConstraints();
			gbc_btnSaveFile.insets = new Insets(0, 0, 0, 5);
			gbc_btnSaveFile.gridx = 0;
			gbc_btnSaveFile.gridy = 1;
			panelFileChooser.add(buttonsAndFields.saveFile, gbc_btnSaveFile);
		}

		/**
		 * Add a text field used to inform about file name
		 */
		private void addTextFieldWithFilePath() {
			GridBagConstraints gbc_textFieldOpenFile = new GridBagConstraints();
			gbc_textFieldOpenFile.insets = new Insets(0, 0, 5, 0);
			gbc_textFieldOpenFile.fill = GridBagConstraints.HORIZONTAL;
			gbc_textFieldOpenFile.gridx = 1;
			gbc_textFieldOpenFile.gridy = 0;
			panelFileChooser.add(buttonsAndFields.textFieldFileName, gbc_textFieldOpenFile);

		}

		/**
		 * Add a play button in play and record panel.
		 */
		private void addPlayButton() {
			GridBagConstraints gbc_btnPRPlay = new GridBagConstraints();
			gbc_btnPRPlay.insets = new Insets(0, 0, 0, 5);
			gbc_btnPRPlay.gridx = 0;
			gbc_btnPRPlay.gridy = 0;
			panelPlayAndRecord.add(buttonsAndFields.playFile, gbc_btnPRPlay);
		}

		/**
		 * Add a pause button in play and record panel.
		 */
		private void addPauseButton() {
			GridBagConstraints gbc_btnPRPause = new GridBagConstraints();
			gbc_btnPRPause.insets = new Insets(0, 0, 0, 5);
			gbc_btnPRPause.gridx = 1;
			gbc_btnPRPause.gridy = 0;
			panelPlayAndRecord.add(buttonsAndFields.pauseFile, gbc_btnPRPause);
		}

		/**
		 * Add a stop button in play and record panel.
		 */
		private void addStopButton() {
			GridBagConstraints gbc_btnPRStop = new GridBagConstraints();
			gbc_btnPRStop.insets = new Insets(0, 0, 0, 5);
			gbc_btnPRStop.gridx = 2;
			gbc_btnPRStop.gridy = 0;
			panelPlayAndRecord.add(buttonsAndFields.stopFile, gbc_btnPRStop);
		}

		/**
		 * Add a record button in play and record panel.
		 */
		private void addRecordButton() {
			GridBagConstraints gbc_btnPRRecord = new GridBagConstraints();
			gbc_btnPRRecord.insets = new Insets(0, 0, 0, 5);
			gbc_btnPRRecord.gridx = 3;
			gbc_btnPRRecord.gridy = 0;
			panelPlayAndRecord.add(buttonsAndFields.recordFile, gbc_btnPRRecord);
		}
	}

	/*--------------------------------------------------*
	 * 													*
	 * Analysis panel class								*
	 * 													*
	 * -------------------------------------------------*/

	@SuppressWarnings("serial")
	public class AnalysisPanel extends JPanel {

		private DisplayPanel displayPanelAnalysis;

		private JPanel panelAnalysisResult;
		private JPanel panelAnalysisWaveform;
		private JPanel panelAnalysisButtons;

		/**
		 * Create the panel.
		 */
		public AnalysisPanel() {
			initialize();
		}

		/**
		 * Initialize the contents of the frame.
		 */
		private void initialize() {

			setLayout(new BorderLayout());

			addPanelAnalysisResult();
			addPanelAnalysisResultComponents();
			addPanelAnalysisWaveform();

			addPanelAnalysisButtons();
			addPanleAnalysisButtonsComponents();

		}

		private void addPanelAnalysisResult() {
			panelAnalysisResult = new JPanel();
			panelAnalysisResult.setBackground(Color.WHITE);
			panelAnalysisResult.setBorder(new TitledBorder("Result"));
			((TitledBorder) panelAnalysisResult.getBorder()).setTitleColor(Color.BLACK);
			add(panelAnalysisResult, BorderLayout.NORTH);
			GridBagLayout gbl_panelAnalysisResult = new GridBagLayout();
			gbl_panelAnalysisResult.columnWidths = new int[] { 0, 0, 0 };
			gbl_panelAnalysisResult.rowHeights = new int[] { 0, 0, 0 };
			gbl_panelAnalysisResult.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
			gbl_panelAnalysisResult.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
			panelAnalysisResult.setLayout(gbl_panelAnalysisResult);
		}

		private void addPanelAnalysisResultComponents() {
			JLabel lblAverage = new JLabel(" Average frequency: ");
			GridBagConstraints gbc_lblAverage = new GridBagConstraints();
			gbc_lblAverage.insets = new Insets(0, 0, 5, 5);
			gbc_lblAverage.anchor = GridBagConstraints.WEST;
			gbc_lblAverage.gridx = 0;
			gbc_lblAverage.gridy = 0;
			panelAnalysisResult.add(lblAverage, gbc_lblAverage);

			GridBagConstraints gbc_textFieldAverageFrequency = new GridBagConstraints();
			gbc_textFieldAverageFrequency.insets = new Insets(0, 0, 5, 5);
			gbc_textFieldAverageFrequency.fill = GridBagConstraints.HORIZONTAL;
			gbc_textFieldAverageFrequency.gridx = 1;
			gbc_textFieldAverageFrequency.gridy = 0;
			panelAnalysisResult.add(buttonsAndFields.textFieldAverageFrequency, gbc_textFieldAverageFrequency);

			JLabel lblDecision = new JLabel(" Decision: ");
			GridBagConstraints gbc_lblDecision = new GridBagConstraints();
			gbc_lblDecision.insets = new Insets(0, 0, 5, 0);
			gbc_lblDecision.anchor = GridBagConstraints.WEST;
			gbc_lblDecision.gridx = 0;
			gbc_lblDecision.gridy = 1;
			panelAnalysisResult.add(lblDecision, gbc_lblDecision);

			GridBagConstraints gbc_Decision = new GridBagConstraints();
			gbc_Decision.insets = new Insets(0, 0, 5, 5);
			gbc_Decision.fill = GridBagConstraints.HORIZONTAL;
			gbc_Decision.gridx = 1;
			gbc_Decision.gridy = 1;
			panelAnalysisResult.add(buttonsAndFields.textFieldDecision, gbc_Decision);
		}

		private void addPanelAnalysisWaveform() {
			panelAnalysisWaveform = new JPanel();
			panelAnalysisWaveform.setBackground(Color.WHITE);
			panelAnalysisWaveform.setBorder(new TitledBorder("IMF function waveform"));
			((TitledBorder) panelAnalysisWaveform.getBorder()).setTitleColor(Color.BLACK);
			panelAnalysisWaveform.setLayout(new BoxLayout(panelAnalysisWaveform, BoxLayout.X_AXIS));
			displayPanelAnalysis = new DisplayPanel();
			panelAnalysisWaveform.add(displayPanelAnalysis);
			add(panelAnalysisWaveform, BorderLayout.CENTER);
			buttonsAndFields.getButtonListener().setDisplayPanelAnalysisAutoref(displayPanelAnalysis);
		}

		private void addPanelAnalysisButtons() {
			panelAnalysisButtons = new JPanel();
			panelAnalysisButtons.setBackground(Color.WHITE);
			panelAnalysisButtons.setBorder(new TitledBorder("Analysis"));
			((TitledBorder) panelAnalysisButtons.getBorder()).setTitleColor(Color.BLACK);
			add(panelAnalysisButtons, BorderLayout.SOUTH);
			GridBagLayout gbl_panelAnalysisButtons = new GridBagLayout();
			gbl_panelAnalysisButtons.columnWidths = new int[] { 0, 0, 0 };
			gbl_panelAnalysisButtons.rowHeights = new int[] { 0, 0 };
			gbl_panelAnalysisButtons.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
			gbl_panelAnalysisButtons.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
			panelAnalysisButtons.setLayout(gbl_panelAnalysisButtons);
		}

		private void addPanleAnalysisButtonsComponents() {
			GridBagConstraints gbc_btnStartAnalysis = new GridBagConstraints();
			gbc_btnStartAnalysis.insets = new Insets(0, 0, 5, 5);
			gbc_btnStartAnalysis.gridx = 0;
			gbc_btnStartAnalysis.gridy = 0;
			panelAnalysisButtons.add(buttonsAndFields.startAnalysis, gbc_btnStartAnalysis);

			GridBagConstraints gbc_btnStopAnalysis = new GridBagConstraints();
			gbc_btnStopAnalysis.insets = new Insets(0, 0, 5, 5);
			gbc_btnStopAnalysis.gridx = 1;
			gbc_btnStopAnalysis.gridy = 0;
			panelAnalysisButtons.add(buttonsAndFields.stopAnalysis, gbc_btnStopAnalysis);
		}
	}
}
