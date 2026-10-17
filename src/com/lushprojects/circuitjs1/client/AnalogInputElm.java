/*
    Copyright (C) Kjerag Labs

    This file is part of CircuitJS1 (Kjerag Labs edition).

    CircuitJS1 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    CircuitJS1 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CircuitJS1.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lushprojects.circuitjs1.client;

// Analog input pin, like an MCU ADC input. Shows the pin voltage (relative to
// ground) and the ADC code it would read for the set reference and resolution.
// Optional input resistance to ground (0 = infinite). Max voltage/current use
// the component ratings, so overdriving the pin burns it.
class AnalogInputElm extends CircuitElm {
    double vref, rIn;
    int bits;

    public AnalogInputElm(int xx, int yy) {
	super(xx, yy);
	vref = 3.3;
	bits = 12;
	rIn = 0;
	ratedVoltage = 3.6;   // typical 3.3 V MCU pin: VDD + 0.3 V
    }

    public AnalogInputElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st) {
	super(xa, ya, xb, yb, f);
	vref = new Double(st.nextToken()).doubleValue();
	bits = new Integer(st.nextToken()).intValue();
	rIn = new Double(st.nextToken()).doubleValue();
    }

    int getDumpType() { return 908; }
    String dump() { return super.dump() + " " + vref + " " + bits + " " + rIn; }
    int getPostCount() { return 1; }
    boolean supportsRatings() { return true; }
    double getVoltageDiff() { return volts[0]; }
    boolean hasGroundConnection(int n1) { return rIn > 0; }

    void stamp() {
	if (rIn > 0)
	    sim.stampResistor(nodes[0], 0, rIn);
    }

    // current flowing into the pin
    void calculateCurrent() { current = (rIn > 0) ? volts[0]/rIn : 0; }

    int maxCode() { return (int) Math.round(Math.pow(2, bits)) - 1; }

    // the code an ideal ADC would return, clamped to its range
    int code() {
	if (vref <= 0)
	    return 0;
	// tiny allowance so exact fractions like Vref/2 don't round down from 0.49999...
	double c = Math.floor(volts[0]/vref*(maxCode() + 1) + 1e-6);
	return (int) Math.max(0, Math.min(maxCode(), c));
    }

    void draw(Graphics g) {
	boolean over = volts[0] > vref || volts[0] < 0;
	IoTag.draw(g, this, false, "~", over ? IoTag.tint(IoTag.AMBER, .8) : IoTag.tint(IoTag.GREEN, .1));
	IoTag.drawLabel(g, this, getUnitText(volts[0], "V") + "  [" + code() + "]");
	updateDotCount();
	drawDots(g, point1, IoTag.leadEnd(this), curcount);
	drawPosts(g);
    }

    void getInfo(String arr[]) {
	arr[0] = "analog input";
	arr[1] = "V = " + getVoltageText(volts[0]);
	arr[2] = "reads " + code() + " of " + maxCode() + " (" + bits + "-bit, Vref " + getVoltageText(vref) + ")";
	if (volts[0] > vref)
	    arr[3] = "above Vref: clipped to full scale";
	else if (volts[0] < 0)
	    arr[3] = "below ground: clipped to 0";
	if (rIn > 0) {
	    int i;
	    for (i = 0; arr[i] != null; i++)
		;
	    arr[i] = "input " + getUnitText(rIn, sim.ohmString) + ", I = " + getCurrentText(current);
	}
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0) return new EditInfo("Reference voltage Vref (V)", vref);
	if (n == 1) return new EditInfo("Resolution (bits)", bits).setDimensionless();
	if (n == 2) return new EditInfo("Input resistance (ohms, 0 = infinite)", rIn);
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.value > 0) vref = ei.value;
	if (n == 1 && ei.value >= 1 && ei.value <= 24) bits = (int) Math.round(ei.value);
	if (n == 2 && ei.value >= 0) rIn = ei.value;
    }
}
