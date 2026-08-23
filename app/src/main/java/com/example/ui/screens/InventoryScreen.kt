package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Warehouse
import com.example.data.ItemCategory
import com.example.data.ItemUnit
import com.example.data.Item
import com.example.ui.Localization
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    
    // Core data streams
    val warehouses by viewModel.warehouses.collectAsStateWithLifecycle()
    val categories by viewModel.itemCategories.collectAsStateWithLifecycle()
    val units by viewModel.itemUnits.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()

    // Screen tab selection (0: Items, 1: Warehouses, 2: Categories, 3: Units)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state controllers
    var showAddWarehouseDialog by remember { mutableStateOf(false) }
    var editingWarehouse by remember { mutableStateOf<Warehouse?>(null) }
    var warehouseToDelete by remember { mutableStateOf<Warehouse?>(null) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<ItemCategory?>(null) }

    var showAddUnitDialog by remember { mutableStateOf(false) }
    var editingUnit by remember { mutableStateOf<ItemUnit?>(null) }
    var unitToDelete by remember { mutableStateOf<ItemUnit?>(null) }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }

    val direction = Localization.getLayoutDirection(lang)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (lang == "ar") "إدارة المخازن والمستودعات" else "Warehouse & Stock Console",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (lang == "ar") "تنظيم الفئات، وحدات القياس، وعناوين المستودعات لربط الأصناف وتتبّع مستويات العجز." 
                                   else "Organize item inventory, monitor minimum reorder thresholds, specify storage zones & product categories.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Inventory Metrics Bar
                InventoryMetricsRow(
                    itemsCount = items.size,
                    warehousesCount = warehouses.size,
                    lowStockCount = items.count { it.currentStock <= it.minLimit },
                    lang = lang
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section Tabs (Item, Warehouse, Category, Unit)
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    edgePadding = 0.dp
                ) {
                    val tabs = listOf(
                        if (lang == "ar") "الأصناف (${items.size})" else "Items (${items.size})",
                        if (lang == "ar") "المخازن (${warehouses.size})" else "Warehouses (${warehouses.size})",
                        if (lang == "ar") "الفئات (${categories.size})" else "Categories (${categories.size})",
                        if (lang == "ar") "الوحدات (${units.size})" else "Units (${units.size})"
                    )
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { 
                                selectedTab = index 
                                searchQuery = "" // Reset filter
                            },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar + Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_search_input"),
                        placeholder = { 
                            Text(
                                if (lang == "ar") "بحث عن طريق الاسم أو الرمز..." 
                                else "Search by name or barcode identifier..."
                            ) 
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            when (selectedTab) {
                                0 -> showAddItemDialog = true
                                1 -> showAddWarehouseDialog = true
                                2 -> showAddCategoryDialog = true
                                3 -> showAddUnitDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("inventory_add_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = when (selectedTab) {
                                0 -> if (lang == "ar") "إضافة صنف" else "Add Item"
                                1 -> if (lang == "ar") "إضافة مخزن" else "Add Whse"
                                2 -> if (lang == "ar") "إضافة فئة" else "Add Cat"
                                else -> if (lang == "ar") "إضافة وحدة" else "Add Unit"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Areas depending on active selection
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> ItemsSubSection(
                            items = items,
                            categories = categories,
                            units = units,
                            warehouses = warehouses,
                            searchQuery = searchQuery,
                            lang = lang,
                            onEdit = { editingItem = it },
                            onDelete = { itemToDelete = it }
                        )
                        1 -> WarehousesSubSection(
                            warehouses = warehouses,
                            searchQuery = searchQuery,
                            lang = lang,
                            onEdit = { editingWarehouse = it },
                            onDelete = { warehouseToDelete = it }
                        )
                        2 -> CategoriesSubSection(
                            categories = categories,
                            searchQuery = searchQuery,
                            lang = lang,
                            onEdit = { editingCategory = it },
                            onDelete = { categoryToDelete = it }
                        )
                        3 -> UnitsSubSection(
                            units = units,
                            searchQuery = searchQuery,
                            lang = lang,
                            onEdit = { editingUnit = it },
                            onDelete = { unitToDelete = it }
                        )
                    }
                }
            }

            // Dialog Popups
            if (showAddWarehouseDialog) {
                AddEditWarehouseDialog(
                    initWhse = null,
                    lang = lang,
                    onDismiss = { showAddWarehouseDialog = false },
                    onConfirm = { name, location, manager, phone ->
                        viewModel.addWarehouse(name, location, manager, phone)
                        showAddWarehouseDialog = false
                    }
                )
            }

            if (editingWarehouse != null) {
                AddEditWarehouseDialog(
                    initWhse = editingWarehouse,
                    lang = lang,
                    onDismiss = { editingWarehouse = null },
                    onConfirm = { name, location, manager, phone ->
                        editingWarehouse?.let {
                            viewModel.updateWarehouse(it.copy(name = name, location = location, manager = manager, phone = phone))
                        }
                        editingWarehouse = null
                    }
                )
            }

            if (warehouseToDelete != null) {
                DeleteConfirmDialog(
                    title = if (lang == "ar") "حذف مخزن" else "Delete Warehouse",
                    message = if (lang == "ar") "⚠️ هل أنت متأكد من حذف المخزن '${warehouseToDelete?.name}'؟" else "Are you sure you want to delete '${warehouseToDelete?.name}'?",
                    lang = lang,
                    onDismiss = { warehouseToDelete = null },
                    onConfirm = {
                        warehouseToDelete?.let { viewModel.deleteWarehouse(it) }
                        warehouseToDelete = null
                    }
                )
            }

            // Categories
            if (showAddCategoryDialog) {
                AddEditCategoryDialog(
                    initCategory = null,
                    lang = lang,
                    onDismiss = { showAddCategoryDialog = false },
                    onConfirm = { name, desc ->
                        viewModel.addItemCategory(name, desc)
                        showAddCategoryDialog = false
                    }
                )
            }

            if (editingCategory != null) {
                AddEditCategoryDialog(
                    initCategory = editingCategory,
                    lang = lang,
                    onDismiss = { editingCategory = null },
                    onConfirm = { name, desc ->
                        editingCategory?.let {
                            viewModel.updateItemCategory(it.copy(name = name, description = desc))
                        }
                        editingCategory = null
                    }
                )
            }

            if (categoryToDelete != null) {
                DeleteConfirmDialog(
                    title = if (lang == "ar") "حذف الفئة" else "Delete Category",
                    message = if (lang == "ar") "⚠️ هل أنت متأكد من حذف فئة الأصناف '${categoryToDelete?.name}'؟" else "Are you sure you want to delete the category '${categoryToDelete?.name}'?",
                    lang = lang,
                    onDismiss = { categoryToDelete = null },
                    onConfirm = {
                        categoryToDelete?.let { viewModel.deleteItemCategory(it) }
                        categoryToDelete = null
                    }
                )
            }

            // Units
            if (showAddUnitDialog) {
                AddEditUnitDialog(
                    initUnit = null,
                    lang = lang,
                    onDismiss = { showAddUnitDialog = false },
                    onConfirm = { name, desc ->
                        viewModel.addItemUnit(name, desc)
                        showAddUnitDialog = false
                    }
                )
            }

            if (editingUnit != null) {
                AddEditUnitDialog(
                    initUnit = editingUnit,
                    lang = lang,
                    onDismiss = { editingUnit = null },
                    onConfirm = { name, desc ->
                        editingUnit?.let {
                            viewModel.updateItemUnit(it.copy(name = name, description = desc))
                        }
                        editingUnit = null
                    }
                )
            }

            if (unitToDelete != null) {
                DeleteConfirmDialog(
                    title = if (lang == "ar") "حذف وحدة القياس" else "Delete Measurement Unit",
                    message = if (lang == "ar") "⚠️ هل أنت متأكد من حذف وحدة القياس '${unitToDelete?.name}'؟" else "Are you sure you want to delete the unit '${unitToDelete?.name}'?",
                    lang = lang,
                    onDismiss = { unitToDelete = null },
                    onConfirm = {
                        unitToDelete?.let { viewModel.deleteItemUnit(it) }
                        unitToDelete = null
                    }
                )
            }

            // Items Dialogs
            if (showAddItemDialog) {
                AddEditItemDialog(
                    initItem = null,
                    categories = categories,
                    units = units,
                    warehouses = warehouses,
                    lang = lang,
                    onDismiss = { showAddItemDialog = false },
                    onConfirm = { code, name, catId, uId, whId, purPrice, sPrice, minL, curS, comments ->
                        viewModel.addItem(code, name, catId, uId, whId, purPrice, sPrice, minL, curS, comments)
                        showAddItemDialog = false
                    }
                )
            }

            if (editingItem != null) {
                AddEditItemDialog(
                    initItem = editingItem,
                    categories = categories,
                    units = units,
                    warehouses = warehouses,
                    lang = lang,
                    onDismiss = { editingItem = null },
                    onConfirm = { code, name, catId, uId, whId, purPrice, sPrice, minL, curS, comments ->
                        editingItem?.let {
                            viewModel.updateItem(
                                it.copy(
                                    code = code,
                                    name = name,
                                    categoryId = catId,
                                    unitId = uId,
                                    defaultWarehouseId = whId,
                                    purchasePrice = purPrice,
                                    salePrice = sPrice,
                                    minLimit = minL,
                                    currentStock = curS,
                                    notes = comments
                                )
                            )
                        }
                        editingItem = null
                    }
                )
            }

            if (itemToDelete != null) {
                DeleteConfirmDialog(
                    title = if (lang == "ar") "حذف الصنف" else "Delete Item",
                    message = if (lang == "ar") "⚠️ هل أنت متأكد من حذف الصنف '${itemToDelete?.name}'؟" else "Are you sure you want to delete item '${itemToDelete?.name}'?",
                    lang = lang,
                    onDismiss = { itemToDelete = null },
                    onConfirm = {
                        itemToDelete?.let { viewModel.deleteItem(it) }
                        itemToDelete = null
                    }
                )
            }
        }
    }
}

