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

import access.mypackage.offdebug.Debug;

/**
 * State that describes if file is currently playing, if it has been stopped,
 * paused, resumed, or it is recording.
 */
enum PlayState {
	PAUSE, RESUME, PLAY, STOP, RECORD
}

/**
 * Class used for capture and playback audio
 * 
 */
public class Player {

	ButtonListener buttonListener;

	Playback playback;
	Capture capture;

	/**
	 * State of playing file.
	 */
	volatile private PlayState playState;

	/**
	 * Is audio file playing. Describes if there is a thread for playing or
	 * capturing audio running.
	 */
	volatile boolean isFilePlaying;

	/**
	 * Array with bytes that represents audio signal.
	 */
	byte audioBytes[];

	/**
	 * Obiekt do blokowania podczas zapisu strumienia audio, albo odczytu.
	 */
	Object audioBytesPlayerLock = new Object();

	/**
	 * Audio format used in this application.
	 */
	AudioFormat format;

	static final int BUFFER_SIZE = 8192;

	PlayState getPlayState() {
		return playState;
	}

	private void setIsFilePlaying(boolean isFilePlaying) {
		synchronized (this) {
			this.isFilePlaying = isFilePlaying;
			notifyAll();
		}
	}

	private boolean isFilePlaying() {
		return isFilePlaying;
	}

	/**
	 * Class which play sound from an audio input stream
	 */
	class Playback implements Runnable {

		// Stream where audio bytes are loaded.
		private AudioInputStream audioInputStream;
		// Line that sends audio to speakers

		private SourceDataLine sourceDataLine;

		// Thread of playback
		private Thread thread;

		/**
		 * Start playing audio. If file is stopped create a new thread and start
		 * from beginning, if file were paused resume from that place.
		 */
		void play() {
			if (playState == PlayState.STOP) {
				setIsFilePlaying(true); // file started to play
				thread = new Thread(this);
				thread.setName("Play");
				synchronized (this) {
					playState = PlayState.PLAY;
				}
				thread.start();
			} else if (playState == PlayState.PAUSE) {
				synchronized (this) {
					playState = PlayState.RESUME;
					notifyAll();
				}
			}
		}

		/**
		 * Pause audio file. It is possible only when file were playing or
		 * resumed.
		 */
		void pause() {
			if ((playState == PlayState.PLAY) || (playState == PlayState.RESUME))
				synchronized (this) {
					playState = PlayState.PAUSE;
				}
		}

		/**
		 * Stop playing audio.
		 */
		void stop() {
			synchronized (this) {
				playState = PlayState.STOP;
			}

		}

		/**
		 * If errors occurred while playing audio, shut down the thread.
		 * PlayState gets value STOP and in override method run() play state is
		 * checked to stop the thread.
		 * 
		 * @param message
		 *            message that will show in pop up dialog
		 */
		private void shutDown(String message) {
			if (thread != null) {
				synchronized (this) {
					playState = PlayState.STOP;
				}
				Dialogs.showMessage(message);
			}
		}

		@Override
		public void run() {

			// przerwij je�eli oka�e si�, �e PlayState ma warto�� STOP
			getAudioInputStreamFromByteArray();
			if (playState == PlayState.STOP) {
				setIsFilePlaying(false);
				return;
			}

			getSourceDataLine();
			if (playState == PlayState.STOP) {
				setIsFilePlaying(false);
				return;
			}
			// ready for playing audio
			playAudio();
		}

		/**
		 * Get audio input stream from byte array with audio.
		 */
		private void getAudioInputStreamFromByteArray() {
			synchronized (audioBytesPlayerLock) {
				ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
				audioInputStream = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize());
			}

			// check if there is a stream to play
			if (audioInputStream == null)
				shutDown("No loaded audio to play back.");
		}

		/**
		 * Get source data line to send audio to speakers. First defines the
		 * data line info to check if that line is supported in the operating
		 * system. Then try to obtain that line from audio system.
		 */
		private void getSourceDataLine() {

			DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);

