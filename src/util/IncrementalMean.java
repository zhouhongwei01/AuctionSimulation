package util;

import java.util.Collection;

public class IncrementalMean {
	protected int numElements;
	protected double average;
	
	public IncrementalMean() {
		numElements = 0;
		average = 0;
	}
	
	public IncrementalMean(int numElements, double average) {
		this.numElements = numElements;
		this.average = average;
	}
	
	public void add(double newValue) {
		this.average = this.average + (newValue - this.average)/(++numElements);
		assert !Double.isNaN(this.average);
	}
	
	public void add(Collection<Double> values) {
		for (Double value : values) {
			add(value);
		}
	}
	
	public void addAverage(int numElements, double average) {
		this.average = this.average + (average - this.average) * numElements / (this.numElements + numElements);
		this.numElements += numElements;
	}
	
	public void removeAverage(int numElements, double average) {
		this.average = ((this.average * this.numElements) - (average * numElements))/(this.numElements - numElements);
		this.numElements -= numElements;
	}
	
	public double average() {
		if (numElements == 0)
			return Double.NaN;
		return average;
	}
	public int numElements() {
		return numElements;
	}
	
	@Override
	public String toString() {
		return "(avg: " + this.average + ", numEle: " + this.numElements + ")";
	}
}
