package vsa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JOptionPane;

/***************************************************
 * Klasa SoundTest
 * 
 * Klasa odtwarzaj�ca i nagrywaj�ca d�wi�k przy pomocy wybranego miksera
 * 
 * *************************************************
 */
public class SoundTest {

	/**
	 * ---------------------------------------------------------------------
	 * 
	 * Pola klasy
	 * 
	 * ---------------------------------------------------------------------
	 */

	/**
	 * Autoreferencja do p�l steruj�cych dzia�aniem klasy SoundTest.
	 */
	private MixerChooserFields mixerChooserFieldsAutoref;

	/**
	 * Klasy wewn�trzne inpuSoundTest i outputSoundTest.
	 */
	InputSoundTest inputSoundTest;
	OutputSoundTest outputSoundTest;

	/**
	 * ---------------------------------------------------------------------
	 * 
	 * Metody klasy
	 * 
	 * ---------------------------------------------------------------------
	 */

	/**
	 * Domy�lny konstruktor.
	 */
	public SoundTest() {
		inputSoundTest = new InputSoundTest();
		outputSoundTest = new OutputSoundTest();
	}

	/**
	 * Ustawienie autoreferencji do MixerChooserFields.
	 * 
	 * @param mixerChooserFields
	 *            autoreferencja do obiektu klasy
	 */
	void setMixerChooserFieldsAutoref(MixerChooserFields mixerChooserFields) {
		mixerChooserFieldsAutoref = mixerChooserFields;
	}

	/**
	 * -------------------------------------------------------------------
	 * 
	 * Klasy wewn�trzne klasy SounTest
	 * 
	 * -------------------------------------------------------------------
	 */

	/**********************************************
	 * Klasa InputSundTest
	 * 
	 * Test przechwytywania d�wi�ku z mikrofon�w
	 * 
	 * ********************************************
	 */
	class InputSoundTest {

		/**
		 * -------------------------------------------------------------------
		 * 
		 * Pola klasy
		 * 
		 * -------------------------------------------------------------------
		 */

		final int bufSize = 16384;
		Capture capture = new Capture();
		Playback playback = new Playback();
		AudioInputStream audioInputStream = null;

		public boolean isRecordedStramToPlay() {
			return (audioInputStream != null);
		}

		public void close() {
			if (playback.thread != null) {
				mixerChooserFieldsAutoref.btnPlaybackInputTest.doClick(0);
			}
			if (capture.thread != null) {
				mixerChooserFieldsAutoref.btnCaptureInputTest.doClick(0);
			}
		}

		/**
		 * Odtwarzanie audio.
		 */
		public class Playback implements Runnable {

			SourceDataLine line;

			Thread thread;

			public void start() {
				thread = new Thread(this);
				thread.setName("Playback");
				thread.start();
			}

			public void stop() {
				thread = null;
			}

			private void shutDown(String message) {
				if (message != null) {
					JOptionPane.showMessageDialog(mixerChooserFieldsAutoref.mixerChooserWindow.getFrame(), message);
				}
				if (thread != null) {
					thread = null;
				}
				mixerChooserFieldsAutoref.setDefaultState();
			}

