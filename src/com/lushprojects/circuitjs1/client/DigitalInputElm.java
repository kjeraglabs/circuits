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

// Digital input pin, like an MCU GPIO set as input. Reads the pin voltage
// (relative to ground) against VIL/VIH: H, L, or ? when in between.
// Optional internal pull-up (to VDD) or pull-down resistor; 0 ohms = none.
// Max voltage and current use the component ratings, so the pin can burn out.
class DigitalInputElm extends CircuitElm {
    static final int PULL_NONE = 0, PULL_UP = 1, PULL_DOWN = 2;
    double vil, vih, vdd, pullR;
    int pull;

    public DigitalInputElm(int xx, int yy) {
	super(xx, yy);
	vil = .8;
	vih = 2.0;
	vdd = 3.3;
	pull = PULL_NONE;
	pullR = 40000;
	ratedVoltage = 3.6;   // typical 3.3 V MCU pin: VDD + 0.3 V
    }

    public DigitalInputElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st) {
	super(xa, ya, xb, yb, f);
	vil = new Double(st.nextToken()).doubleValue();
	vih = new Double(st.nextToken()).doubleValue();
	vdd = new Double(st.nextToken()).doubleValue();
	pull = new Integer(st.nextToken()).intValue();
	pullR = new Double(st.nextToken()).doubleValue();
    }

    int getDumpType() { return 903; }
    String dump() { return super.dump() + " " + vil + " " + vih + " " + vdd + " " + pull + " " + pullR; }
    int getShortcut() { return 'I'; }
    int getPostCount() { return 1; }
    boolean supportsRatings() { return true; }
    double getVoltageDiff() { return volts[0]; }
    boolean hasPull() { return pull != PULL_NONE && pullR > 0; }
    boolean hasGroundConnection(int n1) { return hasPull(); }

    void stamp() {
	if (!hasPull())
	    return;
	// pull-up: Norton equivalent of VDD through R; pull-down: R to ground
	sim.stampResistor(nodes[0], 0, pullR);
	if (pull == PULL_UP)
	    sim.stampCurrentSource(0, nodes[0], vdd/pullR);
    }

    // current flowing into the pin from the circuit
    void calculateCurrent() {
	if (!hasPull())
	    current = 0;
	else if (pull == PULL_UP)
	    current = (volts[0] - vdd)/pullR;
	else
	    current = volts[0]/pullR;
    }

    // 1 = high, 0 = low, -1 = undefined
    int level() {
	if (volts[0] >= vih) return 1;
	if (volts[0] <= vil) return 0;
	return -1;
    }

    void draw(Graphics g) {
	int l = level();
	String s = (l == 1) ? "H" : (l == 0) ? "L" : "?";
	Color fill = (l == 1) ? IoTag.tint(IoTag.GREEN, 1) :
	    (l == 0) ? IoTag.tint(IoTag.GREEN, .1) : IoTag.tint(IoTag.AMBER, .8);
	IoTag.draw(g, this, false, s, fill);
	if (hasPull()) {
	    Point p = interpPoint(point1, IoTag.leadEnd(this), .5, -10);
	    Font old = g.getFont();
	    g.setFont(new Font("SansSerif", 0, 9));
	    g.setColor(lightGrayColor);
	    drawCenteredText(g, pull == PULL_UP ? "PU" : "PD", p.x, p.y, true);
	    g.setFont(old);
	}
	updateDotCount();
	drawDots(g, point1, IoTag.leadEnd(this), curcount);
	drawPosts(g);
    }

    void getInfo(String arr[]) {
	int l = level();
	arr[0] = "digital input";
	arr[1] = (l == 1) ? "high" : (l == 0) ? "low" : "undefined (between VIL and VIH)";
	arr[2] = "V = " + getVoltageText(volts[0]);
	arr[3] = "VIL = " + getVoltageText(vil) + ", VIH = " + getVoltageText(vih);
	if (hasPull()) {
	    arr[4] = (pull == PULL_UP ? "pull-up " : "pull-down ") + getUnitText(pullR, sim.ohmString);
	    arr[5] = "I = " + getCurrentText(current);
	}
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0) return new EditInfo("Low threshold VIL (V)", vil);
	if (n == 1) return new EditInfo("High threshold VIH (V)", vih);
	if (n == 2) {
	    EditInfo ei = new EditInfo("Internal pull resistor", 0);
	    ei.choice = new Choice();
	    ei.choice.add("None");
	    ei.choice.add("Pull-up to VDD");
	    ei.choice.add("Pull-down to ground");
	    ei.choice.select(pull);
	    return ei;
	}
	if (n == 3) return new EditInfo("Pull resistor (ohms, 0 = none)", pullR);
	if (n == 4) return new EditInfo("VDD for pull-up (V)", vdd);
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (n == 0) vil = ei.value;
	if (n == 1) vih = ei.value;
	if (n == 2) pull = ei.choice.getSelectedIndex();
	if (n == 3 && ei.value >= 0) pullR = ei.value;
	if (n == 4) vdd = ei.value;
	if (vih < vil) vih = vil;
    }
}
