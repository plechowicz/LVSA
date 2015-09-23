package vsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import access.mypackage.offdebug.Debug;

/****************************************************
 * Klasa IMFFunction
 * 
 * klasa przechowuj�ca wyliczon� funkcj� IMF.
 * 
 ****************************************************
 */
class IMFFunction {
	/**
	 * Funkcja IMF w postaci tablicy float
	 */
	float[] samples;
	/**
	 * Cz�stotliwo�� funkcji IMF
	 */
	float freq;

	/**
	 * Konstruktor 1. Domy�lnie ustawia cz�stotliwo�� jako 0.
	 * 
	 * @param size
	 *            rozmiar funkcji IMF
	 */
	IMFFunction(int size) {
		samples = new float[size];
		freq = 0;
	}

	/**
	 * Konstruktor 2.
	 * 
	 * @param samples
	 *            funkcja IMF w postaci tablicy float
	 * @param freq
	 *            wyliczona cz�stotliwo��
	 */
	IMFFunction(float[] samples, float freq) {
		this.samples = samples;
		this.freq = freq;
	}
}

/****************************************************************************
 * Klasa MicrotremorFunction
 * 
 * Klasa opisuj�ca funkcje zawieraj�c� mikrodr�enie. Zawiera pole prywatne
 * b�d�ce decyzj� wykrycia stresu - warto�� true; oraz pola zawieraj�ce przebieg
 * funkcji i u�rednion� cz�stotliwo��.
 * 
 * **************************************************************************
 */
class MicrotremorFunction extends IMFFunction {

	/**
	 * Zmienna okre�laj�ca czy stres zosta� wykryty
	 */
	private boolean stressDetected = false;

	/**
	 * Konstruktor.
	 * 
	 * @param samples
	 *            tablica przedstawiaj�ca funkcj� znajduj�c� si� najbli�ej
	 *            mikrodr�enia
	 * @param freq
	 *            cz�stotliwo�� funkcji
	 * @param stressDetected
	 *            stwierdzenie wyst�powania stresu
	 */
	MicrotremorFunction(float[] samples, float freq, boolean stressDetected) {
		super(samples, freq);
		this.stressDetected = stressDetected;
	}

	/**
	 * Zwraca informacje czy w danej funkcji stwierdzono wyst�powanie stresu.
	 * 
	 * @return zmienna typu boolean okre�laj�ca wyst�powanie stresu
	 */
	public boolean getDecision() {
		return stressDetected;
	}
}

/*****************************************************************************
 * Klasa AnalysisInterruptedException
 * 
 * Wyj�tek stworzony do obs�ugi przerwania analizy. Dziedziczy po klasie
 * Exception.
 * 
 * ***************************************************************************
 */
@SuppressWarnings("serial")
class AnalysisInterruptedException extends Exception {

	public AnalysisInterruptedException() {
		Debug.debug("AnalysisInterruptedException");
	}
}

/*************************************************************************
 * Klasa Zeros
 * 
 * Klasa zawieraj�ca liczb� oraz po�o�enie miejsc zerowych funkcji
 * 
 *************************************************************************
 */
class Zeros {
	/**
	 * Liczba miejsc zerowych
	 */
	int nrOfZeros;
	/**
	 * Po�o�enie miejsc zerowych
	 */
	ArrayList<Integer> zeroPoints;

	/**
	 * Konstruktor
	 */
	public Zeros() {
		nrOfZeros = 0;
		zeroPoints = new ArrayList<Integer>();
	}
}

/**************************************************************************
 * Klasa Analysis
 * 
 * Klasa wykonuj�ca algorytm EMD oraz szereg operacji z nim zwi�zanych.
 *
 **************************************************************************
 */
public class Analysis {

	/**
	 * --------------------------------------------------------------------
	 * 
	 * Typy wyliczeniowe
	 * 
	 * --------------------------------------------------------------------
	 */

	/**
	 * Typ wyliczeniowy okre�laj�cy stan w jakim jest proces analizy Umo�liwia
	 * przerwanie wykonywania
	 */
	enum AnalysisState {
		INACTIVITY, ANALYSING, INTERRUPTING
	}

	/**
	 * Typ wyliczeniowy okre�laj�cy tendencj� badanej funkcji
	 */
	enum Tendency {
		INCREASING, DECREASING, NO_TENDENCY
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * Pola prywatne
	 * 
	 * ----------------------------------------------------------------------
	 */