			if (MixerChooser.getOutputMixerState() == MixerChooser.MixerState.DEFAULT) {
				if (!AudioSystem.isLineSupported(dataLineInfo)) {
					shutDown("Unsuported data line type: " + dataLineInfo);
					return;
				}

				// pobierz i otw�rz source data line do odtwarzania.

				try {
					sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
					try {
						sourceDataLine.open(format, BUFFER_SIZE);

					} catch (LineUnavailableException e) {
						sourceDataLine.close();
						shutDown("Unable to open the line.");
					}
				} catch (LineUnavailableException e) {
					shutDown("Unable to open the line.");
				}
			} else {
				Mixer mixer = MixerChooser.getOutputMixer();
				if (!mixer.isLineSupported(dataLineInfo)) {
					shutDown("Unsuported data line type: " + dataLineInfo);
					return;
				}

				// pobierz i otw�rz source data line do odtwarzania

				try {
					sourceDataLine = (SourceDataLine) mixer.getLine(dataLineInfo);
					try {
						sourceDataLine.open(format, BUFFER_SIZE);

					} catch (LineUnavailableException e) {
						sourceDataLine.close();
						shutDown("Unable to open the line.");
					}
				} catch (LineUnavailableException e) {
					shutDown("Unable to open the line.");
				}
			}

		}

		/**
		 * Playing audio from audio input stream (writing to source data line).
		 */
		private void playAudio() {

			// byte array used for passing data from input stream to source data
			// line
			byte[] data = new byte[BUFFER_SIZE];

			sourceDataLine.start();

			// number of bytes red from audio input stream
			int numBytesRead;

			while (((playState == PlayState.PLAY) || (playState == PlayState.PAUSE) || (playState == PlayState.RESUME))
					&& (thread != null)) {
				// if play state is pause wait for resume (resume will do
				// notifyAll())
				if (playState == PlayState.PAUSE) {
					synchronized (this) {
						try {
							while (playState != PlayState.RESUME)
								wait();
						} catch (InterruptedException e) {
							break;
						}
					}
				}

				try {
					if ((numBytesRead = audioInputStream.read(data)) == -1)
						break;
					int numBytesRemaning = numBytesRead;
					while (numBytesRemaning > 0)
						numBytesRemaning -= sourceDataLine.write(data, 0, numBytesRemaning);
				} catch (Exception e) {
					shutDown("Error while playing: " + e.toString());
					sourceDataLine.flush();
					sourceDataLine.stop();
					sourceDataLine.close();
					sourceDataLine = null;
					audioInputStream = null;
					return;
				}
			}
			// If true audio file ended a there were no action from user.
			// It is necessary to switch buttons (like there were pressed stop
			// button).
			if (playState != PlayState.STOP) {
				sourceDataLine.drain();
				synchronized (this) {
					playState = PlayState.STOP;
				}
				// change buttons state
				synchronized (buttonListener) {
					buttonListener.buttonsStateStopFile();
				}
			}

			sourceDataLine.stop();
			sourceDataLine.close();
			sourceDataLine = null;

			setIsFilePlaying(false);

		}

		/**
		 * Initialize the playback.
		 */
		Playback() {
			sourceDataLine = null;
			thread = null;
		}

	}

	class Capture implements Runnable {

		// Line from microphone to computer
		private TargetDataLine targetDataLine;
		// Thread of capture
		private Thread thread;

		/**
		 * Start recording audio. It is possible only when there is no audio
		 * playing at this moment.
		 */
		void start() {
			if (playState == PlayState.STOP) {
				setIsFilePlaying(true);
				thread = new Thread(this);
				thread.setName("Capture");
				synchronized (this) {
					playState = PlayState.RECORD;
				}
				thread.start();
			}
		}

		/**
		 * Stop capturing audio.
		 */
		void stop() {
			synchronized (this) {
				playState = PlayState.STOP;
			}
		}

		/**
		 * If errors occurred while recording audio, shut down the thread.
		 * PlayState gets value STOP and in override method run() play state is
		 * checked to stop the thread.
		 * 
		 * @param message
		 *            message that will show in pop up dialog
		 */
		private void shutDown(String message) {
			if (thread != null) {
				synchronized (this) {
					playState = PlayState.STOP;
				}
				Dialogs.showMessage(message);
			}
		}

		/**
		 * Override from Runnable.
		 */
		@Override
		public void run() {

			getTargetDataLine();
			if (playState == PlayState.STOP) {
				setIsFilePlaying(false);
				return;
			}
			startRecording();
		}

		/**
		 * Get target data line to capture audio from microphone. First defines
		 * the data line info to check if that line is supported in the
		 * operating system. Then try to obtain that line from audio system.
		 */
		void getTargetDataLine() {
			DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);

			if (MixerChooser.getInputMixerState() == MixerChooser.MixerState.DEFAULT) {
				if (!AudioSystem.isLineSupported(dataLineInfo)) {
					shutDown("Unsupported data line: " + dataLineInfo);
					return;
				}

				// pobierz i otw�rz source data line do odtwarzania.

				try {
					targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
					targetDataLine.open(format, BUFFER_SIZE);
				} catch (LineUnavailableException ex) {
					shutDown("Line is unavaliable.");
					return;
				}
			} else {
				Mixer mixer = MixerChooser.getInputMixer();
				if (!mixer.isLineSupported(dataLineInfo)) {
					shutDown("Unsupported data line: " + dataLineInfo);
					return;
				}

				// pobierz i otw�rz source data line do odtwarzania

				try {
					targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
					targetDataLine.open(format, BUFFER_SIZE);
				} catch (LineUnavailableException e) {
					shutDown("Line is unavaliable.");
				}
			}
		}

		/**
		 * Recording an audio.
		 */
		void startRecording() {
			// Stream where audio will be stored
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// Byte array for capturing audio
			byte[] data = new byte[BUFFER_SIZE];
			// Number of bytes that are red.
			int numBytesRead;

			targetDataLine.start();

			synchronized (audioBytesPlayerLock) {
				while ((playState == PlayState.RECORD) && (thread != null)) {
					if ((numBytesRead = targetDataLine.read(data, 0, BUFFER_SIZE)) == -1)
						break;
					out.write(data, 0, numBytesRead);
				}

				targetDataLine.stop();
				targetDataLine.flush();
				targetDataLine.close();
				targetDataLine = null;

				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					Debug.debug(e.toString());
					setIsFilePlaying(false);
					return;
				}

				audioBytes = new byte[out.size()];
				audioBytes = out.toByteArray();
			}

			setIsFilePlaying(false);

		}

		Capture() {
			targetDataLine = null;
			thread = null;
		}
	}

	/**
	 * Initialize player.
	 */
	public Player() {
		format = AudioFileOperations.getAudioFormat();
		playback = new Playback();
		capture = new Capture();
		playState = PlayState.STOP;
		setIsFilePlaying(false);
	}

	public void setByteArrayToPlay(byte[] data) {
		synchronized (audioBytesPlayerLock) {
			audioBytes = data;
		}
	}

	byte[] getAudioBytes() {
		synchronized (audioBytesPlayerLock) {
			return audioBytes;
		}
	}

	public void setButtonListenerAutoref(ButtonListener buttonListener) {
		this.buttonListener = buttonListener;
	}

	void systemExit() {
		synchronized (this) {
			playState = PlayState.STOP;
			while (isFilePlaying() == true)
				try {
					wait();
				} catch (InterruptedException e) {
					Debug.debug(e.toString());
				}
		}
	}
}
