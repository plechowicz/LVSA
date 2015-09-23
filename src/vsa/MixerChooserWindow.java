package vsa;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/*********************************************************************
 * 
 * Klasa b�d�ca interfejsem graficznym s�u��cym do wyboru miksera.
 *
 *********************************************************************/
public class MixerChooserWindow {

	/**
	 * --------------------------------------------------------------------
	 * 
	 * Pola klasy
	 * 
	 * --------------------------------------------------------------------
	 */

	/**
	 * Okno Mixer Choosera.
	 */
	private JFrame frame;

	/**
	 * Referencja do nadrz�dnego okna.
	 */
	private JFrame parentFrame;

	/**
	 * Klasa zawieraj�ca elementy do wype�nienia w panelach oraz ActionListenery
	 * do nich.
	 */
	private MixerChooserFields mixerChooserFields;

	/**
	 * Panele u�ywane do konstrukcji okna wyboru miksera
	 */
	private JPanel panelSelectMixerChooser;
	private JPanel panelMixerTester;
	private JPanel panelMixerConfirm;
	private JPanel panelTesterLeft;
	private JPanel panelTesterRight;

	/**
	 * ---------------------------------------------------------------------
	 * 
	 * Metody klasy
	 * 
	 * ---------------------------------------------------------------------
	 */

