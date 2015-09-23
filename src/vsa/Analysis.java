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
 * klasa przechowuj¹ca wyliczon¹ funkcjê IMF.
 * 
 ****************************************************
 */
class IMFFunction {
	/**
	 * Funkcja IMF w postaci tablicy float
	 */
	float[] samples; 
	/**
	 * Czêstotliwoœæ funkcji IMF
	 */
	float freq;
	
	/**
	 * Konstruktor 1. Domyœlnie ustawia czêstotliwoœæ jako 0.
	 * @param size rozmiar funkcji IMF
	 */
	IMFFunction(int size) {
		samples = new float[size];
		freq = 0;
	}
	
	/**
	 * Konstruktor 2.
	 * @param samples funkcja IMF w postaci tablicy float
	 * @param freq wyliczona czêstotliwoœæ
	 */
	IMFFunction(float [] samples, float freq) {
		this.samples = samples;
		this.freq = freq;
	}
}

/****************************************************************************
 * Klasa MicrotremorFunction
 * 
 * Klasa opisuj¹ca funkcje zawieraj¹c¹ mikrodr¿enie.
 * Zawiera pole prywatne bêd¹ce decyzj¹ wykrycia stresu - wartoœæ true;
 * oraz pola zawieraj¹ce przebieg funkcji i uœrednion¹ czêstotliwoœæ.
 * 
 * **************************************************************************
 */
class MicrotremorFunction extends IMFFunction {
	
	/**
	 * Zmienna okreœlaj¹ca czy stres zosta³ wykryty
	 */
	private boolean stressDetected = false;
	
	/**
	 * Konstruktor.
	 * @param samples tablica przedstawiaj¹ca funkcjê znajduj¹c¹ siê najbli¿ej mikrodr¿enia
	 * @param freq czêstotliwoœæ funkcji
	 * @param stressDetected stwierdzenie wystêpowania stresu
	 */
	MicrotremorFunction(float[] samples, float freq, boolean stressDetected) {
		super(samples, freq);
		this.stressDetected = stressDetected;
	}
	
	/**
	 * Zwraca informacje czy w danej funkcji stwierdzono wystêpowanie stresu.
	 * @return zmienna typu boolean okreœlaj¹ca wystêpowanie stresu
	 */
	public boolean getDecision(){
		return stressDetected;
	}
}

/*****************************************************************************
 * Klasa AnalysisInterruptedException
 * 
 * Wyj¹tek stworzony do obs³ugi przerwania analizy. 
 * Dziedziczy po klasie Exception.
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
 * Klasa zawieraj¹ca liczbê oraz po³o¿enie miejsc zerowych funkcji
 * 
 *************************************************************************
 */
class Zeros {
	/**
	 * Liczba miejsc zerowych
	 */
	int nrOfZeros;
	/**
	 * Po³o¿enie miejsc zerowych
	 */
	ArrayList<Integer> zeroPoints;
	/**
	 * Konstruktor
	 */
	public Zeros(){
		nrOfZeros = 0;
		zeroPoints = new ArrayList<Integer>();
	}
}

/**************************************************************************
 * Klasa Analysis
 * 
 * Klasa wykonuj¹ca algorytm EMD oraz szereg operacji z nim zwi¹zanych.
 *
 **************************************************************************
 */
public class Analysis {
	
	/**--------------------------------------------------------------------
	 * 
	 * Typy wyliczeniowe
	 * 
	 * --------------------------------------------------------------------
	 */
	
	/**
	 * Typ wyliczeniowy okreœlaj¹cy stan w jakim jest proces analizy
	 * Umo¿liwia przerwanie wykonywania
	 */
	enum AnalysisState {
		INACTIVITY, ANALYSING, INTERRUPTING
	}
	
	/**
	 * Typ wyliczeniowy okreœlaj¹cy tendencjê badanej funkcji
	 */
	enum Tendency {
		INCREASING, DECREASING, NO_TENDENCY
	}
	
	/**----------------------------------------------------------------------
	 * 
	 * Pola prywatne
	 * 
	 * ----------------------------------------------------------------------
	 */
	
	// czêstotliwoœæ próbkowania
	private static final float sampleRate = (float)AudioFileOperations.getAudioFormat().getSampleRate();
	
	// przedzia³ tolerancji eliminuj¹cy szum kwantyzacji ->  wartoœæ jednego bita zrzutowana na liczbê typu float
	private static final float delta = 1.0F/(float)(Math.pow(2.0, 15) ); 
	
