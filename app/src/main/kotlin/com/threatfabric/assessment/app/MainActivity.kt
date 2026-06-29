package com.threatfabric.assessment.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.threatfabric.assessment.storage.KeyValueEntry
import com.threatfabric.assessment.storage.KeyValueStore
import com.threatfabric.assessment.storage.KeyValueStoreFactory
import com.threatfabric.assessment.storage.StoredValue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val storeState = mutableStateOf<KeyValueStore?>(null)
    private val resultState = mutableStateOf("Loading store...")
    private val entriesState = mutableStateOf<List<KeyValueEntry>>(emptyList())
    private val storeModeState = mutableStateOf(StoreMode.IN_MEMORY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeStore(storeModeState.value)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DemoScreen(
                        result = resultState.value,
                        entries = entriesState.value,
                        storeMode = storeModeState.value,
                        onAddOrUpdate = ::addOrUpdate,
                        onGet = ::getValue,
                        onDelete = ::deleteValue,
                        onSearchPrefix = ::searchPrefix,
                        onRandomValue = ::showRandomValue,
                        onStoreModeSelected = ::switchStoreMode,
                        onMessage = { resultState.value = it },
                    )
                }
            }
        }
    }

    private fun initializeStore(storeMode: StoreMode) {
        resultState.value = "Loading ${storeMode.label.lowercase()} store..."
        entriesState.value = emptyList()
        backgroundExecutor.execute {
            val initializedStore = when (storeMode) {
                StoreMode.IN_MEMORY -> KeyValueStoreFactory.createInMemory()
                StoreMode.PERSISTENT -> KeyValueStoreFactory.createPersistent(applicationContext)
            }
            val initialEntries = initializedStore.entries()
            runOnUiThread {
                storeState.value = initializedStore
                entriesState.value = initialEntries
                resultState.value = "${storeMode.label} store ready."
            }
        }
    }

    private fun switchStoreMode(storeMode: StoreMode) {
        if (storeModeState.value == storeMode) {
            return
        }

        storeState.value?.close()
        storeState.value = null
        storeModeState.value = storeMode
        initializeStore(storeMode)
    }

    private fun addOrUpdate(key: String, value: StoredValue) {
        val store = storeOrNotify() ?: return
        store.put(key, value)
        refreshEntries(store)
        resultState.value = "Stored $key = ${formatValue(value)}"
    }

    private fun getValue(key: String) {
        val store = storeOrNotify() ?: return
        val value = store[key]
        resultState.value = if (value == null) {
            "No value stored for key \"$key\"."
        } else {
            "$key = ${formatValue(value)}"
        }
    }

    private fun deleteValue(key: String) {
        val store = storeOrNotify() ?: return
        val removed = store.remove(key)
        refreshEntries(store)
        resultState.value = if (removed == null) {
            "No value removed for key \"$key\"."
        } else {
            "Removed $key = ${formatValue(removed)}"
        }
    }

    private fun searchPrefix(prefix: String) {
        val store = storeOrNotify() ?: return
        val keys = store.getKeysByPrefix(prefix, limit = 20)
        resultState.value = if (keys.isEmpty()) {
            "No keys match prefix \"$prefix\"."
        } else {
            "Matches for \"$prefix\":\n${keys.joinToString(separator = "\n")}"
        }
    }

    private fun showRandomValue() {
        val store = storeOrNotify() ?: return
        val randomValue = store.getRandomValue()
        resultState.value = if (randomValue == null) {
            "Store is empty."
        } else {
            "Random value: ${formatValue(randomValue)}"
        }
    }

    private fun refreshEntries(store: KeyValueStore) {
        entriesState.value = store.entries()
    }

    private fun storeOrNotify(): KeyValueStore? {
        val currentStore = storeState.value
        if (currentStore == null) {
            resultState.value = "Store is still initializing."
        }
        return currentStore
    }

    private fun formatValue(value: StoredValue): String = when (value) {
        is StoredValue.Text -> "\"${value.content}\" (String)"
        is StoredValue.Int32 -> "${value.content} (Int)"
        is StoredValue.Int64 -> "${value.content} (Long)"
        is StoredValue.Decimal -> "${value.content} (Double)"
        is StoredValue.Flag -> "${value.content} (Boolean)"
        is StoredValue.Json -> "${value.payload} (JSON via ${value.adapterId})"
    }

    override fun onDestroy() {
        storeState.value?.close()
        backgroundExecutor.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun DemoScreen(
    result: String,
    entries: List<KeyValueEntry>,
    storeMode: StoreMode,
    onAddOrUpdate: (key: String, value: StoredValue) -> Unit,
    onGet: (key: String) -> Unit,
    onDelete: (key: String) -> Unit,
    onSearchPrefix: (prefix: String) -> Unit,
    onRandomValue: () -> Unit,
    onStoreModeSelected: (StoreMode) -> Unit,
    onMessage: (String) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DemoValueType.STRING) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Key-Value Store Demo",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Small screen for trying the storage API and checking persisted data.",
            style = MaterialTheme.typography.body2,
        )

        StoreModeDropdown(
            selectedMode = storeMode,
            onSelectedMode = onStoreModeSelected,
        )

        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Value") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = selectedType.keyboardType),
            singleLine = true,
        )

        ValueTypeDropdown(
            selectedType = selectedType,
            onSelectedType = { selectedType = it },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val parsedValue = selectedType.toStoredValue(value)
                    when {
                        key.isBlank() -> onMessage("Key is required.")
                        parsedValue == null -> onMessage("Value does not match the selected type.")
                        else -> onAddOrUpdate(key.trim(), parsedValue)
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Add / Update")
            }

            Button(
                onClick = { onGet(key.trim()) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Get")
            }

            Button(
                onClick = { onDelete(key.trim()) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Delete")
            }
        }

        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            label = { Text("Prefix search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onSearchPrefix(prefix) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Search Prefix")
            }

            OutlinedButton(
                onClick = onRandomValue,
                modifier = Modifier.weight(1f),
            ) {
                Text("Random Value")
            }
        }

        SectionCard(
            title = "Result",
            content = result,
            background = Color(0xFFECEFF1),
        )

        SectionCard(
            title = "Stored Entries",
            content = if (entries.isEmpty()) {
                "No entries yet."
            } else {
                entries.joinToString(separator = "\n") { entry ->
                    "${entry.key} = ${formatStoredValue(entry.value)}"
                }
            },
            background = Color(0xFFF5F5F5),
        )
    }
}

