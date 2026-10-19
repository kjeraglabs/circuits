package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;

// values with sliders
public class Adjustable implements Command {
    CircuitElm elm;
    double minValue, maxValue;
    String sliderText;
    
    // index of value in getEditInfo() list that this slider controls
    int editItem;
    
    Label label;
    Scrollbar slider;
    boolean settingValue;

    // Kjerag Labs: a part can follow another part's slider instead of having
    // its own. sharedSlider is that slider's Adjustable (null = own slider).
    // Saved as two extra tokens on the 38 line: the slider's element index and edit item.
    Adjustable sharedSlider;
    int sharedElm = -1, sharedItem;

    Adjustable(CircuitElm ce, int item, Adjustable master) {
	this(ce, item);
	sharedSlider = master;
	minValue = master.minValue;
	maxValue = master.maxValue;
	sliderText = master.sliderText;
    }
    
    Adjustable(CircuitElm ce, int item) {
	minValue = 1;
	maxValue = 1000;
	elm = ce;
	editItem = item;
    }

    // undump
    Adjustable(StringTokenizer st, CirSim sim) {
	int e = new Integer(st.nextToken()).intValue();
	if (e == -1)
	    return;
	elm = sim.getElm(e);
	editItem = new Integer(st.nextToken()).intValue();
	minValue = new Double(st.nextToken()).doubleValue();
	maxValue = new Double(st.nextToken()).doubleValue();
	sliderText = CustomLogicModel.unescape(st.nextToken());
	if (st.hasMoreTokens()) {
	    sharedElm = new Integer(st.nextToken()).intValue();
	    sharedItem = new Integer(st.nextToken()).intValue();
	}
    }
    
    void createSlider(CirSim sim) {
	if (sharedElm >= 0) {
	    // follow the slider saved with this one, if it's there
	    Adjustable m = sim.findAdjustable(sim.getElm(sharedElm), sharedItem);
	    sharedElm = -1;
	    if (m != null && m != this) {
		while (m.sharedSlider != null)
		    m = m.sharedSlider;
		sharedSlider = m;
		return;
	    }
	}
	double value = elm.getEditInfo(editItem).value;
	createSlider(sim, value);
    }

    void createSlider(CirSim sim, double value) {
        sim.addWidgetToVerticalPanel(label = new Label(sim.LS(sliderText)));
        label.addStyleName("topSpace");
        int intValue = (int) ((value-minValue)*100/(maxValue-minValue));
        sim.addWidgetToVerticalPanel(slider = new Scrollbar(Scrollbar.HORIZONTAL, intValue, 1, 0, 101, this, elm));
    }

    void setSliderValue(double value) {
	if (slider == null)
	    return;
        int intValue = (int) ((value-minValue)*100/(maxValue-minValue));
        settingValue = true; // don't recursively set value again in execute()
        slider.setValue(intValue);
        settingValue = false;
    }
    
    public void execute() {
	elm.sim.analyzeFlag = true;
	if (settingValue)
	    return;
	double v = getSliderValue();
	setValue(elm, editItem, v);
	// parts following this slider
	for (Adjustable a : elm.sim.adjustables)
	    if (a.sharedSlider == this)
		setValue(a.elm, a.editItem, v);
	elm.sim.repaint();
    }

    static void setValue(CircuitElm e, int item, double v) {
	EditInfo ei = e.getEditInfo(item);
	ei.value = v;
	e.setEditValue(item, ei);
    }
    
    double getSliderValue() {
	return minValue + (maxValue-minValue)*slider.getValue()/100;
    }
    
    void deleteSlider(CirSim sim) {
	if (slider == null)
	    return;
        sim.removeWidgetFromVerticalPanel(label);
        sim.removeWidgetFromVerticalPanel(slider);
    }
    
    String dump() {
	Adjustable m = (sharedSlider != null) ? sharedSlider : this;
	String s = elm.sim.locateElm(elm) + " " + editItem + " " + m.minValue + " " + m.maxValue + " " + CustomLogicModel.escape(m.sliderText);
	if (sharedSlider != null)
	    s += " " + elm.sim.locateElm(sharedSlider.elm) + " " + sharedSlider.editItem;
	return s;
    }
}