	// zmienna okreœlaj¹ca stan w jakim znajduje sie proces analizy
	private volatile AnalysisState analysisState = AnalysisState.INACTIVITY;
	
	// obiekt s³u¿¹cy do synchronizacji zmiany stanu procesu analizy
	private Object analysisLock = new Object();
	
	/**---------------------------------------------------------------------------
	 * 
	 * Metody publiczne
	 * 
	 * ---------------------------------------------------------------------------
	 */
	
	/**
	 * Rozpoczêcie analizy wyszukiwania mikrodr¿enia.
	 * @param samples tablica float z audio, na którym ma byæ przeprowadzona analiza
	 * @return funkcja najbli¿sza mikrodr¿eniu
	 * @throws InterruptedException przerwanie analizy
	 */
	public MicrotremorFunction startAnalysis(float[] samples) throws AnalysisInterruptedException{
		//Mapa zawieraj¹ca nr porz¹dkowy, oraz klasê IMFFunction	
		HashMap<Integer, IMFFunction> imfMap = new HashMap<Integer, IMFFunction>();
		
		//Ustaw stan procesu analizy na Analysing
		setAnalysisState(AnalysisState.ANALYSING);
		
		// Na³ó¿ filtr dolnoprzepustowy. Je¿eli d³ugoœæ próbek by³a zbyt krótka
		// Funkcja zwraca null i jest przerywany proces analizy
		if((samples = LowPassFilter.filetring(samples)) == null)
			setAnalysisState(AnalysisState.INTERRUPTING);;
		if(getAnalysisState() == AnalysisState.INTERRUPTING)
			throw new AnalysisInterruptedException();
		
		// Wykonaj algorytm EMD
		emd(samples, imfMap);
		if(getAnalysisState() == AnalysisState.INTERRUPTING)
			throw new AnalysisInterruptedException();
		
		// Zwróæ funkcjê najbli¿sz¹ mikrodr¿eniu
		return findClosestFrequencyToMicrotremor(imfMap);
	}
	
	/**----------------------------------------------------------------------------
	 * 
	 * Gettery i settery
	 * 
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Zmiana stanu w jakim znajduje siê proces analizy
	 * @param state stan jaki ma przyj¹æ zmienna
	 */
	public void setAnalysisState(AnalysisState state) {
		synchronized(analysisLock) {
			analysisState = state;
		}
	}
	
	/**
	 * Zwróæ stan procesu analizy
	 * @return stan procesu analizy
	 */
	public AnalysisState getAnalysisState() {
		return analysisState;
	}
	
	
	/**--------------------------------------------------------------------------
	 * 
	 * Metody prywatne
	 *
	 * --------------------------------------------------------------------------
	 */
	
