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

package org.projectbuendia.client.sync;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Pair;

import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.ChartSection;
import org.projectbuendia.client.models.ChartSectionType;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Form;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.ChartItems;
import org.projectbuendia.client.providers.Contracts.ConceptNames;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

/** A helper class for retrieving and localizing data to show in patient charts. */
public class ChartDataHelper {
    @Deprecated
    public static final String CHART_GRID_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";
    @Deprecated
    public static final String CHART_TILES_UUID = "975afbce-d4e3-4060-a25f-afcd0e5564ef";
    public static final String ENGLISH_LOCALE = "en";

    /** UUIDs for concepts that mean everything is normal; there is no worrying symptom. */
    public static final ImmutableSet<String> NO_SYMPTOM_VALUES = ImmutableSet.of(
        ConceptUuids.NO_UUID, // NO
        ConceptUuids.SOLID_FOOD_UUID, // Solid food
        ConceptUuids.NORMAL_UUID, // NORMAL
        ConceptUuids.NONE_UUID); // None

    private final ContentResolver mContentResolver;

    private static final Logger LOG = Logger.create();

    /** When non-null, sConceptNames and sConceptTypes contain valid data for this locale. */
    private static Object sLoadingLock = new Object();
    private static String sLoadedLocale;

    private static Map<String, String> sConceptNames;
    private static Map<String, String> sConceptTypes;

    public ChartDataHelper(ContentResolver contentResolver) {
        mContentResolver = checkNotNull(contentResolver);
    }

    /** Marks in-memory concept data out of date.  Call this when concepts change in the app db. */
    public static void invalidateLoadedConceptData() {
        sLoadedLocale = null;
    }

    /** Loads concept names and types from the app db into HashMaps in memory. */
    public void loadConceptData(String locale) {
        synchronized (sLoadingLock) {
            if (!locale.equals(sLoadedLocale)) {
                sConceptNames = new HashMap<>();
                try (Cursor c = mContentResolver.query(
                    ConceptNames.CONTENT_URI, new String[] {"concept_uuid", "name"},
                    "locale = ?", new String[] {locale}, null)) {
                    while (c.moveToNext()) {
                        sConceptNames.put(c.getString(0), c.getString(1));
                    }
                }
                sConceptTypes = new HashMap<>();
                try (Cursor c = mContentResolver.query(
                    Contracts.Concepts.CONTENT_URI, new String[] {"_id", "concept_type"},
                    null, null, null)) {
                    while (c.moveToNext()) {
                        sConceptTypes.put(c.getString(0), c.getString(1));
                    }
                }
                sLoadedLocale = locale;
            }
        }
    }

    /** Gets all the orders for a given patient. */
    public List<Order> getOrders(String patientUuid) {
        Cursor c = mContentResolver.query(
            Orders.CONTENT_URI, null,
            Orders.PATIENT_UUID + " = ?", new String[] {patientUuid},
            Orders.START_MILLIS);
        List<Order> orders = new ArrayList<>();
        while (c.moveToNext()) {
            orders.add(new Order(
                Utils.getString(c, Orders.UUID, ""),
                Utils.getString(c, Orders.INSTRUCTIONS, ""),
                Utils.getLong(c, Orders.START_MILLIS, null),
                Utils.getLong(c, Orders.STOP_MILLIS, null)));
        }
        c.close();
        return orders;
    }

    /** Gets all observations for a given patient from the local cache, localized to English. */
    public List<ObsValue> getObservations(String patientUuid) {
        return getObservations(patientUuid, ENGLISH_LOCALE);
    }

    private ObsValue obsFromCursor(Cursor c) {
        long millis = c.getLong(c.getColumnIndex(Observations.ENCOUNTER_MILLIS));
        String conceptUuid = c.getString(c.getColumnIndex(Observations.CONCEPT_UUID));
        String conceptName = sConceptNames.get(conceptUuid);
        String conceptType = sConceptTypes.get(conceptUuid);
        String value = c.getString(c.getColumnIndex(Observations.VALUE));
        String localizedValue = value;
        if (ConceptType.CODED.name().equals(conceptType)) {
            localizedValue = sConceptNames.get(value);
        }
        return new ObsValue(millis, conceptUuid, conceptName, conceptType, value, localizedValue);
    }

