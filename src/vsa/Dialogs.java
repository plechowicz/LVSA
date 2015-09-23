package vsa;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Dialogs {
	
	private static JFrame frame = null;
	
	
	public static void setFrame(JFrame f) {
		frame = f;
	}
	
	public static void showMessage(String message) {
		JOptionPane.showMessageDialog(frame, message);
	}
}
