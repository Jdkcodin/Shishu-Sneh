package com.mindmatrix.shishusneh.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mindmatrix.shishusneh.data.local.BabyProfile
import com.mindmatrix.shishusneh.data.local.GrowthRecord
import com.mindmatrix.shishusneh.data.local.VaccinationRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * JsonBackupManager — Handles full JSON backup and restore (Phase 4)
 *
 * Serializes all Room database records into a single JSON file
 * and parses them back for restoration.
 */
object JsonBackupManager {

    data class BackupData(
        val profiles: List<BabyProfile>,
        val growthRecords: List<GrowthRecord>,
        val vaccinationRecords: List<VaccinationRecord>
    )

    fun createBackupFile(
        context: Context,
        data: BackupData
    ): Uri {
        val rootObj = JSONObject()
        
        // Profiles
        val profilesArr = JSONArray()
        data.profiles.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("babyName", p.babyName)
            obj.put("dateOfBirth", p.dateOfBirth)
            obj.put("motherName", p.motherName)
            obj.put("gender", p.gender)
            obj.put("createdAt", p.createdAt)
            profilesArr.put(obj)
        }
        rootObj.put("profiles", profilesArr)
        
        // Growth Records
        val growthArr = JSONArray()
        data.growthRecords.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("babyProfileId", g.babyProfileId)
            obj.put("date", g.date)
            obj.put("weightKg", g.weightKg.toDouble())
            obj.put("heightCm", g.heightCm.toDouble())
            if (g.headCircumferenceCm != null) {
                obj.put("headCircumferenceCm", g.headCircumferenceCm.toDouble())
            }
            obj.put("notes", g.notes)
            obj.put("createdAt", g.createdAt)
            growthArr.put(obj)
        }
        rootObj.put("growthRecords", growthArr)
        
        // Vaccination Records
        val vaxArr = JSONArray()
        data.vaccinationRecords.forEach { v ->
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("vaccineId", v.vaccineId)
            obj.put("babyProfileId", v.babyProfileId)
            obj.put("dateGiven", v.dateGiven)
            obj.put("notes", v.notes)
            obj.put("createdAt", v.createdAt)
            vaxArr.put(obj)
        }
        rootObj.put("vaccinationRecords", vaxArr)
        
        val jsonString = rootObj.toString(4) // Pretty print
        
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val backupFile = File(exportDir, "shishu_sneh_backup.json")
        backupFile.writeText(jsonString)
        
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
    }

    fun parseBackup(context: Context, uri: Uri): BackupData {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open file")
        
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val rootObj = JSONObject(jsonString)
        
        val profiles = mutableListOf<BabyProfile>()
        if (rootObj.has("profiles")) {
            val arr = rootObj.getJSONArray("profiles")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                profiles.add(BabyProfile(
                    id = obj.getInt("id"),
                    babyName = obj.getString("babyName"),
                    dateOfBirth = obj.getLong("dateOfBirth"),
                    motherName = obj.optString("motherName", ""),
                    gender = obj.getString("gender"),
                    createdAt = obj.getLong("createdAt")
                ))
            }
        }
        
        val growthRecords = mutableListOf<GrowthRecord>()
        if (rootObj.has("growthRecords")) {
            val arr = rootObj.getJSONArray("growthRecords")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                growthRecords.add(GrowthRecord(
                    id = obj.getInt("id"),
                    babyProfileId = obj.getInt("babyProfileId"),
                    date = obj.getLong("date"),
                    weightKg = obj.getDouble("weightKg").toFloat(),
                    heightCm = obj.getDouble("heightCm").toFloat(),
                    headCircumferenceCm = if (obj.has("headCircumferenceCm")) obj.getDouble("headCircumferenceCm").toFloat() else null,
                    notes = obj.optString("notes", ""),
                    createdAt = obj.getLong("createdAt")
                ))
            }
        }
        
        val vaxRecords = mutableListOf<VaccinationRecord>()
        if (rootObj.has("vaccinationRecords")) {
            val arr = rootObj.getJSONArray("vaccinationRecords")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                vaxRecords.add(VaccinationRecord(
                    id = obj.getInt("id"),
                    vaccineId = obj.getInt("vaccineId"),
                    babyProfileId = obj.getInt("babyProfileId"),
                    dateGiven = obj.getLong("dateGiven"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.getLong("createdAt")
                ))
            }
        }
        
        return BackupData(profiles, growthRecords, vaxRecords)
    }
}
