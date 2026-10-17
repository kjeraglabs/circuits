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

// Analog output pin, like a DAC output: drives a set voltage (relative to
// ground) through an output resistance (0 = ideal). The voltage is shown
// next to the pin and can be put on a slider (Edit > Sliders).
class AnalogOutputElm extends CircuitElm {
    static final double MIN_R = 1e-3;
    double vOut, rOut;

    public AnalogOutputElm(int xx, int yy) {
	super(xx, yy);
	vOut = 1.65;
	rOut = 0;
    }

    public AnalogOutputElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st) {
	super(xa, ya, xb, yb, f);
	vOut = new Double(st.nextToken()).doubleValue();
	rOut = new Double(st.nextToken()).doubleValue();
    }

    int getDumpType() { return 905; }
    String dump() { return super.dump() + " " + vOut + " " + rOut; }
    int getShortcut() { return 'U'; }
    int getPostCount() { return 1; }
    boolean supportsRatings() { return true; }
    double getVoltageDiff() { return volts[0]; }
    boolean hasGroundConnection(int n1) { return true; }
    double r() { return Math.max(rOut, MIN_R); }

    void stamp() {
	sim.stampResistor(nodes[0], 0, r());
	sim.stampCurrentSource(0, nodes[0], vOut/r());
    }

    // current flowing out of the pin into the circuit
    void calculateCurrent() { current = (vOut - volts[0])/r(); }

    void draw(Graphics g) {
	IoTag.draw(g, this, true, "~", IoTag.tint(IoTag.GREEN, .1));
	// voltage label beside the tag, on the side away from the pin
	Point c = IoTag.center(this);
	Point t = interpPoint(point1, point2, 1 + 6/dn);
	String s = getUnitText(vOut, "V");
	Font old = g.getFont();
	g.setFont(new Font("SansSerif", Font.BOLD, 12));
	g.setColor(whiteColor);
	// put the text past the end of the tag, left- or right-aligned by direction
	int w = (int) g.context.measureText(s).getWidth();
	int tx = t.x, ty = t.y;
	if (dx > 0) tx += 2;
	else if (dx < 0) tx -= w + 2;
	else { tx -= w/2; ty += (dy > 0 ? 12 : -4); }
	g.drawString(s, tx, ty + (dx != 0 ? 4 : 0));
	g.setFont(old);
	adjustBbox(new Point(tx, ty - 12), new Point(tx + w, ty + 4));
	updateDotCount();
	drawDots(g, IoTag.leadEnd(this), point1, curcount);
	drawPosts(g);
    }

    void getInfo(String arr[]) {
	arr[0] = "analog output";
	arr[1] = "set to " + getVoltageText(vOut);
	arr[2] = "V = " + getVoltageText(volts[0]);
	arr[3] = "I = " + getCurrentText(current);
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0) return new EditInfo("Output voltage (V)", vOut);
	if (n == 1) return new EditInfo("Output resistance (ohms, 0 = ideal)", rOut).disallowSliders();
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (n == 0) vOut = ei.value;
	if (n == 1 && ei.value >= 0) rOut = ei.value;
    }
}
