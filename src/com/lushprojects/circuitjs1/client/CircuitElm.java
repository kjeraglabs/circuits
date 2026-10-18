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

import java.util.Vector;

import com.google.gwt.canvas.dom.client.CanvasGradient;
import com.google.gwt.canvas.dom.client.Context2d.LineCap;
import com.google.gwt.i18n.client.NumberFormat;

// circuit element class
public abstract class CircuitElm implements Editable {
    static double voltageRange = 5;
    static int colorScaleCount = 32;
    static Color colorScale[];
    static double currentMult, powerMult;
    
    // scratch points for convenience
    static Point ps1, ps2;
    
    static CirSim sim;
    static Color whiteColor, selectColor, lightGrayColor;
    static Font unitsFont;

    static NumberFormat showFormat, shortFormat;//, noCommaFormat;
    static final double pi = 3.14159265358979323846;
    static CircuitElm mouseElmRef = null;

    static final int SCALE_AUTO = 0;
    static final int SCALE_1 = 1;
    static final int SCALE_M = 2;
    static final int SCALE_MU = 3;
 
    // initial point where user created element.  For simple two-terminal elements, this is the first node/post.
    int x, y;
    
    // point to which user dragged out element.  For simple two-terminal elements, this is the second node/post
    int x2, y2;
    
    int flags, nodes[], voltSource;
    
    // length along x and y axes, and sign of difference
    int dx, dy, dsign;

    int lastHandleGrabbed=-1;
    
    // length of element
    double dn;
    
    double dpx1, dpy1;
    
    // (x,y) and (x2,y2) as Point objects
    Point point1, point2;
    
    // lead points (ends of wire stubs for simple two-terminal elements)  
    Point lead1, lead2;
    
    // voltages at each node
    double volts[];
    
    double current, curcount;
    Rectangle boundingBox;
    
    // if subclasses set this to true, element will be horizontal or vertical only 
    boolean noDiagonal;
    
    public boolean selected;
    
//    abstract int getDumpType();
    int getDumpType() {
	
	throw new IllegalStateException(); // Seems necessary to work-around what appears to be a compiler
	// bug affecting OTAElm to make sure this method (which should really be abstract) throws
	// an exception
 }
    
    // leftover from java, doesn't do anything anymore. 
    Class getDumpClass() { return getClass(); }
    
    int getDefaultFlags() { return 0; }

    static void initClass(CirSim s) {
	unitsFont = new Font("SansSerif", 0, 12);
	sim = s;
	
	colorScale = new Color[colorScaleCount];
	
	
	ps1 = new Point();
	ps2 = new Point();

	showFormat=NumberFormat.getFormat("####.###");

	shortFormat=NumberFormat.getFormat("####.#");
    }

    static public Color positiveColor, negativeColor, neutralColor;
    
    static void setColorScale() {

	int i;

	if (positiveColor == null)
	    positiveColor = Color.green;
	if (negativeColor == null)
	    negativeColor = Color.red;
	if (neutralColor == null)
	    neutralColor = Color.gray;
	
	for (i = 0; i != colorScaleCount; i++) {
	    double v = i * 2. / colorScaleCount - 1;
	    if (v < 0) {
		colorScale[i] = new Color(neutralColor, negativeColor, -v);
	    } else {
		colorScale[i] = new Color(neutralColor, positiveColor, v);
	    }
	}

    }
    
    // create new element with one post at xx,yy, to be dragged out by user
    CircuitElm(int xx, int yy) {
	x = x2 = xx;
	y = y2 = yy;
	flags = getDefaultFlags();
	allocNodes();
	initBoundingBox();
    }
    
    // create element between xa,ya and xb,yb from undump
    CircuitElm(int xa, int ya, int xb, int yb, int f) {
	x = xa; y = ya; x2 = xb; y2 = yb; flags = f;
	allocNodes();
	initBoundingBox();
    }
    
    void initBoundingBox() {
	boundingBox = new Rectangle();
	boundingBox.setBounds(min(x, x2), min(y, y2),
			      abs(x2-x)+1, abs(y2-y)+1);
    }
    
    // allocate nodes/volts arrays we need
    void allocNodes() {
	int n = getPostCount() + getInternalNodeCount();
	// preserve voltages if possible
	if (nodes == null || nodes.length != n) {
	    nodes = new int[n];
	    volts = new double[n];
	}
    }
    
    // dump component state for export/undo
    String dump() {
	int t = getDumpType();
	return (t < 127 ? ((char)t)+" " : t+" ") + x + " " + y + " " +
	    x2 + " " + y2 + " " + flags;
    }
    
    // handle reset button
    void reset() {
	int i;
	for (i = 0; i != getPostCount()+getInternalNodeCount(); i++)
	    volts[i] = 0;
	curcount = 0;
    }
    void draw(Graphics g) {}
    
    // set current for voltage source vn to c.  vn will be the same value as in a previous call to setVoltageSource(n, vn) 
    void setCurrent(int vn, double c) { current = c; }
    
    // get current for one- or two-terminal elements
    double getCurrent() { return current; }

    void setParentList(Vector<CircuitElm> elmList) {}
    
    // stamp matrix values for linear elements.
    // for non-linear elements, use this to stamp values that don't change each iteration, and call stampRightSide() or stampNonLinear() as needed
    void stamp() {}
    
    // stamp matrix values for non-linear elements
    void doStep() {}
    
    void delete() {
	if (mouseElmRef==this)
	    mouseElmRef=null;
	sim.deleteSliders(this);
    }
    void startIteration() {}
    
    // get voltage of x'th node
    double getPostVoltage(int x) { return volts[x]; }
    
    // set voltage of x'th node, called by simulator logic
    void setNodeVoltage(int n, double c) {
	volts[n] = c;
	if (failed)
	    current = (volts[0]-(getPostCount() > 1 ? volts[1] : 0))/getFailResistance();
	else
	    calculateCurrent();
    }

    // ---------------------------------------------------------------
    // Component ratings / burn-out (Kjerag Labs addition)
    //
    // Each element that supports ratings can have a max power, max voltage
    // and max current.  0 means "not rated".  Power and current are passed
    // through a first-order filter with time constant ratingTau so short
    // spikes don't destroy parts; voltage is checked instantly.
    // stress >= RATING_WARN draws a yellow triangle, stress > 1 kills the
    // part: it is replaced in the matrix by an open or a short.
    // ---------------------------------------------------------------
    static final double RATING_WARN = 0.8;
    static final int FAIL_OPEN = 0;
    static final int FAIL_SHORT = 1;
    static final double FAIL_OPEN_R = 1e9;
    static final double FAIL_SHORT_R = 1e-2;
    static final int RATING_DUMP_TYPE = 900;