	// cz�stotliwo�� pr�bkowania
	private static final float sampleRate = (float) AudioFileOperations.getAudioFormat().getSampleRate();

	// przedzia� tolerancji eliminuj�cy szum kwantyzacji -> warto�� jednego bita
	// zrzutowana na liczb� typu float
	private static final float delta = 1.0F / (float) (Math.pow(2.0, 15));

	// zmienna okre�laj�ca stan w jakim znajduje sie proces analizy
	private volatile AnalysisState analysisState = AnalysisState.INACTIVITY;

	// obiekt s�u��cy do synchronizacji zmiany stanu procesu analizy
	private Object analysisLock = new Object();

	/**
	 * -------------------------------------------------------------------------
	 * --
	 * 
	 * Metody publiczne
	 * 
	 * -------------------------------------------------------------------------
	 * --
	 */

	/**
	 * Rozpocz�cie analizy wyszukiwania mikrodr�enia.
	 * 
	 * @param samples
	 *            tablica float z audio, na kt�rym ma by� przeprowadzona analiza
	 * @return funkcja najbli�sza mikrodr�eniu
	 * @throws InterruptedException
	 *             przerwanie analizy
	 */
	public MicrotremorFunction startAnalysis(float[] samples) throws AnalysisInterruptedException {
		// Mapa zawieraj�ca nr porz�dkowy, oraz klas� IMFFunction
		HashMap<Integer, IMFFunction> imfMap = new HashMap<Integer, IMFFunction>();

		// Ustaw stan procesu analizy na Analysing
		setAnalysisState(AnalysisState.ANALYSING);

		// Na�� filtr dolnoprzepustowy. Je�eli d�ugo�� pr�bek by�a zbyt kr�tka
		// Funkcja zwraca null i jest przerywany proces analizy
		if ((samples = LowPassFilter.filetring(samples)) == null)
			setAnalysisState(AnalysisState.INTERRUPTING);
		;
		if (getAnalysisState() == AnalysisState.INTERRUPTING)
			throw new AnalysisInterruptedException();

		// Wykonaj algorytm EMD
		emd(samples, imfMap);
		if (getAnalysisState() == AnalysisState.INTERRUPTING)
			throw new AnalysisInterruptedException();

		// Zwr�� funkcj� najbli�sz� mikrodr�eniu
		return findClosestFrequencyToMicrotremor(imfMap);
	}

	/**
	 * -------------------------------------------------------------------------
	 * ---
	 * 
	 * Gettery i settery
	 * 
	 * -------------------------------------------------------------------------
	 * ---
	 */

	/**
	 * Zmiana stanu w jakim znajduje si� proces analizy
	 * 
	 * @param state
	 *            stan jaki ma przyj�� zmienna
	 */
	public void setAnalysisState(AnalysisState state) {
		synchronized (analysisLock) {
			analysisState = state;
		}
	}

	/**
	 * Zwr�� stan procesu analizy
	 * 
	 * @return stan procesu analizy
	 */
	public AnalysisState getAnalysisState() {
		return analysisState;
	}

	/**
	 * -------------------------------------------------------------------------
	 * -
	 * 
	 * Metody prywatne
	 *
	 * -------------------------------------------------------------------------
	 * -
	 */