@Composable
private fun ValueTypeDropdown(
    selectedType: DemoValueType,
    onSelectedType: (DemoValueType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Value Type: ${selectedType.label}")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.94f),
        ) {
            DemoValueType.entries.forEach { type ->
                DropdownMenuItem(
                    onClick = {
                        onSelectedType(type)
                        expanded = false
                    },
                ) {
                    Text(type.label)
                }
            }
        }
    }
}

@Composable
private fun StoreModeDropdown(
    selectedMode: StoreMode,
    onSelectedMode: (StoreMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Store Mode: ${selectedMode.label}")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.94f),
        ) {
            StoreMode.entries.forEach { mode ->
                DropdownMenuItem(
                    onClick = {
                        onSelectedMode(mode)
                        expanded = false
                    },
                ) {
                    Text(mode.label)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: String,
    background: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(content)
        }
    }
}

private fun formatStoredValue(value: StoredValue): String = when (value) {
    is StoredValue.Text -> "\"${value.content}\" (String)"
    is StoredValue.Int32 -> "${value.content} (Int)"
    is StoredValue.Int64 -> "${value.content} (Long)"
    is StoredValue.Decimal -> "${value.content} (Double)"
    is StoredValue.Flag -> "${value.content} (Boolean)"
    is StoredValue.Json -> "${value.payload} (JSON via ${value.adapterId})"
}

private enum class DemoValueType(
    val label: String,
    val keyboardType: KeyboardType,
) {
    STRING("String", KeyboardType.Text),
    INT("Int", KeyboardType.Number),
    LONG("Long", KeyboardType.Number),
    DOUBLE("Double", KeyboardType.Decimal),
    BOOLEAN("Boolean", KeyboardType.Text);

    fun toStoredValue(rawValue: String): StoredValue? = when (this) {
        STRING -> StoredValue.Text(rawValue)
        INT -> rawValue.toIntOrNull()?.let(StoredValue::Int32)
        LONG -> rawValue.toLongOrNull()?.let(StoredValue::Int64)
        DOUBLE -> rawValue.toDoubleOrNull()?.let(StoredValue::Decimal)
        BOOLEAN -> rawValue.toBooleanStrictOrNull()?.let(StoredValue::Flag)
    }
}

private enum class StoreMode(val label: String) {
    IN_MEMORY("In-Memory"),
    PERSISTENT("Persistent");
}