	/**
	 * Znalezienie kolejnych funkcji IMF
	 * @param residuum sygna³ wejœciowy
	 * @param imfMap mapa, w której mo¿e byæ przechowany numer oraz postaæ funkcji IMF
	 * @throws AnalysisInterruptedException wyj¹tek przerywaj¹cy dzia³anie metody
	 */
	private void emd(float[] residuum, HashMap<Integer, IMFFunction> imfMap) throws AnalysisInterruptedException{
		
		// zmienne przeznaczone do przechowywania górnej i dolnej obwiedni
		float[] minEnvelope = new float[residuum.length];
		float[] maxEnvelope= new float[residuum.length];
		
		// sprawdzenie czy jest odpowiednia liczba sampli
		if(residuum.length < 6)
			throw new AnalysisInterruptedException();
		
		// zmienna okreœlaj¹ca odchylenie standardowe
		float squaredDifference; 
		
		// listy zawieraj¹ce po³o¿enie minimów i maksimów
		ArrayList<Integer> max = new ArrayList<Integer>();
		ArrayList<Integer> min = new ArrayList<Integer>();
		
		// zmienna przechowuj¹ce kopie sygna³u
		// do iteracji wewn¹trz pêtli while (kolejne komponenty sygna³u)
		float[] component = new float[residuum.length];			
		
		// zmienna na przechowywanie œredniej z obwiedni
		float[] meanEnvelope = new float[component.length];		
		
		// zmienna do wyliczania odchylenia standardowego
		// potrzebny jest sygna³ przed i po odjêciu œredniej z obwiedni
		float[] previousComponent = new float[component.length];				
		
		// zewnêtrzna pêtla wykonuj¹ca siê a¿ do przerwania
		outer : while(true) {
			// skopiuj sygna³ wejœciowy do komponentu
			component = residuum.clone();
			
			//pêtla wewnêtrzna
			do {
				
				//zresetuj listy zawieraj¹ce minima i maksima
				max.clear();
				min.clear();
				
				//znajdŸ ekstrema funkcji component
				extremum(max, min, component);
				
				if(getAnalysisState() == AnalysisState.INTERRUPTING)
					throw new AnalysisInterruptedException();
				
				// warunki przerywaj¹ce pêtle zewnêtrzn¹
				// punkty na krawêdziach przedzia³ów s¹ przyjmowane jako ekstrema (stany nieustalone funkcji)
				if(max.size() < 3) break outer;
				if(min.size() < 3) break outer;
				
				// interpolacja maksimów i minimów za pomoc¹ funkcji sklejanych 3 rzêdu
				maxEnvelope = interpolation(component, max);
				minEnvelope = interpolation(component, min);
				
				if(getAnalysisState() == AnalysisState.INTERRUPTING)
					throw new AnalysisInterruptedException();
				
				// wyliczenie œredniej z obwiedni
				for(int i = 0; i < component.length; i++)
					meanEnvelope[i] = (maxEnvelope[i]+minEnvelope[i])/2;
				
				// zapamiêtanie obecnego komponentu
				previousComponent = component.clone();
				
				// odjecie œredniej z obwiedni od komponentu
				for(int i = 0; i < component.length; i++)
					component[i] -= meanEnvelope[i];
				
				// wyzerowanie odchylenia standardowego
				squaredDifference = 0;
				
				//obliczenie odchylenia	standardowego
				for(int i =0; i < component.length; i++)
					squaredDifference += (component[i]-previousComponent[i])*(component[i]-previousComponent[i])/(previousComponent[i]*previousComponent[i]+1e-8);
				
				if(getAnalysisState() == AnalysisState.INTERRUPTING)
					throw new AnalysisInterruptedException();
			
				// warunek koñcz¹cy pêtlê wewnêtrzn¹
			} while(squaredDifference > 5);
			
			//sprawdzenie czêstotliwoœci otrzymanej funkcji
			float freq = avgerageFrequency(component);
			
			// je¿eli jej czêstotliwoœæ jest ju¿ na tyle niska, ¿e nie ma
			// to wp³ywu na wykrywanie mikrodr¿enia mo¿na przerwaæ obliczenia
			if (freq < 4F)
				break;   		// przerywa pêtlê zewnêtrzn¹
			
			// je¿eli uda³o siê otrzymaæ czêstotliwoœæ to
			// podana funkcja component bêdzie zapisana jako funkcja IMF
			IMFFunction imfFunction = new IMFFunction(component.clone(), freq);
			
			if(getAnalysisState() == AnalysisState.INTERRUPTING)
				throw new AnalysisInterruptedException();
			
			// dodaj do mapy znalezion¹ funkcjê IMF
			imfMap.put(imfMap.size(), imfFunction);
			
			//odjecie od sygna³u pocz¹tkowego funkcji IMF
			for(int i =0; i <residuum.length; i++) {
				residuum[i] -= component[i];
			}
		} // koniec pêtli zewnêtrznej
	} // koniec metody emd
	
	/**
	 * Metoda interpoluj¹ca sygna³ funkcjami sklejanymi trzeciego rzêdu, na podstawie podanych punktów. 
	 * Lista okreœla po³o¿enie w osi czasu punktów.
	 * Ich wartoœci s¹ pobierane z tablicy samples.
	 * @param samples funkcja, z której maj¹ byæ wczytane wartoœci na osi y 
	 * @param list lista po³o¿enia punktów na osi x
	 * @return wynik interpolacji
	 */
	private float[] interpolation(float[] samples, ArrayList<Integer> list) {
		
		// interpolowana funkcja
		float[] interpolatedFunction = new float[samples.length];
		
		//Iterator do poruszania siê po liœcie
		Iterator<Integer> iterator = list.iterator();
		
		//punkty, na podstawie których ma byæ dokonana interpolacja
		double[] x = new double[list.size()];
		double[] y = new double[list.size()];
		
		int j = 0;
		int index = 0;
		// jeœli jest nastêpny punkt
		while(iterator.hasNext()) {
			// zapamiêtaj indeks tego punktu
			index = iterator.next();
			// indeks jest po³o¿eniem próbki na osi x
			x [j] = (double)index;
			// wartoœæ próbki jest przechowywana w tablicy samples
			y [j] = (double)samples[index];
			j++;
		}
		
		// stworzenie obiektu wyliczaj¹cego funkcje s³u¿¹ce do interpolacji
		SplineInterpolator interpolator = new SplineInterpolator();
		// zbiór funkcji na podstawie, których jest okreœlana wartoœæ dowolnego punktu na osi x
		PolynomialSplineFunction poly;
		poly = interpolator.interpolate(x, y);
		
		// obliczenie wartoœci dla ka¿dej próbki w sygnale
		for(int i = 0; i < samples.length; i ++)
			interpolatedFunction[i] = (float) poly.value((double)i);
		
		return interpolatedFunction;
	} // koniec metody interpolation
	
