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

// Generic load: a circuit block that runs between a minimum and maximum supply
// voltage and then draws a fixed current, like an MCU or sensor module.
// Below the minimum it is off (brownout), above the maximum it shuts off
// (overvoltage lockout). Around both limits the current ramps over RAMP volts
// so the solver has a smooth curve to work with. Pin 1 (+) is point1.
class GenericLoadElm extends CircuitElm {
    static final double RAMP = .05;
    static final double LEAK = 1e-8;   // keeps the nodes from floating when off
    double vMin, vMax, iLoad;
    double lastV;

    public GenericLoadElm(int xx, int yy) {
	super(xx, yy);
	vMin = 3.0;
	vMax = 5.5;
	iLoad = .01;
    }

    public GenericLoadElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st) {
	super(xa, ya, xb, yb, f);
	vMin = new Double(st.nextToken()).doubleValue();
	vMax = new Double(st.nextToken()).doubleValue();
	iLoad = new Double(st.nextToken()).doubleValue();
    }

    int getDumpType() { return 902; }
    String dump() { return super.dump() + " " + vMin + " " + vMax + " " + iLoad; }
    int getShortcut() { return 'L'; }
    boolean supportsRatings() { return true; }
    boolean nonLinear() { return true; }

    // fraction of the set current drawn at voltage v (0 = off, 1 = fully on)
    double onFraction(double v) {
	if (v <= vMin - RAMP) return 0;
	if (v < vMin) return (v - (vMin - RAMP))/RAMP;
	if (v <= vMax) return 1;
	if (v < vMax + RAMP) return 1 - (v - vMax)/RAMP;
	return 0;
    }

    double slope(double v) {
	if (v > vMin - RAMP && v < vMin) return 1/RAMP;
	if (v > vMax && v < vMax + RAMP) return -1/RAMP;
	return 0;
    }

    double loadCurrent(double v) { return iLoad*onFraction(v) + LEAK*v; }

    void stamp() {
	sim.stampNonLinear(nodes[0]);
	sim.stampNonLinear(nodes[1]);
    }

    // When a Newton step enters one of the ramps from outside, stop in the middle
    // of that ramp instead of jumping past it. Otherwise a weak supply makes the
    // solver bounce between "off" and "fully on" forever (the true answer is
    // on the ramp: a brownout).
    double limitStep(double v, double last) {
	double r1lo = vMin - RAMP, r1hi = vMin, r2lo = vMax, r2hi = vMax + RAMP;
	boolean in1 = last >= r1lo && last <= r1hi;
	boolean in2 = last >= r2lo && last <= r2hi;
	if (v > last) {
	    if (!in1 && last < r1lo && v > r1lo) return vMin - RAMP/2;
	    if (!in2 && last < r2lo && v > r2lo) return vMax + RAMP/2;
	} else if (v < last) {
	    if (!in2 && last > r2hi && v < r2hi) return vMax + RAMP/2;
	    if (!in1 && last > r1hi && v < r1hi) return vMin - RAMP/2;
	}
	return v;
    }

    void doStep() {
	double v = volts[0] - volts[1];
	if (Math.abs(v - lastV) > 1e-3)
	    sim.converged = false;
	v = limitStep(v, lastV);
	lastV = v;
	double gq = iLoad*slope(v) + LEAK;
	double ieq = loadCurrent(v) - gq*v;
	sim.stampConductance(nodes[0], nodes[1], gq);
	sim.stampCurrentSource(nodes[0], nodes[1], ieq);
    }

    void calculateCurrent() { current = loadCurrent(volts[0] - volts[1]); }

    void reset() {
	super.reset();
	lastV = 0;
    }

    String stateText() {
	double v = getVoltageDiff();
	double f = onFraction(v);
	if (f >= 1) return "on";
	if (v > vMax) return f > 0 ? "shutting off (over voltage)" : "off (over voltage)";
	return f > 0 ? "partly on (brownout)" : "off (under voltage)";
    }

    Point body[];
    void setPoints() {
	super.setPoints();
	calcLeads(36);
	body = newPointArray(4);
	interpPoint2(lead1, lead2, body[0], body[1], 0, 13);
	interpPoint2(lead1, lead2, body[3], body[2], 1, 13);
    }

    void draw(Graphics g) {
	setBbox(point1, point2, 15);
	draw2Leads(g);
	double f = onFraction(getVoltageDiff());
	Polygon p = new Polygon();
	int i;
	for (i = 0; i != 4; i++)
	    p.addPoint(body[i].x, body[i].y);
	g.setColor(IoTag.tint(IoTag.GREEN, .12 + .88*f));
	g.fillPolygon(p);
	g.context.save();
	g.context.setStrokeStyle((needsHighlight() ? selectColor : lightGrayColor).getHexValue());
	g.context.setLineWidth(1.5);
	g.context.beginPath();
	g.context.moveTo(body[0].x, body[0].y);
	for (i = 1; i != 4; i++)
	    g.context.lineTo(body[i].x, body[i].y);
	g.context.closePath();
	g.context.stroke();
	g.context.restore();

	Point c = interpPoint(lead1, lead2, .5);
	Font old = g.getFont();
	g.setColor(whiteColor);
	g.setFont(new Font("SansSerif", Font.BOLD, 8));
	drawCenteredText(g, "LOAD", c.x, c.y, true);
	// + marks pin 1
	Point plus = interpPoint(point1, lead1, .5, 10);
	g.setFont(new Font("SansSerif", 0, 11));
	drawCenteredText(g, "+", plus.x, plus.y, true);
	g.setFont(old);

	if (sim.showValuesCheckItem.getState())
	    drawValues(g, getShortUnitText(iLoad, "A"), 15);
	doDots(g);
	drawPosts(g);
    }

    void getInfo(String arr[]) {
	arr[0] = "generic load";
	arr[1] = stateText();
	arr[2] = "V = " + getVoltageText(getVoltageDiff());
	arr[3] = "I = " + getCurrentText(getCurrent());
	arr[4] = "runs at " + getVoltageText(vMin) + " to " + getVoltageText(vMax) +
	    ", draws " + getCurrentText(iLoad);
	arr[5] = "P = " + getUnitText(getPower(), "W");
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0) return new EditInfo("Minimum operating voltage (V)", vMin);
	if (n == 1) return new EditInfo("Maximum operating voltage (V)", vMax);
	if (n == 2) return new EditInfo("Current when on (A)", iLoad);
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (n == 0) vMin = ei.value;
	if (n == 1) vMax = ei.value;
	if (n == 2 && ei.value >= 0) iLoad = ei.value;
	if (vMax < vMin) vMax = vMin;
    }
}
