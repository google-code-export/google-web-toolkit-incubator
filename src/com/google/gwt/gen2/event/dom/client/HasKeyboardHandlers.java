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

package com.google.gwt.gen2.event.dom.client;

import com.google.gwt.gen2.event.logical.shared.HasHandlerManager;

/**
 * Convenience interface used for widgets that wish to implement all keyboard
 * handlers. Note, as new mouse handlers are added to GWT, this interface will
 * be expanded, so only implement this interface if you wish to have your widget
 * break when a new keyboard event type is introduced.
 * 
 */
public interface HasKeyboardHandlers extends HasKeyUpHandlers,
    HasKeyDownHandlers, HasKeyPressHandlers {

  /**
   * Adaptor used to create and add all the keyboard events at once.
   * 
   * @param <EventSourceType> the event source
   */
  public abstract static class Adaptor<EventSourceType extends HasHandlerManager & HasKeyDownHandlers & HasKeyUpHandlers & HasKeyPressHandlers>
      implements KeyDownHandler, KeyUpHandler, KeyPressHandler {
    /**
     * Adds all the keyboard handler defined in this adaptor to the given event
     * source.
     * 
     * @param source
     */
    public Adaptor(EventSourceType source) {
      source.addKeyDownHandler(this);
      source.addKeyPressHandler(this);
      source.addKeyUpHandler(this);
    }
  }
}