    double ratedPower, ratedVoltage, ratedCurrent;
    double ratingTau = 1e-3;
    int failMode = FAIL_OPEN;
    double filtPower, filtCurrent, stress;
    boolean failed;
    String failReason;

    // override to return true in elements that can be rated
    boolean supportsRatings() { return false; }

    boolean hasRatings() {
	return supportsRatings() && (ratedPower > 0 || ratedVoltage > 0 || ratedCurrent > 0);
    }

    double getFailResistance() {
	return failMode == FAIL_SHORT ? FAIL_SHORT_R : FAIL_OPEN_R;
    }

    // stamp a failed element in place of its normal stamp
    void stampFailed() {
	sim.stampResistor(nodes[0], getPostCount() > 1 ? nodes[1] : 0, getFailResistance());
	// keep any internal nodes (eg diode series resistance) from floating
	int i;
	for (i = getPostCount(); i < getPostCount()+getInternalNodeCount(); i++)
	    sim.stampResistor(nodes[0], nodes[i], FAIL_OPEN_R);
    }

    // called once per completed time step.  returns true if the part just failed.
    boolean ratingStep(double dt) {
	if (failed || !hasRatings())
	    return false;
	double a = (ratingTau > 0) ? Math.min(1, dt/ratingTau) : 1;
	filtPower   += (Math.abs(getPower())   - filtPower)*a;
	filtCurrent += (Math.abs(getCurrent()) - filtCurrent)*a;
	double s = 0;
	String why = null;
	if (ratedPower > 0 && filtPower/ratedPower > s) {
	    s = filtPower/ratedPower; why = "power";
	}
	if (ratedVoltage > 0 && Math.abs(getVoltageDiff())/ratedVoltage > s) {
	    s = Math.abs(getVoltageDiff())/ratedVoltage; why = "voltage";
	}
	if (ratedCurrent > 0 && filtCurrent/ratedCurrent > s) {
	    s = filtCurrent/ratedCurrent; why = "current";
	}
	stress = s;
	if (s > 1 && sim.breakingEnabled) {
	    failed = true;
	    failReason = why;
	    return true;
	}
	return false;
    }

    void resetRating() {
	failed = false;
	failReason = null;
	filtPower = filtCurrent = stress = 0;
    }

    String dumpRating() {
	if (!hasRatings())
	    return null;
	return RATING_DUMP_TYPE + " " + ratedPower + " " + ratedVoltage + " " +
	    ratedCurrent + " " + ratingTau + " " + failMode;
    }

    void undumpRating(StringTokenizer st) {
	ratedPower   = new Double(st.nextToken()).doubleValue();
	ratedVoltage = new Double(st.nextToken()).doubleValue();
	ratedCurrent = new Double(st.nextToken()).doubleValue();
	ratingTau    = new Double(st.nextToken()).doubleValue();
	failMode     = new Integer(st.nextToken()).intValue();
    }

    // one line for the info box, or null
    String getRatingInfo() {
	if (!supportsRatings())
	    return null;
	if (failed)
	    return "BURNED (" + failReason + ", " + (failMode == FAIL_SHORT ? "shorted" : "open") + ")";
	if (!hasRatings())
	    return null;
	if (!sim.breakingEnabled)
	    return null;
	return "stress = " + Math.round(stress*100) + "% of rating";
    }