    /** Gets all observations for a given patient, localized for a given locale. */
    public List<ObsValue> getObservations(String patientUuid, String locale) {
        loadConceptData(locale);
        List<ObsValue> results = new ArrayList<>();
        try (Cursor c = mContentResolver.query(
            Observations.CONTENT_URI, null,
            "patient_uuid = ?", new String[] {patientUuid}, null)) {
            while (c.moveToNext()) {
                results.add(obsFromCursor(c));
            }
        }
        return results;
    }

    /** Gets the latest observation of each concept for a given patient, localized to English. */
    public Map<String, ObsValue> getLatestObservations(String patientUuid) {
        return getLatestObservations(patientUuid, ENGLISH_LOCALE);
    }

    /** Gets the latest observation of each concept for a given patient from the app db. */
    public Map<String, ObsValue> getLatestObservations(String patientUuid, String locale) {
        Map<String, ObsValue> result = new HashMap<>();
        for (ObsValue obs : getObservations(patientUuid, locale)) {
            ObsValue existing = result.get(obs.conceptUuid);
            if (existing == null || obs.obsTime.isAfter(existing.obsTime)) {
                result.put(obs.conceptUuid, obs);
            }
        }
        return result;
    }

    /**
     * Gets the most recent observations for all concepts for a set of patients from the local
     * cache. Ordering will be by concept uuid, and there are not groups or other chart-based
     * configurations.
     */
    public Map<String, Map<String, ObsValue>>
    getLatestObservationsForPatients(String[] patientUuids, String locale) {
        Map<String, Map<String, ObsValue>> observations = new HashMap<String, Map<String, ObsValue>>();
        for (String patientUuid : patientUuids) {
            observations.put(patientUuid, getLatestObservations(patientUuid, locale));
        }
        return observations;
    }

    /** Gets the latest observation of the specified concept for all patients. */
    public Map<String, ObsValue> getLatestObservationsForConcept(
        String conceptUuid, String locale) {
        loadConceptData(locale);
        try (Cursor c = mContentResolver.query(
            Observations.CONTENT_URI, null,
            Observations.CONCEPT_UUID + " = ?", new String[] {conceptUuid},
            Observations.ENCOUNTER_MILLIS + " DESC")) {
            Map<String, ObsValue> result = new HashMap<>();
            while (c.moveToNext()) {
                String patientUuid = Utils.getString(c, Observations.PATIENT_UUID);
                if (result.containsKey(patientUuid)) continue;
                result.put(patientUuid, obsFromCursor(c));
            }
            return result;
        }
    }

    /** Retrieves and assembles a Chart from the local datastore. */
    public Chart getChart(String uuid) {
        Map<Long, ChartSection> tileGroupsById = new HashMap<>();
        Map<Long, ChartSection> rowGroupsById = new HashMap<>();
        List<ChartSection> tileGroups = new ArrayList<>();
        List<ChartSection> rowGroups = new ArrayList<>();

        try (Cursor c = mContentResolver.query(
            ChartItems.CONTENT_URI, null, "chart_uuid = ?", new String[] {uuid}, "weight")) {
            while (c.moveToNext()) {
                Long id = Utils.getLong(c, ChartItems._ID);
                Long parentId = Utils.getLong(c, ChartItems.PARENT_ID);
                String label = Utils.getString(c, ChartItems.LABEL, "");
                if (parentId == null) {
                    // Add a section.
                    switch (ChartSectionType.valueOf(Utils.getString(c, ChartItems.SECTION_TYPE))) {
                        case TILE_ROW:
                            ChartSection tileGroup = new ChartSection(label);
                            tileGroups.add(tileGroup);
                            tileGroupsById.put(id, tileGroup);
                            break;
                        case GRID_SECTION:
                            ChartSection rowGroup = new ChartSection(label);
                            rowGroups.add(rowGroup);
                            rowGroupsById.put(id, rowGroup);
                            break;
                    }
                } else {
                    // Add a tile to its tile group or a grid row to its row group.
                    ChartItem item = new ChartItem(label,
                        Utils.getString(c, ChartItems.TYPE),
                        Utils.getLong(c, ChartItems.REQUIRED, 0L) > 0L,
                        Utils.getString(c, ChartItems.CONCEPT_UUIDS, "").split(","),
                        Utils.getString(c, ChartItems.FORMAT),
                        Utils.getString(c, ChartItems.CAPTION_FORMAT),
                        Utils.getString(c, ChartItems.CSS_CLASS),
                        Utils.getString(c, ChartItems.CSS_STYLE),
                        Utils.getString(c, ChartItems.SCRIPT));
                    ChartSection section = tileGroupsById.containsKey(parentId)
                        ? tileGroupsById.get(parentId) : rowGroupsById.get(parentId);
                    section.items.add(item);
                }
            }
        }
        return new Chart(uuid, tileGroups, rowGroups);
    }