	/**
	 * Znalezienie kolejnych funkcji IMF
	 * 
	 * @param residuum
	 *            sygna� wej�ciowy
	 * @param imfMap
	 *            mapa, w kt�rej mo�e by� przechowany numer oraz posta� funkcji
	 *            IMF
	 * @throws AnalysisInterruptedException
	 *             wyj�tek przerywaj�cy dzia�anie metody
	 */
	private void emd(float[] residuum, HashMap<Integer, IMFFunction> imfMap) throws AnalysisInterruptedException {

		// zmienne przeznaczone do przechowywania g�rnej i dolnej obwiedni
		float[] minEnvelope = new float[residuum.length];
		float[] maxEnvelope = new float[residuum.length];

		// sprawdzenie czy jest odpowiednia liczba sampli
		if (residuum.length < 6)
			throw new AnalysisInterruptedException();

		// zmienna okre�laj�ca odchylenie standardowe
		float squaredDifference;

		// listy zawieraj�ce po�o�enie minim�w i maksim�w
		ArrayList<Integer> max = new ArrayList<Integer>();
		ArrayList<Integer> min = new ArrayList<Integer>();

		// zmienna przechowuj�ce kopie sygna�u
		// do iteracji wewn�trz p�tli while (kolejne komponenty sygna�u)
		float[] component = new float[residuum.length];

		// zmienna na przechowywanie �redniej z obwiedni
		float[] meanEnvelope = new float[component.length];

		// zmienna do wyliczania odchylenia standardowego
		// potrzebny jest sygna� przed i po odj�ciu �redniej z obwiedni
		float[] previousComponent = new float[component.length];

		// zewn�trzna p�tla wykonuj�ca si� a� do przerwania
		outer: while (true) {
			// skopiuj sygna� wej�ciowy do komponentu
			component = residuum.clone();

			// p�tla wewn�trzna
			do {

				// zresetuj listy zawieraj�ce minima i maksima
				max.clear();
				min.clear();

				// znajd� ekstrema funkcji component
				extremum(max, min, component);

				if (getAnalysisState() == AnalysisState.INTERRUPTING)
					throw new AnalysisInterruptedException();

				// warunki przerywaj�ce p�tle zewn�trzn�
				// punkty na kraw�dziach przedzia��w s� przyjmowane jako
				// ekstrema (stany nieustalone funkcji)
				if (max.size() < 3)
					break outer;
				if (min.size() < 3)
					break outer;

				// interpolacja maksim�w i minim�w za pomoc� funkcji sklejanych
				// 3 rz�du
				maxEnvelope = interpolation(component, max);
				minEnvelope = interpolation(component, min);

				if (getAnalysisState() == AnalysisState.INTERRUPTING)
					throw new AnalysisInterruptedException();

				// wyliczenie �redniej z obwiedni
				for (int i = 0; i < component.length; i++)
					meanEnvelope[i] = (maxEnvelope[i] + minEnvelope[i]) / 2;

				// zapami�tanie obecnego komponentu
				previousComponent = component.clone();

				// odjecie �redniej z obwiedni od komponentu
				for (int i = 0; i < component.length; i++)
					component[i] -= meanEnvelope[i];

				// wyzerowanie odchylenia standardowego
				squaredDifference = 0;

				// obliczenie odchylenia standardowego
				for (int i = 0; i < component.length; i++)
					squaredDifference += (component[i] - previousComponent[i]) * (component[i] - previousComponent[i])
							/ (previousComponent[i] * previousComponent[i] + 1e-8);

				if (getAnalysisState() == AnalysisState.INTERRUPTING)
					throw new AnalysisInterruptedException();

				// warunek ko�cz�cy p�tl� wewn�trzn�
			} while (squaredDifference > 5);

			// sprawdzenie cz�stotliwo�ci otrzymanej funkcji
			float freq = avgerageFrequency(component);

			// je�eli jej cz�stotliwo�� jest ju� na tyle niska, �e nie ma
			// to wp�ywu na wykrywanie mikrodr�enia mo�na przerwa� obliczenia
			if (freq < 4F)
				break; // przerywa p�tl� zewn�trzn�

			// je�eli uda�o si� otrzyma� cz�stotliwo�� to
			// podana funkcja component b�dzie zapisana jako funkcja IMF
			IMFFunction imfFunction = new IMFFunction(component.clone(), freq);

			if (getAnalysisState() == AnalysisState.INTERRUPTING)
				throw new AnalysisInterruptedException();

			// dodaj do mapy znalezion� funkcj� IMF
			imfMap.put(imfMap.size(), imfFunction);

			// odjecie od sygna�u pocz�tkowego funkcji IMF
			for (int i = 0; i < residuum.length; i++) {
				residuum[i] -= component[i];
			}
		} // koniec p�tli zewn�trznej
	} // koniec metody emd