    EditInfo getRatingEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("<b>Ratings</b> (0 = not rated)<br>Max power (W)", ratedPower).disallowSliders();
	if (n == 1)
	    return new EditInfo("Max voltage (V)", ratedVoltage).disallowSliders();
	if (n == 2)
	    return new EditInfo("Max current (A)", ratedCurrent).disallowSliders();
	if (n == 3)
	    return new EditInfo("Thermal time constant (s)", ratingTau).disallowSliders();
	if (n == 4) {
	    EditInfo ei = new EditInfo("Fails as", 0);
	    ei.choice = new Choice();
	    ei.choice.add("Open circuit");
	    ei.choice.add("Short circuit");
	    ei.choice.select(failMode);
	    return ei;
	}
	return null;
    }

    void setRatingEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.value >= 0) ratedPower = ei.value;
	if (n == 1 && ei.value >= 0) ratedVoltage = ei.value;
	if (n == 2 && ei.value >= 0) ratedCurrent = ei.value;
	if (n == 3 && ei.value >= 0) ratingTau = ei.value;
	if (n == 4) failMode = ei.choice.getSelectedIndex();
	// editing a part repairs it
	resetRating();
    }

    // ---------------------------------------------------------------
    // Tolerance testing (Kjerag Labs addition)
    //
    // Parts can have a tolerance (fraction, 0 = none). In tolerance
    // testing mode each such part gets an up/down switch: up uses
    // value*(1+tol), down value*(1-tol), middle the nominal value.
    // Some parts use an absolute amount instead (op amp offset voltage):
    // up adds +tol, down adds -tol.
    // Saved as an extra "906 amount dev" line after the part (amount in %
    // for relative tolerances, in base units for absolute ones).
    // ---------------------------------------------------------------
    static final int TOLERANCE_DUMP_TYPE = 906;
    double tolerance;
    int tolDev;   // -1 down, 0 nominal, +1 up

    boolean supportsTolerance() { return false; }

    boolean hasTolerance() { return supportsTolerance() && tolerance > 0; }

    // true if the tolerance is an absolute amount (toleranceUnit) instead of a fraction
    boolean toleranceAbsolute() { return false; }
    String toleranceUnit() { return "V"; }
    // what the tolerance applies to, for the info box ("hFE"), or null for the part's value
    String toleranceName() { return null; }
    String toleranceEditLabel() { return "Tolerance (%) (0 = none)"; }

    // multiply the nominal value by this when stamping
    double tolFactor() {
	if (!sim.toleranceMode || !hasTolerance() || toleranceAbsolute())
	    return 1;
	return 1 + tolDev*tolerance;
    }

    // add this to the nominal value (absolute tolerances)
    double tolOffset() {
	if (!sim.toleranceMode || !hasTolerance() || !toleranceAbsolute())
	    return 0;
	return tolDev*tolerance;
    }

    // "5%" or "5 mV"
    String toleranceText() {
	if (toleranceAbsolute())
	    return getUnitText(tolerance, toleranceUnit());
	return showFormat.format(tolerance*100) + "%";
    }

    // called after tolDev or the mode changes, for parts that cache their value
    void toleranceChanged() {}

    String dumpTolerance() {
	if (!hasTolerance())
	    return null;
	return TOLERANCE_DUMP_TYPE + " " + (toleranceAbsolute() ? tolerance : tolerance*100) + " " + tolDev;
    }

    void undumpTolerance(StringTokenizer st) {
	tolerance = new Double(st.nextToken()).doubleValue();
	if (!toleranceAbsolute())
	    tolerance /= 100;
	tolDev = new Integer(st.nextToken()).intValue();
	if (tolDev > 1 || tolDev < -1)
	    tolDev = 0;
    }

    EditInfo getToleranceEditInfo(int n) {
	if (n == 0 && toleranceAbsolute())
	    return new EditInfo(toleranceEditLabel(), tolerance).disallowSliders();
	if (n == 0)
	    return new EditInfo(toleranceEditLabel(), tolerance*100).disallowSliders();
	return null;
    }

    void setToleranceEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.value >= 0 && (toleranceAbsolute() || ei.value < 100)) {
	    tolerance = toleranceAbsolute() ? ei.value : ei.value/100;
	    if (tolerance == 0)
		tolDev = 0;
	    toleranceChanged();
	}
    }

    // e.g. "+5%" for the info box, or null
    String getToleranceInfo() {
	if (!hasTolerance())
	    return null;
	String pct = toleranceText();
	String name = (toleranceName() == null) ? "tolerance" : toleranceName() + " tolerance";
	if (!sim.toleranceMode || tolDev == 0)
	    return name + " \u00b1" + pct + (sim.toleranceMode ? ", at nominal" : "");
	return name + " \u00b1" + pct + ", testing " + (tolDev > 0 ? "+" : "\u2212") + pct;
    }

    // centre of the up/down switch: beside the part, on the opposite side from its value label
    Point toleranceSwitchCenter() {
	int xc = (x+x2)/2, yc = (y+y2)/2;
	int d = 24;
	if (dy == 0)
	    return new Point(xc, yc + d);
	if (dx == 0) {
	    boolean labelLeft = (this instanceof VoltageElm || (x < x2 && y > y2));
	    return new Point(labelLeft ? xc + d : xc - d, yc);
	}
	return interpPoint(point1, point2, .5, d);
    }

    static final int TOL_W = 12, TOL_H = 28;

    Rectangle toleranceSwitchRect() {
	Point c = toleranceSwitchCenter();
	return new Rectangle(c.x - TOL_W/2 - 2, c.y - TOL_H/2 - 2, TOL_W + 4, TOL_H + 4);
    }

    // click at (gx, gy): returns true if it hit this part's switch
    boolean toleranceClick(int gx, int gy) {
	if (!sim.toleranceMode || !hasTolerance() || point1 == null)
	    return false;
	Rectangle r = toleranceSwitchRect();
	if (!r.contains(gx, gy))
	    return false;
	Point c = toleranceSwitchCenter();
	int want = (gy < c.y) ? 1 : -1;
	tolDev = (tolDev == want) ? 0 : want;   // clicking the active half goes back to nominal
	toleranceChanged();
	return true;
    }

    void drawToleranceSwitch(Graphics g) {
	if (!sim.toleranceMode || !hasTolerance() || point1 == null)
	    return;
	Point c = toleranceSwitchCenter();
	double x0 = c.x - TOL_W/2., y0 = c.y - TOL_H/2.;
	String ink = lightGrayColor.getHexValue();
	String bg = sim.getBackgroundColor().getHexValue();
	g.context.save();
	// body
	g.context.setFillStyle(bg);
	g.context.setStrokeStyle(ink);
	g.context.setLineWidth(1.2);
	roundRect(g, x0, y0, TOL_W, TOL_H, 4);
	g.context.fill();
	g.context.stroke();
	// active half
	if (tolDev != 0) {
	    g.context.setFillStyle((tolDev > 0 ? positiveColor : negativeColor).getHexValue());
	    roundRect(g, x0 + 1.5, tolDev > 0 ? y0 + 1.5 : c.y + .5, TOL_W - 3, TOL_H/2. - 2, 3);
	    g.context.fill();
	}
	// divider and arrows
	g.context.beginPath();
	g.context.moveTo(x0 + 2, c.y);
	g.context.lineTo(x0 + TOL_W - 2, c.y);
	g.context.stroke();
	drawTriangle(g, c.x, y0 + 4, c.y - 3, tolDev > 0 ? "#ffffff" : ink);
	drawTriangle(g, c.x, y0 + TOL_H - 4, c.y + 3, tolDev < 0 ? "#ffffff" : ink);
	// percentage next to the switch when not nominal
	if (tolDev != 0) {
	    String s = (tolDev > 0 ? "+" : "\u2212") + toleranceText();
	    g.context.setFont("bold 10px sans-serif");
	    g.context.setFillStyle((tolDev > 0 ? positiveColor : negativeColor).getHexValue());
	    g.context.setTextAlign("center");
	    g.context.fillText(s, c.x, tolDev > 0 ? y0 - 3 : y0 + TOL_H + 10);
	}
	g.context.restore();
    }

    static void roundRect(Graphics g, double x, double y, double w, double h, double r) {
	g.context.beginPath();
	g.context.moveTo(x + r, y);
	g.context.lineTo(x + w - r, y);
	g.context.arcTo(x + w, y, x + w, y + r, r);
	g.context.lineTo(x + w, y + h - r);
	g.context.arcTo(x + w, y + h, x + w - r, y + h, r);
	g.context.lineTo(x + r, y + h);
	g.context.arcTo(x, y + h, x, y + h - r, r);
	g.context.lineTo(x, y + r);
	g.context.arcTo(x, y, x + r, y, r);
	g.context.closePath();
    }

    // small triangle with its tip at (cx, tipY) and base at baseY
    static void drawTriangle(Graphics g, double cx, double tipY, double baseY, String color) {
	g.context.setFillStyle(color);
	g.context.beginPath();
	g.context.moveTo(cx, tipY);
	g.context.lineTo(cx + 3.5, baseY);
	g.context.lineTo(cx - 3.5, baseY);
	g.context.closePath();
	g.context.fill();
    }

    // draw warning triangle / burned X on top of the element
    void drawRatingOverlay(Graphics g) {
	// with component ratings off the simulator behaves like plain CircuitJS
	if (!supportsRatings() || point1 == null || !sim.breakingEnabled)
	    return;
	if (failed) {
	    Point c = interpPoint(point1, point2, .5);
	    int s = 9;
	    g.context.save();
	    g.context.setLineCap("round");
	    g.context.beginPath();
	    g.context.moveTo(c.x-s, c.y-s); g.context.lineTo(c.x+s, c.y+s);
	    g.context.moveTo(c.x+s, c.y-s); g.context.lineTo(c.x-s, c.y+s);
	    // outline in the background colour so the X stands out from red wires
	    g.context.setStrokeStyle(sim.getBackgroundColor().getHexValue());
	    g.context.setLineWidth(7);
	    g.context.stroke();
	    g.context.setStrokeStyle(sim.isPaperTheme() ? "#c0171c" : "#ff2020");
	    g.context.setLineWidth(4);
	    g.context.stroke();
	    g.context.restore();
	    return;
	}
	if (hasRatings() && stress >= RATING_WARN) {
	    Point c = interpPoint(point1, point2, .5, -22);
	    if (sim.toleranceMode && hasTolerance()) {
		// the tolerance switch is beside the part: put the triangle just beyond it
		Point sw = toleranceSwitchCenter();
		int mx = (x+x2)/2, my = (y+y2)/2;
		double vx = sw.x - mx, vy = sw.y - my, len = Math.sqrt(vx*vx + vy*vy);
		if (len > 0)
		    c = new Point((int) (sw.x + vx/len*20), (int) (sw.y + vy/len*20));
	    }
	    int s = 9;
	    g.context.save();
	    g.context.setFillStyle("#ffd000");
	    g.context.setStrokeStyle("#000000");
	    g.context.setLineWidth(1.5);
	    g.context.beginPath();
	    g.context.moveTo(c.x, c.y-s);
	    g.context.lineTo(c.x+s, c.y+s*.8);
	    g.context.lineTo(c.x-s, c.y+s*.8);
	    g.context.closePath();
	    g.context.fill();
	    g.context.stroke();
	    g.context.setFillStyle("#000000");
	    g.context.setFont("bold 11px sans-serif");
	    g.context.setTextAlign("center");
	    g.context.fillText("!", c.x, c.y+s*.65);
	    g.context.restore();
	}
    }
    
    // calculate current in response to node voltages changing
    void calculateCurrent() {}
    
    // calculate post locations and other convenience values used for drawing.  Called when element is moved 
    void setPoints() {
    	dx = x2-x; dy = y2-y;
    	dn = Math.sqrt(dx*dx+dy*dy);
    	dpx1 = dy/dn;
    	dpy1 = -dx/dn;
    	dsign = (dy == 0) ? sign(dx) : sign(dy);
    	point1 = new Point(x , y );
    	point2 = new Point(x2, y2);
    }
    
    // calculate lead points for an element of length len.  Handy for simple two-terminal elements.
    // Posts are where the user connects wires; leads are ends of wire stubs drawn inside the element.
    void calcLeads(int len) {
	if (dn < len || len == 0) {
	    lead1 = point1;
	    lead2 = point2;
	    return;
	}
	lead1 = interpPoint(point1, point2, (dn-len)/(2*dn));
	lead2 = interpPoint(point1, point2, (dn+len)/(2*dn));
    }

    // calculate point fraction f between a and b, linearly interpolated
    Point interpPoint(Point a, Point b, double f) {
	Point p = new Point();
	interpPoint(a, b, p, f);
	return p;
    }
    
    // calculate point fraction f between a and b, linearly interpolated, return it in c
    void interpPoint(Point a, Point b, Point c, double f) {
	c.x = (int) Math.floor(a.x*(1-f)+b.x*f+.48);
	c.y = (int) Math.floor(a.y*(1-f)+b.y*f+.48);
    }
    
    /**
     * Returns a point fraction f along the line between a and b and offset perpendicular by g
     * @param a 1st Point
     * @param b 2nd Point
     * @param f Fraction along line
     * @param g Fraction perpendicular to line
     * Returns interpolated point in c
     */
    void interpPoint(Point a, Point b, Point c, double f, double g) {
	int gx = b.y-a.y;
	int gy = a.x-b.x;
	g /= Math.sqrt(gx*gx+gy*gy);
	c.x = (int) Math.floor(a.x*(1-f)+b.x*f+g*gx+.48);
	c.y = (int) Math.floor(a.y*(1-f)+b.y*f+g*gy+.48);
    }
    
    /**
     * Returns a point fraction f along the line between a and b and offset perpendicular by g
     * @param a 1st Point
     * @param b 2nd Point
     * @param f Fraction along line
     * @param g Fraction perpendicular to line
     * @return Interpolated point
     */
    Point interpPoint(Point a, Point b, double f, double g) {
	Point p = new Point();
	interpPoint(a, b, p, f, g);
	return p;
    }
    
    
    /**
     * Calculates two points fraction f along the line between a and b and offest perpendicular by +/-g
     * @param a 1st point (In)
     * @param b 2nd point (In)
     * @param c 1st point (Out)
     * @param d 2nd point (Out)
     * @param f Fraction along line
     * @param g Fraction perpendicular to line
     */
    void interpPoint2(Point a, Point b, Point c, Point d, double f, double g) {
//	int xpd = b.x-a.x;
//	int ypd = b.y-a.y;
	int gx = b.y-a.y;
	int gy = a.x-b.x;
	g /= Math.sqrt(gx*gx+gy*gy);
	c.x = (int) Math.floor(a.x*(1-f)+b.x*f+g*gx+.48);
	c.y = (int) Math.floor(a.y*(1-f)+b.y*f+g*gy+.48);
	d.x = (int) Math.floor(a.x*(1-f)+b.x*f-g*gx+.48);
	d.y = (int) Math.floor(a.y*(1-f)+b.y*f-g*gy+.48);
    }
    
    void draw2Leads(Graphics g) {
	// draw first lead
	setVoltageColor(g, volts[0]);
	drawThickLine(g, point1, lead1);

	// draw second lead
	setVoltageColor(g, volts[1]);
	drawThickLine(g, lead2, point2);
    }
    Point [] newPointArray(int n) {
	Point a[] = new Point[n];
	while (n > 0)
	    a[--n] = new Point();
	return a;
    }

    // draw current dots from point a to b
    void drawDots(Graphics g, Point pa, Point pb, double pos) {
	 if ((!sim.simIsRunning()) || pos == 0 || !sim.dotsCheckItem.getState())
	    return;
	int dx = pb.x-pa.x;
	int dy = pb.y-pa.y;
	double dn = Math.sqrt(dx*dx+dy*dy);
	if (sim.isPaperTheme())
	    g.setColor(sim.conventionCheckItem.getState() ? new Color(0xB8, 0x86, 0x00) : new Color(0x00, 0x7C, 0x80));
	else
	    g.setColor(sim.conventionCheckItem.getState()?Color.yellow:Color.cyan);
	int ds = 16;
	pos %= ds;
	if (pos < 0)
	    pos += ds;
	double di = 0;
	for (di = pos; di < dn; di += ds) {
	    int x0 = (int) (pa.x+di*dx/dn);
	    int y0 = (int) (pa.y+di*dy/dn);
	    g.fillRect(x0-2, y0-2, 4, 4);
	}
    }

    Polygon calcArrow(Point a, Point b, double al, double aw) {
	Polygon poly = new Polygon();
	Point p1 = new Point();
	Point p2 = new Point();
	int adx = b.x-a.x;
	int ady = b.y-a.y;
	double l = Math.sqrt(adx*adx+ady*ady);
	poly.addPoint(b.x, b.y);
	interpPoint2(a, b, p1, p2, 1-al/l, aw);
	poly.addPoint(p1.x, p1.y);
	poly.addPoint(p2.x, p2.y);
	return poly;
    }
    Polygon createPolygon(Point a, Point b, Point c) {
	Polygon p = new Polygon();
	p.addPoint(a.x, a.y);
	p.addPoint(b.x, b.y);
	p.addPoint(c.x, c.y);
	return p;
    }
    Polygon createPolygon(Point a, Point b, Point c, Point d) {
	Polygon p = new Polygon();
	p.addPoint(a.x, a.y);
	p.addPoint(b.x, b.y);
	p.addPoint(c.x, c.y);
	p.addPoint(d.x, d.y);
	return p;
    }
    Polygon createPolygon(Point a[]) {
	Polygon p = new Polygon();
	int i;
	for (i = 0; i != a.length; i++)
	    p.addPoint(a[i].x, a[i].y);
	return p;
    }
    
    // draw second point to xx, yy
    void drag(int xx, int yy) {
	xx = sim.snapGrid(xx);
	yy = sim.snapGrid(yy);
	if (noDiagonal) {
	    if (Math.abs(x-xx) < Math.abs(y-yy)) {
		xx = x;
	    } else {
		yy = y;
	    }
	}
	x2 = xx; y2 = yy;
	setPoints();
    }
    
    void move(int dx, int dy) {
	x += dx; y += dy; x2 += dx; y2 += dy;
	boundingBox.translate(dx, dy);
	setPoints();
    }

    // called when an element is done being dragged out; returns true if it's zero size and should be deleted
    boolean creationFailed() {
	return (x == x2 && y == y2);
    }

    // this is used to set the position of an internal element so we can draw it inside the parent
    void setPosition(int x_, int y_, int x2_, int y2_) {
	x = x_;
	y = y_;
	x2 = x2_;
	y2 = y2_;
	setPoints();
    }
    
    // determine if moving this element by (dx,dy) will put it on top of another element
    boolean allowMove(int dx, int dy) {
	int nx = x+dx;
	int ny = y+dy;
	int nx2 = x2+dx;
	int ny2 = y2+dy;
	int i;
	for (i = 0; i != sim.elmList.size(); i++) {
	    CircuitElm ce = sim.getElm(i);
	    if (ce.x == nx && ce.y == ny && ce.x2 == nx2 && ce.y2 == ny2)
		return false;
	    if (ce.x == nx2 && ce.y == ny2 && ce.x2 == nx && ce.y2 == ny)
		return false;
	}
	return true;
    }
    
    void movePoint(int n, int dx, int dy) {
    	// modified by IES to prevent the user dragging points to create zero sized nodes
    	// that then render improperly
    	int oldx=x;
    	int oldy=y;
    	int oldx2=x2;
    	int oldy2=y2;
    	if (n == 0) {
    		x += dx; y += dy;
    	} else {
    		x2 += dx; y2 += dy;
    	}
    	if (x==x2 && y==y2) {
    		x=oldx;
    		y=oldy;
    		x2=oldx2;
    		y2=oldy2;
    	}
    	setPoints();
    }
    
    void drawPosts(Graphics g) {
	// we normally do this in updateCircuit() now because the logic is more complicated.
	// we only handle the case where we have to draw all the posts.  That happens when
	// this element is selected or is being created
	if (sim.dragElm == null && !needsHighlight())
	    return;
	if (sim.mouseMode == CirSim.MODE_DRAG_ROW ||
	    sim.mouseMode == CirSim.MODE_DRAG_COLUMN)
	    return;
	int i;
	for (i = 0; i != getPostCount(); i++) {
	    Point p = getPost(i);
	    drawPost(g, p);
	}
    }
    
    void drawHandles(Graphics g, Color c) {
    	g.setColor(c);
    	if (lastHandleGrabbed==-1)
    		g.fillRect(x-3, y-3, 7, 7);
    	else if (lastHandleGrabbed==0)
    		g.fillRect(x-4, y-4, 9, 9);
    	if (getPostCount() > 1 || this instanceof ScopeElm) {
    		if (lastHandleGrabbed==-1)
    			g.fillRect(x2-3, y2-3, 7, 7);
    		else if (lastHandleGrabbed==1)
    			g.fillRect(x2-4, y2-4, 9, 9);
    	}
    }
    
    int getHandleGrabbedClose(int xtest, int ytest, int deltaSq, int minSize) {
    	lastHandleGrabbed=-1;
    	if ( Graphics.distanceSq(x , y , x2, y2)>=minSize) {
    		if (Graphics.distanceSq(x, y, xtest,ytest) <= deltaSq)
    			lastHandleGrabbed=0;
    		else if (Graphics.distanceSq(x2, y2, xtest,ytest) <= deltaSq)
    			lastHandleGrabbed=1;
    	}
    	return lastHandleGrabbed;
    }
    
    // number of voltage sources this element needs 
    int getVoltageSourceCount() { return 0; }
    
    // number of internal nodes (nodes not visible in UI that are needed for implementation)
    int getInternalNodeCount() { return 0; }
    
    // notify this element that its pth node is n.  This value n can be passed to stampMatrix()
    void setNode(int p, int n) { nodes[p] = n; }
    
    // notify this element that its nth voltage source is v.  This value v can be passed to stampVoltageSource(), etc and will be passed back in calls to setCurrent()
    void setVoltageSource(int n, int v) {
	// default implementation only makes sense for subclasses with one voltage source.  If we have 0 this isn't used, if we have >1 this won't work 
	voltSource = v;
    }
    
