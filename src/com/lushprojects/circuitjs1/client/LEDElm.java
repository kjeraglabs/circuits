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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

    class LEDElm extends DiodeElm {
	double colorR, colorG, colorB, maxBrightnessCurrent;
	static String lastLEDModelName = "default-led";
	
	public LEDElm(int xx, int yy) {
	    super(xx, yy);
	    modelName = lastLEDModelName;
	    setup();
	    maxBrightnessCurrent = .01;
	    colorR = 1; colorG = colorB = 0;
	    ratedCurrent = 0.03;
	    ratedVoltage = 5;
	}
	public LEDElm(int xa, int ya, int xb, int yb, int f,
		      StringTokenizer st) {
	    super(xa, ya, xb, yb, f, st);
	    if ((f & (FLAG_MODEL|FLAG_FWDROP)) == 0) {
		final double fwdrop = 2.1024259;
	        model = DiodeModel.getModelWithParameters(fwdrop, 0);
	        modelName = model.name;
	        CirSim.console("model name wparams = " + modelName);
		setup();
	    }
	    colorR = new Double(st.nextToken()).doubleValue();
	    colorG = new Double(st.nextToken()).doubleValue();
	    colorB = new Double(st.nextToken()).doubleValue();
	    maxBrightnessCurrent = .01;
	    try {
		maxBrightnessCurrent = new Double(st.nextToken()).doubleValue();
	    } catch (Exception e) { }
	}
	int getDumpType() { return 162; }
	String dump() {
	    return super.dump() + " " + colorR + " " + colorG + " " + colorB + " " +
		    maxBrightnessCurrent;
	}

	// LED drawn as a diode (triangle + bar) with two emission arrows (Kjerag Labs).
	// The triangle and bar take the LED's colour, brighter with more current;
	// the arrows light up in the same colour.
	Point arrowFrom[], arrowTo[];
	Polygon arrowHead[];

	void setPoints() {
	    super.setPoints();
	    arrowFrom = new Point[2];
	    arrowTo = new Point[2];
	    arrowHead = new Polygon[2];
	    int i;
	    for (i = 0; i != 2; i++) {
		double f = -.05 + .5*i;
		arrowFrom[i] = interpPoint(lead1, lead2, f, hs+3);
		arrowTo[i]   = interpPoint(lead1, lead2, f+.55, hs+14);
		arrowHead[i] = calcArrow(arrowFrom[i], arrowTo[i], 6, 3.5);
	    }
	}

	// 0 (dark) .. 1 (full brightness)
	double getBrightness() {
	    double w = current/maxBrightnessCurrent;
	    if (w <= 0)
		return 0;
	    w = 1+.2*Math.log(w);
	    return Math.max(0, Math.min(1, w));
	}

	Color getLedColor() {
	    return new Color((int) Math.round(colorR*255), (int) Math.round(colorG*255),
			     (int) Math.round(colorB*255));
	}

	void draw(Graphics g) {
	    setBbox(point1, point2, hs);
	    adjustBbox(arrowTo[0], arrowTo[1]);
	    draw2Leads(g);

	    double b = getBrightness();
	    Color lit = getLedColor();
	    // unlit LEDs keep a faint tint of their colour, like the plastic body
	    Color fill = new Color(sim.getBackgroundColor(), lit, .25 + .75*b);
	    String ink = (needsHighlight() ? selectColor : lightGrayColor).getHexValue();

	    // triangle
	    g.setColor(fill);
	    g.fillPolygon(poly);
	    g.context.save();
	    g.context.setStrokeStyle(ink);
	    g.context.setLineWidth(1.5);
	    g.context.setLineJoin("round");
	    g.context.beginPath();
	    g.context.moveTo(poly.xpoints[0], poly.ypoints[0]);
	    g.context.lineTo(poly.xpoints[1], poly.ypoints[1]);
	    g.context.lineTo(poly.xpoints[2], poly.ypoints[2]);
	    g.context.closePath();
	    g.context.stroke();

	    // bar: outline, then fill colour
	    g.context.beginPath();
	    g.context.moveTo(cathode[0].x, cathode[0].y);
	    g.context.lineTo(cathode[1].x, cathode[1].y);
	    g.context.setLineWidth(6);
	    g.context.stroke();
	    g.context.setStrokeStyle(fill.getHexValue());
	    g.context.setLineWidth(3);
	    g.context.stroke();

	    // emission arrows
	    String arrowColor = new Color(lightGrayColor, lit, b).getHexValue();
	    g.context.setStrokeStyle(arrowColor);
	    g.context.setFillStyle(arrowColor);
	    g.context.setLineWidth(1.5);
	    int i;
	    for (i = 0; i != 2; i++) {
		Point tip = interpPoint(arrowFrom[i], arrowTo[i], 1 - 5/distance(arrowFrom[i], arrowTo[i]));
		g.context.beginPath();
		g.context.moveTo(arrowFrom[i].x, arrowFrom[i].y);
		g.context.lineTo(tip.x, tip.y);
		g.context.stroke();
		Polygon h = arrowHead[i];
		g.context.beginPath();
		g.context.moveTo(h.xpoints[0], h.ypoints[0]);
		g.context.lineTo(h.xpoints[1], h.ypoints[1]);
		g.context.lineTo(h.xpoints[2], h.ypoints[2]);
		g.context.closePath();
		g.context.fill();
	    }
	    g.context.restore();

	    doDots(g);
	    drawPosts(g);
	}

	void getInfo(String arr[]) {
	    super.getInfo(arr);
	    if (model.oldStyle)
		arr[0] = "LED";
	    else
		arr[0] = sim.LS("LED") + " (" + modelName + ")";
	}

	// colour presets shown as swatches in the edit dialog
	static final String PRESET_COLORS[] = {
	    "#FF0000", "#FF7A00", "#FFD400", "#20E040", "#00B0FF", "#2050FF", "#8A2BE2", "#FFFFFF" };
	static final String PRESET_NAMES[] = {
	    "Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Violet / UV", "White" };

	TextBox colorBox;

	String getColorHex() {
	    return getLedColor().getHexValue().toUpperCase();
	}

	// accepts #RRGGBB, RRGGBB, #RGB or RGB; returns false if it can't be read
	boolean setColorHex(String s) {
	    s = s.trim();
	    if (s.startsWith("#"))
		s = s.substring(1);
	    if (s.length() == 3)
		s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
	    if (!s.matches("[0-9A-Fa-f]{6}"))
		return false;
	    colorR = Integer.parseInt(s.substring(0, 2), 16)/255.;
	    colorG = Integer.parseInt(s.substring(2, 4), 16)/255.;
	    colorB = Integer.parseInt(s.substring(4, 6), 16)/255.;
	    return true;
	}

	// copy the colour picker's value into the hex box while the picker is open
	static native void linkPicker(Element picker, Element hexBox) /*-{
	    picker.addEventListener('input', function() { hexBox.value = picker.value.toUpperCase(); });
	}-*/;

	Widget makeColorEditor() {
	    VerticalPanel vp = new VerticalPanel();
	    HorizontalPanel row = new HorizontalPanel();
	    row.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
	    colorBox = new TextBox();
	    colorBox.setText(getColorHex());
	    colorBox.setVisibleLength(8);
	    colorBox.setMaxLength(7);
	    row.add(colorBox);

	    // the browser's own colour picker, opened by clicking the colour square
	    final TextBox picker = new TextBox();
	    picker.getElement().setAttribute("type", "color");
	    picker.getElement().setAttribute("title", "Pick a colour");
	    picker.setValue(getColorHex().toLowerCase());
	    picker.getElement().getStyle().setProperty("width", "36px");
	    picker.getElement().getStyle().setProperty("height", "26px");
	    picker.getElement().getStyle().setProperty("padding", "0 2px");
	    picker.getElement().getStyle().setProperty("marginLeft", "6px");
	    picker.getElement().getStyle().setProperty("cursor", "pointer");
	    linkPicker(picker.getElement(), colorBox.getElement());
	    row.add(picker);

	    // typing a valid hex code updates the picker
	    colorBox.addKeyUpHandler(new KeyUpHandler() {
		public void onKeyUp(KeyUpEvent e) {
		    String t = colorBox.getText().trim();
		    if (t.matches("#?[0-9A-Fa-f]{6}"))
			picker.setValue("#" + t.replace("#", "").toLowerCase());
		}
	    });
	    vp.add(row);

	    HorizontalPanel swatches = new HorizontalPanel();
	    swatches.getElement().getStyle().setProperty("marginTop", "4px");
	    int i;
	    for (i = 0; i != PRESET_COLORS.length; i++) {
		final String hex = PRESET_COLORS[i];
		Label sw = new Label();
		sw.setTitle(PRESET_NAMES[i] + " " + hex);
		sw.getElement().getStyle().setProperty("width", "18px");
		sw.getElement().getStyle().setProperty("height", "18px");
		sw.getElement().getStyle().setProperty("marginRight", "4px");
		sw.getElement().getStyle().setProperty("background", hex);
		sw.getElement().getStyle().setProperty("border", "1px solid #888");
		sw.getElement().getStyle().setProperty("borderRadius", "3px");
		sw.getElement().getStyle().setProperty("cursor", "pointer");
		sw.addClickHandler(new ClickHandler() {
		    public void onClick(ClickEvent e) {
			colorBox.setText(hex);
			picker.setValue(hex.toLowerCase());
		    }
		});
		swatches.add(sw);
	    }
	    vp.add(swatches);
	    return vp;
	}

	public EditInfo getEditInfo(int n) {
	    if (n == 0) {
		EditInfo ei = new EditInfo("Color (hex)", 0);
		ei.widget = makeColorEditor();
		return ei;
	    }
	    if (n == 1)
		return new EditInfo("Max Brightness Current (A)", maxBrightnessCurrent, 0, .1);
	    return super.getEditInfo(n-2);
	}
	public void setEditValue(int n, EditInfo ei) {
	    if (n == 0) {
		if (colorBox != null)
		    setColorHex(colorBox.getText());
		return;
	    }
	    if (n == 1) {
		maxBrightnessCurrent = ei.value;
		return;
	    }
	    super.setEditValue(n-2, ei);
	}
	int getShortcut() { return 'D'; }
	
	void setLastModelName(String n) {
	    lastLEDModelName = n;
	}
    }
