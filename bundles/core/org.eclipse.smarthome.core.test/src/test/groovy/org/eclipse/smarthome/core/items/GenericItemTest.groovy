/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.items

import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.collection.IsCollectionWithSize.hasSize
import static org.junit.Assert.*
import static org.mockito.Mockito.mock

import org.eclipse.smarthome.core.events.Event
import org.eclipse.smarthome.core.events.EventPublisher
import org.eclipse.smarthome.core.i18n.UnitProvider
import org.eclipse.smarthome.core.items.events.ItemEventFactory
import org.eclipse.smarthome.core.items.events.ItemStateChangedEvent
import org.eclipse.smarthome.core.items.events.ItemStateEvent
import org.eclipse.smarthome.core.library.types.OnOffType
import org.eclipse.smarthome.core.library.types.PercentType
import org.eclipse.smarthome.core.library.types.RawType
import org.eclipse.smarthome.core.library.types.StringType
import org.junit.Before
import org.junit.Test

/**
 * The GenericItemTest tests functionality of the GenericItem.
 *
 * @author Christoph Knauf - Initial contribution, event tests
 */
class GenericItemTest {

    List<Event> events = []
    EventPublisher publisher

    @Before
    void setUp() {
        publisher = [
            post : { event ->
                events.add(event)
            }
        ] as EventPublisher
    }

    @Test
    void 'assert that item posts events for updates and changes correctly'() {
        def item = new TestItem("member1")
        item.setEventPublisher(publisher)
        def oldState = item.getState()

        //State changes -> one change event is fired
        item.setState(new RawType())

        def changes = events.findAll{it instanceof ItemStateChangedEvent}
        def updates = events.findAll{it instanceof ItemStateEvent}

        assertThat events.size(), is(1)
        assertThat changes.size(), is(1)
        assertThat updates.size(), is(0)

        def change = changes.getAt(0) as ItemStateChangedEvent
        assertTrue change.getItemName().equals(item.getName())
        assertTrue change.getTopic().equals(
                ItemEventFactory.ITEM_STATE_CHANGED_EVENT_TOPIC.replace("{itemName}", item.getName())
                )
        assertTrue change.getOldItemState().equals(oldState)
        assertTrue change.getItemState().equals(item.getState())
        assertTrue change.getType().equals(ItemStateChangedEvent.TYPE)

        events.clear()

        //State doesn't change -> no event is fired
        item.setState(item.getState())
        assertThat events.size(), is(0)
    }

    @Test(expected = IllegalArgumentException.class)
    void 'assert that null as group name is not allowed for addGroupName'() {
        def item = new TestItem("member1")
        item.addGroupName(null)
    }

    @Test(expected = IllegalArgumentException.class)
    void 'assert that null as group name is not allowed for addGroupNames'() {
        def item = new TestItem("member1")
        item.addGroupNames(["group-a", null, "group-b"])
    }

    @Test(expected = IllegalArgumentException.class)
    void 'assert that null as group name is not allowed for removeGroupName'() {
        def item = new TestItem("member1")
        item.removeGroupName(null)
    }

    @Test
    void 'assert that getStateAs works with the same type for a Convertible'() {
        def item = new TestItem("member1")
        item.setState(PercentType.HUNDRED)
        assertThat item.getStateAs(PercentType), isA(PercentType)
    }

    @Test
    void 'assert that getStateAs works with a different type for a Convertible'() {
        def item = new TestItem("member1")
        item.setState(PercentType.HUNDRED)
        assertThat item.getStateAs(OnOffType), isA(OnOffType)
    }

    @Test
    void 'assert that getStateAs works with the same type for a non-Convertible'() {
        def item = new TestItem("member1")
        item.setState(StringType.valueOf("Hello World"))
        assertThat item.getStateAs(StringType), isA(StringType)
    }

    @Test
    void 'assert that getStateAs works with null'() {
        def item = new TestItem("member1")
        item.setState(StringType.valueOf("Hello World"))
        assertThat item.getStateAs(null), is(nullValue())
    }

    @Test
    void 'assert that dispose clears all services and listeners'() {
        def item = new TestItem("test");
        item.setEventPublisher(mock(EventPublisher.class));
        item.setItemStateConverter(mock(ItemStateConverter.class));
        item.setStateDescriptionService(null);
        item.setUnitProvider(mock(UnitProvider.class));

        item.addStateChangeListener(mock(StateChangeListener.class));

        item.dispose();

        assertThat(item.eventPublisher, is(nullValue()));
        assertThat(item.itemStateConverter, is(nullValue()));
        // can not be tested as stateDescriptionProviders is private in GenericItem
        // assertThat(item.stateDescriptionProviders, is(nullValue())); 
        assertThat(item.unitProvider, is(nullValue()));
        assertThat(item.listeners, hasSize(0));
    }

}