	/**
	 * Metoda interpoluj�ca sygna� funkcjami sklejanymi trzeciego rz�du, na
	 * podstawie podanych punkt�w. Lista okre�la po�o�enie w osi czasu punkt�w.
	 * Ich warto�ci s� pobierane z tablicy samples.
	 * 
	 * @param samples
	 *            funkcja, z kt�rej maj� by� wczytane warto�ci na osi y
	 * @param list
	 *            lista po�o�enia punkt�w na osi x
	 * @return wynik interpolacji
	 */
	private float[] interpolation(float[] samples, ArrayList<Integer> list) {

		// interpolowana funkcja
		float[] interpolatedFunction = new float[samples.length];

		// Iterator do poruszania si� po li�cie
		Iterator<Integer> iterator = list.iterator();

		// punkty, na podstawie kt�rych ma by� dokonana interpolacja
		double[] x = new double[list.size()];
		double[] y = new double[list.size()];

		int j = 0;
		int index = 0;
		// je�li jest nast�pny punkt
		while (iterator.hasNext()) {
			// zapami�taj indeks tego punktu
			index = iterator.next();
			// indeks jest po�o�eniem pr�bki na osi x
			x[j] = (double) index;
			// warto�� pr�bki jest przechowywana w tablicy samples
			y[j] = (double) samples[index];
			j++;
		}

		// stworzenie obiektu wyliczaj�cego funkcje s�u��ce do interpolacji
		SplineInterpolator interpolator = new SplineInterpolator();
		// zbi�r funkcji na podstawie, kt�rych jest okre�lana warto�� dowolnego
		// punktu na osi x
		PolynomialSplineFunction poly;
		poly = interpolator.interpolate(x, y);

		// obliczenie warto�ci dla ka�dej pr�bki w sygnale
		for (int i = 0; i < samples.length; i++)
			interpolatedFunction[i] = (float) poly.value((double) i);

		return interpolatedFunction;
	} // koniec metody interpolation

	/**
	 * Metoda obliczaj�c� �redni� cz�stotliwo�� funkcji na podstawie miejsc
	 * zerowych
	 * 
	 * @param samples
	 *            sygna�, z kt�rego ma by� obliczona �rednia cz�stotliwo��
	 * @return �rednia cz�stotliwo��
	 * @throws AnalysisInterruptedException
	 *             wyj�tek przerywaj�cy dzia�anie metody
	 */
	private float avgerageFrequency(float[] samples) throws AnalysisInterruptedException {

		float freq = 0F;

		// znalezienie miejsc zerowych w sygnale
		Zeros zeros = findingZeros(samples);

		// gdy nieparzysta liczba zer
		if ((zeros.nrOfZeros % 2) != 0) {
			if (zeros.nrOfZeros >= 3)
				// je�eli wi�cej ni� lub r�wne trzy to odejmij jedno od liczby
				// zer
				// w ten spos�b dostaje si� liczb� podw�jnych przej�� przez zero
				// sygna�u
				// (czyli podwojon� liczb� okres�w),
				// nast�pnie ta liczba jest dzielona przez ostatnie zero
				// odj�te od pierwszego i odpowiednio
				// wymna�a si� przez cz�stotliwo�� pr�bkowania.
				freq = (zeros.nrOfZeros - 1)
						/ (float) (zeros.zeroPoints.get(zeros.nrOfZeros - 1) - (float) zeros.zeroPoints.get(0)) / 2.0F
						* sampleRate;
			else
				// je�eli jest mniej ni� 3 zera to nie da si� wyliczy�
				// cz�stotliwo�ci
				// (jedno miejsce zerowe)
				freq = 0;
		}
		// liczba zer jest parzysta
		else {
			if (zeros.nrOfZeros >= 4)
				// je�eli liczba zer jest wi�ksza ni� lub r�wna cztery
				// to pomi� ostatnie zero w obliczaniu cz�stotliwo�ci
				freq = (zeros.nrOfZeros - 2)
						/ (float) (zeros.zeroPoints.get(zeros.nrOfZeros - 2) - zeros.zeroPoints.get(0)) / 2.0F
						* sampleRate;
			else if (zeros.nrOfZeros == 2)
				// je�eli liczba zer jest r�wna dwa
				// to powiel r�nic� odleg�o�ci drugiego zera i pierwszego zera,
				// �eby mie� pe�en okres funkcji
				freq = (zeros.nrOfZeros)
						/ (float) (((zeros.zeroPoints.get(zeros.nrOfZeros - 1) - (zeros.zeroPoints.get(0))) * 2)) / 2.0F
						* sampleRate;
			else
				freq = 0;

			if (getAnalysisState() == AnalysisState.INTERRUPTING)
				throw new AnalysisInterruptedException();
		}
		Debug.debug(Float.toString(freq));

		return freq;
	} // koniec metody averageFrequency