	/**
	 * Metoda obliczaj¹c¹ œredni¹ czêstotliwoœæ funkcji na podstawie miejsc zerowych
	 * @param samples sygna³, z którego ma byæ obliczona œrednia czêstotliwoœæ
	 * @return œrednia czêstotliwoœæ
	 * @throws AnalysisInterruptedException wyj¹tek przerywaj¹cy dzia³anie metody
	 */
	private float avgerageFrequency(float[] samples) throws AnalysisInterruptedException{
		
		float freq = 0F;
			
		// znalezienie miejsc zerowych w sygnale
		Zeros zeros = findingZeros(samples);
			
		// gdy nieparzysta liczba zer
		if((zeros.nrOfZeros % 2)!= 0) {
			if (zeros.nrOfZeros >= 3)
				// je¿eli wiêcej ni¿ lub równe trzy to odejmij jedno od liczby zer
				// w ten sposób dostaje siê liczbê podwójnych przejœæ przez zero sygna³u
				// (czyli podwojon¹ liczbê okresów),
				// nastêpnie ta liczba jest dzielona przez ostatnie zero 
				// odjête od pierwszego i odpowiednio 
				// wymna¿a siê przez czêstotliwoœæ próbkowania.
				freq = (zeros.nrOfZeros - 1)/(float)(zeros.zeroPoints.get(zeros.nrOfZeros - 1) - (float)zeros.zeroPoints.get(0))/2.0F*sampleRate;
			else
				// je¿eli jest mniej ni¿ 3 zera to nie da siê wyliczyæ czêstotliwoœci 
				// (jedno miejsce zerowe)
				freq = 0;
		}
		// liczba zer jest parzysta
		else {
			if(zeros.nrOfZeros >= 4)
				//je¿eli liczba zer jest wiêksza ni¿ lub równa cztery 
				// to pomiñ ostatnie zero w obliczaniu czêstotliwoœci
				freq = (zeros.nrOfZeros - 2)/(float)(zeros.zeroPoints.get(zeros.nrOfZeros - 2) - zeros.zeroPoints.get(0))/2.0F*sampleRate;
			else if (zeros.nrOfZeros == 2)
				// je¿eli liczba zer jest równa dwa 
				// to powiel ró¿nicê odleg³oœci drugiego zera i pierwszego zera, 
				// ¿eby mieæ pe³en okres funkcji
				freq = (zeros.nrOfZeros)/(float)(((zeros.zeroPoints.get(zeros.nrOfZeros - 1) - (zeros.zeroPoints.get(0)))*2))/2.0F*sampleRate;
			else
				freq = 0;
			
			if(getAnalysisState() == AnalysisState.INTERRUPTING)
				throw new AnalysisInterruptedException();
		}
		Debug.debug(Float.toString(freq));
		
		return freq;
	} // koniec metody averageFrequency
	
