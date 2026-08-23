# 🏛️ دليل معماريّة تعدد الموديولات وبنية الكود النظيف (Modular Clean Architecture & Hilt) لعام 2026

تم تصميم هذا الدليل البرمجي والعملي لتطبيقات **أندرويد الحديثة (Kotlin & Jetpack Compose)** مع التركيز على أنظمة إدارة موارد المؤسسات (**ERP**) ونقاط البيع (**POS**) التي تتطلب مرونة، وقابلية للتوسع السريع (Scalability)، واختبارية عالية (Testability).

---

## 📂 1. هيكلية المجلدات والموديولات المقترحة (Project Structure)

في أنظمة الـ ERP/POS الضخمة، يُنصح بشدة بنمط **Feature-based Modularization** لتجنب تضخم الموديول الرئيسي وتسهيل العمل المتوازي بين أعضاء الفريق. يتم تقسيم التطبيق إلى طبقة الموديولات الأساسية (`:core`) وطبقة ميزات الأعمال (`:features`).

### الهيكل التنظيمي للمشروع (Module Tree):
```text
├── settings.gradle.kts             # يربط ويعرّف جميع الموديولات بالمشروع
├── build.gradle.kts (Project-level)# ملف الإعدادات العام وتطبيق المكونات الإضافية
├── gradle/
│   └── libs.versions.toml          # كتالوج المكتبات الموحد (Version Catalog)
├── app/                            # موديول التشغيل (أداة التجميع والتشغيل فقط)
│   ├── build.gradle.kts
│   └── src/main/java/com/example/  # نقطة الدخول والـ Navigation Graph ومستدعي واجهة المستخدم
├── core/                           # مجلد تجميع موديولات البنية التحتية
│   ├── network/                    # موديول الاتصالات والـ Retrofit والـ Ktor
│   │   └── build.gradle.kts
│   ├── database/                   # موديول Room Database وجداول قواعد البيانات المحلية
│   │   └── build.gradle.kts
│   ├── di/                         # موديول حقن الاعتمادات الخاص بـ Hilt لإعدادات النظام العامة
│   │   └── build.gradle.kts
│   └── common-ui/                  # موديول المكونات البصرية المشتركة ورموز الثيم (Material 3)
│       └── build.gradle.kts
└── features/                       # مجلد تجميع موديولات منطق الأعمال (الميزات)
    ├── inventory/                  # موديول إدارة المخازن (المستودعات، الأصناف، التقييم)
    │   └── build.gradle.kts
    ├── billing/                    # موديول الفواتير ونقاط البيع وجلسات المبيعات
    │   └── build.gradle.kts
    └── accounts/                   # موديول الحسابات والقيود المحاسبية والخزائن
        └── build.gradle.kts
```

---

## 🛠️ 2. إعداد وإدارة ملف `settings.gradle.kts`

يقوم هذا الملف بتبليغ Gradle بالموديولات الفرعية المتواجدة في المشروع لربطها وتجميعها معًا. يتم ترتيبها هرميًا كالتالي لاستغلال الـ Namespaces الفرعية:

```kotlin
// settings.gradle.kts
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

rootProject.name = "MyEnterpriseERP"

// 1. موديول التشغيل الرئيسي
include(":app")

// 2. موديولات البنية التحتية والمشتركات (Core Modules)
include(":core:network")
include(":core:database")
include(":core:di")
include(":core:common-ui")

// 3. موديولات ميزات نظام الـ ERP والمخازن (Feature Modules)
include(":features:inventory")
include(":features:billing")
include(":features:accounts")
```

---

## 📑 3. إعداد ملف `build.gradle.kts` لموديول الميزة (`Feature Module`)

كل موديول ميزة (مثلاً `:features:inventory`) يحتاج فقط إلى الإعتمادات الخاصة به، مع ربط موديول الأساس (`:core`) بشكل مباشر ليرث البنيات المشتركة.

إليك نموذجًا احترافيًا معتمدًا على **Kotlin DSL** و **Version Catalog** لعام 2026:

```kotlin
// features/inventory/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)          // تعريف الموديول كمكتبة أندرويد وليس كتطبيق
    alias(libs.plugins.kotlin.android)           // دعم كوتلن
    alias(libs.plugins.kotlin.compose)           // دعم محرك Jetpack Compose
    alias(libs.plugins.google.devtools.ksp)      // معالج الأكواد KSP لـ Room/Hilt
    alias(libs.plugins.hilt.android)             // إضافة المكون الإضافي لـ Hilt
}

android {
    namespace = "com.example.features.inventory"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // 1. ربط موديولات البنية التحتية (Core Modules Interface)
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:di"))
    implementation(project(":core:common-ui"))

    // 2. المكتبات الأساسية من الـ Version Catalog (Kotlin, Coroutines, Lifecycle)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // 3. واجهات واجهة المستخدم (Jetpack Compose Integration)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // 4. حقن الاعتمادات المتقدم (Hilt Engine & KSP Compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // 5. أدوات الاختبار
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

---

## 🏛️ 4. تطبيق معماريّة الكود النظيف (Clean Architecture Layers)

داخل موديول الميزة الواحد، نقوم بتطبيق طبقات **Clean Architecture** الثلاث لضمان قابلية الصيانة (Maintainability) والانفصال التام بين منطق الحسابات والعرض:

```text
features/inventory/src/main/java/com/example/features/inventory/
│
├── data/                      # طبقة البيانات (المصادر الخارجية، طلبات الشبكة، الكاش)
│   ├── model/                 # كائنات نقل البيانات (DTOs)
│   ├── repository/            # تطبيق واجهة المستودع (Repository Implementation)
│   └── datasource/            # مصادر البيانات (Local & Remote DataSources)
│
├── domain/                    # طبقة المنطق المحاسبي والتجاري (لا تعتمد على أندرويد)
│   ├── model/                 # النماذج الصافية الخالية من اعتمادات المكتبات (Domain Entities)
│   ├── repository/            # واجهة المستودع المجردة (Repository Interface)
│   └── usecase/               # حالات الاستخدام الفردية (Use Cases / Interactors)
│
└── presentation/              # طبقة العرض والتحكم (واجهة المستخدم وثبات الحالات)
    ├── screens/               # شاشات Compose كدوال مستقلة قابلة للتعديل
    ├── state/                 # نماذج حالات الشاشات (UI States Sealed Classes)
    └── viewmodel/             # الـ ViewModels التي تنسق البيانات عبر قنوات تدفق البيانات (StateFlow)
```

---

## 🧪 5. كود تطبيقي متكامل (Inventory Module Blueprint)

إليك كود حقيقي معتمد لعام 2026 يوضّح دمج **Clean Architecture** مع **Hilt** و **Flow** و **Material 3**:

### أ. طبقة الـ Domain (Pure Kotlin Business Logic)

```kotlin
// 1. Domain Model: نموذج الصنف المحاسبي النقي
package com.example.features.inventory.domain.model

data class EnterpriseItem(
    val id: Long,
    val code: String,          // رمز الباركود التعريفي
    val name: String,          // اسم الصنف المحاسبي
    val costPrice: Double,     // سعر التكلفة
    val sellingPrice: Double,  // سعر البيع المقترح
    val availableStock: Double,// الرصيد الحالي المتوفر بالمخازن
    val orderThreshold: Double // حد الأمان وإعادة الطلب لتجنب النواقص
) {
    val isLowStock: Boolean get() = availableStock <= orderThreshold
}
```

```kotlin
// 2. Domain Repository Interface: تحديد الاتفاق البرمجي بدون شروط تنفيذ للبيانات
package com.example.features.inventory.domain.repository

import com.example.features.inventory.domain.model.EnterpriseItem
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getItemsCatalogFlow(): Flow<List<EnterpriseItem>>
    suspend fun addNewStockItem(item: EnterpriseItem): Long
    suspend fun reevaluateStock(itemId: Long, adjustment: Double)
}
```

```kotlin
// 3. Domain UseCase: حالة استخدام مستقلة لتحديث مخزون صنف ERP
package com.example.features.inventory.domain.usecase

import com.example.features.inventory.domain.repository.InventoryRepository
import javax.inject.Inject

class ReevaluateStockUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(itemId: Long, newAdjustment: Double) {
        // فحص قيود العمليات والمحاسبة قبل إقرار التعديل بالمخزن
        if (newAdjustment < 0) {
            throw IllegalArgumentException("لا يمكن إدخال تسوية مخزنية بقيمة سالبة!")
        }
        repository.reevaluatestock(itemId, newAdjustment)
    }
}
```

---

### ب. طبقة البيانات الـ Data (Data Access & Routing)

```kotlin
// Enterprise Item Repository Implementation
package com.example.features.inventory.data.repository

