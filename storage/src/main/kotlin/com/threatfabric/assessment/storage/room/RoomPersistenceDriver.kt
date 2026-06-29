package com.threatfabric.assessment.storage.room

import com.threatfabric.assessment.storage.KeyValueEntry
import com.threatfabric.assessment.storage.internal.PersistenceDriver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class RoomPersistenceDriver(
    private val dao: StoredEntryDao,
    private val databaseCloser: () -> Unit,
) : PersistenceDriver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val commands = Channel<PersistenceCommand>(capacity = Channel.UNLIMITED)

    init {
        scope.launch {
            for (command in commands) {
                when (command) {
                    is PersistenceCommand.Upsert -> dao.upsert(command.entry.toEntity())
                    is PersistenceCommand.Delete -> dao.deleteByKey(command.key)
                    PersistenceCommand.Clear -> dao.clearAll()
                    is PersistenceCommand.Flush -> command.ack.complete(Unit)
                    is PersistenceCommand.Close -> {
                        command.ack.complete(Unit)
                        break
                    }
                }
            }
            databaseCloser()
        }
    }

    override fun enqueueUpsert(entry: KeyValueEntry) {
        commands.trySend(PersistenceCommand.Upsert(entry))
    }

    override fun enqueueDelete(key: String) {
        commands.trySend(PersistenceCommand.Delete(key))
    }

    override fun enqueueClear() {
        commands.trySend(PersistenceCommand.Clear)
    }

    override fun flush() {
        runBlocking {
            val ack = CompletableDeferred<Unit>()
            commands.send(PersistenceCommand.Flush(ack))
            ack.await()
        }
    }

    override fun close() {
        runBlocking {
            val ack = CompletableDeferred<Unit>()
            commands.send(PersistenceCommand.Close(ack))
            ack.await()
        }
    }

    private sealed interface PersistenceCommand {
        data class Upsert(val entry: KeyValueEntry) : PersistenceCommand

        data class Delete(val key: String) : PersistenceCommand

        data object Clear : PersistenceCommand

        data class Flush(val ack: CompletableDeferred<Unit>) : PersistenceCommand

        data class Close(val ack: CompletableDeferred<Unit>) : PersistenceCommand
    }
}
