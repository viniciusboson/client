// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.events.data;

import org.projectbuendia.client.events.DefaultCrudEventBus;

/**
 * An event bus event indicating that adding an order failed.
 * <p/>
 * <p>This event should only be posted on a {@link DefaultCrudEventBus}.
 */
public class OrderSaveFailedEvent {
    public final Reason reason;
    public final Exception exception;

    public enum Reason {
        UNKNOWN,
        UNKNOWN_SERVER_ERROR,
        CLIENT_ERROR,
        INTERRUPTED
    }

    public OrderSaveFailedEvent(Reason reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
