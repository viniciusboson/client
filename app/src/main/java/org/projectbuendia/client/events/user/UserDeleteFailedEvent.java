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

package org.projectbuendia.client.events.user;

import org.projectbuendia.client.json.JsonUser;

/** An event bus event indicating that a user could not be successfully deleted. */
public class UserDeleteFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_INVALID_USER = 1;
    public static final int REASON_USER_DOES_NOT_EXIST_LOCALLY = 2;
    public static final int REASON_USER_DOES_NOT_EXIST_ON_SERVER = 3;
    public static final int REASON_SERVER_ERROR = 4;

    public final JsonUser user;
    public final int reason;

    public UserDeleteFailedEvent(JsonUser user, int reason) {
        this.user = user;
        this.reason = reason;
    }
}
