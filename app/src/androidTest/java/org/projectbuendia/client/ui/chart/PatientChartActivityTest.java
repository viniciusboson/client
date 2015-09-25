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

package org.projectbuendia.client.ui.chart;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import org.odk.collect.android.views.MediaLayout;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets2.group.TableWidgetGroup;
import org.odk.collect.android.widgets2.selectone.ButtonsSelectOneWidget;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.events.sync.SyncFinishedEvent;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.sync.EventBusIdlingResource;
import org.projectbuendia.client.utils.Logger;

import java.util.UUID;

import static org.hamcrest.Matchers.not;

/** Functional tests for {@link PatientChartActivity}. */
@MediumTest
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    private static final int ROW_HEIGHT = 84;

    private static final String FORM_LABEL = "[test] Form";
    private static final String TEMPERATURE_LABEL = "[test] Temperature(°C)";
    private static final String RESPIRATORY_RATE_LABEL = "[test] Respiratory rate (bpm)";
    private static final String SPO2_OXYGEN_SAT_LABEL = "[test] SpO2 oxygen sat (%%)";
    private static final String BLOOD_PRESSURE_SYSTOLIC_LABEL = "[test] Blood pressure, systolic";
    private static final String BLOOD_PRESSURE_DIASTOLIC_LABEL = "[test] Blood pressure, diastolic";
    private static final String WEIGHT_LABEL = "[test] Weight (kg)";
    private static final String HEIGHT_LABEL = "[test] Height (cm)";
    private static final String SHOCK_LABEL = "Shock";
    private static final String SHOCK_VALUE = "1. [test] Mild";
    private static final String CONSCIOUSNESS_LABEL = "[test] Consciousness (AVPU)";
    private static final String CONSCIOUSNESS_VALUE = "V. [test] Responds to voice";
    private static final String OTHER_SYMPTOMS_LABEL = "[test] Other symptoms";
    private static final String OTHER_SYMPTOMS_VALUE = "[test] Cough";
    private static final String HICCUPS_LABEL = "[test] Hiccups";
    private static final String HEADACHE_LABEL = "[test] Headache";
    private static final String SORE_THROAT_LABEL = "[test] Sore throat";
    private static final String HEARTBURN_LABEL = "[test] Heartburn";
    private static final String PREGNANT_LABEL = "Pregnant";
    private static final String CONDITION_LABEL = "Condition";
    private static final String CONDITION_VALUE = "2. Unwell";
    private static final String NOTES_LABEL = "[test] Notes";


    public PatientChartActivityTest() {
        super();
    }

    /** Tests that the general condition dialog successfully changes general condition. */
    public void testGeneralConditionDialog_AppliesGeneralConditionChange() {
        inUserLoginGoToDemoPatientChart();
        click(viewWithId(R.id.patient_chart_vital_general_parent));
        screenshot("General Condition Dialog");
        click(viewWithText(R.string.status_well));

        // Wait for a sync operation to update the chart.
        EventBusIdlingResource<SyncFinishedEvent> syncFinishedIdlingResource =
            new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        Espresso.registerIdlingResources(syncFinishedIdlingResource);

        // Check for updated vital view.
        expectVisibleSoon(viewWithText(R.string.status_well));

        // Check for updated chart view.
        expectVisible(viewThat(
            hasText(R.string.status_short_desc_well),
            not(hasId(R.id.patient_chart_vital_general_condition_number))));
    }

    /** Tests that the encounter form can be opened more than once. */
    public void testPatientChart_CanOpenEncounterFormMultipleTimes() {
        inUserLoginGoToDemoPatientChart();
        // Load the form once and dismiss it
        openEncounterForm();
        click(viewWithText("Discard"));

        // Load the form again and dismiss it
        openEncounterForm();
        click(viewWithText("Discard"));
    }

    /**
     * Tests that the admission date is correctly displayed in the header.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
    /*public void testPatientChart_ShowsCorrectAdmissionDate() {
        mDemoPatient.admissionDate = Optional.of(DateTime.now().minusDays(5));
        inUserLoginGoToDemoPatientChart();
        expectVisible(viewThat(
                hasAncestorThat(hasId(R.id.attribute_admission_days)),
                hasText("Day 6")));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows the correct symptoms onset date.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
    /*public void testPatientChart_ShowsCorrectSymptomsOnsetDate() {
        inUserLoginGoToDemoPatientChart();
        expectVisible(viewThat(
                hasAncestorThat(hasId(R.id.attribute_symptoms_onset_days)),
                hasText("Day 8")));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows all days, even when no observations are present.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
     /*public void testPatientChart_ShowsAllDaysInChartWhenNoObservations() {
        inUserLoginGoToDemoPatientChart();
        expectVisibleWithin(5000, viewThat(hasTextContaining("Today (Day 6)")));
        screenshot("Patient Chart");
    }*/

    // TODO/completeness: Disabled as there seems to be no easy way of
    // scrolling correctly with no adapter view.

    /** Tests that encounter time can be set to a date in the past and still displayed correctly. */
    /*public void testCanSubmitObservationsInThePast() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        selectDateFromDatePicker("2015", "Jan", null);
        answerTextQuestion("Temperature", "29.1");
        saveForm();
        checkObservationValueEquals(0, "29.1", "1 Jan"); // Temperature
    }*/
    protected void openEncounterForm() {
        openActionBarOptionsMenu();

        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
            new EventBusIdlingResource<FetchXformSucceededEvent>(
                UUID.randomUUID().toString(),
                mEventBus);
        click(viewWithText(FORM_LABEL));
        Espresso.registerIdlingResources(xformIdlingResource);

        // Give the form time to be parsed on the client (this does not result in an event firing).
        expectVisibleSoon(viewWithText("Encounter"));
    }

    /** Tests that dismissing a form immediately closes it if no changes have been made. */
    public void testDismissButtonReturnsImmediatelyWithNoChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        discardForm();
    }

    private void discardForm() {
        click(viewWithText("Discard"));
    }

    /** Tests that dismissing a form results in a dialog if changes have been made. */
    public void testDismissButtonShowsDialogWithChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        answerTextQuestion(RESPIRATORY_RATE_LABEL, "17");

        // Try to discard and give up.
        discardForm();
        expectVisible(viewWithText(R.string.title_discard_observations));
        click(viewWithText(R.string.no));

        // Try to discard and actually go back.
        discardForm();
        expectVisible(viewWithText(R.string.title_discard_observations));
        click(viewWithText(R.string.yes));
    }

    private void answerTextQuestion(String questionText, String answerText) {
        scrollToAndType(answerText, viewThat(
                isA(EditText.class),
                hasSiblingThat(
                        isA(MediaLayout.class),
                        hasDescendantThat(hasTextContaining(questionText)))));
    }

    private void answerCodedQuestion(String questionText, String answerText) {
        // Close the soft keyboard before answering any toggle questions -- on rare occasions,
        // if Espresso answers one of these questions and is then instructed to type into another
        // field, the input event will actually be generated as the keyboard is hiding and will be
        // lost, but Espresso won't detect this case.
        Espresso.closeSoftKeyboard();

        scrollToAndClick(viewThat(
                isAnyOf(CheckBox.class, RadioButton.class),
                hasAncestorThat(
                        isAnyOf(ButtonsSelectOneWidget.class, TableWidgetGroup.class, ODKView.class),
                        hasDescendantThat(hasTextContaining(questionText))),
                hasTextContaining(answerText)));
    }

    private void saveForm() {
        IdlingResource xformWaiter = getXformSubmissionIdlingResource();
        click(viewWithText("Save"));
        Espresso.registerIdlingResources(xformWaiter);
    }

    private IdlingResource getXformSubmissionIdlingResource() {
        return new EventBusIdlingResource<SubmitXformSucceededEvent>(
            UUID.randomUUID().toString(),
            mEventBus);
    }

    /**
     * Tests that, when multiple encounters for the same encounter time are submitted within a short
     * period of time, that only the latest encounter is present in the relevant column.
     */
    public void testEncounter_latestEncounterIsAlwaysShown() {
        inUserLoginGoToDemoPatientChart();

        // Update  a couple of observations (respiratory rate, blood pressure), and verify that
        // the latest value is visible for each.
        for (int i = 0; i < 6; i++) {
            openEncounterForm();

            String respiratoryRate = Integer.toString(i + 80);
            String bpSystolic = Integer.toString(i + 80);
            String bpDiastolic = Integer.toString(5 + 100);
            answerTextQuestion(RESPIRATORY_RATE_LABEL, respiratoryRate);
            answerTextQuestion(BLOOD_PRESSURE_SYSTOLIC_LABEL, bpSystolic);
            answerTextQuestion(BLOOD_PRESSURE_DIASTOLIC_LABEL, bpDiastolic);
            saveForm();

            //TODO: FIXME
            checkVitalValueContains("Pulse", respiratoryRate);
            checkObservationValueEquals(0 /*Temperature*/, bpSystolic, "Today");
            checkObservationValueEquals(6 /*Vomiting*/, bpDiastolic, "Today");
        }
    }

    private void checkVitalValueContains(String vitalName, String vitalValue) {
        // Check for updated vital view.
        expectVisibleSoon(viewThat(
            hasTextContaining(vitalValue),
            hasSiblingThat(hasTextContaining(vitalName))));
    }

    private void checkObservationValueEquals(int row, String value, String dateKey) {
        // TODO/completeness: actually check dateKey

        scrollToAndExpectVisible(viewThat(
            hasText(value),
            hasAncestorThat(isInRow(row, ROW_HEIGHT))));
    }

    /** Ensures that non-overlapping observations for the same encounter are combined. */
    public void testCombinesNonOverlappingObservationsForSameEncounter() {
        inUserLoginGoToDemoPatientChart();
        // Enter first set of observations for this encounter.
        openEncounterForm();
        answerTextQuestion(TEMPERATURE_LABEL, "37.5");
        answerTextQuestion(RESPIRATORY_RATE_LABEL, "23");
        answerTextQuestion(SPO2_OXYGEN_SAT_LABEL, "95");
        answerTextQuestion(BLOOD_PRESSURE_SYSTOLIC_LABEL, "80");
        answerTextQuestion(BLOOD_PRESSURE_DIASTOLIC_LABEL, "100");
        saveForm();

        // Enter second set of observations for this encounter.
        openEncounterForm();
        answerTextQuestion(WEIGHT_LABEL, "80");
        answerTextQuestion(HEIGHT_LABEL, "170");
        answerCodedQuestion(SHOCK_LABEL, SHOCK_VALUE);
        answerCodedQuestion(CONSCIOUSNESS_LABEL, CONSCIOUSNESS_VALUE);
        answerCodedQuestion(OTHER_SYMPTOMS_LABEL, OTHER_SYMPTOMS_VALUE);
        saveForm();

        // Enter second set of observations for this encounter.
        openEncounterForm();
        answerCodedQuestion(HICCUPS_LABEL, "Yes");
        answerCodedQuestion(HEADACHE_LABEL, "Yes");
        answerCodedQuestion(SORE_THROAT_LABEL, "Yes");
        answerCodedQuestion(HEARTBURN_LABEL, "No");
        answerCodedQuestion(PREGNANT_LABEL, "Yes");
        answerCodedQuestion(CONDITION_LABEL, CONDITION_VALUE);
        answerTextQuestion(NOTES_LABEL, "Call the family");
        saveForm();

        //TODO: Fixme
        // Check that all values are now visible.
        checkVitalValueContains("Pulse", "74");
        checkVitalValueContains("Respiration", "23");
        checkObservationValueEquals(0, "36.1", "Today"); // Temp
        checkObservationSet(5, "Today"); // Nausea
        checkObservationValueEquals(6, "2", "Today"); // Vomiting
        checkObservationValueEquals(7, "5", "Today"); // Diarrhoea
    }

    private void checkObservationSet(int row, String dateKey) {
        // TODO/completeness: actually check dateKey
        scrollToAndExpectVisible(viewThat(
            hasAncestorThat(isInRow(row, ROW_HEIGHT)),
            hasBackground(getActivity().getResources().getDrawable(R.drawable.chart_cell_active))));
    }

    /** Exercises all fields in the encounter form, except for encounter time. */
    public void testEncounter_allFieldsWorkOtherThanEncounterTime() {
        // TODO/robustness: Get rid of magic numbers in these tests.
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        answerTextQuestion(TEMPERATURE_LABEL, "37.5");
        answerTextQuestion(RESPIRATORY_RATE_LABEL, "23");
        answerTextQuestion(SPO2_OXYGEN_SAT_LABEL, "95");
        answerTextQuestion(BLOOD_PRESSURE_SYSTOLIC_LABEL, "80");
        answerTextQuestion(BLOOD_PRESSURE_DIASTOLIC_LABEL, "100");
        answerTextQuestion(WEIGHT_LABEL, "80");
        answerTextQuestion(HEIGHT_LABEL, "170");
        answerCodedQuestion(SHOCK_LABEL, SHOCK_VALUE);
        answerCodedQuestion(CONSCIOUSNESS_LABEL, CONSCIOUSNESS_VALUE);
        answerCodedQuestion(OTHER_SYMPTOMS_LABEL, OTHER_SYMPTOMS_VALUE);
        answerCodedQuestion(HICCUPS_LABEL, "Yes");
        answerCodedQuestion(HEADACHE_LABEL, "Yes");
        answerCodedQuestion(SORE_THROAT_LABEL, "Yes");
        answerCodedQuestion(HEARTBURN_LABEL, "No");
        answerCodedQuestion(PREGNANT_LABEL, "Yes");
        answerCodedQuestion(CONDITION_LABEL, CONDITION_VALUE);
        answerTextQuestion(NOTES_LABEL, "Call the family");
        saveForm();

        //TODO - FIXME
        checkVitalValueContains("Pulse", "80");
        checkVitalValueContains("Respiration", "20");
        checkVitalValueContains("Consciousness", "Responds to voice");
        checkVitalValueContains("Mobility", "Assisted");
        checkVitalValueContains("Diet", "Fluids");
        checkVitalValueContains("Hydration", "Needs ORS");
        checkVitalValueContains("Condition", "5");
        checkVitalValueContains("Pain level", "Severe");

        checkObservationValueEquals(0, "31.0", "Today"); // Temp
        checkObservationValueEquals(1, "90", "Today"); // Weight
        checkObservationValueEquals(2, "5", "Today"); // Condition
        checkObservationValueEquals(3, "V", "Today"); // Consciousness
        checkObservationValueEquals(4, "As", "Today"); // Mobility
        checkObservationSet(5, "Today"); // Nausea
        checkObservationValueEquals(6, "4", "Today"); // Vomiting
        checkObservationValueEquals(7, "6", "Today"); // Diarrhoea
        checkObservationValueEquals(8, "3", "Today"); // Pain level
        checkObservationSet(9, "Today"); // Bleeding
        checkObservationValueEquals(10, "2", "Today"); // Weakness
        checkObservationSet(13, "Today"); // Hiccups
        checkObservationSet(14, "Today"); // Red eyes
        checkObservationSet(15, "Today"); // Headache
        checkObservationSet(21, "Today"); // Back pain
        checkObservationSet(24, "Today"); // Nosebleed

        expectVisible(viewThat(hasTextContaining("Pregnant")));
        expectVisible(viewThat(hasTextContaining("IV Fitted")));

        // TODO/completeness: exercise the Notes field
    }
}
