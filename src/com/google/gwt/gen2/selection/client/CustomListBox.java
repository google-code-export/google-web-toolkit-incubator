/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.gen2.selection.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.gen2.commonwidget.client.Decorator;
import com.google.gwt.gen2.commonwidget.impl.StandardCssImpl;
import com.google.gwt.gen2.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.gen2.event.logical.shared.SelectionEvent;
import com.google.gwt.gen2.event.logical.shared.SelectionHandler;
import com.google.gwt.gen2.event.shared.HandlerRegistration;
import com.google.gwt.gen2.widgetbase.client.Gen2Composite;
import com.google.gwt.gen2.widgetbase.client.WidgetCss;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

/**
 * A custom list box. Used as a base class for {@link DropDownListBox} and
 * (eventually) a multi-select list box. The abstract custom list box is a
 * composite to allow inherited classes to specialize the look and feel of the
 * widget.
 * 
 * <br/>
 * 
 * @param <ValueType> the type of the values backing this list box
 */
public abstract class CustomListBox<ValueType> extends Gen2Composite<Widget>
    implements HasSelectionHandlers<ValueType> {

  /**
   * Interface used to allow the widget access to css style names. <p/> The
   * class names indicate the default gwt names for these styles. <br>
   * 
   */
  public static interface Css extends WidgetCss {
    String baseName();

    /**
     * Disabled item.
     */
    String disabledItem();

    /**
     * Highlighted item.
     */
    String highlightedItem();

    /**
     * Inner div of a list separator.
     */
    String innerSeparator();

    /**
     * List item.
     */
    String item();

    /**
     * List box wrapper.
     */
    String listBox();

    /**
     * Outer div of a list separator.
     */
    String outerSeparator();

    /**
     * Selected and disabled item.
     */
    String selectedAndDisabledItem();

    /**
     * Selected and highlighted item.
     */
    String selectedAndHighlightedItem();

    /**
     * Selected item.
     */
    String selectedItem();
  }

  private class ItemList extends CellGridImpl<ValueType, ItemList.Item> {
    private class Item extends CellGridImpl<ValueType, ItemList.Item>.Cell {
      private String summary;

      public Item(Element element, ValueType value, String summary) {
        super(element, value);

        this.summary = summary;
        updateStyle();
      }

      public String getSummary() {
        return summary;
      }

      @Override
      public void onEnabled(boolean enabled) {
      }

      @Override
      public void onHighlight(boolean highlight) {
        updateStyle();
      }

      @Override
      public void onSelected(boolean selected) {
        updateStyle();
      }

      private void updateStyle() {
        String style = css.item() + " ";
        if (isHighlighted() && isSelected()) {
          style += css.selectedAndHighlightedItem();
        } else if (isHighlighted()) {
          style += css.highlightedItem();
        } else if (isSelected()) {
          style += css.selectedItem();
        }
        setStyleName(style);
      }
    }

    public ItemList() {
      resizeColumns(1);
    }

    public void addItem(String html, ValueType value, String tooltip,
        String summary) {
      assert (html != null);
      assert (value != null);
      Element e = itemList.addHtml(supplyItemDecorator().wrapHTML(html));
      e.setTitle(tooltip);
      new Item(e, value, summary);
    }

    public void addSeparator() {
      itemList.addHtml("<div class='" + css.innerSeparator()
          + "'> <div class='" + css.outerSeparator() + "'/></div>");
    }

    @Override
    protected void onSelected(Item oldItem, Item newItem) {
      currentSummary = defaultSummary;
      if (newItem != null && newItem.summary != null) {
        currentSummary = newItem.summary;
      }
      fireEvent(new SelectionEvent(getValue(oldItem), getValue(newItem)));
    }

    private Element addHtml(String html) {
      int nextRow = this.numRows;
      resizeRows(nextRow + 1);
      setHTML(nextRow, 0, html);
      return getCellFormatter().getElement(nextRow, 0);
    }
  }

  /**
   * Creates the standard css instance used for this widget.
   */
  private static class StandardCss extends StandardCssImpl<CustomListBox>
      implements CustomListBox.Css {

    /**
     * Constructor.
     */
    public StandardCss(String baseStyleName) {
      super(baseStyleName);
    }

    public String disabledItem() {
      return wrap("disabledItem");
    }

    public String highlightedItem() {
      return wrap("highlightedItem");
    }

    public String innerSeparator() {
      return wrap("innerSeparator");
    }

    public String item() {
      return wrap("item");
    }

    public String listBox() {
      return wrap("listBox");
    }

    public String outerSeparator() {
      return wrap("outerSeparator");
    }

    public String selectedAndDisabledItem() {
      return wrap("selectedAndDisabledItem");
    }

    public String selectedAndHighlightedItem() {
      return wrap("selectedAndHighlightedItem");
    }

    public String selectedItem() {
      return wrap("selectedItem");
    }
  }

  /**
   * Creates a {@link Css} instance with the given base name.
   * 
   * @param baseName base style name.
   */
  public static Css createCss(String baseName) {
    return new StandardCss(baseName);
  }

  private Css css;

  // The fact that we are backed by an item grid is not part of the public API.
  private ItemList itemList = new ItemList();

  /**
   * default summary for this list box.
   */
  private String defaultSummary;

  /**
   * Current summary of this list box.
   */
  private String currentSummary;

  /**
   * Creates a custom list box with the given css.
   */
  public CustomListBox(Css css) {
    this(css, "");
  }

  /**
   * Creates a custom list box with the given css and default summary describing
   * the list.
   */
  public CustomListBox(Css css, String defaultSummary) {
    setCss(css);
    this.defaultSummary = defaultSummary;
    this.currentSummary = defaultSummary;
  }

  /**
   * Adds an item to the custom list box.
   * 
   * @param html the html of the item to be displayed
   * @param value the item's value.
   */
  public final void addItem(String html, ValueType value) {
    addItem(html, value, null, null);
  }

  /**
   * Adds an item to the custom list box.
   * 
   * @param html the html of the item to be added
   * @param value the item's value.
   * @param tooltip tooltip
   */
  public final void addItem(String html, ValueType value, String tooltip) {
    itemList.addItem(html, value, tooltip, null);
  }

  /**
   * Adds an item to the custom list box.
   * 
   * @param html the html of the item to be added
   * @param value the item's value.
   * @param tooltip tooltip
   * @param buttonFace the button face that should be displayed when this item
   *          is selected
   */
  public final void addItem(String html, ValueType value, String tooltip,
      String buttonFace) {
    itemList.addItem(html, value, tooltip, buttonFace);
  }

  public final HandlerRegistration addSelectionHandler(
      SelectionHandler<ValueType> handler) {
    return super.addHandler(SelectionEvent.KEY, handler);
  }

  /**
   * Adds a list item separator.
   */
  public final void addSeparator() {
    itemList.addSeparator();
  }

  /**
   * Gets the number of items in the list.
   */
  public int getNumItems() {
    return itemList.getNumCells();
  }

  @Override
  public void onBrowserEvent(Event e) {
    itemList.onBrowserEvent(e);
    super.onBrowserEvent(e);
  }

  /**
   * Sets the selected value.
   * 
   * @param value the selected value
   */
  public final void setSelectedValue(ValueType value) {
    itemList.setSelectedValue(value);
  }

  @Override
  public void setStyleName(String styleName) {
    setCss(new StandardCss(styleName));
    super.setStyleName(styleName);
  }

  @Override
  public void setStylePrimaryName(String styleName) {
    setCss(new StandardCss(styleName));
    super.setStylePrimaryName(styleName);
  }

  /**
   * Gets the title for the most recently selected value.
   */
  protected final ValueType getLastSelectedValue() {
    return itemList.getSelectedValue();
  }

  /**
   * Returns the actual list box widget.
   */
  protected Widget getListBox() {
    return itemList;
  }

  /**
   * Gets an html summary for this list box's current state. Each item may
   * supply its own summary, and that summary will be active when the item is
   * selected.
   */
  protected String getSummary() {
    return currentSummary;
  }

  /**
   * Supply a decorator for the list items.
   * 
   * @return a decorator
   */
  protected Decorator supplyItemDecorator() {
    return Decorator.DEFAULT;
  }

  /**
   * Supply a decorator for the list box.
   * 
   * @return a decorator
   */
  protected Decorator supplyListBoxDecorator() {
    return Decorator.DEFAULT;
  }

  /**
   * Sets the css for this widget.
   */
  private void setCss(Css css) {
    this.css = css;
    itemList.setStylePrimaryName(css.listBox());
  }
}
