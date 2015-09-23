package vsa;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.DefaultCaret;

import access.mypackage.offdebug.Debug;

/**
 * Klasa prezentuj¹ca w dodatkowym oknie wyniki obecnej i poprzednich analiz.
 *
 */
public class AnalysisDataWindow implements ActionListener{
	
	private JFrame frame;
	private JButton btnClear;
	private JButton btnSave;
	private JTextArea txtArea;
	
	private Font monospacedFontBold = new Font(Font.MONOSPACED, Font.BOLD, 12);
	private Font monospacedFontPlain = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	
	private int id = 0;
	
	
	
	
	public AnalysisDataWindow() {
		initialize();
	}
	
	public void setVisible(boolean state) {
		frame.setVisible(state);
	}
	
	private void initialize() {
		frame = new JFrame("Results of Analyses");
		frame.setVisible(false);
		frame.setBounds(100, 100, 680, 300);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				frame.setVisible(false);;
			}
			
		});
		
		JPanel outerPanel = new JPanel();
		frame.getContentPane().add(outerPanel, BorderLayout.CENTER);
		outerPanel.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outerPanel.add(scrollPane, BorderLayout.CENTER);
		
		JTextField txtColumnHeader = new JTextField();
		
		txtColumnHeader.setFont(monospacedFontBold);
		txtColumnHeader.setEditable(false);
		txtColumnHeader.setAutoscrolls(false);
		
		String columnHeader = createColumnHeader();
		
		txtColumnHeader.setText(columnHeader);
		scrollPane.setColumnHeaderView(txtColumnHeader);
		
		
		txtArea = new JTextArea();
		DefaultCaret caret = (DefaultCaret) txtArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		
		txtArea.setFont(monospacedFontPlain);
		txtArea.setAutoscrolls(true);
		txtArea.setEditable(false);
		scrollPane.setViewportView(txtArea);
		
		JPanel innerPanel = new JPanel();
		outerPanel.add(innerPanel, BorderLayout.SOUTH);
		
		btnSave = new JButton("Save");
		btnSave.addActionListener(this);
		innerPanel.add(btnSave);
		
		btnClear = new JButton("Clear");
		btnClear.addActionListener(this);
		innerPanel.add(btnClear);
		
		frame.setResizable(false);
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnClear) {
			txtArea.setText("");
			id = 0;
		}
		else if (e.getSource() == btnSave) {
			
			@SuppressWarnings("serial")
			class SaveTxtFileChooser extends JFileChooser {
				
				public SaveTxtFileChooser() {
					super(new File("."));
				}
				@Override
				public void approveSelection() {
					File f = getSelectedFile();
					if(f.exists() && getDialogType() == SAVE_DIALOG) {
						int result = JOptionPane.showConfirmDialog(this, "The file exsist, overwrite?", "Exsisting file", JOptionPane.YES_NO_OPTION);
						switch(result) {
						case JOptionPane.YES_OPTION:
							super.approveSelection();
							return;
						case JOptionPane.NO_OPTION:
						case JOptionPane.CLOSED_OPTION:
							return;
						}
					}
					super.approveSelection();
				}
			}
			
			SaveTxtFileChooser fileChooser = new SaveTxtFileChooser();
			
			if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
				String pathToSave = fileChooser.getSelectedFile().getAbsolutePath();
				int i = pathToSave.lastIndexOf('.');
				if(( i>0) && (i < pathToSave.length() - 1)) {
					String ext = pathToSave.substring(i+1).toLowerCase();
					if (!((ext.equals("txt")))) {
						ext = "txt";
						pathToSave = pathToSave.substring(0, i + 1) + ext;
					}
				}
				else {
					pathToSave += ".txt";
				}
				
				synchronized(this) {
					PrintWriter pw = null;
					try {
						pw = new PrintWriter(new FileWriter(pathToSave, false));
						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						Date date = new Date();
						pw.println("Voice Stress Analysis");
						pw.println(dateFormat.format(date));
						pw.println();
						pw.println(createColumnHeader());
						pw.println();
						pw.print(txtArea.getText());
					} catch (IOException e1) {
						Debug.debug(e1.toString());
					} finally {
						pw.close();
					}
				}
			}
		}
	}
	
	private int emptySpacesFileFrequency = 40;
	private int emptySpacesFrequencyDecision = 15;
	private int emptySpacesIdFile = 3;
	
	/**
	 * Stworzenie nag³ówka dla kolumn.
	 * @return ci¹g znaków umieszczany w nag³ówku
	 */
	private String createColumnHeader() {
		String str = "ID";
		for(int i = 0; i < emptySpacesIdFile; i++)
			str += " ";
		str += "File";
		for(int i = 0; i < emptySpacesFileFrequency; i++)
			str += " ";
		str += "Frequency";
		for(int i = 0; i < emptySpacesFrequencyDecision; i++)
			str += " ";
		str += "Decision";
		return str;
	}
	
	private int spaceForFileName = emptySpacesFileFrequency + 4;
	private int spaceForFrequency = emptySpacesFrequencyDecision + 9;
	
	/**
	 * Dodanie kolejnego wiersza do pola przechowuj¹cego wyniki wykonanych analiz.
	 * @param fileName nazwa pliku
	 * @param frequency czêstotliwoœæ mikrodr¿enia
	 * @param decision decyzja o wykryciu stresu
	 */
	public void addAnalysisData(String fileName, String frequency, String decision) {
		id++;
		String name, freq, number;
		if (id < 10)
			number = Integer.toString(id) + ".   ";
		else if (id < 100)
			number = Integer.toString(id) + ".  ";
		else
			number = Integer.toString(id) + ". ";
		
		if(fileName.length() > spaceForFileName - 2)
			name = fileName.substring(0, 3) + "..." + fileName.substring(fileName.length() - (spaceForFileName - 9), fileName.length()) + "  ";
		else {
			name = fileName;
			for(int i = 0; i < spaceForFileName - fileName.length(); i++)
				name += " ";
		}
		
		freq = frequency;
		for(int i = 0; i < spaceForFrequency - frequency.length(); i++)
			freq += " ";
		
		synchronized(this) {
			txtArea.append(number + name + freq + decision + '\n');
		}
	}
}
