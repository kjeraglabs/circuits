/*
    Copyright (C) Paul Falstad and Iain Sharp

    This file is part of CircuitJS1.

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

// Op amp with V+ and V- supply pins (Kjerag Labs addition).
// The output is limited to the supply rails (minus an optional swing loss), and
// the current the output sources or sinks is drawn from V+ or returned to V-,
// so supplies see the load current.  Mirroring top/bottom swaps the inputs but
// keeps V+ on top.
class OpAmpSupplyElm extends OpAmpElm {
    final int FLAG_SUPPLY_SWAP = 16;
    double swingLoss;           // volts lost to each rail
    double lastIout;
    Point vccp[], vssp[];       // [0] = pin, [1] = where the lead meets the triangle

    public OpAmpSupplyElm(int xx, int yy) {
	super(xx, yy);
	swingLoss = 0;
    }

    public OpAmpSupplyElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	swingLoss = 0;
	try {
	    swingLoss = new Double(st.nextToken()).doubleValue();
	} catch (Exception e) {}
    }

    int getDumpType() { return 901; }
    String dump() { return super.dump() + " " + swingLoss; }
    int getShortcut() { return 'A'; }

    void flipSupplySide() { flags ^= FLAG_SUPPLY_SWAP; }

    int getPostCount() { return 5; }
    Point getPost(int n) {
	if (n == 3) return vccp[0];
	if (n == 4) return vssp[0];
	return super.getPost(n);
    }

    void setPoints() {
	super.setPoints();
	int sgn = ((flags & FLAG_SUPPLY_SWAP) != 0) ? -1 : 1;
	// supply pins sit on the grid point nearest the middle of the body
	Point mid = interpPoint(point1, point2, .5);
	Point sm = new Point(sim.snapGrid(mid.x), sim.snapGrid(mid.y));
	double f = ((sm.x-point1.x)*(double) dx + (sm.y-point1.y)*(double) dy)/(dn*dn);
	// triangle half-height at that point along the body
	double l1 = distance(point1, lead1), l2 = distance(point1, lead2);
	double t = (f*dn - l1)/(l2 - l1);
	t = Math.max(0, Math.min(1, t));
	double edge = 2*opheight*(1-t);
	int g = dsign*sgn;    // V+ toward the top for a left-to-right op amp
	vccp = newPointArray(2);
	vssp = newPointArray(2);
	vccp[0] = interpPoint(point1, point2, f,  g*2*opheight);
	vccp[1] = interpPoint(point1, point2, f,  g*edge);
	vssp[0] = interpPoint(point1, point2, f, -g*2*opheight);
	vssp[1] = interpPoint(point1, point2, f, -g*edge);
    }

    void draw(Graphics g) {
	setVoltageColor(g, volts[3]);
	drawThickLine(g, vccp[0], vccp[1]);
	setVoltageColor(g, volts[4]);
	drawThickLine(g, vssp[0], vssp[1]);
	super.draw(g);
	g.setFont(unitsFont);
	g.setColor(whiteColor);
	// small V+ / V- marks next to the supply pins
	Point a = interpPoint(vccp[0], vccp[1], .5, 9);
	Point b = interpPoint(vssp[0], vssp[1], .5, -9);
	drawCenteredText(g, "+", a.x, a.y, true);
	drawCenteredText(g, "−", b.x, b.y, true);
    }

    void stamp() {
	super.stamp();
	// supply currents change every iteration
	sim.stampRightSide(nodes[3]);
	sim.stampRightSide(nodes[4]);
    }

    double railHigh() { return volts[3]; }
    double railLow() { return volts[4]; }

    void doStep() {
	maxOut = volts[3] - swingLoss;
	minOut = volts[4] + swingLoss;
	if (maxOut < minOut)
	    maxOut = minOut = (volts[3] + volts[4])/2;
	super.doStep();
	// output current comes from V+ when sourcing, goes to V- when sinking
	double iout = -current;
	if (Math.abs(iout - lastIout) > 1e-9 + 1e-4*Math.abs(iout))
	    sim.converged = false;
	lastIout = iout;
	sim.stampCurrentSource(iout > 0 ? nodes[3] : nodes[4], 0, iout);
    }

    void reset() {
	super.reset();
	lastIout = 0;
    }

    boolean getConnection(int n1, int n2) { return false; }
    boolean hasGroundConnection(int n1) { return n1 == 2; }

    @Override double getCurrentIntoNode(int n) {
	double iout = -current;
	if (n == 2) return iout;
	if (n == 3) return iout > 0 ? -iout : 0;
	if (n == 4) return iout < 0 ? -iout : 0;
	return 0;
    }

    void getInfo(String arr[]) {
	super.getInfo(arr);
	arr[0] = "op-amp (with supply pins)";
	arr[5] = "V+ supply = " + getVoltageText(volts[3]);
	arr[6] = "V- supply = " + getVoltageText(volts[4]);
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Output swing loss to each rail (V)", swingLoss, 0, 3);
	if (n == 1)
	    return new EditInfo("Gain", gain, 10, 1000000);
	return null;
    }
    public void setEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.value >= 0)
	    swingLoss = ei.value;
	if (n == 1 && ei.value > 0)
	    gain = ei.value;
    }
}