//    int getVoltageSource() { return voltSource; } // Never used except for debug code which is commented out
    
    double getVoltageDiff() {
	return volts[0] - volts[1];
    }
    boolean nonLinear() { return false; }
    int getPostCount() { return 2; }
    
    // get (global) node number of nth node
    int getNode(int n) { return nodes[n]; }
    
    // get position of nth node
    Point getPost(int n) {
	return (n == 0) ? point1 : (n == 1) ? point2 : null;
    }
    
    int getNodeAtPoint(int xp, int yp) {
	if (getPostCount() == 2)
	    return (x == xp && y == yp) ? 0 : 1;
	int i;
	for (i = 0; i != getPostCount(); i++) {
	    Point p = getPost(i);
	    if (p.x == xp && p.y == yp)
		return i;
	}
	return 0;
    }
    
    /*
    void drawPost(Graphics g, int x0, int y0, int n) {
	if (sim.dragElm == null && !needsHighlight() &&
	    sim.getCircuitNode(n).links.size() == 2)
	    return;
	if (sim.mouseMode == CirSim.MODE_DRAG_ROW ||
	    sim.mouseMode == CirSim.MODE_DRAG_COLUMN)
	    return;
	drawPost(g, x0, y0);
    }
    */
    static void drawPost(Graphics g, Point pt) {
	g.setColor(whiteColor);
	g.fillOval(pt.x-3, pt.y-3, 7, 7);
    }
    
    // set/adjust bounding box used for selecting elements.  getCircuitBounds() does not use this!
    void setBbox(int x1, int y1, int x2, int y2) {
	if (x1 > x2) { int q = x1; x1 = x2; x2 = q; }
	if (y1 > y2) { int q = y1; y1 = y2; y2 = q; }
	boundingBox.setBounds(x1, y1, x2-x1+1, y2-y1+1);
    }
    
    // set bounding box for an element from p1 to p2 with width w
    void setBbox(Point p1, Point p2, double w) {
	setBbox(p1.x, p1.y, p2.x, p2.y);
	int dpx = (int) (dpx1*w);
	int dpy = (int) (dpy1*w);
	adjustBbox(p1.x+dpx, p1.y+dpy, p1.x-dpx, p1.y-dpy);
    }

    // enlarge bbox to contain an additional rectangle
    void adjustBbox(int x1, int y1, int x2, int y2) {
	if (x1 > x2) { int q = x1; x1 = x2; x2 = q; }
	if (y1 > y2) { int q = y1; y1 = y2; y2 = q; }
	x1 = min(boundingBox.x, x1);
	y1 = min(boundingBox.y, y1);
	x2 = max(boundingBox.x+boundingBox.width,  x2);
	y2 = max(boundingBox.y+boundingBox.height, y2);
	boundingBox.setBounds(x1, y1, x2-x1, y2-y1);
    }
    void adjustBbox(Point p1, Point p2) {
	adjustBbox(p1.x, p1.y, p2.x, p2.y);
    }
    
    // needed for calculating circuit bounds (need to special-case centered text elements)
    boolean isCenteredText() { return false; }
    
    void drawCenteredText(Graphics g, String s, int x, int y, boolean cx) {
	// FontMetrics fm = g.getFontMetrics();
	//int w = fm.stringWidth(s);
//    	int w=0;
//	if (cx)
//	    x -= w/2;
//	g.drawString(s, x, y+fm.getAscent()/2);
//	adjustBbox(x, y-fm.getAscent()/2,
//		   x+w, y+fm.getAscent()/2+fm.getDescent());
    	int w=(int)g.context.measureText(s).getWidth();
    	int h2=(int)g.currentFontSize/2;
		g.context.save();
		g.context.setTextBaseline("middle");
		if (cx) {
			g.context.setTextAlign("center");
			adjustBbox(x-w/2,y-h2,x+w/2,y+h2);
		} else {
			adjustBbox(x,y-h2,x+w,y+h2);
		}
		
		if (cx)
			g.context.setTextAlign("center");
		g.drawString(s, x, y);
		g.context.restore();
    }
    
    // draw component values (number of resistor ohms, etc).  hs = offset
    void drawValues(Graphics g, String s, double hs) {
	if (s == null)
	    return;
	g.setFont(unitsFont);
	//FontMetrics fm = g.getFontMetrics();
	int w = (int)g.context.measureText(s).getWidth();
	g.setColor(whiteColor);
	if (tolFactor() != 1)
	    g.setColor(tolDev > 0 ? positiveColor : negativeColor);
	int ya = (int)g.currentFontSize/2;
	int xc, yc;
	if (this instanceof RailElm || this instanceof SweepElm) {
	    xc = x2;
	    yc = y2;
	} else {
	    xc = (x2+x)/2;
	    yc = (y2+y)/2;
	}
	int dpx = (int) (dpx1*hs);
	int dpy = (int) (dpy1*hs);
	if (dpx == 0)
	    g.drawString(s, xc-w/2, yc-abs(dpy)-2);
	else {
	    int xx = xc+abs(dpx)+2;
	     if (this instanceof VoltageElm || (x < x2 && y > y2))
		xx = xc-(w+abs(dpx)+2);
	    g.drawString(s, xx, yc+dpy+ya);
	}
    }
    void drawCoil(Graphics g, int hs, Point p1, Point p2,
		  double v1, double v2) {
	double len = distance(p1, p2);

	g.context.save();
	g.context.setLineWidth(3.0);
	g.context.transform(((double)(p2.x-p1.x))/len, ((double)(p2.y-p1.y))/len,
		-((double)(p2.y-p1.y))/len,((double)(p2.x-p1.x))/len,p1.x,p1.y);
	if (sim.voltsCheckItem.getState() ) {
	    CanvasGradient grad = g.context.createLinearGradient(0,0,len,0);
	    grad.addColorStop(0, getVoltageColor(g,v1).getHexValue());
	    grad.addColorStop(1.0, getVoltageColor(g,v2).getHexValue());
	    g.context.setStrokeStyle(grad);
	}
	g.context.setLineCap(LineCap.ROUND);
	g.context.scale(1, hs > 0 ? 1 : -1);

	int loop;
	// draw more loops for a longer coil
	int loopCt = (int)Math.ceil(len/11);
	for (loop = 0; loop != loopCt; loop++) {
	    g.context.beginPath();
	    double start = len*loop/loopCt;
	    g.context.moveTo(start,0);
	    g.context.arc(len*(loop+.5)/loopCt, 0, len/(2*loopCt), Math.PI, Math.PI*2);
	    g.context.lineTo(len*(loop+1)/loopCt, 0);
	    g.context.stroke();
	}

	g.context.restore();
    }
    
    static void drawThickLine(Graphics g, int x, int y, int x2, int y2) {
    	g.setLineWidth(3.0);
    	g.drawLine(x,y,x2,y2);
    	g.setLineWidth(1.0);
    }

    static void drawThickLine(Graphics g, Point pa, Point pb) {
    	g.setLineWidth(3.0);
    	g.drawLine(pa.x, pa.y, pb.x, pb.y);
    	g.setLineWidth(1.0);
    }

    static void drawThickPolygon(Graphics g, int xs[], int ys[], int c) {
//	int i;
//	for (i = 0; i != c-1; i++)
//	    drawThickLine(g, xs[i], ys[i], xs[i+1], ys[i+1]);
//	drawThickLine(g, xs[i], ys[i], xs[0], ys[0]);
    	g.setLineWidth(3.0);
    	g.drawPolyline(xs, ys, c);
    	g.setLineWidth(1.0);
    }
    
    static void drawThickPolygon(Graphics g, Polygon p) {
	drawThickPolygon(g, p.xpoints, p.ypoints, p.npoints);
    }
    
    static void drawPolygon(Graphics g, Polygon p) {
    	g.drawPolyline(p.xpoints, p.ypoints, p.npoints);
/*	int i;
	int xs[] = p.xpoints;
	int ys[] = p.ypoints;
	int np = p.npoints;
	np -= 3;
	for (i = 0; i != np-1; i++)
	    g.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);
	g.drawLine(xs[i], ys[i], xs[0], ys[0]);*/
    }
    
    static void drawThickCircle(Graphics g, int cx, int cy, int ri) {
    	g.setLineWidth(3.0);
    	g.context.beginPath();
    	g.context.arc(cx, cy, ri*.98, 0, 2*Math.PI);
    	g.context.stroke();
    	g.setLineWidth(1.0);
    }
    
    Polygon getSchmittPolygon(float gsize, float ctr) {
	Point pts[] = newPointArray(6);
	float hs = 3*gsize;
	float h1 = 3*gsize;
	float h2 = h1*2;
	double len = distance(lead1, lead2);
	pts[0] = interpPoint(lead1, lead2, ctr-h2/len, hs);
	pts[1] = interpPoint(lead1, lead2, ctr+h1/len,  hs);
	pts[2] = interpPoint(lead1, lead2, ctr+h1/len, -hs);
	pts[3] = interpPoint(lead1, lead2, ctr+h2/len, -hs);
	pts[4] = interpPoint(lead1, lead2, ctr-h1/len, -hs);
	pts[5] = interpPoint(lead1, lead2, ctr-h1/len, hs);
	return createPolygon(pts); 
    }

    static String getVoltageDText(double v) {
	return getUnitText(Math.abs(v), "V");
    }
    static String getVoltageText(double v) {
	return getUnitText(v, "V");
    }
    static String getTimeText(double v) {
	if (v >= 60) {
	    double h = Math.floor(v/3600);
	    v -= 3600*h;
	    double m = Math.floor(v/60);
	    v -= 60*m;
	    if (h == 0)
		return m + ":" + ((v >= 10) ? "" : "0") + showFormat.format(v);
	    return h + ":" + ((m >= 10) ? "" : "0") + m + ":" + ((v >= 10) ? "" : "0") + showFormat.format(v); 
	}
	return getUnitText(v, "s");
    }
    
    static String format(double v, boolean sf) {
//	if (sf && Math.abs(v) > 10)
//	    return shortFormat.format(Math.round(v));
	return (sf ? shortFormat : showFormat).format(v);
    }
    
    static String getUnitText(double v, String u) {
    	return getUnitText(v,u, false);
    }

    static String getShortUnitText(double v, String u) {
    	return getUnitText(v,u, true);
    }
    
    private static String getUnitText(double v, String u, boolean sf) {
	String sp = sf ? "" : " ";
	double va = Math.abs(v);
	if (va < 1e-14)
	    // this used to return null, but then wires would display "null" with 0V
	    return "0" + sp + u;
	if (va < 1e-9)
	    return format(v*1e12, sf) + sp + "p" + u;
	if (va < 1e-6)
	    return format(v*1e9, sf) + sp + "n" + u;
	if (va < 1e-3)
	    return format(v*1e6, sf) + sp + CirSim.muString + u;
	if (va < 1)
	    return format(v*1e3, sf) + sp + "m" + u;
	if (va < 1e3)
	    return format(v, sf) + sp + u;
	if (va < 1e6)
	    return format(v*1e-3, sf) + sp + "k" + u;
	if (va < 1e9)
	    return format(v*1e-6, sf) + sp + "M" + u;
	return format(v*1e-9, sf) + sp + "G" + u;
    }
    
    static String getCurrentText(double i) {
	return getUnitText(i, "A");
    }
    static String getCurrentDText(double i) {
	return getUnitText(Math.abs(i), "A");
    }

    static String getUnitTextWithScale(double val, String utext, int scale) {
	if (scale == SCALE_1)
	    return showFormat.format(val) + " " + utext;
	if (scale == SCALE_M)
	    return showFormat.format(1e3*val) + " m" + utext;
	if (scale == SCALE_MU)
	    return showFormat.format(1e6*val) + " " + CirSim.muString + utext;
	return getUnitText(val, utext);
    }

    // update dot positions (curcount) for drawing current (simple case for single current)
    void updateDotCount() {
	curcount = updateDotCount(current, curcount);
    }

    // update dot positions (curcount) for drawing current (general case for multiple currents)
    double updateDotCount(double cur, double cc) {
  
	 if (!sim.simIsRunning())
	    return cc;
	double cadd = cur*currentMult;
	/*if (cur != 0 && cadd <= .05 && cadd >= -.05)
	  cadd = (cadd < 0) ? -.05 : .05;*/
	cadd %= 8;
	/*if (cadd > 8)
	  cadd = 8;
	  if (cadd < -8)
	  cadd = -8;*/
	return cc + cadd;
    }
    
    // update and draw current for simple two-terminal element
    void doDots(Graphics g) {
	updateDotCount();
	if (sim.dragElm != this)
	    drawDots(g, point1, point2, curcount);
    }
    
    void doAdjust() {}
    void setupAdjust() {}
    
    // get component info for display in lower right
    void getInfo(String arr[]) {
    }
    
    int getBasicInfo(String arr[]) {
	arr[1] = "I = " + getCurrentDText(getCurrent());
	arr[2] = "Vd = " + getVoltageDText(getVoltageDiff());
	return 3;
    }
    String getScopeText(int v) {
        String info[] = new String[10];
        getInfo(info);
        return info[0];
    }
    
    Color getVoltageColor(Graphics g, double volts) {
    	if (needsHighlight()) {
    	    	return (selectColor);
    	}
    	if (!sim.voltsCheckItem.getState()) {
    	    	return(whiteColor);
    	}
    	int c = (int) ((volts+voltageRange)*(colorScaleCount-1)/
    		       (voltageRange*2));
    	if (c < 0)
    	    c = 0;
    	if (c >= colorScaleCount)
    	    c = colorScaleCount-1;
    	return (colorScale[c]);
    }
    
    void setVoltageColor(Graphics g, double volts) {
    	g.setColor(getVoltageColor(g, volts));
    }
    
    // yellow argument is unused, can't remember why it was there
    void setPowerColor(Graphics g, boolean yellow) {

	/*if (conductanceCheckItem.getState()) {
	  setConductanceColor(g, current/getVoltageDiff());
	  return;
	  }*/
	if (!sim.powerCheckItem.getState() )
	    return;
	setPowerColor(g, getPower());
    }
    
    void setPowerColor(Graphics g, double w0) {
	if (!sim.powerCheckItem.getState() )
	    return;
    	if (needsHighlight()) {
	    	g.setColor(selectColor);
	    	return;
    	}
	w0 *= powerMult;
	//System.out.println(w);
	int i = (int) ((colorScaleCount/2)+(colorScaleCount/2)*-w0);
	if (i<0)
	    i=0;
	if (i>=colorScaleCount)
	    i=colorScaleCount-1;
	 g.setColor(colorScale[i]);
    }
    void setConductanceColor(Graphics g, double w0) {
	w0 *= powerMult;
	//System.out.println(w);
	double w = (w0 < 0) ? -w0 : w0;
	if (w > 1)
	    w = 1;
	int rg = (int) (w*255);
	g.setColor(new Color(rg, rg, rg));
    }
    double getPower() { return getVoltageDiff()*current; }
    double getScopeValue(int x) {
	return (x == Scope.VAL_CURRENT) ? getCurrent() :
	    (x == Scope.VAL_POWER) ? getPower() : getVoltageDiff();
    }
    int getScopeUnits(int x) {
	return (x == Scope.VAL_CURRENT) ? Scope.UNITS_A :
	    (x == Scope.VAL_POWER) ? Scope.UNITS_W : Scope.UNITS_V;
    }
    public EditInfo getEditInfo(int n) { return null; }
    public void setEditValue(int n, EditInfo ei) {}
    
    // get number of nodes that can be retrieved by getConnectionNode()
    int getConnectionNodeCount() { return getPostCount(); }
    
    // get nodes that can be passed to getConnection(), to test if this element connects
    // those two nodes; this is the same as getNode() for all but labeled nodes.
    int getConnectionNode(int n) { return getNode(n); }
    
    // are n1 and n2 connected by this element?  this is used to determine
    // unconnected nodes, and look for loops
    boolean getConnection(int n1, int n2) { return true; }
    
    // is n1 connected to ground somehow?
    boolean hasGroundConnection(int n1) { return false; }
    
    // is this a wire or equivalent to a wire?
    boolean isWire() { return false; }
    
    boolean canViewInScope() { return getPostCount() <= 2; }
    boolean comparePair(int x1, int x2, int y1, int y2) {
	return ((x1 == y1 && x2 == y2) || (x1 == y2 && x2 == y1));
    }
    boolean needsHighlight() { 
	return mouseElmRef==this || selected || sim.plotYElm == this ||
		// Test if the current mouseElm is a ScopeElm and, if so, does it belong to this elm
		(mouseElmRef instanceof ScopeElm && ((ScopeElm) mouseElmRef).elmScope.getElm()==this); 
    }
    boolean isSelected() { return selected; }
    boolean canShowValueInScope(int v) { return false; }
    void setSelected(boolean x) { selected = x; }
    void selectRect(Rectangle r) {
	selected = r.intersects(boundingBox);
    }
    static int abs(int x) { return x < 0 ? -x : x; }
    static int sign(int x) { return (x < 0) ? -1 : (x == 0) ? 0 : 1; }
    static int min(int a, int b) { return (a < b) ? a : b; }
    static int max(int a, int b) { return (a > b) ? a : b; }
    static double distance(Point p1, Point p2) {
	double x = p1.x-p2.x;
	double y = p1.y-p2.y;
	return Math.sqrt(x*x+y*y);
    }
    Rectangle getBoundingBox() { return boundingBox; }
    boolean needsShortcut() { return getShortcut() > 0; }
    int getShortcut() { return 0; }

    boolean isGraphicElmt() { return false; }
    
    void setMouseElm(boolean v) {
	if (v)
	    mouseElmRef=this;
	else if (mouseElmRef==this)
	    mouseElmRef=null;
    }
    void draggingDone() {}
    
    String dumpModel() { return null; }
    
    boolean isMouseElm() {
	return mouseElmRef==this; 
    }
    
    void updateModels() {}
    void stepFinished() {}
    
    double getCurrentIntoNode(int n) {
	// if we take out the getPostCount() == 2 it gives the wrong value for rails
	if (n==0 && getPostCount() == 2)
	    return -current;
	else
	    return current;
    }
    
    // ---- rotate / mirror support (Kjerag Labs addition) ----
    // Most parts are defined by two points plus a flag saying which side of the
    // point1->point2 axis their extra pins go on.  The side is also scaled by
    // dsign, so reversing the axis keeps pins on the same screen side.  After a
    // rotation or mirror we toggle the side flag where needed so the result is a
    // true rotated or mirrored copy.

    // swap which side of the axis the part's extra pins are on.  No-op for symmetric parts.
    void flipSide() {}
    // swap the side of supply pins (op amp with supply pins only)
    void flipSupplySide() {}
    // false for parts placed by a single anchor point (chips); they are moved, not rotated
    boolean canRotate() { return true; }
    // mirror in place using the part's own flags (chips)
    void mirrorFlagX() {}
    void mirrorFlagY() {}

    static int rawDsign(int x, int y, int x2, int y2) {
	return (y2 == y) ? sign(x2-x) : sign(y2-y);
    }

    // rotate 90 degrees clockwise (on screen) around (cx, cy)
    void rotatePoints(int cx, int cy) {
	int d0 = rawDsign(x, y, x2, y2);
	int nx  = cx - (y - cy),  ny  = cy + (x - cx);
	int nx2 = cx - (y2 - cy), ny2 = cy + (x2 - cx);
	x = nx; y = ny; x2 = nx2; y2 = ny2;
	if (rawDsign(x, y, x2, y2) != d0) {
	    flipSide();
	    flipSupplySide();
	}
	setPoints();
    }

    // mirror left/right around the vertical line x = sum2/2
    void mirrorPointsX(int sum2) {
	int d0 = rawDsign(x, y, x2, y2);
	x = sum2 - x; x2 = sum2 - x2;
	if (rawDsign(x, y, x2, y2) == d0)
	    flipSide();
	setPoints();
    }

    // mirror top/bottom around the horizontal line y = sum2/2
    void mirrorPointsY(int sum2) {
	int d0 = rawDsign(x, y, x2, y2);
	y = sum2 - y; y2 = sum2 - y2;
	if (rawDsign(x, y, x2, y2) == d0)
	    flipSide();
	setPoints();
    }

    void flipPosts() {
	int oldx = x;
	int oldy = y;
	x = x2;
	y = y2;
	x2 = oldx;
	y2 = oldy;
	setPoints();
    }
}