			public void run() {

				// nie ma nic do odtworzenia w streamie
				if (audioInputStream == null) {
					shutDown("No loaded audio to play back");
					return;
				}
				// Zresetowanie na poczatek streamu
				try {
					audioInputStream.reset();
				} catch (Exception e) {
					shutDown("Unable to reset the stream\n" + e);
					return;
				}

				// Otrzymanie streamu z pozadanym formatem audio do odtworzenia

				AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
				float rate = 44100.0f;
				int channels = 1;
				int frameSize = 2;
				int sampleSize = 16;
				boolean bigEndian = false;

				AudioFormat format = new AudioFormat(encoding, rate, sampleSize, channels, frameSize, rate, bigEndian);

				AudioInputStream playbackInputStream = AudioSystem.getAudioInputStream(format, audioInputStream);

				if (playbackInputStream == null) {
					shutDown("Unable to convert stream of format " + audioInputStream + " to format " + format);
					return;
				}

				// Zdefiniowanie atrybut�w dla linii,
				// i upewnienie si�, �e dana linia jest wspierana.

				DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

				if (MixerChooser.getOutputMixerState() == MixerChooser.MixerState.DEFAULT) {
					if (!AudioSystem.isLineSupported(info)) {
						shutDown("Line matching " + info + " not supported.");
						return;
					}

					// pobierz i otw�rz source data line do odtwarzania.

					try {
						line = (SourceDataLine) AudioSystem.getLine(info);
						line.open(format, bufSize);
					} catch (LineUnavailableException ex) {
						shutDown("Unable to open the line: " + ex);
						return;
					}
				} else {
					Mixer mixer = MixerChooser.getOutputMixer();
					if (!mixer.isLineSupported(info)) {
						shutDown("Line matching " + info + " not supported in this mixer.");
						return;
					}

					// pobierz i otw�rz source data line do odtwarzania

					try {
						line = (SourceDataLine) mixer.getLine(info);
						line.open(format, bufSize);
					} catch (LineUnavailableException ex) {
						shutDown("Unable to open the line: " + ex);
						return;
					}
				}

				// odtworzenie przechwyconego audio

				int bufferLengthInBytes = line.getBufferSize();
				byte[] data = new byte[bufferLengthInBytes];
				int numBytesRead = 0;

				// rozpocz�cie linii
				line.start();

				while (thread != null) {
					try {
						if ((numBytesRead = playbackInputStream.read(data)) == -1) {
							break;
						}
						int numBytesRemaining = numBytesRead;
						while (numBytesRemaining > 0) {
							numBytesRemaining -= line.write(data, 0, numBytesRemaining);
						}
					} catch (Exception e) {
						shutDown("Error during playback: " + e);
						break;
					}
				}
				// koniec streamu
				// wysuszenie linii
				// zamkni�cie linii
				if (thread != null) {
					line.drain();
				}
				line.stop();
				line.close();
				line = null;
				shutDown(null);
			}
		} // Koniec klasy Playback

		/**
		 * Zczytanie strumienia wej�ciowego
		 */
		class Capture implements Runnable {

			TargetDataLine line;

			Thread thread;

			public void start() {
				thread = new Thread(this);
				thread.setName("Capture");
				thread.start();
			}

			public void stop() {
				thread = null;
			}

			private void shutDown(String message) {
				if ((message) != null && thread != null) {
					thread = null;
					JOptionPane.showMessageDialog(mixerChooserFieldsAutoref.mixerChooserWindow.getFrame(), message);
					mixerChooserFieldsAutoref.setDefaultState();
				}
			}

			public void run() {

				audioInputStream = null;

				// Zdefiniowanie potrzebnej lini
				// i upewnienie si�, �e dana linia jest wspierana

				AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
				float rate = 44100.0f;
				int channels = 1;
				int frameSize = 2;
				int sampleSize = 16;
				boolean bigEndian = false;

				AudioFormat format = new AudioFormat(encoding, rate, sampleSize, channels, frameSize, rate, bigEndian);

				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

				try {
					if (MixerChooser.getInputMixerState() == MixerChooser.MixerState.DEFAULT) {
						if (!AudioSystem.isLineSupported(info)) {
							shutDown("Line matching " + info + " not supported.");
							return;
						}

						// pobierz i otw�rz source data line do odtwarzania.

						try {
							line = (TargetDataLine) AudioSystem.getLine(info);
							line.open(format, bufSize);
						} catch (LineUnavailableException ex) {
							shutDown("Unable to open the line: " + ex);
							return;
						}
					} else {
						Mixer mixer = MixerChooser.getInputMixer();
						if (!mixer.isLineSupported(info)) {
							shutDown("Line matching " + info + " not supported in this mixer.");
							return;
						}

						// pobierz i otw�rz source data line do odtwarzania

						try {
							line = (TargetDataLine) mixer.getLine(info);
							line.open(format, bufSize);
						} catch (LineUnavailableException ex) {
							shutDown("Unable to open the line: " + ex);
							return;
						}
					}
				} catch (Exception ex) {
					shutDown(ex.toString());
					return;
				}

				// Zapisanie przechwyconego audio z wej�cia
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int bufferLengthInBytes = line.getBufferSize();
				int frameSizeInBytes = line.getFormat().getFrameSize();
				byte[] data = new byte[bufferLengthInBytes];
				int numBytesRead;

				line.start();

				while (thread != null) {
					if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
						break;
					}
					baos.write(data, 0, numBytesRead);
				}

				// koniec strumienia
				// zatrzymanie i zamkni�cie lini
				line.stop();
				line.close();
				line = null;