	/**
	 * Znalezienie ekstrem�w w sygnale.
	 * 
	 * @param max
	 *            lista liczb ca�kowitych okre�laj�ca po�o�enie maksim�w
	 * @param min
	 *            lista liczb ca�kowitych okre�laj�ca po�o�enie minim�w
	 * @param samples
	 *            sygna�, w kt�rym maj� by� znalezione ekstrema
	 */
	private void extremum(ArrayList<Integer> max, ArrayList<Integer> min, float[] samples) {

		// tendencja funkcji
		Tendency tendency = Tendency.NO_TENDENCY;

		// pierwszy punkt traktowany jako stan nieustalony, jednocze�nie jako
		// maksimum i jako minimum
		max.add(0);
		min.add(0);

		// Sprawdzenie tendencji funkcji na pocz�tku sygna�u
		int i;
		for (i = 0; i < samples.length - 1 && tendency == Tendency.NO_TENDENCY; i++) {
			if (samples[i] > delta / 2)
				tendency = Tendency.INCREASING;
			else if (samples[i] < -delta / 2)
				tendency = Tendency.DECREASING;
		}

		// je�eli funkcja jest p�aska to nie ma �adnej tendencji
		// nale�y doda� ostatnie punkty jako ekstrema i wyj�� z funkcji
		if (tendency == Tendency.NO_TENDENCY) {
			min.add(samples.length - 1);
			max.add(samples.length - 1);
			return;
		}

		// tymczasowe maksimum i minimum, ich warto�� b�dzie si� zmienia� wraz z
		// badaniem funkcji
		int maxTemp = 0;
		int minTemp = 0;

		// Pierwsze przej�cie musi by� w innej p�tli, bo mo�e sie zdarzy� tak,
		// �e dwa razy zapisze si� ta sama warto�� na osi x jako minimum albo
		// maksimum.
		// Je�eli przyk�adowo pierwsza warto�� w sygnale jest wystarczaj�co
		// du�a,
		// �eby funkcja przyj�a tendencj� rosn�c�,
		// a nast�pna warto�� jest wystarczaj�co ma�a, �eby algorytm stwierdzi�
		// ekstremum,
		// to nie mo�e zapisa� tej wi�kszej warto�ci jako maksimum, poniewa�
		// raz ju� zosta�o zapisane. (powsta� by problem w funkcji interpolate)
		// Ta sytuacja wyst�puje tylko do momentu, w kt�rym dla tendencji
		// rosn�cej zmieni�a si�
		// warto�� tymczasowego maksimum, albo zmieni�a sie tendencja funkcji,
		// a dla tendencji malej�cej zmieni�a si� warto�� minimum,
		// albo zmieni�a si� tendencja funkcji.

		// tendencja rosn�ca
		for (; i < samples.length - 1; i++) {
			if (tendency == Tendency.INCREASING) {
				// je�eli nast�pna pr�bka jest wi�ksza to we� j� jako maksimum
				if (samples[maxTemp] < samples[i]) {
					maxTemp = i;
					break;
				}
				// obecne maksimum jest wi�ksze o delt�
				else if (samples[maxTemp] > (samples[i] + delta)) {
					minTemp = i;
					tendency = Tendency.DECREASING;
					break;
				}
				// nie jest wi�kszy o delt� - szukaj dalej
				else {
					// maxTemp zostawiam bez zmian
					minTemp = i;
				}
				// tendencja malej�ca
			} else {
				// je�eli nast�pna pr�bka jest mniejsza to we� j� jako minimum
				if (samples[minTemp] > samples[i]) {
					minTemp = i;
					break;
				}
				// je�eli obecne minimum jest mniejsze o delt�
				else if (samples[minTemp] < (samples[i] - delta)) {
					maxTemp = i;
					tendency = Tendency.INCREASING;
					break;
				} else {
					maxTemp = i;
				}
			}
		}

		// dalsza cz�� odbywa si� ju� normalnie
		for (; i < samples.length - 1; i++) {
			// tendencja rosn�ca
			if (tendency == Tendency.INCREASING) {
				// je�eli nast�pna pr�bka jest wi�ksza to we� j� jako maksimum
				if (samples[maxTemp] < samples[i])
					maxTemp = i;
				// obecne maksimum jest wi�ksze o delt�
				else if (samples[maxTemp] > (samples[i] + delta)) {
					max.add(maxTemp);
					minTemp = i;
					tendency = Tendency.DECREASING;
				}
				// nie jest wi�ksze o delt� - szukaj dalej
				else {
					minTemp = i;
				}
				// tendencja malej�ca
			} else {
				// je�eli nast�pna pr�bka jest mniejsza od to we� j� jako
				// minimum
				if (samples[minTemp] > samples[i])
					minTemp = i;
				// je�eli obecne minimum jest mniejsze o delt�
				else if (samples[minTemp] < (samples[i] - delta)) {
					min.add(minTemp);
					maxTemp = i;
					tendency = Tendency.INCREASING;
				} else {
					maxTemp = i;
				}
			}

		}

		// Dodaj ostatni� pr�bk�
		min.add(samples.length - 1);
		max.add(samples.length - 1);
	} // koniec metody ekstremum

