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

// Drawing for the one-pin I/O parts (digital input/output, analog output).
// The part is a lead from its pin (point1) to a tag shape ending at point2.
// Inputs point away from the pin (signal goes in), outputs point at the pin.
class IoTag {
    static final int LEN = 28;     // tag length
    static final int HALF = 9;     // tag half height
    static final Color GREEN = new Color(0x2f, 0xbf, 0x4f);
    static final Color AMBER = new Color(0xe8, 0xa8, 0x20);

    // where the lead meets the tag
    static Point leadEnd(CircuitElm e) {
	double len = Math.min(LEN, e.dn);
	return e.interpPoint(e.point1, e.point2, 1 - len/e.dn);
    }

    static Point center(CircuitElm e) {
	double len = Math.min(LEN, e.dn);
	return e.interpPoint(e.point1, e.point2, 1 - len/(2*e.dn));
    }

    static void draw(Graphics g, CircuitElm e, boolean output, String text, Color fill) {
	Point l = leadEnd(e);
	double len = Math.min(LEN, e.dn);
	double f0 = 1 - len/e.dn;               // tag start (pin side)
	double fs = 1 - (output ? len - 8 : 8)/e.dn;   // shoulder of the point
	Point a1 = new Point(), a2 = new Point(), b1 = new Point(), b2 = new Point();
	Polygon p = new Polygon();
	if (output) {
	    // point at the pin side
	    e.interpPoint2(e.point1, e.point2, a1, a2, fs, HALF);
	    e.interpPoint2(e.point1, e.point2, b1, b2, 1, HALF);
	    p.addPoint(l.x, l.y);
	    p.addPoint(a1.x, a1.y); p.addPoint(b1.x, b1.y);
	    p.addPoint(b2.x, b2.y); p.addPoint(a2.x, a2.y);
	} else {
	    // flat at the pin side, point at the far end
	    e.interpPoint2(e.point1, e.point2, a1, a2, f0, HALF);
	    e.interpPoint2(e.point1, e.point2, b1, b2, fs, HALF);
	    p.addPoint(a1.x, a1.y); p.addPoint(b1.x, b1.y);
	    p.addPoint(e.point2.x, e.point2.y);
	    p.addPoint(b2.x, b2.y); p.addPoint(a2.x, a2.y);
	}
	e.setBbox(e.point1, e.point2, HALF + 2);

	// lead
	e.setVoltageColor(g, e.volts[0]);
	CircuitElm.drawThickLine(g, e.point1, l);

	// tag
	g.setColor(fill);
	g.fillPolygon(p);
	g.context.save();
	g.context.setStrokeStyle((e.needsHighlight() ? CircuitElm.selectColor : CircuitElm.lightGrayColor).getHexValue());
	g.context.setLineWidth(1.5);
	g.context.setLineJoin("round");
	g.context.beginPath();
	int i;
	for (i = 0; i != p.npoints; i++) {
	    if (i == 0)
		g.context.moveTo(p.xpoints[i], p.ypoints[i]);
	    else
		g.context.lineTo(p.xpoints[i], p.ypoints[i]);
	}
	g.context.closePath();
	g.context.stroke();
	g.context.restore();

	// state text in the middle of the tag
	Point c = center(e);
	g.setColor(CircuitElm.whiteColor);
	Font old = g.getFont();
	g.setFont(new Font("SansSerif", Font.BOLD, 12));
	e.drawCenteredText(g, text, c.x, c.y, true);
	g.setFont(old);
    }

    // text just past the end of the tag (away from the pin), e.g. a voltage
    static void drawLabel(Graphics g, CircuitElm e, String s) {
	Point t = e.interpPoint(e.point1, e.point2, 1 + 6/e.dn);
	Font old = g.getFont();
	g.setFont(new Font("SansSerif", Font.BOLD, 12));
	g.setColor(CircuitElm.whiteColor);
	int w = (int) g.context.measureText(s).getWidth();
	int tx = t.x, ty = t.y;
	if (e.dx > 0) tx += 2;
	else if (e.dx < 0) tx -= w + 2;
	else { tx -= w/2; ty += (e.dy > 0 ? 12 : -4); }
	g.drawString(s, tx, ty + (e.dx != 0 ? 4 : 0));
	g.setFont(old);
	e.adjustBbox(new Point(tx, ty - 12), new Point(tx + w, ty + 4));
    }

    // fill colour: background tinted towards col by amount 0..1
    static Color tint(Color col, double amount) {
	return new Color(CircuitElm.sim.getBackgroundColor(), col, amount);
    }
}