// Sub components

@Composable
fun InventoryMetricsRow(
    itemsCount: Int,
    warehousesCount: Int,
    lowStockCount: Int,
    lang: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Inventory,
            title = if (lang == "ar") "إجمالي الأصناف" else "Items Cataloged",
            value = itemsCount.toString(),
            color = MaterialTheme.colorScheme.primary
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Warehouse,
            title = if (lang == "ar") "المستودعات الناشطة" else "Whse Storage",
            value = warehousesCount.toString(),
            color = MaterialTheme.colorScheme.secondary
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Warning,
            title = if (lang == "ar") "نواقص (تحت الحد)" else "Low Stock Alerts",
            value = lowStockCount.toString(),
            color = if (lowStockCount > 0) RoseRed else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = "", tint = color, modifier = Modifier.size(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------- Sub sections listings -------------------------

@Composable
fun ItemsSubSection(
    items: List<Item>,
    categories: List<ItemCategory>,
    units: List<ItemUnit>,
    warehouses: List<Warehouse>,
    searchQuery: String,
    lang: String,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit
) {
    val filtered = items.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.code.contains(searchQuery, ignoreCase = true) ||
        it.notes.contains(searchQuery, ignoreCase = true)
    }

    if (filtered.isEmpty()) {
        EmptyListPlaceholder(
            icon = Icons.Outlined.Inventory2,
            message = if (lang == "ar") "قائمة الأصناف فارغة حالياً. أضف الأصناف لتتبع المخزون وسعر المشتريات والمبيعات ومكان التخزين."
                      else "The items catalog is currently empty. Define stock descriptors to track procurement indices."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { item ->
                val category = categories.find { it.id == item.categoryId }
                val unit = units.find { it.id == item.unitId }
                val warehouse = warehouses.find { it.id == item.defaultWarehouseId }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("item_card_${item.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Barcode Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.code.ifEmpty { "N/A" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Breadcrumb tags: Whse, Category, Unit
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AssistTag(label = category?.name ?: (if (lang == "ar") "بدون فئة" else "Uncategorized"))
                                    AssistTag(label = unit?.name ?: (if (lang == "ar") "وحدة غير محددة" else "No Unit"))
                                    AssistTag(label = warehouse?.name ?: (if (lang == "ar") "بدون مخزن معين" else "No Default Whse"))
                                }
                            }

                            // Actions
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onEdit(item) }, modifier = Modifier.testTag("item_edit_${item.id}")) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { onDelete(item) }, modifier = Modifier.testTag("item_del_${item.id}")) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        // Stock and Price info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Prices
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text(
                                        if (lang == "ar") "سعر الشراء" else "Purchase Price",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${String.format("%.3f", item.purchasePrice)} LYD",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column {
                                    Text(
                                        if (lang == "ar") "سعر البيع" else "Sale Price",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${String.format("%.3f", item.salePrice)} LYD",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Stock Indicator badge
                            val isLow = item.currentStock <= item.minLimit
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isLow) RoseRed.copy(alpha = 0.15f) 
                                        else EmeraldGreen.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isLow) RoseRed.copy(alpha = 0.5f) 
                                        else EmeraldGreen.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLow) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = "",
                                        tint = if (isLow) RoseRed else EmeraldGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${if (lang == "ar") "الكمية: " else "Stock: "}${item.currentStock}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLow) RoseRed else EmeraldGreen
                                    )
                                }
                            }
                        }

                        if (item.notes.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "✍️ " + item.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssistTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WarehousesSubSection(
    warehouses: List<Warehouse>,
    searchQuery: String,
    lang: String,
    onEdit: (Warehouse) -> Unit,
    onDelete: (Warehouse) -> Unit
) {
    val filtered = warehouses.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.location.contains(searchQuery, ignoreCase = true) ||
        it.manager.contains(searchQuery, ignoreCase = true)
    }

    if (filtered.isEmpty()) {
        EmptyListPlaceholder(
            icon = Icons.Outlined.Warehouse,
            message = if (lang == "ar") "لا توجد مستودعات مطابقة للبحث." else "No warehouses matching the layout filters."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { whse ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Store, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
                                Text(whse.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "📍 ${if (whse.location.isEmpty()) (if (lang == "ar") "غير محدد" else "Unspecified") else whse.location}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (whse.manager.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "👤 ${if (lang == "ar") "الأمين: " else "Custodian: "}${whse.manager} (${whse.phone.ifEmpty { "N/A" }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { onEdit(whse) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { onDelete(whse) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesSubSection(
    categories: List<ItemCategory>,
    searchQuery: String,
    lang: String,
    onEdit: (ItemCategory) -> Unit,
    onDelete: (ItemCategory) -> Unit
) {
    val filtered = categories.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true)
    }

    if (filtered.isEmpty()) {
        EmptyListPlaceholder(
            icon = Icons.Outlined.Category,
            message = if (lang == "ar") "لا توجد فئات مطابقة للبحث." else "No categories found matching criteria."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (cat.description.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    cat.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { onEdit(cat) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { onDelete(cat) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnitsSubSection(
    units: List<ItemUnit>,
    searchQuery: String,
    lang: String,
    onEdit: (ItemUnit) -> Unit,
    onDelete: (ItemUnit) -> Unit
) {
    val filtered = units.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true)
    }

    if (filtered.isEmpty()) {
        EmptyListPlaceholder(
            icon = Icons.Outlined.SquareFoot,
            message = if (lang == "ar") "لا توجد وحدات قياس مطابقة للبحث." else "No units of measurement found."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { unit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(unit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (unit.description.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    unit.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { onEdit(unit) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { onDelete(unit) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyListPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ------------------------ CRUD dialogs -----------------------------------

@Composable
fun AddEditWarehouseDialog(
    initWhse: Warehouse?,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, location: String, manager: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(initWhse?.name ?: "") }
    var location by remember { mutableStateOf(initWhse?.location ?: "") }
    var manager by remember { mutableStateOf(initWhse?.manager ?: "") }
    var phone by remember { mutableStateOf(initWhse?.phone ?: "") }

    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initWhse == null) {
                        if (lang == "ar") "إضافة مخزن جديد" else "Register Warehouse"
                    } else {
                        if (lang == "ar") "تعديل بيانات المخزن" else "Modify Warehouse"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(if (lang == "ar") "اسم المستودع / المخزن *" else "Warehouse Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("whse_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(if (lang == "ar") "موقع المخزن" else "Storage Location / Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = manager,
                    onValueChange = { manager = it },
                    label = { Text(if (lang == "ar") "أمين المخزن" else "Warehouse Custodian") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (lang == "ar") "هاتف التواصل" else "Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                if (error != null) {
                    Text(error!!, color = RoseRed, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                error = if (lang == "ar") "اسم المستودع مطلوب" else "Warehouse name is required!"
                            } else {
                                onConfirm(name.trim(), location.trim(), manager.trim(), phone.trim())
                            }
                        },
                        modifier = Modifier.testTag("whse_confirm_btn")
                    ) {
                        Text(if (lang == "ar") "حفظ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCategoryDialog(
    initCategory: ItemCategory?,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf(initCategory?.name ?: "") }
    var desc by remember { mutableStateOf(initCategory?.description ?: "") }

    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initCategory == null) {
                        if (lang == "ar") "إضافة فئة جديدة" else "Create Item Category"
                    } else {
                        if (lang == "ar") "تعديل الفئة" else "Edit Category"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(if (lang == "ar") "اسم الفئة *" else "Category Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("cat_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(if (lang == "ar") "الوصف" else "Category Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = RoseRed, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                error = if (lang == "ar") "اسم الفئة مطلوب" else "Category name is required!"
                            } else {
                                onConfirm(name.trim(), desc.trim())
                            }
                        },
                        modifier = Modifier.testTag("cat_confirm_btn")
                    ) {
                        Text(if (lang == "ar") "حفظ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditUnitDialog(
    initUnit: ItemUnit?,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf(initUnit?.name ?: "") }
    var desc by remember { mutableStateOf(initUnit?.description ?: "") }

    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initUnit == null) {
                        if (lang == "ar") "إضافة وحدة قياس" else "Add Unit"
                    } else {
                        if (lang == "ar") "تعديل وحدة القياس" else "Edit Unit"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(if (lang == "ar") "اسم الوحدة (مثال: متر، كيلو، حبة) *" else "Unit Symbol/Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("unit_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(if (lang == "ar") "الوصف" else "Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = RoseRed, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                error = if (lang == "ar") "اسم الوحدة مطلوب" else "Unit name is required!"
                            } else {
                                onConfirm(name.trim(), desc.trim())
                            }
                        },
                        modifier = Modifier.testTag("unit_confirm_btn")
                    ) {
                        Text(if (lang == "ar") "حفظ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditItemDialog(
    initItem: Item?,
    categories: List<ItemCategory>,
    units: List<ItemUnit>,
    warehouses: List<Warehouse>,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (
        code: String,
        name: String,
        categoryId: Long?,
        unitId: Long?,
        defaultWarehouseId: Long?,
        purchasePrice: Double,
        salePrice: Double,
        minLimit: Double,
        currentStock: Double,
        notes: String
    ) -> Unit
) {
    var code by remember { mutableStateOf(initItem?.code ?: "") }
    var name by remember { mutableStateOf(initItem?.name ?: "") }
    
    var categoryId by remember { mutableStateOf(initItem?.categoryId) }
    var unitId by remember { mutableStateOf(initItem?.unitId) }
    var defaultWarehouseId by remember { mutableStateOf(initItem?.defaultWarehouseId) }

    var purchasePriceStr by remember { mutableStateOf(initItem?.purchasePrice?.toString() ?: "0.0") }
    var salePriceStr by remember { mutableStateOf(initItem?.salePrice?.toString() ?: "0.0") }
    
    var minLimitStr by remember { mutableStateOf(initItem?.minLimit?.toString() ?: "5.0") }
    var currentStockStr by remember { mutableStateOf(initItem?.currentStock?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(initItem?.notes ?: "") }

    var error by remember { mutableStateOf<String?>(null) }

    // Dropdown expanded states
    var catExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var whseExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (initItem == null) {
                        if (lang == "ar") "إضافة صنف جديد" else "Create New Product/Item"
                    } else {
                        if (lang == "ar") "تعديل الصنف" else "Update Item Profiles"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; error = null },
                            label = { Text(if (lang == "ar") "اسم الصنف *" else "Item Descriptor Name *") },
                            modifier = Modifier.fillMaxWidth().testTag("item_dialog_name"),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text(if (lang == "ar") "الرمز / الباركود" else "SKU / Barcode ID") },
                            modifier = Modifier.fillMaxWidth().testTag("item_dialog_code"),
                            singleLine = true
                        )
                    }

                    // Dropdown for Category
                    item {
                        ExposedDropdown(
                            label = if (lang == "ar") "الفئة" else "Category Group",
                            selectedText = categories.find { it.id == categoryId }?.name ?: (if (lang == "ar") "غير مصنّف" else "Uncategorized"),
                            expanded = catExpanded,
                            onExpandChange = { catExpanded = it },
                            onDismiss = { catExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (lang == "ar") "غير مصنّف" else "Uncategorized") },
                                onClick = { categoryId = null; catExpanded = false }
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { categoryId = cat.id; catExpanded = false }
                                )
                            }
                        }
                    }

                    // Dropdown for Unit
                    item {
                        ExposedDropdown(
                            label = if (lang == "ar") "وحدة القياس" else "Measurement Unit",
                            selectedText = units.find { it.id == unitId }?.name ?: (if (lang == "ar") "غير محدد" else "Not Specified"),
                            expanded = unitExpanded,
                            onExpandChange = { unitExpanded = it },
                            onDismiss = { unitExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (lang == "ar") "غير محدد" else "Not Specified") },
                                onClick = { unitId = null; unitExpanded = false }
                            )
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.name) },
                                    onClick = { unitId = u.id; unitExpanded = false }
                                )
                            }
                        }
                    }

                    // Dropdown for Warehouse
                    item {
                        ExposedDropdown(
                            label = if (lang == "ar") "المستودع الافتراضي" else "Default Storage Warehouse",
                            selectedText = warehouses.find { it.id == defaultWarehouseId }?.name ?: (if (lang == "ar") "غير محدد" else "None"),
                            expanded = whseExpanded,
                            onExpandChange = { whseExpanded = it },
                            onDismiss = { whseExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (lang == "ar") "غير محدد" else "None") },
                                onClick = { defaultWarehouseId = null; whseExpanded = false }
                            )
                            warehouses.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = { defaultWarehouseId = w.id; whseExpanded = false }
                                )
                            }
                        }
                    }

                    // Purchase and Sale Prices
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = purchasePriceStr,
                                onValueChange = { purchasePriceStr = it },
                                label = { Text(if (lang == "ar") "سعر الشراء" else "Purchase Price") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("purchase_price_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = salePriceStr,
                                onValueChange = { salePriceStr = it },
                                label = { Text(if (lang == "ar") "سعر البيع" else "Sale Price") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("sale_price_input"),
                                singleLine = true
                            )
                        }
                    }

                    // Stocks and Alert Limit
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = currentStockStr,
                                onValueChange = { currentStockStr = it },
                                label = { Text(if (lang == "ar") "الكمية المتوفرة" else "Initial Stock") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("initial_stock_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = minLimitStr,
                                onValueChange = { minLimitStr = it },
                                label = { Text(if (lang == "ar") "حد إعادة الطلب" else "Minimum Limit") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("min_limit_input"),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (lang == "ar") "ملاحظات إضافية" else "Notes / Storage Descriptions") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (error != null) {
                    Text(error!!, color = RoseRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                error = if (lang == "ar") "اسم الصنف مطلوب" else "Item name is required!"
                            } else {
                                val purPrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
                                val sPrice = salePriceStr.toDoubleOrNull() ?: 0.0
                                val minL = minLimitStr.toDoubleOrNull() ?: 5.0
                                val curS = currentStockStr.toDoubleOrNull() ?: 0.0
                                onConfirm(
                                    code.trim(),
                                    name.trim(),
                                    categoryId,
                                    unitId,
                                    defaultWarehouseId,
                                    purPrice,
                                    sPrice,
                                    minL,
                                    curS,
                                    notes.trim()
                                )
                            }
                        },
                        modifier = Modifier.testTag("item_confirm_btn")
                    ) {
                        Text(if (lang == "ar") "حفظ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ExposedDropdown(
    label: String,
    selectedText: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                .clickable { onExpandChange(!expanded) }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedText, style = MaterialTheme.typography.bodyMedium)
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "",
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismiss,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                content()
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
            ) {
                Text(if (lang == "ar") "حذف" else "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == "ar") "إلغاء" else "Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
