package vsa;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import access.mypackage.offdebug.Debug;

@SuppressWarnings("serial")
public class DisplayPanel extends JPanel{
	
	public enum State {
		INACTIVITY, INTERRUPTING, DRAWING
	}
	
	private State displayState;
	private final Path2D.Float path = new Path2D.Float();
	private final Object pathLock = new Object();
	private final Object stringLock = new Object();
	private final Path2D.Float verticalLines = new Path2D.Float();
	private final Path2D.Float horizontalLine = new Path2D.Float();
	
	public void setState(State state) {
		synchronized(this) {
			displayState = state;
		}
	}
	
	public State getState() {
		return displayState;
	}
	
	
	public DisplayPanel() {
		setState(State.INACTIVITY);
		setOpaque(false);
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
	}
	
	public void displayFunction(float[] samples) {
		setState(State.DRAWING);
		resetPath();
		repaint();
		makeFunction(path, samples);
		makeLinearScale(horizontalLine, verticalLines, samples);
		synchronized(stringLock) {
			values = makeValues();
		}
		if(displayState == State.INTERRUPTING);
		repaint();
	}
	
	private void resetPath() {
		synchronized(pathLock) {
			path.reset();
			verticalLines.reset();
			horizontalLine.reset();
		}
	}
	
	String units;
	float yAxes[];
	int orderOfMagnitude;
	int nrOfYAxes = 0;
	String[] values = null;
	
	private String[] makeValues() {
		String[] values = new String[nrOfYAxes+1];
		if(orderOfMagnitude >= 0)
			for(int i = 0; i <= nrOfYAxes; i++) 
				values[i] = Integer.toString((int)(i * Math.pow(10d,orderOfMagnitude))) + units;
		else
			for(int i = 0; i <= nrOfYAxes; i++)
				values[i] = Integer.toString((int)(i * Math.pow(10d,orderOfMagnitude + 3)))  + units;
		return values;
	}
	
	
	private void makeLinearScale(Path2D.Float horizontalPath, Path2D.Float verticalPath, float[] samples){
		
		float sampleRate = AudioFileOperations.getAudioFormat().getSampleRate();
		Debug.debug(Float.toString(sampleRate));
		
		nrOfYAxes = 0;
		orderOfMagnitude = -3;
		units = "ms";
		
		int width = this.getSize().width;
		int height = this.getSize().height;
		
		horizontalPath.moveTo(0, height/2);
		horizontalPath.lineTo(width-1, height/2);
		
		while(true) {
			if((nrOfYAxes = (int) (samples.length /sampleRate/Math.pow(10d, orderOfMagnitude))) > 10)
				orderOfMagnitude++;
			else
				break;
		}
		if(orderOfMagnitude >= 0)
			units = "s";
		
		yAxes = new float[nrOfYAxes];
		synchronized (pathLock) {
			for(int i = 0; i < nrOfYAxes; i++) {
				yAxes[i] = (float)(sampleRate * Math.pow(10d, orderOfMagnitude)*(i+1))/samples.length * width;
				verticalPath.moveTo(yAxes[i], 0);
				verticalPath.lineTo(yAxes[i], height);
			}
		}
	}
	
	
	
	//narysowanie przebiegu
	//kazdy sampel to jedno miejsce do narysowania bo sygnal jest mono, wiêc nie trzeba nic uœredniaæ
	private void makeFunction(Path2D.Float path, float[] samples){
		synchronized (pathLock) {
			
			if (samples.length < 2) {
				return;
			}
			
			int hd2 = this.getSize().height / 2;
			path.moveTo(0, hd2 - samples[0] * hd2);
			
			int width = this.getSize().width;
			
			AudioFileOperations.normalized(samples);
			
			synchronized (pathLock) {
				for(int i = 1; i < samples.length; i ++)
					path.lineTo((float)i / (float)samples.length * (width-1), hd2 - samples[i] * (hd2-1) * 0.99);
			}
		}
	}
	
	public void reset() {
		resetPath();
		synchronized(stringLock) {
			values = null;
		}
		repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2d = (Graphics2D) g.create();
		drawBackground(g2d);
		drawAxis(g2d);
		drawValues(g2d);
		drawFunction(g2d);
		g2d.dispose();
		setState(State.INACTIVITY);
	}
	
	private void drawValues(Graphics2D g2d) {
		synchronized(stringLock) {
			if (values != null) {
				Font f = new Font("Dialog", Font.PLAIN, 10);
				g2d.setFont(f);
				FontMetrics fm = g2d.getFontMetrics();
				int fontHeight = fm.getAscent() + fm.getDescent();
				
				if(orderOfMagnitude >= 0) {
					g2d.drawString(values[0], 2, this.getSize().height-2);
					for(int i = 1; i < values.length; i++)
						g2d.drawString(values[i], yAxes[i-1] + 2, this.getSize().height-2);
				}
				else {
					g2d.drawString(values[0].substring(0, values[0].length() - 2), 2, this.getSize().height-2-fontHeight);
					g2d.drawString(values[0].substring(values[0].length() - 2), 2, this.getSize().height-2);
					for(int i = 1; i < values.length; i++) {
						g2d.drawString(values[i].substring(0, values[i].length() - 2), yAxes[i-1] + 2, this.getSize().height-2-fontHeight);
						g2d.drawString(values[i].substring(values[i].length()-2), yAxes[i-1] + 2, this.getSize().height-2);
					}
				}
			}
		}
	}
	
	private void drawAxis(Graphics2D g2d) {
		g2d.setColor(Color.GRAY);
		synchronized(pathLock) {
			g2d.draw(verticalLines);
			g2d.draw(horizontalLine);
		}
	}
	
	private void drawBackground(Graphics2D g2d) {
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, this.getSize().width-1, this.getSize().height-1);
	}
	
	private void drawFunction(Graphics2D g2d) {
		g2d.setColor(Color.BLACK);
		synchronized (pathLock) {
			g2d.draw(path);
		}
	}
}


