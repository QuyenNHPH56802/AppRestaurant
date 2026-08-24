package com.restaurant.staff.fcm

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * V2.3 / V18 — Channel routing for the various server-side notification types.
 *
 * We test the [NotificationChannels.channelIdForType] function directly so the
 * mapping remains stable when new server-side types are added. The build-time
 * channel definitions rely on Android system services and aren't unit-testable
 * without Robolectric.
 */
class NotificationChannelsTest {

    @Test
    fun shiftTypesMapToShiftChannel() {
        assertEquals(NotificationChannels.CHANNEL_ID_SHIFT, NotificationChannels.channelIdForType("SHIFT_ASSIGNED"))
        assertEquals(NotificationChannels.CHANNEL_ID_SHIFT, NotificationChannels.channelIdForType("SHIFT_CHANGED"))
        assertEquals(NotificationChannels.CHANNEL_ID_SHIFT, NotificationChannels.channelIdForType("SHIFT_CANCELLED"))
        assertEquals(NotificationChannels.CHANNEL_ID_SHIFT, NotificationChannels.channelIdForType("SCHEDULE_PUBLISHED"))
    }

    @Test
    fun zoneTypesMapToZoneChannel() {
        assertEquals(NotificationChannels.CHANNEL_ID_ZONE, NotificationChannels.channelIdForType("ZONE_CHANGED"))
        assertEquals(NotificationChannels.CHANNEL_ID_ZONE, NotificationChannels.channelIdForType("ZONE_ASSIGNED"))
    }

    @Test
    fun managerMessageMapsToManagerChannel() {
        assertEquals(NotificationChannels.CHANNEL_ID_MANAGER, NotificationChannels.channelIdForType("MANAGER_MESSAGE"))
    }

    @Test
    fun systemAlertMapsToDefault() {
        assertEquals(NotificationChannels.CHANNEL_ID_DEFAULT, NotificationChannels.channelIdForType("SYSTEM_ALERT"))
    }

    @Test
    fun unknownTypeFallsBackToDefault() {
        assertEquals(NotificationChannels.CHANNEL_ID_DEFAULT, NotificationChannels.channelIdForType(null))
        assertEquals(NotificationChannels.CHANNEL_ID_DEFAULT, NotificationChannels.channelIdForType(""))
        assertEquals(NotificationChannels.CHANNEL_ID_DEFAULT, NotificationChannels.channelIdForType("SOMETHING_NEW"))
    }
}