	/**
	 * Konstruktor. Tworzy nowe okno.
	 */
	public MixerChooserWindow(JFrame parentFrame) {
		this.parentFrame = parentFrame;
		parentFrame.setEnabled(false);
		initialize();
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * Metody prywatne
	 * 
	 * ----------------------------------------------------------------------
	 */

	/**
	 * Inicjalizacja zawarto�ci okna.
	 */
	private void initialize() {
		frame = new JFrame("Audio Input and Output Devices");
		frame.setBounds(100, 100, 450, 300);

		// okno nie ma si� zamyka� po wci�ni�ciu krzy�yka tylko wykona� jeszcze
		// operacje
		// porz�dkuj�ce. Ustawienie mikser�w na domy�lne. Przerwanie w�tk�w,
		// kt�re mog� odtwarza� lub nagrywa� d�wi�k.
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				shutDown();
			}

		});

		mixerChooserFields = new MixerChooserFields(this);

		addPanelSelectMixerChooser();
		addPanelSelectMixerChooserContent();
		addPanelMixerTester();
		addPanelMixerTesterContent();
		addPanelMixerConfirm();
		addPanelMixerConfirmContent();

		// dodanie listener�w do element�w umieszczonych w panelach
		mixerChooserFields.addActionListeners();

		frame.setVisible(true);
		frame.pack();
		frame.setResizable(false);

	}

	/**
	 * Dodanie panelu przeznaczonego do wyboru miksera.
	 */
	private void addPanelSelectMixerChooser() {
		panelSelectMixerChooser = new JPanel();
		frame.getContentPane().add(panelSelectMixerChooser, BorderLayout.NORTH);
		GridBagLayout gbl_panelSelectMixerChooser = new GridBagLayout();
		gbl_panelSelectMixerChooser.columnWidths = new int[] { 0, 0 };
		gbl_panelSelectMixerChooser.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_panelSelectMixerChooser.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelSelectMixerChooser.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelSelectMixerChooser.setLayout(gbl_panelSelectMixerChooser);
	}

	/**
	 * Dodanie komponent�w do panelu PanelSelectMixerChooser.
	 */
	private void addPanelSelectMixerChooserContent() {
		mixerChooserFields.lblInputMixerLabel = new JLabel("Input Device: ");
		GridBagConstraints gbc_lblInputMixerLabel = new GridBagConstraints();
		gbc_lblInputMixerLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblInputMixerLabel.gridx = 0;
		gbc_lblInputMixerLabel.gridy = 0;
		panelSelectMixerChooser.add(mixerChooserFields.lblInputMixerLabel, gbc_lblInputMixerLabel);

		mixerChooserFields.comboBoxInputMixer = new JComboBox<String>(MixerChooser.getInputMixersNames());
		GridBagConstraints gbc_comboBoxInputMixer = new GridBagConstraints();
		gbc_comboBoxInputMixer.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxInputMixer.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxInputMixer.gridx = 1;
		gbc_comboBoxInputMixer.gridy = 0;
		panelSelectMixerChooser.add(mixerChooserFields.comboBoxInputMixer, gbc_comboBoxInputMixer);

		mixerChooserFields.lblOutputMixerLabel = new JLabel("Output Device: ");
		GridBagConstraints gbc_lblOutputMixerLabel = new GridBagConstraints();
		gbc_lblOutputMixerLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblOutputMixerLabel.gridx = 0;
		gbc_lblOutputMixerLabel.gridy = 1;
		panelSelectMixerChooser.add(mixerChooserFields.lblOutputMixerLabel, gbc_lblOutputMixerLabel);

		mixerChooserFields.comboBoxOutputMixer = new JComboBox<String>(MixerChooser.getOutputMixersNames());
		GridBagConstraints gbc_comboBoxOutputMixer = new GridBagConstraints();
		gbc_comboBoxOutputMixer.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxOutputMixer.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxOutputMixer.gridx = 1;
		gbc_comboBoxOutputMixer.gridy = 1;
		panelSelectMixerChooser.add(mixerChooserFields.comboBoxOutputMixer, gbc_comboBoxOutputMixer);
	}

	/**
	 * Dodanie panelu przeznaczonego do testowania wybranego miksera
	 */
	private void addPanelMixerTester() {
		panelMixerTester = new JPanel();
		GridBagLayout gbl_panelMixerTester = new GridBagLayout();
		gbl_panelMixerTester.columnWidths = new int[] { 0, 0 };
		gbl_panelMixerTester.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_panelMixerTester.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelMixerTester.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelMixerTester.setLayout(gbl_panelMixerTester);

		panelTesterLeft = new JPanel();
		GridBagConstraints gbc_panelTesterLeft = new GridBagConstraints();
		gbc_panelTesterLeft.insets = new Insets(0, 0, 5, 5);
		gbc_panelTesterLeft.fill = GridBagConstraints.HORIZONTAL;
		gbc_panelTesterLeft.gridx = 0;
		gbc_panelTesterLeft.gridy = 0;
		panelMixerTester.add(panelTesterLeft, gbc_panelTesterLeft);

		panelTesterLeft.setBorder(new TitledBorder("Input Test"));
		((TitledBorder) panelTesterLeft.getBorder()).setTitleColor(Color.BLACK);

		panelTesterRight = new JPanel();
		GridBagConstraints gbc_panelTesterRight = new GridBagConstraints();
		gbc_panelTesterRight.insets = new Insets(0, 0, 5, 5);
		gbc_panelTesterRight.fill = GridBagConstraints.HORIZONTAL;
		gbc_panelTesterRight.gridx = 0;
		gbc_panelTesterRight.gridy = 1;
		panelMixerTester.add(panelTesterRight, gbc_panelTesterRight);

		panelTesterRight.setBorder(new TitledBorder("Output Test"));
		((TitledBorder) panelTesterRight.getBorder()).setTitleColor(Color.BLACK);
		frame.getContentPane().add(panelMixerTester, BorderLayout.CENTER);
	}

	/**
	 * Dodanie komponent�w do panelu PanelMixerTester.
	 */
	private void addPanelMixerTesterContent() {

		mixerChooserFields.btnPlaybackInputTest = new JButton("Play");
		mixerChooserFields.btnPlaybackInputTest.setEnabled(false);
		mixerChooserFields.btnCaptureInputTest = new JButton("Record");
		panelTesterLeft.add(mixerChooserFields.btnPlaybackInputTest);
		panelTesterLeft.add(mixerChooserFields.btnCaptureInputTest);

		mixerChooserFields.btnPlaybackOutputTest = new JButton("Play");
		panelTesterRight.add(mixerChooserFields.btnPlaybackOutputTest);
	}

	/**
	 * Dodanie panelu s�u��cego do potwierdzenia wyboru miksera.
	 */
	private void addPanelMixerConfirm() {
		panelMixerConfirm = new JPanel();
		panelMixerConfirm.setBorder(new TitledBorder("Confirm"));
		((TitledBorder) panelMixerConfirm.getBorder()).setTitleColor(Color.BLACK);
		frame.getContentPane().add(panelMixerConfirm, BorderLayout.SOUTH);
	}

	/**
	 * Dodanie komponent�w do panelu PanelMixerConfirm.
	 */
	private void addPanelMixerConfirmContent() {
		mixerChooserFields.btnMixerAccepted = new JButton("Ok");
		panelMixerConfirm.add(mixerChooserFields.btnMixerAccepted);
	}

	public JFrame getParentFrame() {
		return parentFrame;
	}

	public JFrame getFrame() {
		return frame;
	}

	/**
	 * W przypadku zamkniecia okna wyboru krzy�ykiem ustawiane sa domyslne
	 * miksery na wejscie i wyjscie. Oraz zostaj� podj�te odpowiednie kroki
	 * przerywaj�ce prac� w�tk�w, kt�re mog� odtwarza� albo nagrywa� d�wi�k.
	 */
	private void shutDown() {
		mixerChooserFields.shutDown();
		frame.setVisible(false);
		frame.dispose();
		parentFrame.setEnabled(true);
		parentFrame.setVisible(true);

	}

}
