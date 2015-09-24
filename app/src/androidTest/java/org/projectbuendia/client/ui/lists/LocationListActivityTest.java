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

package org.projectbuendia.client.ui.lists;

import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;

import static android.support.test.espresso.Espresso.pressBack;

/** Tests for {@link LocationListActivity}. */
public class LocationListActivityTest extends FunctionalTestCase {

    /** Looks for the expected zones and tents. */
    public void testZonesAndTentsDisplayed() {
        inUserLoginGoToLocationSelection();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from round view. */
    public void testZonesAndTentsDisplayed_afterRoundView() {
        inUserLoginGoToLocationSelection();
        inLocationSelectionClickLocation(LOCATION_NAME);
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from list view. */
    public void testZonesAndTentsDisplayed_afterPatientListView() {
        inUserLoginGoToLocationSelection();
        // TODO/i18n: Use a string resource instead of the literal button text.
        inLocationSelectionClickLocation("ALL PRESENT PATIENTS");
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from settings view. */
    public void testZonesAndTentsDisplayed_afterSettingsView() {
        inUserLoginGoToLocationSelection();

        // Enter settings view and return.
        click(viewWithText("GU"));
        click(viewWithId(R.id.button_settings));
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from chart view. */
    public void testZonesAndTentsDisplayed_afterChartView() {
        inUserLoginInitDemoPatient();
        inUserLoginGoToLocationSelection();
        inLocationSelectionClickLocation(LOCATION_NAME);
        inPatientListClickFirstPatient(); // open patient chart

        pressBack(); // back to search fragment
        pressBack(); // back to tent selection screen
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after changing a patient's location. */
    public void testZonesAndTentsDisplayed_afterPatientLocationChanged() {
        inUserLoginInitDemoPatient();
        inUserLoginGoToLocationSelection();
        inLocationSelectionClickLocation(LOCATION_NAME);
        inPatientListClickFirstPatient(); // open patient chart

        // Relocate the patient to C1.
        click(viewWithId(R.id.attribute_location));
        click(viewWithText("C1"));

        pressBack(); // back to search fragment
        pressBack(); // back to tent selection screen
        inLocationSelectionCheckZonesAndTentsDisplayed();

        invalidateDemoPatient(); // don't reuse the relocated patient for future tests
    }
}
