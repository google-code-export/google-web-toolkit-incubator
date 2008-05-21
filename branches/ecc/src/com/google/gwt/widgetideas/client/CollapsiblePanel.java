/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.widgetideas.client;

import com.google.gwt.libideas.logging.shared.Log;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.client.overrides.WidgetIterators;

import java.util.Iterator;

/**
 * {@link CollapsiblePanel} makes its contained contents collapsible. By
 * default, the contents are fully expanded. When collapsed, the contents of the
 * panel will be displayed only when the user mouse hovers over the hover bar,
 * otherwise is will stay collapsed to the left. A change event is fired
 * whenever the {@link CollapsablePanel} switched between its expanded and
 * collapsed states.
 * <p>
 * The default style name is gwt-CollapsiblePanel.
 * <p>
 * Planned enhancements: Allow panel to be collapsed in arbitrary direction.
 * 
 */
public class CollapsiblePanel extends Composite implements SourcesChangeEvents,
    HasWidgets {

  /**
   * {@link CollapsiblePanel} styles.
   * 
   */
  public static class Styles {
    static String DEFAULT = "gwt-CollapsiblePanel";
    static String CONTAINER = "container";
    static String HOVER_BAR = "hover-bar";
  }

  /**
   * Delays showing of the {@link CollapsiblePanel}.
   */
  private class DelayHide extends Timer {

    public void activate() {
      setState(State.WILL_HIDE);
      delayedHide.schedule(DELAY_MILLI);
    }

    public void run() {
      hide();
    }
  }

  /**
   * Delays showing of the {@link CollapsiblePanel}.
   */
  private class DelayShow extends Timer {

    public void activate() {
      setState(State.WILL_SHOW);
      delayedShow.schedule(DELAY_MILLI);
    }

    public void run() {
      show();
    }
  }

  /**
   * Hides the {@link CollapsiblePanel}.
   */
  private class HidingTimer extends SlidingTimer {
    protected void finish() {
      setState(State.IS_HIDDEN);
    }

    protected boolean processSizeChange(float shouldBe) {
      currentOffshift = (int) (maxOffshift * ((float) 1.0 - shouldBe))
          - MIN_SLIDE_STEP;
      currentOffshift = Math.max(currentOffshift, 0);
      setPanelPos(currentOffshift);
      return currentOffshift > 0;
    }
  }

  /**
   * Shows the hovering {@link CollapsiblePanel}.
   */
  private class ShowingTimer extends SlidingTimer {
    protected void finish() {
      setState(State.IS_SHOWN);
    }

    protected boolean processSizeChange(float shouldBe) {
      currentOffshift = (int) (maxOffshift * shouldBe) + MIN_SLIDE_STEP;
      currentOffshift = Math.min(currentOffshift, maxOffshift);
      setPanelPos(currentOffshift);
      return currentOffshift < maxOffshift;
    }
  }

  /**
   * Common code for {@link HidingTimer} and {@link ShowingTimer}.
   */
  private abstract class SlidingTimer extends Timer {

    long started;

    public void run() {
      float hasTaken = System.currentTimeMillis() - started;
      float shouldBe = hasTaken / TIME_TO_SLIDE;

      if (processSizeChange(shouldBe)) {
        this.schedule(OVERLAY_SPEED);
      } else {
        finish();
      }
    }

    protected abstract void finish();

    protected abstract boolean processSizeChange(float shouldBe);
  }

  /**
   * Current {@link CollapsiblePanel} state.
   */
  private enum State {
    WILL_HIDE, HIDING, IS_HIDDEN, WILL_SHOW, SHOWING, IS_SHOWN, EXPANDED;
  }

  /**
   * Pause between each interval.
   */
  private static int OVERLAY_SPEED = 2;

  /**
   * Number of intervals used to display panel.
   */
  private static float TIME_TO_SLIDE = 200;

  /**
   * Minimum increment change per slide.
   */
  private static int MIN_SLIDE_STEP = 20;

  /**
   * How many milliseconds to delay a hover event before executing it.
   */
  private static final int DELAY_MILLI = 400;

  private State state;

  private ShowingTimer overlayTimer = new ShowingTimer();

  private HidingTimer hidingTimer = new HidingTimer();

  private DelayShow delayedShow = new DelayShow();

  private DelayHide delayedHide = new DelayHide();

  private int width;

  private int maxOffshift;

  private int currentOffshift;
  private Panel container;
  private SimplePanel hoverBar;
  private ToggleButton collapseToggle;
  private AbsolutePanel master;
  private ChangeListenerCollection changeListeners = new ChangeListenerCollection();
  private Widget contents;

  /**
   * Constructor.
   */

  public CollapsiblePanel() {

    // Create the composite widget.
    master = new AbsolutePanel() {
      {
        sinkEvents(Event.ONMOUSEOUT | Event.ONMOUSEOVER);
      }

      public void onBrowserEvent(Event event) {
        // Cannot handle browser events until contents are initialized.
        if (contents == null) {
          return;
        }
        if (!CollapsiblePanel.this.collapseToggle.isDown()) {
          switch (DOM.eventGetType(event)) {

            case Event.ONMOUSEOUT:
              Element to = DOM.eventGetToElement(event);
              if (to != null && DOM.isOrHasChild(master.getElement(), to)) {
                break;
              }

              if (getState() == State.WILL_SHOW) {
                setState(State.IS_HIDDEN);
                delayedShow.cancel();
                break;
              }
              if (shouldHide()) {
                delayedHide.activate();
              }
              break;
            case Event.ONMOUSEOVER:
              if ((shouldShow(DOM.eventGetToElement(event)))) {
                if (state == State.WILL_HIDE) {
                  delayedHide.cancel();
                  setState(State.IS_SHOWN);
                } else {
                  delayedShow.activate();
                }
              }
              break;
          }
        }

        super.onBrowserEvent(event);
      }
    };

    DOM.setStyleAttribute(master.getElement(), "overflow", "visible");
    initWidget(master);
    setStyleName(Styles.DEFAULT);

    // Create hovering container.
    hoverBar = new SimplePanel();
    hoverBar.setStyleName(Styles.HOVER_BAR);
    master.add(hoverBar, 0, 0);

    // Create the contents container.
    container = new SimplePanel();
    container.setStyleName(Styles.CONTAINER);
    master.add(container, 0, 0);
    setState(State.EXPANDED);
  }

  /**
   * 
   */
  public CollapsiblePanel(Widget widget) {
    this();
    initContents(widget);
  }

  public void add(Widget w) {
    initContents(w);
  }

  public void addChangeListener(ChangeListener listener) {
    changeListeners.add(listener);
  }

  public void clear() {
    throw new IllegalStateException(
        "Collapsible Panel cannot be cleared once initialized");
  }

  public Widget getContents() {
    return contents;
  }

  /**
   * Uses the given toggle button to control whether the panel is collapsed or
   * not.
   * 
   */
  public void hookupControlToggle(ToggleButton button) {
    this.collapseToggle = button;
    collapseToggle.setDown(!this.isCollapsed());
    collapseToggle.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        setCollapsed(!CollapsiblePanel.this.collapseToggle.isDown());
      }
    });
  }

  /**
   * Initialize the panel's contents.
   * 
   * @param contents contents
   */
  public void initContents(Widget contents) {
    if (this.contents != null) {
      throw new IllegalStateException("Contents have already be set");
    }

    this.contents = contents;
    container.add(contents);

    if (isAttached()) {
      refreshWidth();
    }
  }

  /**
   * Is the panel currently in its collapsed state.
   */
  public boolean isCollapsed() {
    return getState() != State.EXPANDED;
  }

  public Iterator<Widget> iterator() {
    return WidgetIterators.createWidgetIterator(this, new Widget[] {contents});
  }

  public boolean remove(Widget w) {
    return false;
  }

  public void removeChangeListener(ChangeListener listener) {
    changeListeners.remove(listener);
  }

  /**
   * Sets the contents of the hover bar.
   */
  public void setHoverBarContents(Widget bar) {
    hoverBar.setWidget(bar);
  }

  @Override
  public void setWidth(String width) {
    if (contents == null) {
      throw new IllegalStateException(
          "Cannot set the width of the collapsible panel before its contents are initialized");
    }
    contents.setWidth(width);
    refreshWidth();
  }

  /**
   * Display this panel in its collapsed state.
   */
  protected void becomeCollapsed() {
    int hoverBarWidth = hoverBar.getOffsetWidth();
    int aboutHalf = (hoverBarWidth / 2) + 1;
    int newWidth = width + aboutHalf;
    maxOffshift = newWidth;
    currentOffshift = width;
    master.setWidth(hoverBarWidth + "px");

    // Width is now hoverBarWidth.
    setPanelPos(maxOffshift);
    setState(State.IS_SHOWN);
  }

  /**
   * Display this panel in its expanded state.
   */
  protected void becomeExpanded() {
    // The master width needs to be readjusted back to it's original size.
    if (isAttached()) {
      refreshWidth();
    }
    DOM.setStyleAttribute(container.getElement(), "left", "0px");
    setState(State.EXPANDED);
  }

  protected void hide() {
    setState(State.HIDING);
    overlayTimer.cancel();
    hidingTimer.cancel();
    hidingTimer.started = System.currentTimeMillis();
    hidingTimer.run();
  }

  /**
   * This method is called immediately after a widget becomes attached to the
   * browser's document.
   */
  protected void onLoad() {
    if (contents != null) {
      refreshWidth();
    }
  }

  protected void setPanelPos(int pos) {
    DOM.setStyleAttribute(container.getElement(), "left", pos - width + "px");
  }

  protected void show() {
    setState(State.SHOWING);

    overlayTimer.cancel();
    hidingTimer.cancel();
    overlayTimer.started = System.currentTimeMillis();
    overlayTimer.run();
  }

  State getState() {
    return state;
  }

  void setState(State state) {
    if ((this.state == State.IS_HIDDEN && state == State.WILL_HIDE)
        || ((this.state != State.WILL_HIDE && state == State.HIDING))
        || ((this.state != State.WILL_SHOW && state == State.SHOWING))) {
      Log.severe("state: " + this.state + "->" + state + " is illegal");
      throw new IllegalStateException();
    } else {
      Log.fine("state: " + this.state + "->" + state);
    }
    this.state = state;
  }

  private void refreshWidth() {

    // Now include borders into master.
    width = container.getOffsetWidth();
    if (width == 0) {
      throw new IllegalStateException(
          "The underlying content width cannot be 0. Please ensure that the .container css style has a fixed width");
    }
    master.setWidth(width + "px");
  }

  private void setCollapsed(boolean collapsed) {
    if (isCollapsed() == collapsed) {
      return;
    }
    if (collapseToggle != null) {
      collapseToggle.setDown(!collapsed);
    }
    if (collapsed) {
      becomeCollapsed();
    } else {
      becomeExpanded();
    }
    changeListeners.fireChange(this);
  }

  private boolean shouldHide() {
    return state == State.WILL_SHOW || state == State.SHOWING
        || state == State.IS_SHOWN;
  }

  private boolean shouldShow(Element e) {
    if (state == State.IS_HIDDEN || state == State.WILL_HIDE) {
      return true;
    } else if (state == State.HIDING) {
      return (e != null && e.equals(hoverBar.getElement()));
    } else {
      return false;
    }
  }
}