	/**
	 * Znalezienie ekstremów w sygnale.
	 * @param max lista liczb ca³kowitych okreœlaj¹ca po³o¿enie maksimów
	 * @param min lista liczb ca³kowitych okreœlaj¹ca po³o¿enie minimów
	 * @param samples sygna³, w którym maj¹ byæ znalezione ekstrema
	 */
	private void extremum(ArrayList<Integer> max, ArrayList<Integer> min, float[] samples) {
		
		// tendencja funkcji
		Tendency tendency = Tendency.NO_TENDENCY;
		
		//pierwszy punkt traktowany jako stan nieustalony, jednoczeœnie jako maksimum i jako minimum
		max.add(0);
		min.add(0);
		
		//Sprawdzenie tendencji funkcji na pocz¹tku sygna³u
		int i;
		for(i = 0; i < samples.length - 1 && tendency == Tendency.NO_TENDENCY; i++) {
			if (samples[i] > delta/2)
				tendency = Tendency.INCREASING;
			else if (samples[i] < - delta/2)
				tendency = Tendency.DECREASING;
		}
		
		//je¿eli funkcja jest p³aska to nie ma ¿adnej tendencji
		//nale¿y dodaæ ostatnie punkty jako ekstrema i wyjœæ z funkcji
		if (tendency == Tendency.NO_TENDENCY) {
			min.add(samples.length -1);
			max.add(samples.length -1);
			return;
		}
		
		//tymczasowe maksimum i minimum, ich wartoœæ bêdzie siê zmieniaæ wraz z badaniem funkcji
		int maxTemp = 0;
		int minTemp = 0;
		
		// Pierwsze przejœcie musi byæ w innej pêtli, bo mo¿e sie zdarzyæ tak, 
		// ¿e dwa razy zapisze siê ta sama wartoœæ na osi x jako minimum albo maksimum.
		// Je¿eli przyk³adowo pierwsza wartoœæ w sygnale jest wystarczaj¹co du¿a, 
		// ¿eby funkcja przyjê³a tendencjê rosn¹c¹, 
		// a nastêpna wartoœæ jest wystarczaj¹co ma³a, ¿eby algorytm stwierdzi³ ekstremum,
		// to nie mo¿e zapisaæ tej wiêkszej wartoœci jako maksimum, poniewa¿
		// raz ju¿ zosta³o zapisane. (powsta³ by problem w funkcji interpolate)
		// Ta sytuacja wystêpuje tylko do momentu, w którym dla tendencji rosn¹cej zmieni³a siê
		// wartoœæ tymczasowego maksimum, albo zmieni³a sie tendencja funkcji,
		// a dla tendencji malej¹cej zmieni³a siê wartoœæ minimum, 
		// albo zmieni³a siê tendencja funkcji.
		
		//tendencja rosn¹ca
		for (; i < samples.length - 1; i++) {
			if(tendency == Tendency.INCREASING) {
				// je¿eli nastêpna próbka jest wiêksza to weŸ j¹ jako maksimum
				if(samples[maxTemp] < samples[i]) {
					maxTemp = i;
					break;
				}
				//obecne maksimum jest wiêksze o deltê 
				else if (samples[maxTemp] > (samples[i] + delta)) {
					minTemp = i;
					tendency = Tendency.DECREASING;
					break;
				}
				//nie jest wiêkszy o deltê - szukaj dalej
				else {
					//maxTemp zostawiam bez zmian
					minTemp = i; 
				}
			//tendencja malej¹ca
			} else {
				//je¿eli nastêpna próbka jest mniejsza to weŸ j¹ jako minimum
				if(samples[minTemp] > samples[i]) {
					minTemp = i;
					break;
				}
				//je¿eli obecne minimum jest mniejsze o deltê
				else if (samples[minTemp] < (samples[i] - delta)) {
					maxTemp = i;
					tendency = Tendency.INCREASING;
					break;
				} 
				else {
					maxTemp = i;
				}
			}
		}
		
		// dalsza czêœæ odbywa siê ju¿ normalnie
		for(; i < samples.length - 1; i++) {
			//tendencja rosn¹ca
			if(tendency == Tendency.INCREASING) {
				// je¿eli nastêpna próbka jest wiêksza to weŸ j¹ jako maksimum
				if(samples[maxTemp] < samples[i])
					maxTemp = i;
				//obecne maksimum jest wiêksze o deltê
				else if (samples[maxTemp] > (samples[i] + delta)) {
					max.add(maxTemp);
					minTemp = i;
					tendency = Tendency.DECREASING;
				}
				//nie jest wiêksze o deltê - szukaj dalej
				else {
					minTemp = i; 
				}
			//tendencja malej¹ca
			} else {
				//je¿eli nastêpna próbka jest mniejsza od to weŸ j¹ jako minimum
				if(samples[minTemp] > samples[i])
					minTemp = i;
				//je¿eli obecne minimum jest mniejsze o deltê
				else if (samples[minTemp] < (samples[i] - delta)) {
					min.add(minTemp);
					maxTemp = i;
					tendency = Tendency.INCREASING;
				} 
				else {
					maxTemp = i;
				}
			}
			
			
		}
		
		//Dodaj ostatni¹ próbkê
		min.add(samples.length - 1);
		max.add(samples.length - 1);
	} // koniec metody ekstremum
	