				// zatrzymanie i zamkniecie strumienia wyj�ciowego
				try {
					baos.flush();
					baos.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}

				// za�adowanie bajt�w do audio input stream w celu ich
				// odtworzenia p�niej

				byte audioBytes[] = baos.toByteArray();
				ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
				audioInputStream = new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);

				try {
					audioInputStream.reset();
				} catch (Exception ex) {
					ex.printStackTrace();
					return;
				}

			}
		} // Koniec klasy Capture

	}// Koniec klasy InputSoundTest

	/**
	 * Odtwarzanie d�wi�ku z pliku
	 */
	class OutputSoundTest {
		final int bufSize = 16384;
		Playback playback = new Playback();
		AudioInputStream audioInputStream;
		BrownNoiseGenerator brownNoiseGenerator = new BrownNoiseGenerator();

		public void close() {
			if (playback.thread != null) {
				mixerChooserFieldsAutoref.btnPlaybackOutputTest.doClick(0);
			}
		}

		/**
		 * Odtwarzanie audio.
		 */
		public class Playback implements Runnable {

			SourceDataLine line;

			Thread thread;

			public void start() {
				thread = new Thread(this);
				thread.setName("Playback output");
				thread.start();
			}

			public void stop() {
				thread = null;
			}

			private void shutDown(String message) {
				if (message != null) {
					JOptionPane.showMessageDialog(mixerChooserFieldsAutoref.mixerChooserWindow.getFrame(), message);
				}
				if (thread != null) {
					thread = null;
				}
				mixerChooserFieldsAutoref.setDefaultState();
			}

			public void run() {

				// Otrzymanie streamu z pozadanym formatem audio do odtworzenia

				AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
				float rate = 44100.0f;
				int channels = 1;
				int frameSize = 2;
				int sampleSize = 16;
				boolean bigEndian = false;

				AudioFormat format = new AudioFormat(encoding, rate, sampleSize, channels, frameSize, rate, bigEndian);

				// Zdefiniowanie atrybut�w dla linii,
				// i upewnienie si�, �e dana linia jest wspierana.

				DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

				if (MixerChooser.getOutputMixerState() == MixerChooser.MixerState.DEFAULT) {
					if (!AudioSystem.isLineSupported(info)) {
						shutDown("Line matching " + info + " not supported.");
						return;
					}

					// pobierz i otw�rz source data line do odtwarzania.

					try {
						line = (SourceDataLine) AudioSystem.getLine(info);
						line.open(format, bufSize);
					} catch (LineUnavailableException ex) {
						shutDown("Unable to open the line: " + ex);
						return;
					}
				} else {
					Mixer mixer = MixerChooser.getOutputMixer();
					if (!mixer.isLineSupported(info)) {
						shutDown("Line matching " + info + " not supported in this mixer.");
						return;
					}

					// pobierz i otw�rz source data line do odtwarzania

					try {
						line = (SourceDataLine) mixer.getLine(info);
						line.open(format, bufSize);
					} catch (LineUnavailableException ex) {
						shutDown("Unable to open the line: " + ex);
						return;
					}
				}

				// odtworzenie przechwyconego audio

				int bufferLengthInBytes = line.getBufferSize();
				byte[] data = new byte[bufferLengthInBytes];
				double[] brownNoise = new double[bufferLengthInBytes / 2];

				// rozpocz�cie linii
				line.start();

				while (thread != null) {
					try {
						for (int i = 0; i < bufferLengthInBytes / 2; i++) {
							brownNoise[i] = brownNoiseGenerator.getNext();
							// little-indian
							data[2 * i] = (byte) ((long) (brownNoise[i] * 32767) & 0xFF);
							data[2 * i + 1] = (byte) ((((long) (brownNoise[i] * 32767)) & 0xFF00) >> 8);
						}
						int numBytesRemaining = data.length;
						while (numBytesRemaining > 0) {
							numBytesRemaining -= line.write(data, 0, numBytesRemaining);
						}
					} catch (Exception e) {
						shutDown("Error during playback: " + e);
						break;
					}
				}
				// koniec streamu
				// wysuszenie lini
				// zamkni�cie lini
				if (thread != null) {
					line.drain();
				}
				line.stop();
				line.close();
				line = null;
				shutDown(null);
			}
		} // Koniec klasy Playback
	}// koniec klasy OutputSoundTest
}