    /** Gets a list of the concept UUIDs and names to show in the chart tiles. */
    @Deprecated
    public List<Pair<String, String>> getTileConcepts() {
        Map<String, String> conceptNames = new HashMap<>();
        Cursor cursor = mContentResolver.query(Contracts.ConceptNames.CONTENT_URI, null,
            "locale = ?", new String[] {ENGLISH_LOCALE}, null);
        try {
            while (cursor.moveToNext()) {
                conceptNames.put(Utils.getString(cursor, "concept_uuid"),
                    Utils.getString(cursor, "name"));
            }
        } finally {
            cursor.close();
        }
        List<Pair<String, String>> conceptUuidsAndNames = new ArrayList<>();
        cursor = mContentResolver.query(Contracts.ChartItems.CONTENT_URI, null,
            "chart_uuid = ?", new String[] {CHART_TILES_UUID}, "chart_row");
        try {
            while (cursor.moveToNext()) {
                String uuid = Utils.getString(cursor, "concept_uuid");
                conceptUuidsAndNames.add(new Pair<>(uuid, conceptNames.get(uuid)));
            }
        } finally {
            cursor.close();
        }
        return conceptUuidsAndNames;
    }

    /** Gets a list of the concept UUIDs and names to show in the rows of the chart grid. */
    @Deprecated
    public List<Pair<String, String>> getGridRowConcepts() {
        Map<String, String> conceptNames = new HashMap<>();
        Cursor cursor = mContentResolver.query(Contracts.ConceptNames.CONTENT_URI, null,
            "locale = ?", new String[] {ENGLISH_LOCALE}, null);
        try {
            while (cursor.moveToNext()) {
                conceptNames.put(Utils.getString(cursor, "concept_uuid"),
                    Utils.getString(cursor, "name"));
            }
        } finally {
            cursor.close();
        }
        List<Pair<String, String>> conceptUuidsAndNames = new ArrayList<>();
        cursor = mContentResolver.query(Contracts.ChartItems.CONTENT_URI, null,
            "chart_uuid = ?", new String[] {CHART_GRID_UUID}, "chart_row");
        try {
            while (cursor.moveToNext()) {
                String uuid = Utils.getString(cursor, "concept_uuid");
                conceptUuidsAndNames.add(new Pair<>(uuid, conceptNames.get(uuid)));
            }
        } finally {
            cursor.close();
        }
        return conceptUuidsAndNames;
    }

    public List<Form> getForms() {
        Cursor cursor = mContentResolver.query(
            Contracts.Forms.CONTENT_URI, null, null, null, null);
        SortedSet<Form> forms = new TreeSet<>();
        try {
            while (cursor.moveToNext()) {
                forms.add(new Form(
                    Utils.getString(cursor, Contracts.Forms._ID),
                    Utils.getString(cursor, Contracts.Forms.UUID),
                    Utils.getString(cursor, Contracts.Forms.NAME),
                    Utils.getString(cursor, Contracts.Forms.VERSION)));
            }
        } finally {
            cursor.close();
        }
        List<Form> sortedForms = new ArrayList<>();
        sortedForms.addAll(forms);
        return sortedForms;
    }
}
