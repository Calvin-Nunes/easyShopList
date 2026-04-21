# EasyShopList — Documento de Implementação Completo

> **Stack:** Android · Jetpack Compose · Navigation Compose · Room (SQLite) · MVVM · Kotlin Coroutines/Flow  
> **Package:** `com.cnx.easyshoplist` · minSdk 28 · Groovy DSL

---

## Sumário

1. [Visão Geral da Arquitetura](#1-visão-geral-da-arquitetura)
2. [Banco de Dados — Room](#2-banco-de-dados--room)
3. [Estrutura de Navegação](#3-estrutura-de-navegação)
4. [Telas e ViewModels](#4-telas-e-viewmodels)
5. [Dependências](#5-dependências)
6. [Estrutura de Arquivos](#6-estrutura-de-arquivos)
7. [Código Completo de Cada Arquivo](#7-código-completo-de-cada-arquivo)
8. [Ordem de Implementação](#8-ordem-de-implementação)
9. [Considerações Importantes](#9-considerações-importantes)

---

## 1. Visão Geral da Arquitetura

### Padrão Arquitetural: MVVM + Unidirectional Data Flow

```
UI (Composables)
      ↕  observa StateFlow / envia eventos
ViewModel
      ↕  chama suspend funs / coleta Flow
Repository
      ↕  delega para DAO
DAO (Room)
      ↕
SQLite (Room Database)
```

### Camadas

| Camada | Responsabilidade |
|--------|-----------------|
| **UI (Screens)** | Composables Jetpack Compose. Observam `UiState` via `collectAsStateWithLifecycle`. |
| **ViewModel** | Contém `UiState` (StateFlow). Orquestra chamadas ao Repository. Sobrevive a recomposições. |
| **Repository** | Abstração entre ViewModel e DAOs. Contém regras de negócio (ex: upsert de item por nome normalizado). |
| **DAO** | Interfaces Room com queries SQL. Retornam `Flow<List<T>>` para dados reativos. |
| **Entities** | Data classes anotadas com `@Entity`. Representam as tabelas do banco. |

### Fluxo de Navegação

```
[Home — Listas]
     |
     ├──(clique na lista)──► [Editar Lista]──(FAB "Usar")──► [Usar Lista]
     |
     ├──(botão "+")──────────► [Criar Lista] (mesma tela de Editar Lista, listId = -1L)
     |
     └──(botão "Banco")──────► [Banco de Itens]
```

---

## 2. Banco de Dados — Room

### Diagrama de Entidades

```
SETOR                    ITEM
──────                   ──────
id PK                    id PK
nome                     nome
ordem                    tipoMedida (enum → String)
                         precoBase
                         idSetor FK → SETOR.id (nullable)

LISTA                    LISTA_ITEM
──────                   ──────────
id PK                    id PK
nome (nullable)          idLista FK → LISTA.id
dataCriacao (Long ms)    idItem FK → ITEM.id
finalizada (Boolean)     quantidade (Float)
                         precoBase (Double)
                         precoTotal (Double)
```

### Enum `TipoMedida`

| Valor | Display |
|-------|---------|
| `KG` | Kg |
| `GRAMA` | Grama |
| `PACOTE` | Pacote |
| `CAIXA` | Caixa |
| `LITRO` | Litro |
| `GARRAFA` | Garrafa |
| `UNIDADE` | Unidade |
| `DUZIA` | Dúzia |
| `LATA` | Lata |
| `SACO` | Saco |

### Regra de Upsert de Item

Ao adicionar um item a uma lista:
1. Normalizar o nome digitado: `normalizeString(nome)` → remove acentos, lowercase, trim
2. Buscar no banco por `itemDao.getByNomeNormalizado(nomeNorm)`
3. **Se existir:** atualizar `tipoMedida` e `precoBase` do item existente
4. **Se não existir:** inserir novo item
5. Criar `ListaItem` com referência ao `Item.id` obtido

---

## 3. Estrutura de Navegação

### Rotas

| Constante | Rota | Parâmetros |
|-----------|------|------------|
| `AppRoutes.HOME` | `"home"` | — |
| `AppRoutes.CREATE_LIST` | `"create_list"` | — |
| `AppRoutes.EDIT_LIST` | `"edit_list/{listId}"` | `listId: Long` |
| `AppRoutes.USE_LIST` | `"use_list/{listId}"` | `listId: Long` |
| `AppRoutes.ITEM_BANK` | `"item_bank"` | — |

### Itens da NavigationSuiteScaffold

| Ícone | Label | Rota |
|-------|-------|------|
| `ic_home` | Listas | `home` |
| `ic_add` | Nova Lista | `create_list` |
| `ic_inventory` | Banco de Itens | `item_bank` |

> **Nota:** Adicionar `ic_add.xml` e `ic_inventory.xml` (ou `ic_list.xml`) em `res/drawable/`.

---

## 4. Telas e ViewModels

### 4.1 ListasScreen

- `LazyColumn` com as listas ordenadas por `dataCriacao DESC`
- Item da lista: nome (ou data formatada) + data de criação
- Listas finalizadas: badge/ícone de check ao lado do nome
- Estado vazio: `Text("Nenhuma lista criada")` centralizado
- Clique → `navController.navigate("edit_list/${lista.id}")`

### 4.2 ListaDetalheScreen (`create_list` e `edit_list/{listId}`)

- `listId == -1L` → criar nova lista (inserir ao entrar na tela)
- **Header:** `Row` com nome da lista + `IconButton(edit)` → `AlertDialog` para editar nome
- **Formulário de adição rápida:**
  ```
  [TextField: nome do item]
  [ExposedDropdownMenu: TipoMedida] [TextField: preço]
  [Button: Adicionar]
  ```
- **Lista de itens:** `LazyColumn` agrupado por setor
  - Header de setor: `Row` com nome do setor + preço total do setor (à direita)
  - Item: nome + tipo medida + quantidade + preço + `IconButton(delete)` + `IconButton(edit)`
  - Setor "Geral" ao final para itens sem setor
- **FAB "Usar Lista":** visível se `itens.isNotEmpty() && !lista.finalizada`
  - Navega para `"use_list/${listId}"`

### 4.3 UsarListaScreen

- `LazyColumn` com itens agrupados por setor
- Cada item: `Row` com `Checkbox` + nome + quantidade + preço
- Item marcado → `TextDecoration.LineThrough` no nome
- `Button("Concluir Compra")` no rodapé → `lista.finalizada = true` → `popBackStack()`

### 4.4 BancoItensScreen

- `LazyColumn` em ordem alfabética de todos os itens
- Cada item: nome + tipo medida + preço base + setor (se houver)
- `IconButton(edit)` → `AlertDialog` com:
  - `TextField`: nome
  - `ExposedDropdownMenu`: TipoMedida
  - `TextField`: preço
  - `ExposedDropdownMenu`: Setor (+ opção "Nenhum")
  - `TextButton("Excluir", color=Red)` → `itemDao.delete(item)` → fechar dialog

---

## 5. Dependências

### `gradle/libs.versions.toml` — versões a adicionar/atualizar

```toml
[versions]
agp = "9.1.0"
coreKtx = "1.18.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.9.0"        # ← atualizar de 2.6.1
activityCompose = "1.10.1"           # ← atualizar de 1.8.0
kotlin = "2.2.10"
composeBom = "2025.12.00"
room = "2.7.0"                        # ← novo
navCompose = "2.9.0"                  # ← novo
ksp = "2.2.10-2.0.1"                 # ← novo

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material3-adaptive-navigation-suite = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite" }
# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
# Navigation
nav-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navCompose" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }  # ← novo
```

### `app/build.gradle` — atualizar

```groovy
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)              // ← novo: annotation processing do Room
}

// ... android { } sem alterações ...

dependencies {
    implementation libs.androidx.core.ktx
    implementation libs.androidx.lifecycle.runtime.ktx
    implementation libs.androidx.lifecycle.viewmodel.compose   // ← novo
    implementation libs.androidx.activity.compose
    implementation platform(libs.androidx.compose.bom)
    implementation libs.androidx.compose.ui
    implementation libs.androidx.compose.ui.graphics
    implementation libs.androidx.compose.ui.tooling.preview
    implementation libs.androidx.compose.material3
    implementation libs.androidx.compose.material3.adaptive.navigation.suite
    // Room
    implementation libs.room.runtime
    implementation libs.room.ktx
    ksp libs.room.compiler                                     // ← ksp, não annotationProcessor
    // Navigation
    implementation libs.nav.compose                            // ← novo
    // Tests (sem alterações)
    testImplementation libs.junit
    androidTestImplementation libs.androidx.junit
    androidTestImplementation libs.androidx.espresso.core
    androidTestImplementation platform(libs.androidx.compose.bom)
    androidTestImplementation libs.androidx.compose.ui.test.junit4
    debugImplementation libs.androidx.compose.ui.tooling
    debugImplementation libs.androidx.compose.ui.test.manifest
}
```

### `settings.gradle` — adicionar plugin KSP ao classpath

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```
> Normalmente não precisa alterar o `settings.gradle`, o KSP é resolvido pelo `libs.versions.toml`.

---

## 6. Estrutura de Arquivos

```
app/src/main/java/com/cnx/easyshoplist/
├── MainActivity.kt                         ← atualizado
│
├── data/
│   ├── db/
│   │   ├── EasyShopDatabase.kt
│   │   ├── dao/
│   │   │   ├── ListaDao.kt
│   │   │   ├── ListaItemDao.kt
│   │   │   ├── ItemDao.kt
│   │   │   └── SetorDao.kt
│   │   ├── entity/
│   │   │   ├── Lista.kt
│   │   │   ├── ListaItem.kt
│   │   │   ├── Item.kt
│   │   │   └── Setor.kt
│   │   └── converter/
│   │       └── Converters.kt
│   ├── enums/
│   │   └── TipoMedida.kt
│   ├── repository/
│   │   ├── ListaRepository.kt
│   │   ├── ListaItemRepository.kt
│   │   ├── ItemRepository.kt
│   │   └── SetorRepository.kt
│   └── util/
│       └── StringUtils.kt
│
└── ui/
    ├── navigation/
    │   └── AppNavigation.kt
    ├── screens/
    │   ├── listas/
    │   │   ├── ListasScreen.kt
    │   │   └── ListasViewModel.kt
    │   ├── lista_detalhe/
    │   │   ├── ListaDetalheScreen.kt
    │   │   └── ListaDetalheViewModel.kt
    │   ├── usar_lista/
    │   │   ├── UsarListaScreen.kt
    │   │   └── UsarListaViewModel.kt
    │   └── banco_itens/
    │       ├── BancoItensScreen.kt
    │       └── BancoItensViewModel.kt
    └── theme/
        ├── Color.kt                        ← sem alteração
        ├── Theme.kt                        ← sem alteração
        └── Type.kt                         ← sem alteração

app/src/main/res/drawable/
├── ic_home.xml                             ← já existe
├── ic_add.xml                              ← criar
└── ic_inventory.xml                        ← criar (ou ic_list.xml)
```

---

## 7. Código Completo de Cada Arquivo

---

### 7.1 `data/enums/TipoMedida.kt`

```kotlin
package com.cnx.easyshoplist.data.enums

enum class TipoMedida(val displayName: String) {
    KG("Kg"),
    GRAMA("Grama"),
    PACOTE("Pacote"),
    CAIXA("Caixa"),
    LITRO("Litro"),
    GARRAFA("Garrafa"),
    UNIDADE("Unidade"),
    DUZIA("Dúzia"),
    LATA("Lata"),
    SACO("Saco")
}
```

---

### 7.2 `data/util/StringUtils.kt`

```kotlin
package com.cnx.easyshoplist.data.util

import java.text.Normalizer

/**
 * Remove acentos e converte para lowercase para comparação normalizada de nomes.
 * Exemplo: "Açúcar" → "acucar"
 */
fun normalizeString(input: String): String {
    val normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
    return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
        .lowercase()
        .trim()
}
```

---

### 7.3 `data/db/entity/Setor.kt`

```kotlin
package com.cnx.easyshoplist.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setor")
data class Setor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val ordem: Int = 0
)
```

---

### 7.4 `data/db/entity/Item.kt`

```kotlin
package com.cnx.easyshoplist.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cnx.easyshoplist.data.enums.TipoMedida

@Entity(
    tableName = "item",
    foreignKeys = [
        ForeignKey(
            entity = Setor::class,
            parentColumns = ["id"],
            childColumns = ["idSetor"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("idSetor")]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val tipoMedida: TipoMedida = TipoMedida.UNIDADE,
    val precoBase: Double = 0.0,
    val idSetor: Long? = null
)
```

---

### 7.5 `data/db/entity/Lista.kt`

```kotlin
package com.cnx.easyshoplist.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lista")
data class Lista(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String? = null,
    val dataCriacao: Long = System.currentTimeMillis(),
    val finalizada: Boolean = false
)
```

---

### 7.6 `data/db/entity/ListaItem.kt`

```kotlin
package com.cnx.easyshoplist.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lista_item",
    foreignKeys = [
        ForeignKey(
            entity = Lista::class,
            parentColumns = ["id"],
            childColumns = ["idLista"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["idItem"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idLista"), Index("idItem")]
)
data class ListaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val idLista: Long,
    val idItem: Long,
    val quantidade: Float = 1f,
    val precoBase: Double = 0.0,
    val precoTotal: Double = 0.0
)
```

---

### 7.7 `data/db/converter/Converters.kt`

```kotlin
package com.cnx.easyshoplist.data.db.converter

import androidx.room.TypeConverter
import com.cnx.easyshoplist.data.enums.TipoMedida

class Converters {
    @TypeConverter
    fun fromTipoMedida(value: TipoMedida): String = value.name

    @TypeConverter
    fun toTipoMedida(value: String): TipoMedida = TipoMedida.valueOf(value)
}
```

---

### 7.8 `data/db/dao/SetorDao.kt`

```kotlin
package com.cnx.easyshoplist.data.db.dao

import androidx.room.*
import com.cnx.easyshoplist.data.db.entity.Setor
import kotlinx.coroutines.flow.Flow

@Dao
interface SetorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setor: Setor): Long

    @Update
    suspend fun update(setor: Setor)

    @Delete
    suspend fun delete(setor: Setor)

    @Query("SELECT * FROM setor ORDER BY ordem ASC, nome ASC")
    fun getAllFlow(): Flow<List<Setor>>

    @Query("SELECT * FROM setor ORDER BY ordem ASC, nome ASC")
    suspend fun getAll(): List<Setor>

    @Query("SELECT * FROM setor WHERE id = :id")
    suspend fun getById(id: Long): Setor?
}
```

---

### 7.9 `data/db/dao/ItemDao.kt`

```kotlin
package com.cnx.easyshoplist.data.db.dao

import androidx.room.*
import com.cnx.easyshoplist.data.db.entity.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM item ORDER BY nome ASC")
    fun getAllFlow(): Flow<List<Item>>

    @Query("SELECT * FROM item ORDER BY nome ASC")
    suspend fun getAll(): List<Item>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun getById(id: Long): Item?

    /**
     * Busca por nome normalizado. A normalização (lowercase sem acentos)
     * deve ser feita ANTES de chamar esta query.
     * Usamos LOWER() no SQLite para comparar. A coluna nomeNorm seria ideal,
     * mas como não temos, fazemos LOWER(nome) LIKE ao custo de um full scan
     * (aceitável para banco de itens doméstico — centenas de linhas).
     */
    @Query("SELECT * FROM item WHERE LOWER(nome) = :nomeNormalizado LIMIT 1")
    suspend fun getByNomeNormalizado(nomeNormalizado: String): Item?
}
```

---

### 7.10 `data/db/dao/ListaDao.kt`

```kotlin
package com.cnx.easyshoplist.data.db.dao

import androidx.room.*
import com.cnx.easyshoplist.data.db.entity.Lista
import kotlinx.coroutines.flow.Flow

@Dao
interface ListaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lista: Lista): Long

    @Update
    suspend fun update(lista: Lista)

    @Delete
    suspend fun delete(lista: Lista)

    @Query("SELECT * FROM lista ORDER BY dataCriacao DESC")
    fun getAllFlow(): Flow<List<Lista>>

    @Query("SELECT * FROM lista WHERE id = :id")
    suspend fun getById(id: Long): Lista?

    @Query("SELECT * FROM lista WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<Lista?>
}
```

---

### 7.11 `data/db/dao/ListaItemDao.kt`

```kotlin
package com.cnx.easyshoplist.data.db.dao

import androidx.room.*
import com.cnx.easyshoplist.data.db.entity.ListaItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ListaItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(listaItem: ListaItem): Long

    @Update
    suspend fun update(listaItem: ListaItem)

    @Delete
    suspend fun delete(listaItem: ListaItem)

    @Query("SELECT * FROM lista_item WHERE idLista = :idLista")
    fun getByListaIdFlow(idLista: Long): Flow<List<ListaItem>>

    @Query("SELECT * FROM lista_item WHERE idLista = :idLista")
    suspend fun getByListaId(idLista: Long): List<ListaItem>

    @Query("DELETE FROM lista_item WHERE idLista = :idLista")
    suspend fun deleteByListaId(idLista: Long)

    @Query("SELECT * FROM lista_item WHERE id = :id")
    suspend fun getById(id: Long): ListaItem?
}
```

---

### 7.12 `data/db/EasyShopDatabase.kt`

```kotlin
package com.cnx.easyshoplist.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cnx.easyshoplist.data.db.converter.Converters
import com.cnx.easyshoplist.data.db.dao.*
import com.cnx.easyshoplist.data.db.entity.*

@Database(
    entities = [Lista::class, ListaItem::class, Item::class, Setor::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EasyShopDatabase : RoomDatabase() {

    abstract fun listaDao(): ListaDao
    abstract fun listaItemDao(): ListaItemDao
    abstract fun itemDao(): ItemDao
    abstract fun setorDao(): SetorDao

    companion object {
        @Volatile
        private var INSTANCE: EasyShopDatabase? = null

        fun getDatabase(context: Context): EasyShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EasyShopDatabase::class.java,
                    "easy_shop_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

### 7.13 `data/repository/SetorRepository.kt`

```kotlin
package com.cnx.easyshoplist.data.repository

import com.cnx.easyshoplist.data.db.dao.SetorDao
import com.cnx.easyshoplist.data.db.entity.Setor
import kotlinx.coroutines.flow.Flow

class SetorRepository(private val setorDao: SetorDao) {

    val allSetores: Flow<List<Setor>> = setorDao.getAllFlow()

    suspend fun insert(setor: Setor): Long = setorDao.insert(setor)

    suspend fun update(setor: Setor) = setorDao.update(setor)

    suspend fun delete(setor: Setor) = setorDao.delete(setor)

    suspend fun getAll(): List<Setor> = setorDao.getAll()

    suspend fun getById(id: Long): Setor? = setorDao.getById(id)
}
```

---

### 7.14 `data/repository/ItemRepository.kt`

```kotlin
package com.cnx.easyshoplist.data.repository

import com.cnx.easyshoplist.data.db.dao.ItemDao
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.util.normalizeString
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {

    val allItems: Flow<List<Item>> = itemDao.getAllFlow()

    suspend fun insert(item: Item): Long = itemDao.insert(item)

    suspend fun update(item: Item) = itemDao.update(item)

    suspend fun delete(item: Item) = itemDao.delete(item)

    suspend fun getAll(): List<Item> = itemDao.getAll()

    suspend fun getById(id: Long): Item? = itemDao.getById(id)

    /**
     * Upsert: insere ou atualiza o item pelo nome normalizado.
     * Retorna o id do item (existente ou recém-criado).
     */
    suspend fun upsertByNome(
        nome: String,
        tipoMedida: TipoMedida,
        precoBase: Double,
        idSetor: Long? = null
    ): Long {
        val nomeNorm = normalizeString(nome)
        val existing = itemDao.getByNomeNormalizado(nomeNorm)
        return if (existing != null) {
            itemDao.update(existing.copy(tipoMedida = tipoMedida, precoBase = precoBase))
            existing.id
        } else {
            itemDao.insert(Item(nome = nome.trim(), tipoMedida = tipoMedida, precoBase = precoBase, idSetor = idSetor))
        }
    }
}
```

---

### 7.15 `data/repository/ListaItemRepository.kt`

```kotlin
package com.cnx.easyshoplist.data.repository

import com.cnx.easyshoplist.data.db.dao.ListaItemDao
import com.cnx.easyshoplist.data.db.entity.ListaItem
import kotlinx.coroutines.flow.Flow

class ListaItemRepository(private val listaItemDao: ListaItemDao) {

    fun getByListaIdFlow(idLista: Long): Flow<List<ListaItem>> =
        listaItemDao.getByListaIdFlow(idLista)

    suspend fun insert(listaItem: ListaItem): Long = listaItemDao.insert(listaItem)

    suspend fun update(listaItem: ListaItem) = listaItemDao.update(listaItem)

    suspend fun delete(listaItem: ListaItem) = listaItemDao.delete(listaItem)

    suspend fun deleteByListaId(idLista: Long) = listaItemDao.deleteByListaId(idLista)

    suspend fun getById(id: Long): ListaItem? = listaItemDao.getById(id)
}
```

---

### 7.16 `data/repository/ListaRepository.kt`

```kotlin
package com.cnx.easyshoplist.data.repository

import com.cnx.easyshoplist.data.db.dao.ListaDao
import com.cnx.easyshoplist.data.db.entity.Lista
import kotlinx.coroutines.flow.Flow

class ListaRepository(private val listaDao: ListaDao) {

    val allListas: Flow<List<Lista>> = listaDao.getAllFlow()

    suspend fun insert(lista: Lista): Long = listaDao.insert(lista)

    suspend fun update(lista: Lista) = listaDao.update(lista)

    suspend fun delete(lista: Lista) = listaDao.delete(lista)

    suspend fun getById(id: Long): Lista? = listaDao.getById(id)

    fun getByIdFlow(id: Long): Flow<Lista?> = listaDao.getByIdFlow(id)
}
```

---

### 7.17 `ui/navigation/AppNavigation.kt`

```kotlin
package com.cnx.easyshoplist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cnx.easyshoplist.data.db.EasyShopDatabase
import com.cnx.easyshoplist.data.repository.*
import com.cnx.easyshoplist.ui.screens.banco_itens.BancoItensScreen
import com.cnx.easyshoplist.ui.screens.lista_detalhe.ListaDetalheScreen
import com.cnx.easyshoplist.ui.screens.listas.ListasScreen
import com.cnx.easyshoplist.ui.screens.usar_lista.UsarListaScreen

object AppRoutes {
    const val HOME = "home"
    const val CREATE_LIST = "create_list"
    const val EDIT_LIST = "edit_list/{listId}"
    const val USE_LIST = "use_list/{listId}"
    const val ITEM_BANK = "item_bank"

    fun editList(listId: Long) = "edit_list/$listId"
    fun useList(listId: Long) = "use_list/$listId"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    db: EasyShopDatabase
) {
    val listaRepo = ListaRepository(db.listaDao())
    val listaItemRepo = ListaItemRepository(db.listaItemDao())
    val itemRepo = ItemRepository(db.itemDao())
    val setorRepo = SetorRepository(db.setorDao())

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            ListasScreen(
                listaRepository = listaRepo,
                onListaClick = { lista ->
                    navController.navigate(AppRoutes.editList(lista.id))
                }
            )
        }

        composable(AppRoutes.CREATE_LIST) {
            ListaDetalheScreen(
                listId = -1L,
                listaRepository = listaRepo,
                listaItemRepository = listaItemRepo,
                itemRepository = itemRepo,
                setorRepository = setorRepo,
                onNavigateUp = { navController.popBackStack() },
                onUsarLista = { listId ->
                    navController.navigate(AppRoutes.useList(listId))
                }
            )
        }

        composable(
            route = AppRoutes.EDIT_LIST,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: -1L
            ListaDetalheScreen(
                listId = listId,
                listaRepository = listaRepo,
                listaItemRepository = listaItemRepo,
                itemRepository = itemRepo,
                setorRepository = setorRepo,
                onNavigateUp = { navController.popBackStack() },
                onUsarLista = { id ->
                    navController.navigate(AppRoutes.useList(id))
                }
            )
        }

        composable(
            route = AppRoutes.USE_LIST,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: -1L
            UsarListaScreen(
                listId = listId,
                listaRepository = listaRepo,
                listaItemRepository = listaItemRepo,
                itemRepository = itemRepo,
                setorRepository = setorRepo,
                onConcluir = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.ITEM_BANK) {
            BancoItensScreen(
                itemRepository = itemRepo,
                setorRepository = setorRepo
            )
        }
    }
}
```

---

### 7.18 `MainActivity.kt` (atualizado)

```kotlin
package com.cnx.easyshoplist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cnx.easyshoplist.data.db.EasyShopDatabase
import com.cnx.easyshoplist.ui.navigation.AppNavigation
import com.cnx.easyshoplist.ui.navigation.AppRoutes
import com.cnx.easyshoplist.ui.theme.EasyShopListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = EasyShopDatabase.getDatabase(this)
        setContent {
            EasyShopListTheme {
                EasyShopListApp(db = db)
            }
        }
    }
}

/**
 * Define os itens do menu de navegação inferior/lateral.
 */
enum class AppNavDestination(
    val label: String,
    val iconRes: Int,
    val route: String
) {
    HOME("Listas", R.drawable.ic_home, AppRoutes.HOME),
    CREATE("Nova Lista", R.drawable.ic_add, AppRoutes.CREATE_LIST),
    ITEM_BANK("Banco de Itens", R.drawable.ic_inventory, AppRoutes.ITEM_BANK),
}

@Composable
fun EasyShopListApp(db: EasyShopDatabase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppNavDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route ||
                    (destination == AppNavDestination.HOME && currentRoute?.startsWith("edit_list") == true)
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.route) {
                            // Evita empilhamento excessivo no back stack
                            popUpTo(AppRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        AppNavigation(navController = navController, db = db)
    }
}
```

---

### 7.19 `ui/screens/listas/ListasViewModel.kt`

```kotlin
package com.cnx.easyshoplist.ui.screens.listas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.repository.ListaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ListasViewModel(
    private val listaRepository: ListaRepository
) : ViewModel() {

    val listas: StateFlow<List<Lista>> = listaRepository.allListas
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
```

---

### 7.20 `ui/screens/listas/ListasScreen.kt`

```kotlin
package com.cnx.easyshoplist.ui.screens.listas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.repository.ListaRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ListasScreen(
    listaRepository: ListaRepository,
    onListaClick: (Lista) -> Unit
) {
    val vm: ListasViewModel = viewModel(
        factory = ListasViewModelFactory(listaRepository)
    )
    val listas by vm.listas.collectAsStateWithLifecycle()

    if (listas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Nenhuma lista criada",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listas, key = { it.id }) { lista ->
                ListaItem(lista = lista, onClick = { onListaClick(lista) })
            }
        }
    }
}

@Composable
fun ListaItem(lista: Lista, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    val dataFormatada = remember(lista.dataCriacao) {
        dateFormat.format(Date(lista.dataCriacao))
    }
    val nomeDisplay = lista.nome ?: dataFormatada

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nomeDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (lista.nome != null) {
                    Text(
                        text = dataFormatada,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (lista.finalizada) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        com.cnx.easyshoplist.R.drawable.ic_check
                    ),
                    contentDescription = "Finalizada",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Factory necessária pois não usamos Hilt/Koin
class ListasViewModelFactory(
    private val listaRepository: ListaRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ListasViewModel(listaRepository) as T
    }
}
```

> **Nota:** Adicionar `ic_check.xml` em `res/drawable/` (ícone de check simples do Material Icons).

---

### 7.21 `ui/screens/lista_detalhe/ListaDetalheViewModel.kt`

```kotlin
package com.cnx.easyshoplist.ui.screens.lista_detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.db.entity.ListaItem
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Modelo de exibição de um item na lista, já com dados do Item e Setor resolvidos
data class ListaItemDisplay(
    val listaItem: ListaItem,
    val item: Item,
    val setor: Setor?
)

data class ListaDetalheUiState(
    val lista: Lista? = null,
    val itensPorSetor: Map<Setor?, List<ListaItemDisplay>> = emptyMap(),
    val setoresDisponiveis: List<Setor> = emptyList(),
    val isLoading: Boolean = true
)

class ListaDetalheViewModel(
    private val listId: Long,
    private val listaRepository: ListaRepository,
    private val listaItemRepository: ListaItemRepository,
    private val itemRepository: ItemRepository,
    private val setorRepository: SetorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListaDetalheUiState())
    val uiState: StateFlow<ListaDetalheUiState> = _uiState.asStateFlow()

    private var currentListId: Long = listId

    init {
        viewModelScope.launch {
            // Se listId == -1, criar nova lista
            if (listId == -1L) {
                val novaLista = Lista()
                currentListId = listaRepository.insert(novaLista)
            }
            loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                listaRepository.getByIdFlow(currentListId),
                listaItemRepository.getByListaIdFlow(currentListId),
                setorRepository.allSetores
            ) { lista, listaItens, setores ->
                Triple(lista, listaItens, setores)
            }.collect { (lista, listaItens, setores) ->
                // Resolver item e setor para cada ListaItem
                val displays = listaItens.mapNotNull { li ->
                    val item = itemRepository.getById(li.idItem) ?: return@mapNotNull null
                    val setor = item.idSetor?.let { setorRepository.getById(it) }
                    ListaItemDisplay(li, item, setor)
                }
                // Agrupar por setor, setores com ordem definida primeiro, depois "Geral" (null)
                val agrupado = displays
                    .groupBy { it.setor }
                    .toSortedMap(compareBy { it?.ordem ?: Int.MAX_VALUE })

                _uiState.update {
                    it.copy(
                        lista = lista,
                        itensPorSetor = agrupado,
                        setoresDisponiveis = setores,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun atualizarNomeLista(novoNome: String) {
        viewModelScope.launch {
            val lista = listaRepository.getById(currentListId) ?: return@launch
            listaRepository.update(lista.copy(nome = novoNome.trim().ifBlank { null }))
        }
    }

    fun adicionarItem(
        nome: String,
        tipoMedida: TipoMedida,
        preco: Double,
        quantidade: Float = 1f
    ) {
        viewModelScope.launch {
            val itemId = itemRepository.upsertByNome(nome, tipoMedida, preco)
            val listaItem = ListaItem(
                idLista = currentListId,
                idItem = itemId,
                quantidade = quantidade,
                precoBase = preco,
                precoTotal = preco * quantidade
            )
            listaItemRepository.insert(listaItem)
        }
    }

    fun removerItem(listaItem: ListaItem) {
        viewModelScope.launch {
            listaItemRepository.delete(listaItem)
        }
    }

    fun editarQuantidadeItem(listaItem: ListaItem, novaQuantidade: Float) {
        viewModelScope.launch {
            listaItemRepository.update(
                listaItem.copy(
                    quantidade = novaQuantidade,
                    precoTotal = listaItem.precoBase * novaQuantidade
                )
            )
        }
    }

    fun getListId(): Long = currentListId

    class Factory(
        private val listId: Long,
        private val listaRepository: ListaRepository,
        private val listaItemRepository: ListaItemRepository,
        private val itemRepository: ItemRepository,
        private val setorRepository: SetorRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ListaDetalheViewModel(
                listId, listaRepository, listaItemRepository, itemRepository, setorRepository
            ) as T
        }
    }
}
```

---

### 7.22 `ui/screens/lista_detalhe/ListaDetalheScreen.kt`

```kotlin
package com.cnx.easyshoplist.ui.screens.lista_detalhe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaDetalheScreen(
    listId: Long,
    listaRepository: ListaRepository,
    listaItemRepository: ListaItemRepository,
    itemRepository: ItemRepository,
    setorRepository: SetorRepository,
    onNavigateUp: () -> Unit,
    onUsarLista: (Long) -> Unit
) {
    val vm: ListaDetalheViewModel = viewModel(
        key = "lista_$listId",
        factory = ListaDetalheViewModel.Factory(
            listId, listaRepository, listaItemRepository, itemRepository, setorRepository
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    // Estados do formulário de adição rápida
    var nomeItem by remember { mutableStateOf("") }
    var tipoMedidaSelecionado by remember { mutableStateOf(TipoMedida.UNIDADE) }
    var precoItem by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Dialog de editar nome da lista
    var showEditNomeDialog by remember { mutableStateOf(false) }
    var editNomeInput by remember { mutableStateOf("") }

    // Dialog de editar quantidade de um item
    var itemParaEditar by remember { mutableStateOf<ListaItemDisplay?>(null) }
    var editQuantidadeInput by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    val lista = uiState.lista
    val itensPorSetor = uiState.itensPorSetor
    val totalItens = itensPorSetor.values.flatten().size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Compras") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (totalItens > 0 && lista?.finalizada == false) {
                ExtendedFloatingActionButton(
                    text = { Text("Usar Lista") },
                    icon = { Icon(painterResource(R.drawable.ic_shopping_cart), contentDescription = null) },
                    onClick = { onUsarLista(vm.getListId()) }
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── HEADER: Nome e data da lista ──────────────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val nomeDisplay = lista?.nome
                            ?: lista?.dataCriacao?.let { dateFormat.format(Date(it)) }
                            ?: "Nova Lista"
                        Text(
                            text = nomeDisplay,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        lista?.dataCriacao?.let {
                            Text(
                                text = dateFormat.format(Date(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = {
                        editNomeInput = lista?.nome ?: ""
                        showEditNomeDialog = true
                    }) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Editar nome")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // ── FORMULÁRIO DE ADIÇÃO RÁPIDA ──────────────────────────────────
            item {
                Text("Adicionar item", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = nomeItem,
                    onValueChange = { nomeItem = it },
                    label = { Text("Nome do item") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Dropdown TipoMedida
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = tipoMedidaSelecionado.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Medida") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            TipoMedida.entries.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text(tipo.displayName) },
                                    onClick = {
                                        tipoMedidaSelecionado = tipo
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    // Campo de preço
                    OutlinedTextField(
                        value = precoItem,
                        onValueChange = { precoItem = it },
                        label = { Text("Preço") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        prefix = { Text("R$") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (nomeItem.isNotBlank()) {
                            val preco = precoItem.replace(",", ".").toDoubleOrNull() ?: 0.0
                            vm.adicionarItem(nomeItem.trim(), tipoMedidaSelecionado, preco)
                            nomeItem = ""
                            precoItem = ""
                            tipoMedidaSelecionado = TipoMedida.UNIDADE
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = nomeItem.isNotBlank()
                ) {
                    Text("Adicionar")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // ── LISTA DE ITENS AGRUPADOS POR SETOR ───────────────────────────
            if (itensPorSetor.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum item adicionado ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itensPorSetor.forEach { (setor, itensDoSetor) ->
                // Header do setor
                val totalSetor = itensDoSetor.sumOf { it.listaItem.precoTotal }
                item(key = "setor_${setor?.id ?: "geral"}") {
                    SetorHeader(
                        setor = setor,
                        totalPreco = totalSetor,
                        currencyFormat = currencyFormat
                    )
                }
                // Itens do setor
                items(itensDoSetor, key = { it.listaItem.id }) { display ->
                    ListaItemRow(
                        display = display,
                        currencyFormat = currencyFormat,
                        onRemover = { vm.removerItem(display.listaItem) },
                        onEditar = {
                            itemParaEditar = display
                            editQuantidadeInput = display.listaItem.quantidade.toString()
                        }
                    )
                }
            }
        }
    }

    // ── DIALOG: Editar nome da lista ──────────────────────────────────────────
    if (showEditNomeDialog) {
        AlertDialog(
            onDismissRequest = { showEditNomeDialog = false },
            title = { Text("Nome da lista") },
            text = {
                OutlinedTextField(
                    value = editNomeInput,
                    onValueChange = { editNomeInput = it },
                    label = { Text("Nome (deixe vazio para usar a data)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.atualizarNomeLista(editNomeInput)
                    showEditNomeDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNomeDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ── DIALOG: Editar quantidade de item na lista ────────────────────────────
    itemParaEditar?.let { display ->
        AlertDialog(
            onDismissRequest = { itemParaEditar = null },
            title = { Text("Editar ${display.item.nome}") },
            text = {
                OutlinedTextField(
                    value = editQuantidadeInput,
                    onValueChange = { editQuantidadeInput = it },
                    label = { Text("Quantidade (${display.item.tipoMedida.displayName})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val qtd = editQuantidadeInput.replace(",", ".").toFloatOrNull() ?: 1f
                    vm.editarQuantidadeItem(display.listaItem, qtd)
                    itemParaEditar = null
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { itemParaEditar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SetorHeader(setor: Setor?, totalPreco: Double, currencyFormat: NumberFormat) {
    val nomeSetor = setor?.nome ?: "Geral"
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = nomeSetor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currencyFormat.format(totalPreco),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ListaItemRow(
    display: ListaItemDisplay,
    currencyFormat: NumberFormat,
    onRemover: () -> Unit,
    onEditar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(display.item.nome, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${display.listaItem.quantidade} ${display.item.tipoMedida.displayName}" +
                        " · ${currencyFormat.format(display.listaItem.precoTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEditar) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = "Editar")
            }
            IconButton(onClick = onRemover) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

> **Ícones adicionais necessários:** `ic_arrow_back`, `ic_edit`, `ic_delete`, `ic_shopping_cart`.

---

### 7.23 `ui/screens/usar_lista/UsarListaViewModel.kt`

```kotlin
package com.cnx.easyshoplist.ui.screens.usar_lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.db.entity.ListaItem
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UsarItemDisplay(
    val listaItem: ListaItem,
    val item: Item,
    val setor: Setor?,
    val marcado: Boolean = false
)

data class UsarListaUiState(
    val lista: Lista? = null,
    val itensPorSetor: Map<Setor?, List<UsarItemDisplay>> = emptyMap(),
    val isLoading: Boolean = true
)

class UsarListaViewModel(
    private val listId: Long,
    private val listaRepository: ListaRepository,
    private val listaItemRepository: ListaItemRepository,
    private val itemRepository: ItemRepository,
    private val setorRepository: SetorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsarListaUiState())
    val uiState: StateFlow<UsarListaUiState> = _uiState.asStateFlow()

    // Mapa de itemId → marcado
    private val _marcados = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    init {
        viewModelScope.launch {
            combine(
                listaRepository.getByIdFlow(listId),
                listaItemRepository.getByListaIdFlow(listId),
                setorRepository.allSetores
            ) { lista, listaItens, setores ->
                Triple(lista, listaItens, setores)
            }.combine(_marcados) { (lista, listaItens, setores), marcados ->
                val displays = listaItens.mapNotNull { li ->
                    val item = itemRepository.getById(li.idItem) ?: return@mapNotNull null
                    val setor = item.idSetor?.let { setorRepository.getById(it) }
                    UsarItemDisplay(li, item, setor, marcado = marcados[li.id] ?: false)
                }
                val agrupado = displays
                    .groupBy { it.setor }
                    .toSortedMap(compareBy { it?.ordem ?: Int.MAX_VALUE })
                UsarListaUiState(lista = lista, itensPorSetor = agrupado, isLoading = false)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleMarcado(listaItemId: Long) {
        _marcados.update { current ->
            current.toMutableMap().also { it[listaItemId] = !(it[listaItemId] ?: false) }
        }
    }

    fun concluirCompra(onConcluido: () -> Unit) {
        viewModelScope.launch {
            val lista = listaRepository.getById(listId) ?: return@launch
            listaRepository.update(lista.copy(finalizada = true))
            onConcluido()
        }
    }

    class Factory(
        private val listId: Long,
        private val listaRepository: ListaRepository,
        private val listaItemRepository: ListaItemRepository,
        private val itemRepository: ItemRepository,
        private val setorRepository: SetorRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return UsarListaViewModel(
                listId, listaRepository, listaItemRepository, itemRepository, setorRepository
            ) as T
        }
    }
}
```

---

### 7.24 `ui/screens/usar_lista/UsarListaScreen.kt`

```kotlin
package com.cnx.easyshoplist.ui.screens.usar_lista

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import java.text.NumberFormat
import java.util.*

@Composable
fun UsarListaScreen(
    listId: Long,
    listaRepository: ListaRepository,
    listaItemRepository: ListaItemRepository,
    itemRepository: ItemRepository,
    setorRepository: SetorRepository,
    onConcluir: () -> Unit
) {
    val vm: UsarListaViewModel = viewModel(
        key = "usar_$listId",
        factory = UsarListaViewModel.Factory(
            listId, listaRepository, listaItemRepository, itemRepository, setorRepository
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { vm.concluirCompra(onConcluir) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Concluir Compra")
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = uiState.lista?.nome ?: "Lista de Compras",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            uiState.itensPorSetor.forEach { (setor, itens) ->
                // Header de setor
                item(key = "setor_usar_${setor?.id ?: "geral"}") {
                    UsarSetorHeader(setor = setor, itens = itens, currencyFormat = currencyFormat)
                }
                // Itens com checkbox
                items(itens, key = { it.listaItem.id }) { display ->
                    UsarItemRow(
                        display = display,
                        currencyFormat = currencyFormat,
                        onToggle = { vm.toggleMarcado(display.listaItem.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UsarSetorHeader(
    setor: Setor?,
    itens: List<UsarItemDisplay>,
    currencyFormat: NumberFormat
) {
    val nome = setor?.nome ?: "Geral"
    val total = itens.sumOf { it.listaItem.precoTotal }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                nome,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                currencyFormat.format(total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsarItemRow(
    display: UsarItemDisplay,
    currencyFormat: NumberFormat,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = display.marcado,
            onCheckedChange = { onToggle() }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = display.item.nome,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (display.marcado) TextDecoration.LineThrough else null,
                color = if (display.marcado)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${display.listaItem.quantidade} ${display.item.tipoMedida.displayName}" +
                    " · ${currencyFormat.format(display.listaItem.precoTotal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

### 7.25 `ui/screens/banco_itens/BancoItensViewModel.kt`

```kotlin
package com.cnx.easyshoplist.ui.screens.banco_itens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ItemDisplay(
    val item: Item,
    val setor: Setor?
)

data class BancoItensUiState(
    val items: List<ItemDisplay> = emptyList(),
    val setores: List<Setor> = emptyList(),
    val isLoading: Boolean = true
)

class BancoItensViewModel(
    private val itemRepository: ItemRepository,
    private val setorRepository: SetorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BancoItensUiState())
    val uiState: StateFlow<BancoItensUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                itemRepository.allItems,
                setorRepository.allSetores
            ) { items, setores ->
                val displays = items.map { item ->
                    val setor = item.idSetor?.let { sid -> setores.firstOrNull { it.id == sid } }
                    ItemDisplay(item, setor)
                }
                BancoItensUiState(items = displays, setores = setores, isLoading = false)
            }.collect { _uiState.value = it }
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch { itemRepository.update(item) }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch { itemRepository.delete(item) }
    }

    class Factory(
        private val itemRepository: ItemRepository,
        private val setorRepository: SetorRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BancoItensViewModel(itemRepository, setorRepository) as T
        }
    }
}
```

---

### 7.26 `ui/screens/banco_itens/BancoItensScreen.kt`

```kotlin
package com.cnx.easyshoplist.ui.screens.banco_itens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BancoItensScreen(
    itemRepository: ItemRepository,
    setorRepository: SetorRepository
) {
    val vm: BancoItensViewModel = viewModel(
        factory = BancoItensViewModel.Factory(itemRepository, setorRepository)
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    var itemEditando by remember { mutableStateOf<ItemDisplay?>(null) }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Nenhum item cadastrado",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(uiState.items, key = { it.item.id }) { display ->
            BancoItemRow(
                display = display,
                currencyFormat = currencyFormat,
                onEditar = { itemEditando = display }
            )
        }
    }

    // ── DIALOG: Editar item do banco ──────────────────────────────────────────
    itemEditando?.let { editDisplay ->
        var nomeEdit by remember(editDisplay.item.id) { mutableStateOf(editDisplay.item.nome) }
        var tipoEdit by remember(editDisplay.item.id) { mutableStateOf(editDisplay.item.tipoMedida) }
        var precoEdit by remember(editDisplay.item.id) {
            mutableStateOf(editDisplay.item.precoBase.toString())
        }
        var setorEdit by remember(editDisplay.item.id) { mutableStateOf(editDisplay.setor) }
        var tipoDropdown by remember { mutableStateOf(false) }
        var setorDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { itemEditando = null },
            title = { Text("Editar item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nomeEdit,
                        onValueChange = { nomeEdit = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Dropdown TipoMedida
                    ExposedDropdownMenuBox(
                        expanded = tipoDropdown,
                        onExpandedChange = { tipoDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = tipoEdit.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Medida") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = tipoDropdown,
                            onDismissRequest = { tipoDropdown = false }
                        ) {
                            TipoMedida.entries.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text(tipo.displayName) },
                                    onClick = { tipoEdit = tipo; tipoDropdown = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = precoEdit,
                        onValueChange = { precoEdit = it },
                        label = { Text("Preço base") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("R$") }
                    )
                    // Dropdown Setor
                    ExposedDropdownMenuBox(
                        expanded = setorDropdown,
                        onExpandedChange = { setorDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = setorEdit?.nome ?: "Nenhum",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Setor") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = setorDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = setorDropdown,
                            onDismissRequest = { setorDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nenhum") },
                                onClick = { setorEdit = null; setorDropdown = false }
                            )
                            uiState.setores.forEach { setor ->
                                DropdownMenuItem(
                                    text = { Text(setor.nome) },
                                    onClick = { setorEdit = setor; setorDropdown = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val preco = precoEdit.replace(",", ".").toDoubleOrNull()
                        ?: editDisplay.item.precoBase
                    vm.updateItem(
                        editDisplay.item.copy(
                            nome = nomeEdit.trim(),
                            tipoMedida = tipoEdit,
                            precoBase = preco,
                            idSetor = setorEdit?.id
                        )
                    )
                    itemEditando = null
                }) { Text("Salvar") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            vm.deleteItem(editDisplay.item)
                            itemEditando = null
                        }
                    ) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { itemEditando = null }) { Text("Cancelar") }
                }
            }
        )
    }
}

@Composable
private fun BancoItemRow(
    display: ItemDisplay,
    currencyFormat: NumberFormat,
    onEditar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(display.item.nome, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${display.item.tipoMedida.displayName}" +
                    " · ${currencyFormat.format(display.item.precoBase)}" +
                    (display.setor?.let { " · ${it.nome}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEditar) {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = "Editar")
        }
    }
    HorizontalDivider()
}
```

---

### 7.27 Ícones necessários em `res/drawable/`

Os seguintes ícones devem ser adicionados como **Vector Drawables** do Material Icons. Todos disponíveis no [Material Symbols](https://fonts.google.com/icons):

| Arquivo | Ícone Material | Uso |
|---------|---------------|-----|
| `ic_home.xml` | `home` | Já existe |
| `ic_add.xml` | `add` | Botão Nova Lista |
| `ic_inventory.xml` | `inventory_2` ou `list` | Banco de Itens |
| `ic_edit.xml` | `edit` | Editar |
| `ic_delete.xml` | `delete` | Remover item |
| `ic_arrow_back.xml` | `arrow_back` | Voltar |
| `ic_shopping_cart.xml` | `shopping_cart` | FAB Usar Lista |
| `ic_check.xml` | `check_circle` | Lista finalizada |

> **Alternativa rápida:** Adicionar a dependência `androidx.compose.material:material-icons-extended` e usar `Icons.Default.*` em vez de `painterResource()`. Isso elimina a necessidade de criar XMLs manualmente, mas aumenta o tamanho do APK.

---

## 8. Ordem de Implementação

Execute nesta sequência para garantir que cada camada funcione antes de construir a próxima:

```
ETAPA 1 — Configuração do Projeto
  1.1 Atualizar gradle/libs.versions.toml (adicionar room, navCompose, ksp, atualizar lifecycle)
  1.2 Atualizar app/build.gradle (plugin ksp, dependências room + nav)
  1.3 Sincronizar projeto (Sync Now)
  1.4 Verificar que o build compila sem erros

ETAPA 2 — Dados base
  2.1 Criar data/enums/TipoMedida.kt
  2.2 Criar data/util/StringUtils.kt

ETAPA 3 — Banco de Dados
  3.1 Criar entity/Setor.kt
  3.2 Criar entity/Item.kt
  3.3 Criar entity/Lista.kt
  3.4 Criar entity/ListaItem.kt
  3.5 Criar converter/Converters.kt
  3.6 Criar dao/SetorDao.kt
  3.7 Criar dao/ItemDao.kt
  3.8 Criar dao/ListaDao.kt
  3.9 Criar dao/ListaItemDao.kt
  3.10 Criar EasyShopDatabase.kt
  3.11 Build → verificar que o Room gera os DAO implementations sem erro

ETAPA 4 — Repositórios
  4.1 Criar repository/SetorRepository.kt
  4.2 Criar repository/ItemRepository.kt (com lógica de upsert)
  4.3 Criar repository/ListaItemRepository.kt
  4.4 Criar repository/ListaRepository.kt

ETAPA 5 — Navegação
  5.1 Adicionar ícones em res/drawable/ (ic_add, ic_inventory, ic_edit, ic_delete, ic_arrow_back, ic_shopping_cart, ic_check)
  5.2 Criar ui/navigation/AppNavigation.kt (com AppRoutes)
  5.3 Atualizar MainActivity.kt (substituir AppDestinations, integrar NavController)

ETAPA 6 — Telas (implementar nesta ordem de dependência)
  6.1 ListasViewModel.kt + ListasScreen.kt
  6.2 ListaDetalheViewModel.kt + ListaDetalheScreen.kt
  6.3 UsarListaViewModel.kt + UsarListaScreen.kt
  6.4 BancoItensViewModel.kt + BancoItensScreen.kt

ETAPA 7 — Testes e Ajustes
  7.1 Testar fluxo: criar lista → adicionar itens → usar lista → concluir
  7.2 Testar fluxo: banco de itens → editar → excluir
  7.3 Testar: item duplicado → verifica upsert por nome normalizado
  7.4 Testar: lista sem nome → exibe data; lista com nome → exibe nome
```

---

## 9. Considerações Importantes

### Sobre a Arquitetura Compose vs Fragments

O projeto foi iniciado com **Jetpack Compose**, portanto toda a UI é baseada em composables, **não em Fragments XML**. Os conceitos equivalentes são:

| Conceito tradicional | Equivalente Compose |
|---------------------|---------------------|
| Fragment | Composable function (`@Composable`) |
| RecyclerView + Adapter | `LazyColumn { items(...) }` |
| FragmentManager + backStack | `NavController` (Navigation Compose) |
| `findNavController()` | `rememberNavController()` passado como parâmetro |
| `LiveData.observe` | `collectAsStateWithLifecycle()` |

### Sem Injeção de Dependência (por simplicidade)

Este projeto **não usa Hilt ou Koin** para manter a complexidade baixa. Os repositórios são criados manualmente em `AppNavigation.kt` a partir da instância do `EasyShopDatabase`. Para projetos maiores, considere adicionar Hilt.

### Formatações de Locale BR

```kotlin
// Data
val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
// Moeda
val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
// Decimal no TextField: aceitar vírgula E ponto
val preco = textInput.replace(",", ".").toDoubleOrNull() ?: 0.0
```

### Normalização de Nome para Upsert

A função `normalizeString()` usa `java.text.Normalizer` (disponível em todas as versões Android suportadas pelo projeto). O SQLite não oferece suporte nativo a normalização Unicode, por isso a comparação é feita em Kotlin antes da query.

```kotlin
// Exemplos de normalização:
normalizeString("Açúcar")    // → "acucar"
normalizeString("CAFÉ")      // → "cafe"  
normalizeString("  Arroz  ") // → "arroz"
```

### Gerenciamento de Estado dos Checkboxes em `UsarListaScreen`

Os checkboxes **não são persistidos** no banco de dados — são estado em memória no ViewModel (`_marcados: MutableStateFlow<Map<Long, Boolean>>`). Ao sair da tela e voltar, o estado é perdido. Isso é intencional para simplificar. Se quiser persistir, adicione uma coluna `comprado: Boolean` em `ListaItem`.

### Room e Threads

Todas as operações de banco de dados são `suspend fun` e executadas em `viewModelScope.launch { }`. O Room automaticamente executa queries em background thread quando chamadas de coroutines.

### Migrações de Banco

A versão 1 do banco não requer migração. Quando você precisar alterar o schema (adicionar coluna, tabela, etc.), incremente `version` no `@Database` e adicione uma `Migration`. Ou, em desenvolvimento, use `fallbackToDestructiveMigration()` no builder do Room.

---

*Documento gerado em: abril/2026 · EasyShopList v1.0*

