# EasyShopList — Pendências

> **Atualizado:** Abril 2026

## ✅ Nada pendente — projeto está completo e compilando.

---

## Referência técnica rápida

| Item | Detalhe |
|------|---------|
| **SGDB** | Room sobre **SQLite** (`easy_shop_database`) |
| **Reatividade** | `Flow<T>` em todos os DAOs |
| **FK** | `PRAGMA foreign_keys = ON` via `RoomDatabase.Callback` |
| **Strings** | Zero hardcoded — `values/strings.xml` (PT) e `values-en/strings.xml` (EN) |

## Arquitetura de telas

Cada tela é composta por **4 arquivos** com responsabilidades separadas:

| Arquivo | Responsabilidade |
|---------|-----------------|
| `*Screen.kt` | **Ponto de entrada**: instancia o ViewModel, coleta estado, chama o Content. Sem código visual. |
| `*Content.kt` | **Template visual**: todo o layout (Scaffold, dialogs, LazyColumn). Recebe dados e callbacks — não conhece o ViewModel. |
| `*Components.kt` | **Componentes atômicos**: Rows, Headers e Cards reutilizáveis. |
| `*ViewModel.kt` | **Lógica de negócio**: estado, repositórios, coroutines. |

### Ícones de navegação / ação

| Ícone | Uso |
|-------|-----|
| `ic_inventory` | Produtos (banco de itens) |
| `ic_category` | Setores (tag de etiqueta — visualmente diferente) |
| `ic_home` (lista) | Menu de Listas |
| `ic_shopping_cart` | Usar lista / Ir às compras |

## Build / instalar

```
.\gradlew assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```