	/**
	 * Wyszukiwanie miejsc zerowych w sygnale
	 * 
	 * @param samples
	 *            sygna�, w kt�rym maj� by� znalezione miejsca zerowe
	 * @return Zeros - obiekt przechowuj�cy po�o�enie oraz liczb� warto�ci
	 *         miejsc zerowych
	 */
	private Zeros findingZeros(float[] samples) {
		Zeros zeros = new Zeros();

		zeros.nrOfZeros = 0;
		zeros.zeroPoints.clear();

		Tendency tendency = Tendency.NO_TENDENCY;

		int i;

		// Je�eli funkcja nie ma jeszcze okre�lonej tendencji sprawdzaj czy
		// warto�� kolejnej pr�bki
		// nie przekroczy�a zadanego przedzia�u tolerancji od warto�ci zerowej
		for (i = 0; i < (samples.length - 1) && (tendency == Tendency.NO_TENDENCY); i++) {
			if (samples[i] > delta / 2)
				tendency = Tendency.INCREASING;
			if (samples[i] < -delta / 2)
				tendency = Tendency.DECREASING;
		}

		// Je�eli funkcja ma ju� okre�lon� tendencj� kontynuuj wyszukiwanie, ale
		// za miejsce zerowe we�
		// miejsce w kt�rym pr�bka przekroczy o� o wyznaczon� tolerancj�
		for (; i < samples.length - 1; i++) {
			if (tendency == Tendency.INCREASING) {
				if (samples[i] < -delta) {
					zeros.nrOfZeros++;
					zeros.zeroPoints.add(i);
					tendency = Tendency.DECREASING;
				}
			} else if (tendency == Tendency.DECREASING) {
				if (samples[i] > delta) {
					zeros.nrOfZeros++;
					zeros.zeroPoints.add(i);
					tendency = Tendency.INCREASING;
				}
			}
		}
		return zeros;
	} // koniec metody findingZeros

	/**
	 * Okre�lenie cz�stotliwo�ci najbli�ej mikrodr�enia spo�r�d wszystkich
	 * otrzymanych funkcji IMF.
	 * 
	 * @param imfMap
	 *            mapa funkcji IMF
	 * @return klasa MicrotremorFunction przechowuj�ca funkcj� IMF najbli�sz�
	 *         mikrodr�eniu
	 */
	private MicrotremorFunction findClosestFrequencyToMicrotremor(HashMap<Integer, IMFFunction> imfMap) {

		// iterator mapy
		Iterator<Map.Entry<Integer, IMFFunction>> iterator = imfMap.entrySet().iterator();
		Map.Entry<Integer, IMFFunction> mapEntry;

		// cz�stotliwo�� mikrodr�enia
		float microtremorFrequency;
		int id = 0;

		// przyj�cie pierwszej funkcji jako mikrodr�enia
		if (iterator.hasNext()) {
			mapEntry = iterator.next();
			microtremorFrequency = mapEntry.getValue().freq;
			id = 0;
		} else {
			Debug.debug("nie ma zadnej czestotliwosci do analizowania w funkcji findClosestFrequencyToMicrottremor()");
			return null;
		}

		while (iterator.hasNext()) {
			mapEntry = iterator.next();

			// znalezienie ID funkcji IMF najbli�szej cz�stotliwo�ci
			// mikrodr�enia
			if (Math.abs(10 - mapEntry.getValue().freq) < Math.abs(10 - microtremorFrequency)) {
				microtremorFrequency = mapEntry.getValue().freq;
				id = mapEntry.getKey();
			}
		}

		// zwr�� funkcj� odpowiadaj�c� mikrodr�eniu, jej cz�stotliwo�� oraz
		// podj�t� decyzj�.
		return new MicrotremorFunction((imfMap.get((Integer) id).samples), microtremorFrequency,
				decision(microtremorFrequency));
	} // koniec metody findClosestFrequencyToMicrotremor

	/**
	 * Podj�cie decyzji odno�nie wyst�powania stresu
	 * 
	 * @param frequency
	 *            cz�stotliwo�� funkcji IMF
	 * @return decyzja
	 */
	private boolean decision(float frequency) {
		// wykryto stres
		if (frequency < 8 || frequency > 12)
			return true;
		// brak stresu
		else
			return false;
	}

} // koniec klasy Analysis
