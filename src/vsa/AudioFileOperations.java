package vsa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import access.mypackage.offdebug.Debug;

/********************************************************************************************
 * Klasa AudioFileOperations 
 * 
 * Klasa przeznaczona do ró¿nych operacji na plikach audio wliczaj¹c w to
 * otwierania i zapisywania plików, konwersjê z danych w postaci tablicy bajtów 
 * na tablicê floatów czy te¿ normalizacjê.
 * 
 * ******************************************************************************************
 */
public class AudioFileOperations {
	
	/**----------------------------------------------------------------------
	 * Pola prywatne klasy
	 * 
	 * ----------------------------------------------------------------------
	 */
	
	private static AudioInputStream audioInputStream = null;
	private static final int BUFFER_SIZE = 8192;
	private static byte[] audioBytes;
	private static AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
	private static float sampleRate = 44100.0F;
	private static int sampleSizeInBits = 16;
	private static int channels = 1;
	private static int frameSize = sampleSizeInBits / 8 * channels;	
	private static float frameRate = 44100.0F;
	private static boolean isBigEndian = false;
	private static AudioFormat audioFormat = new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, isBigEndian);
	private static final Object audioArrayLock = new Object();
	
	
	
	/**----------------------------------------------------------------------------
	 * Metody publiczne
	 * 
	 * ----------------------------------------------------------------------------
	 */
	
	
	/**
	 * Otworzenie strumienia z pliku o podanej œcie¿ce.
	 * Je¿eli wyst¹pi¹ b³êdy audioInputStream jest ustawiony na null.     
	 * @param filePath œcie¿ka do pliku
	 * @return zmienna typu boolean okreœlaj¹ca czy uda³o siê otworzyæ plik
	 */
	public static boolean openFile(String filePath) {
		File file = new File(filePath);
		
		try {
			AudioInputStream audioTempInputStream = AudioSystem.getAudioInputStream(file);
			
			//Stworzenie strumienia z odpowiednim formatem audio
			audioInputStream = AudioSystem.getAudioInputStream(audioFormat, audioTempInputStream);
			
		} catch (UnsupportedAudioFileException e) {
			Dialogs.showMessage("Unsupported audio file format.");
			Debug.debug(e.toString());
			audioInputStream = null;
			return false;			
		} catch (IOException e) {
			Debug.debug(e.toString());
			audioInputStream = null;
			return false;
		}
		return convertStreamIntoByteArray();
		
	}
	
	/**
	 * Zapisanie tablicy bajtów do pliku zawieraj¹cych sygna³ audio.
	 * Je¿eli pojawi¹ siê b³êdy podczas zapisu, funkcja zwraca false.
	 * @param tablica bajtów reprezentuj¹ca sygna³ audio
	 * @param œcie¿ka do pliku
	 * @return zmienna typu boolean okreœlaj¹ca czy operacja siê powiod³a
	 */
	public static boolean saveFile(byte[] data, String path) {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		AudioInputStream audioInputStreamToSave;
		audioInputStreamToSave = new AudioInputStream(bais, audioFormat, data.length / audioFormat.getFrameSize());
		File file = new File(path);
		
		int numBytesToWrite = data.length; 
		while(numBytesToWrite > 0) {
			try {
				numBytesToWrite -= AudioSystem.write(audioInputStreamToSave, AudioFileFormat.Type.WAVE, file);
			} catch (IOException e) {
				Dialogs.showMessage("Unable to save a file");
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Konwersja tablicy bajtów na tablicê floatów z zakresu -1 do 1
	 * @param bytesArray tablica bajtów reprezentuj¹ca sygna³ audio
	 * @return tablica floatów reprezentuj¹ca sygna³ audio
	 */
	public static float[] byteArrayIntoFloatArray(byte[] bytesArray) {
		
		//rozmiar sampla w bitach
		final int bitsPerSample = AudioFileOperations.getAudioFormat().getSampleSizeInBits();
		//rozmiar sampla w bajtach
		//ka¿dy bajt ma 8 bitów
		final int bytesPerSample = bitsPerSample / 8;
		
		int numOfBytes = bytesArray.length;
		int numOfSamples = numOfBytes/bytesPerSample;
		long[] transfer = new long[numOfSamples];
		float[] samples = new float[numOfSamples];
		
		//Dla ka¿dego sampla
		for(int i = 0, k = 0, b; i < numOfBytes; i+= bytesPerSample, k++) {
			transfer[k] = 0L;
			//Dla ka¿dego bajtu w samplu
			//Poniewa¿ kodowanie jest w little-endian to trzeba drugi bajt przesun¹æ o 8 bitów w lewo
			//gdyby by³ 3ci bajt to o 16 bitów w lewo itd.
			for(b = 0; b < bytesPerSample; b++)
				transfer[k] |= ((bytesArray[i+b] & 0xffL) << (8*b));
		}
		
		//policzenie ile bajtów jest niezajêtych przez sampel w zmiennej typu long
		final int signShift = 64 - bitsPerSample;
		
		//Dla ka¿dego sampla przesuñ go w lewo o iloœæ miejsc nie okreœlaj¹cych wartoœci
		//i w prawo o t¹ sam¹ wartoœæ (wype³nienie jedynkami z przodu wartoœci ujemnych)
		for(int i = 0; i < transfer.length; i++)
			transfer[i] = ((transfer[i] << signShift) >> signShift);
		
		//maksymalna wartoœæ dodatnia sygna³u dla danej g³êbi bitowej
		final long fullScale = (long)Math.pow(2.0, bitsPerSample - 1) - 1;
		
		
		//Podziel ka¿d¹ wartoœæ przez najwiêksz¹ mo¿liw¹ w danej g³êbi bitowej
		for(int i = 0; i < transfer.length; i++)
			samples[i] = (float)transfer[i] / (float)fullScale;
		
		return samples;
	}
	
	/**
	 * Normalizacja do najwiêkszej próbki
	 */
	public static void normalized(float[] samples) {
		float maxValue = 0;
		for(int i = 0; i < samples.length; i ++)
			if (maxValue < Math.abs(samples[i]))
				maxValue = Math.abs(samples[i]);
		
		
		for(int i = 0; i < samples.length; i++)
			samples[i] = samples[i]/maxValue;
	}
	
	/**--------------------------------------------------------------------------
	 *
	 *	Gettery
	 *
	 *---------------------------------------------------------------------------
	 */
	
	/**
	 * Otrzymanie tablicy bajtów reprezentuj¹cych sygna³ audio
	 * @return byte[] sygna³ audio w bajtach
	 */
	public static byte[] getByteArrayWithAudio() {
		synchronized(audioArrayLock) {
			return audioBytes.clone();
		}
	}
	
	/**
	 * Zwraca domyœlny format audio u¿ywany w aplikacji.
	 * @return AudioFormat
	 */
	public static AudioFormat getAudioFormat(){
		return audioFormat;
	}
	
	

	/**-------------------------------------------------------------------------------
	 * Metody prywatne
	 * 
	 * -------------------------------------------------------------------------------
	 */
	
	
	/**
	 * Konwersja strumienia wejœciowego audio w tablicê bajtów.
	 * @return zmienna typu boolean okreœlaj¹ca powodzenie operacji 
	 */
	private static boolean convertStreamIntoByteArray(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] data = new byte[BUFFER_SIZE];
		int numBytesRead = 0;
		try {
			while((numBytesRead = audioInputStream.read(data)) != -1)
				out.write(data, 0, numBytesRead);
		} catch (IOException e) {
			Debug.debug(e.toString());
			return false;
		}
		
		synchronized(audioArrayLock) {
			audioBytes = new byte[out.size()];
			audioBytes = out.toByteArray();
		}
		
		audioInputStream = null;
		
		try {
			out.flush();
			out.close();
		} catch (IOException e) {
			Debug.debug(e.toString());
			return false;
		}
		return true;
	}
}