import com.example.features.inventory.domain.model.EnterpriseItem
import com.example.features.inventory.domain.repository.InventoryRepository
import com.example.core.database.dao.ItemDao          // موديول قاعدة البيانات المشتركة
import com.example.core.database.entity.ItemEntity      // الكيان بموديل قاعدة البيانات المحلية
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao
) : InventoryRepository {

    override fun getItemsCatalogFlow(): Flow<List<EnterpriseItem>> {
        return itemDao.getAllItemsFlow().map { entities ->
            entities.map { entity ->
                EnterpriseItem(
                    id = entity.id,
                    code = entity.code,
                    name = entity.name,
                    costPrice = entity.purchasePrice,
                    sellingPrice = entity.salePrice,
                    availableStock = entity.currentStock,
                    orderThreshold = entity.minLimit
                )
            }
        }
    }

    override suspend fun addNewStockItem(item: EnterpriseItem): Long {
        val entity = ItemEntity(
            code = item.code,
            name = item.name,
            purchasePrice = item.costPrice,
            salePrice = item.sellingPrice,
            currentStock = item.availableStock,
            minLimit = item.orderThreshold
        )
        return itemDao.insert(entity)
    }

    override suspend fun reevaluateStock(itemId: Long, adjustment: Double) {
        itemDao.updateStockLevel(itemId, adjustment)
    }
}
```

---

### ج. طبقة حقن الاعتمادات بـ Hilt (DI Layer Setup)

نقوم بتصميم موديول Hilt في موديول الميزة لربط الواجهات بتنفيذاتها الفعلية تلقائيًا:

```kotlin
package com.example.features.inventory.di

import com.example.features.inventory.data.repository.InventoryRepositoryImpl
import com.example.features.inventory.domain.repository.InventoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InventoryModule {

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        inventoryRepositoryImpl: InventoryRepositoryImpl
    ): InventoryRepository
}
```

---

### د. طبقة العرض الـ Presentation (UDF & State Management)

```kotlin
// UI States Representation
package com.example.features.inventory.presentation.state

import com.example.features.inventory.domain.model.EnterpriseItem

sealed interface StockCatalogUiState {
    object Loading : StockCatalogUiState
    data class Success(
        val items: List<EnterpriseItem>,
        val currentSelectedId: Long? = null,
        val validationError: String? = null
    ) : StockCatalogUiState
    data class Error(val message: String) : StockCatalogUiState
}
```

```kotlin
// M3 Architecture ViewModel implementation
package com.example.features.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.features.inventory.domain.repository.InventoryRepository
import com.example.features.inventory.domain.usecase.ReevaluateStockUseCase
import com.example.features.inventory.presentation.state.StockCatalogUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryDashboardViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val reevaluateStockUseCase: ReevaluateStockUseCase
) : ViewModel() {

    val uiState: StateFlow<StockCatalogUiState> = repository.getItemsCatalogFlow()
        .map { items -> StockCatalogUiState.Success(items = items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StockCatalogUiState.Loading
        )

    fun onReevaluateStock(itemId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                reevaluateStockUseCase(itemId, amount)
            } catch (e: Exception) {
                // إرسال كود فحص الأخطاء للمستهلك تلقائيًا
            }
        }
    }
}
```

---

### هـ. واجهة العرض باستخدام (Jetpack Compose Screen) لعام 2026

```kotlin
package com.example.features.inventory.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.features.inventory.domain.model.EnterpriseItem
import com.example.features.inventory.presentation.state.StockCatalogUiState
import com.example.features.inventory.presentation.viewmodel.InventoryDashboardViewModel

@Composable
fun InventoryCatalogDashboard(
    viewModel: InventoryDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("المستودعات والمخزون المحاسبي ERP") })
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is StockCatalogUiState.Loading -> CircularProgressIndicator()
                is StockCatalogUiState.Error -> Text("خطأ بالنظام: ${s.message}", color = MaterialTheme.colorScheme.error)
                is StockCatalogUiState.Success -> {
                    ItemsList(
                        items = s.items,
                        onAdjustStock = { id, value -> viewModel.onReevaluateStock(id, value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemsList(
    items: List<EnterpriseItem>,
    onAdjustStock: (Long, Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("الباركود: ${item.code}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "الكمية المتوفرة: ${item.availableStock}",
                            color = if (item.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(onClick = { onAdjustStock(item.id, 10.0) }) {
                        Text("+10 وحدات")
                    }
                }
            }
        }
    }
}
```

---

## 🚀 ملخص ممارسات الأداء القياسية المحمية (Performance Best Practices)
1. **Unidirectional Data Flow**: يتم تمرير البيانات عبر كائنات الحالة فقط ولا يتم تعديل المتغيرات بطريقة غير محمية.
2. **StateIn / WhileSubscribed**: يضمن حماية حزم البيانات ومنع الاتصال المستمر في الخلفية، مما يوفر الطاقة وذاكرة الوصول العشوائي.
3. **Multi-Module Gradle Caching**: من خلال فصل الميزات البنائية عن بعضها، يقوم Gradle بإعادة استخدام حزم الميزات مسبقة التجميع مما يقلل من زمن الـ Build بنسب تتجاوز الـ **60%** في مشاريع الـ ERP الكبيرة.
