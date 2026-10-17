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

// Digital output pin, like an MCU GPIO set as output. Click it to switch
// between ON and OFF. What the pin does in each state is set separately:
// drive high (to VDD), drive low (to ground) or high-Z, so push-pull,
// open-drain and open-source pins are all possible. The driver has an output
// resistance, and the pin's max current uses the component ratings.
class DigitalOutputElm extends SwitchElm {
    static final int DRIVE_HIGH = 0, DRIVE_LOW = 1, HIGH_Z = 2;
    static final double MIN_R = 1e-3;
    double vdd, rOut;
    int modeOn, modeOff;

    public DigitalOutputElm(int xx, int yy) {
	super(xx, yy, false);
	position = 0;          // 0 = OFF, 1 = ON
	vdd = 3.3;
	rOut = 25;
	modeOn = DRIVE_HIGH;
	modeOff = DRIVE_LOW;
	ratedCurrent = .025;   // typical MCU pin limit
    }

    public DigitalOutputElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	vdd = new Double(st.nextToken()).doubleValue();
	rOut = new Double(st.nextToken()).doubleValue();
	modeOn = new Integer(st.nextToken()).intValue();
	modeOff = new Integer(st.nextToken()).intValue();
    }

    int getDumpType() { return 904; }
    String dump() { return super.dump() + " " + vdd + " " + rOut + " " + modeOn + " " + modeOff; }
    int getShortcut() { return 'O'; }
    int getPostCount() { return 1; }
    int getVoltageSourceCount() { return 0; }
    boolean isWire() { return false; }
    boolean getConnection(int n1, int n2) { return false; }
    boolean supportsRatings() { return true; }
    double getVoltageDiff() { return volts[0]; }
    boolean isOn() { return position == 1; }
    int mode() { return isOn() ? modeOn : modeOff; }
    double r() { return Math.max(rOut, MIN_R); }
    boolean hasGroundConnection(int n1) { return mode() != HIGH_Z; }

    void setPoints() {
	super.setPoints();
	lead1 = IoTag.leadEnd(this);
    }

    Rectangle getSwitchRect() {
	Point c = IoTag.center(this);
	return new Rectangle(c.x - 15, c.y - 15, 30, 30);
    }

    void stamp() {
	int m = mode();
	if (m == HIGH_Z)
	    return;
	sim.stampResistor(nodes[0], 0, r());
	if (m == DRIVE_HIGH)
	    sim.stampCurrentSource(0, nodes[0], vdd/r());
    }

    // current flowing out of the pin into the circuit
    void calculateCurrent() {
	int m = mode();
	if (m == HIGH_Z)
	    current = 0;
	else if (m == DRIVE_HIGH)
	    current = (vdd - volts[0])/r();
	else
	    current = -volts[0]/r();
    }

    static String modeLetter(int m) { return m == DRIVE_HIGH ? "H" : m == DRIVE_LOW ? "L" : "Z"; }

    String typeName() {
	if (modeOn == DRIVE_HIGH && modeOff == DRIVE_LOW) return "push-pull";
	if (modeOn == DRIVE_LOW && modeOff == HIGH_Z) return "open drain";
	if (modeOn == DRIVE_HIGH && modeOff == HIGH_Z) return "open source";
	return "custom";
    }

    void draw(Graphics g) {
	Color fill = isOn() ? IoTag.tint(IoTag.GREEN, 1) : IoTag.tint(IoTag.GREEN, .1);
	IoTag.draw(g, this, true, modeLetter(mode()), fill);
	updateDotCount();
	drawDots(g, IoTag.leadEnd(this), point1, curcount);
	drawPosts(g);
    }

    void getInfo(String arr[]) {
	arr[0] = "digital output (" + typeName() + ")";
	int m = mode();
	arr[1] = (isOn() ? "ON" : "OFF") + ": " +
	    (m == DRIVE_HIGH ? "driving high" : m == DRIVE_LOW ? "driving low" : "high-Z");
	arr[2] = "V = " + getVoltageText(volts[0]);
	arr[3] = "I = " + getCurrentText(current);
	arr[4] = "click to switch";
    }

    Choice modeChoice(int sel) {
	Choice c = new Choice();
	c.add("Drive high (source)");
	c.add("Drive low (sink)");
	c.add("High-Z");
	c.select(sel);
	return c;
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0) {
	    EditInfo ei = new EditInfo("When ON", 0);
	    ei.choice = modeChoice(modeOn);
	    return ei;
	}
	if (n == 1) {
	    EditInfo ei = new EditInfo("When OFF", 0);
	    ei.choice = modeChoice(modeOff);
	    return ei;
	}
	if (n == 2) return new EditInfo("High level VDD (V)", vdd);
	if (n == 3) return new EditInfo("Output resistance (ohms)", rOut);
	if (n == 4) {
	    EditInfo ei = new EditInfo("", 0);
	    ei.checkbox = new Checkbox("Momentary (ON only while held)", momentary);
	    return ei;
	}
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (n == 0) modeOn = ei.choice.getSelectedIndex();
	if (n == 1) modeOff = ei.choice.getSelectedIndex();
	if (n == 2) vdd = ei.value;
	if (n == 3 && ei.value >= 0) rOut = ei.value;
	if (n == 4) momentary = ei.checkbox.getState();
    }
}