	/**
	 * Wyszukiwanie miejsc zerowych w sygnale
	 * @param samples sygna³, w którym maj¹ byæ znalezione miejsca zerowe
	 * @return Zeros - obiekt przechowuj¹cy po³o¿enie oraz liczbê wartoœci miejsc zerowych
	 */
	private Zeros findingZeros(float[] samples) {
		Zeros zeros = new Zeros();
		
		zeros.nrOfZeros = 0;
		zeros.zeroPoints.clear();
		
		Tendency tendency = Tendency.NO_TENDENCY;
		
		int i;
		
		//Je¿eli funkcja nie ma jeszcze okreœlonej tendencji sprawdzaj czy wartoœæ kolejnej próbki 
		//nie przekroczy³a zadanego przedzia³u tolerancji od wartoœci zerowej
		for(i = 0; i <(samples.length - 1) && (tendency == Tendency.NO_TENDENCY); i++) {
			if (samples[i] > delta/2)
				tendency = Tendency.INCREASING;
			if(samples[i] < -delta/2)
				tendency = Tendency.DECREASING;
		}
		
		//Je¿eli funkcja ma ju¿ okreœlon¹ tendencjê kontynuuj wyszukiwanie, ale za miejsce zerowe weŸ
		//miejsce w którym próbka przekroczy oœ o wyznaczon¹ tolerancjê
		for(; i < samples.length-1; i++) {
			if (tendency == Tendency.INCREASING) {
				if (samples[i] < -delta) {
					zeros.nrOfZeros++;
					zeros.zeroPoints.add(i);
					tendency = Tendency.DECREASING;
				}
			}
			else if(tendency == Tendency.DECREASING){
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
	 * Okreœlenie czêstotliwoœci najbli¿ej mikrodr¿enia spoœród wszystkich otrzymanych funkcji IMF.
	 * @param imfMap mapa funkcji IMF
	 * @return klasa MicrotremorFunction przechowuj¹ca funkcjê IMF najbli¿sz¹ mikrodr¿eniu
	 */
	private MicrotremorFunction findClosestFrequencyToMicrotremor(HashMap<Integer, IMFFunction> imfMap) {
		
		// iterator mapy
		Iterator<Map.Entry<Integer, IMFFunction>> iterator = imfMap.entrySet().iterator();
		Map.Entry<Integer, IMFFunction> mapEntry;
		
		// czêstotliwoœæ mikrodr¿enia
		float microtremorFrequency;
		int id = 0;
		
		// przyjêcie pierwszej funkcji jako mikrodr¿enia
		if(iterator.hasNext()) {
			mapEntry = iterator.next();
			microtremorFrequency = mapEntry.getValue().freq;
			id = 0;
		}
		else {
			Debug.debug("nie ma zadnej czestotliwosci do analizowania w funkcji findClosestFrequencyToMicrottremor()");
			return null;
		}
		
		while(iterator.hasNext()) {
			mapEntry = iterator.next();
			
			//znalezienie ID funkcji IMF najbli¿szej czêstotliwoœci mikrodr¿enia 
			if(Math.abs(10 - mapEntry.getValue().freq) < Math.abs(10 - microtremorFrequency)) {
				microtremorFrequency = mapEntry.getValue().freq;
				id = mapEntry.getKey();
			}
		}
		
		// zwróæ funkcjê odpowiadaj¹c¹ mikrodr¿eniu, jej czêstotliwoœæ  oraz podjêt¹ decyzjê.
		return new MicrotremorFunction((imfMap.get((Integer)id).samples), microtremorFrequency, decision(microtremorFrequency));
	} // koniec metody findClosestFrequencyToMicrotremor

	/**
	 * Podjêcie decyzji odnoœnie wystêpowania stresu
	 * @param frequency czêstotliwoœæ funkcji IMF
	 * @return decyzja
	 */
	private boolean decision(float frequency) {
		//wykryto stres
		if (frequency < 8 || frequency > 12)
			return true;
		//brak stresu
		else
			return false;
	}

} // koniec klasy Analysis
