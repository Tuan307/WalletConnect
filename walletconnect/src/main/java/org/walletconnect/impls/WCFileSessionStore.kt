package org.walletconnect.impls

import org.json.JSONObject
import org.walletconnect.entity.WCState
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class WCFileSessionStore(private val storageFile: File) : WCSessionStore {

    private val currentStates: MutableMap<String, WCState> =
        ConcurrentHashMap()

    init {
        val storeContent = storageFile.readText()
        val json = if (storeContent.isNotBlank()) JSONObject(storeContent) else JSONObject()
        val map = mutableMapOf<String, WCState>()
        json.keys().forEach { key ->
            try {
                val item = json.getJSONObject(key)
                map[key] = WCState.fromJSON(item)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
        currentStates.putAll(map)
    }

    override fun load(id: String): WCState? = currentStates[id]

    override fun store(id: String, state: WCState) {
        currentStates[id] = state
        writeToFile()
    }

    override fun remove(id: String) {
        currentStates.remove(id)
        writeToFile()
    }

    override fun list(): List<WCState> = currentStates.values.toList()

    private fun writeToFile() {
        val json = JSONObject()
        currentStates.entries.forEach { entry ->
            val key = entry.key
            val value = entry.value
            json.put(key, value.toJSON())
        }
        storageFile.writeText(json.toString())
    }
}
