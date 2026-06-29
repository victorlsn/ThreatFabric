# ThreatFabric Android Assessment

This repository contains a small Android project with two modules:

- `storage`: the library module
- `app`: a simple demo application that uses the library

The library implements a key-value store with support for:

- storing a value for a `String` key
- reading a value by key
- deleting a value by key
- returning a random stored value
- returning keys that match a prefix

## Modules

### `storage`

The library code lives under [storage](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage).

The main files are:

- [KeyValueStore.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/KeyValueStore.kt)
- [InMemoryKeyValueStore.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/InMemoryKeyValueStore.kt)
- [KeyValueStoreFactory.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/KeyValueStoreFactory.kt)

### `app`

The demo app lives under [app](/Users/victorlsn/Documents/ThreatFabric/app/src/main/kotlin/com/threatfabric/assessment/app) and provides a simple Compose screen for trying the storage operations.

The screen can be switched between:

- an in-memory store
- a persistent store backed by Room

The main file is:

- [MainActivity.kt](/Users/victorlsn/Documents/ThreatFabric/app/src/main/kotlin/com/threatfabric/assessment/app/MainActivity.kt)

## How the Store Works

The current implementation keeps active data in memory and optionally persists it with Room.

In memory, it uses:

- `ConcurrentHashMap` for direct key lookup
- `ConcurrentSkipListSet` for ordered keys and prefix search
- a separate random-key index for efficient random access
- striped per-key locking to avoid a single global write lock

This keeps the common operations fast while still allowing safe concurrent access.

## Values and Typing

Internally, stored values are represented by [StoredValue.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/StoredValue.kt).

Built-in support is provided for:

- `String`
- `Int`
- `Long`
- `Double`
- `Boolean`

The library also supports adapters through:

- [ValueAdapter.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/ValueAdapter.kt)
- [ValueAdapters.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/ValueAdapters.kt)

This allows callers to encode and decode custom types in a controlled way instead of relying on unchecked casts.

## Persistence

Room support is implemented in:

- [KeyValueDatabase.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/room/KeyValueDatabase.kt)
- [StoredEntryDao.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/room/StoredEntryDao.kt)
- [StoredEntryEntity.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/room/StoredEntryEntity.kt)
- [RoomPersistenceDriver.kt](/Users/victorlsn/Documents/ThreatFabric/storage/src/main/kotlin/com/threatfabric/assessment/storage/room/RoomPersistenceDriver.kt)

The persistent version of the store loads the current database contents into memory when it starts and then keeps the database updated in the background.
