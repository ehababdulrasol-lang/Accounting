# نظام التمتير والمقاسات — Sizing & Measurements System

يوفر هذا المستند مرجعاً تكنولوجياً ومحاسبياً شاملاً لكيفية عمل نظام **التمتير والمقاسات** المدمج في التطبيق، والذي يتيح لمديري الحسابات والفنيين تسجيل أبعاد (عرض × ارتفاع) ومساحات الرخام، الزجاج، أو أي مواد أخرى، وحساب قيمتها المالية فورياً، ثم ترحيلها تلقائياً كقيود يومية متوازنة في دفتر الأستاذ العام للعميل.

---

## 🏛️ 1. نظرة عامة والتدفق المحاسبي (Operational Flow)

تم تصميم النظام ليعمل بشكل مستقل أو مترابط بشكل كامل مع شجرة الحسابات العامة (Chart of Accounts) عبر الخطوات التالية:
1. **تسجيل القياسات**: يقوم المستخدم بتحديد الحساب المالي للعميل من قائمة حسابات الذمم والمدينين (التي تبدأ بالرمز `1103`)، أو إدخال اسم يدوي مرن.
2. **إدخال البنود**: تسجيل تفاصيل الأبعاد (العرض × الارتفاع × الكمية) مع سعر المتر لكل بند، حيث يقوم النظام بحساب المساحة الإجمالية والقيمة المالية تلقائياً.
3. **الحفظ كمسودة**: تُحفظ الفاتورة كـ "مسودة تمتير" غير مرحلة مالياً، مما يُمكّن من مراجعتها وتعديلها أو حذفها وسهولة مشاركتها كرسالة نصية منسقة عبر الواتساب.
4. **الترحيل كقيد محاسبي**: بمجرد الضغط على "ترحيل للقيود"، يقوم النظام بإنشاء قيد يومية عام (`JOURNAL`) متوازن آلياً:
   - **الطرف المدين (Debit)**: حساب العميل المستهدف (مثال: ذمم المدار أو ذمم ليبيانا أو أي عميل مضاف تحت `1103`) بقيمة الفاتورة الإجمالية.
   - **الطرف الدائن (Credit)**: حساب إيرادات المبيعات (Sales Income - `4101`) بقيمة الفاتورة الإجمالية.

---

## 🗄️ 2. طبقة بيانات الروم (Room Database Configuration)

يتكون محرك البيانات من جدولين رئيسيين مع تطبيق حماية التحديث المتتالي والربط الأجنبي المتين (Cascading Delete):

### أ. الكيانات البرمجية (Entities)

```kotlin
@Entity(
    tableName = "measurement_headers",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class MeasurementHeader(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val accountId: Long,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val totalMeters: Double = 0.0,
    val totalAmount: Long = 0L,         // مخزن بوحدة الميلي-قرش لضمان الدقة المالية المطلقة
    val isPosted: Boolean = false,
    val voucherHeaderId: Long? = null
)

@Entity(
    tableName = "measurement_lines",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementHeader::class,
            parentColumns = ["id"],
            childColumns = ["headerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MeasurementLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val headerId: Long,
    val itemDescription: String,
    val width: Double,
    val height: Double,
    val quantity: Int,
    val pricePerMeter: Double,
    val totalArea: Double,              // الأمتار الإجمالية = العرض × الارتفاع × الكمية
    val totalAmount: Long               // المبلغ الإجمالي للبند بالمليم/الميلي-قرش
)
```

### ب. واجهة الوصول للبيانات (MeasurementDao)

ضمان سلامة العمليات التعديلية للمقاسات من خلال معاملات ذرية (`@Transaction`):

```kotlin
@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurement_headers ORDER BY date DESC, id DESC")
    fun getAllMeasurementHeadersFlow(): Flow<List<MeasurementHeader>>

    @Query("SELECT * FROM measurement_headers WHERE id = :headerId")
    suspend fun getMeasurementHeaderById(headerId: Long): MeasurementHeader?

    @Query("SELECT * FROM measurement_lines WHERE headerId = :headerId")
    fun getMeasurementLinesForHeaderFlow(headerId: Long): Flow<List<MeasurementLine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: MeasurementHeader): Long

    @Transaction
    suspend fun saveMeasurement(header: MeasurementHeader, lines: List<MeasurementLine>): Long {
        val id = if (header.id == 0L) {
            insertHeader(header)
        } else {
            updateHeader(header)
            deleteLinesForHeader(header.id)
            header.id
        }
        val linesWithHeaderId = lines.map { it.copy(headerId = id) }
        insertLines(linesWithHeaderId)
        return id
    }
}
```

---

## ⚡ 3. إدارة الحالة المحاسبية (Business Logic & ViewModel Integration)

توجد كافة العمليات الحسابية والترسيبية داخل `LedgerViewModel` لضمان فصل منطق العمل عن واجهة المستخدم والحفاظ على البنية أحادية الاتجاه (UDF):

### أ. ترحيل مستند التمتير كقيد محاسبي متوازن

عند استدعاء وظيفة الترحيل، يتم إنشاء مسودة سند عامة وموازنتها بدقة، ثم اعتمادها وترحيلها مباشرة:

```kotlin
fun postMeasurementToLedger(header: MeasurementHeader) {
    viewModelScope.launch {
        try {
            val baseCurrency = repository.getBaseCurrency()
            val activeFy = repository.getActiveFiscalYearsSuspend().firstOrNull() 
                ?: throw IllegalStateException("No active fiscal year.")
            
            val revenueAccount = accounts.value.find { it.accountCode == "4101" } 
                ?: throw IllegalStateException("Revenue account Sales Income (4101) not found.")
            
            // 1. تشكيل سطور القيد المحاسبي المتوازن دائن ومدين
            val voucherLinesList = listOf(
                VoucherLine(
                    headerId = 0,
                    accountId = header.accountId, // العميل المدين
                    debit = header.totalAmount,
                    credit = 0L,
                    currencyId = baseCurrency?.id ?: 1L,
                    exchangeRate = 1.0,
                    amountBase = header.totalAmount,
                    memo = "مستند تمتير ومقاسات #${header.id} للعميل: ${header.customerName}"
                ),
                VoucherLine(
                    headerId = 0,
                    accountId = revenueAccount.id, // حساب المبيعات والإيرادات الدائن
                    debit = 0L,
                    credit = header.totalAmount,
                    currencyId = baseCurrency?.id ?: 1L,
                    exchangeRate = 1.0,
                    amountBase = header.totalAmount,
                    memo = "إيرادات تمتير ومسافات رخام وزجاج للعميل ${header.customerName}"
                )
            )

            val voucherNoCalculated = "M-" + System.currentTimeMillis().toString().takeLast(6)
            val voucherHeader = VoucherHeader(
                voucherNo = voucherNoCalculated,
                date = header.date,
                type = VoucherType.JOURNAL,
                description = "ترحيل الفاتورة التلقائية لمقاسات العميل: ${header.customerName}",
                totalAmountBase = header.totalAmount,
                isPosted = true,
                fiscalYearId = activeFy.id
            )

            // 2. ترحيل السند بشكل متوازن في دليل الذاكرة والأرشفة الذكية للقيود
            val savedVoucherId = repository.saveDraftVoucher(voucherHeader, voucherLinesList)
            val result = repository.postVoucher(savedVoucherId)
            
            if (result is ValidationResult.Error) {
                throw IllegalStateException(result.message)
            }
            
            // 3. تحديث حالة الفاتورة كـ "مرحلة" مع ربطها بالسند المحاسبي المولد
            val updatedHeader = header.copy(isPosted = true, voucherHeaderId = savedVoucherId)
            repository.updateMeasurementHeader(updatedHeader)
            
            // 4. تحديث الأرصدة وبطاقة كشف الحساب الفورية
            repository.recalculateSnapshots()
        } catch (e: Exception) {
            _uiMessage.value = "فشل الترحيل: ${e.localizedMessage}"
        }
    }
}
```

---

## 🎨 4. واجهة المستخدم الرشيقة (Jetpack Compose Screen)

تمت صياغة واجهة `MeasurementsScreen` بمرونة وتجاوب تام طبقاً لأعلى معايير **Material Design 3**:
- **تأثيرات متحركة سلسة (Motion & Animations)**: تضمين `AnimatedVisibility` لتمديد تفاصيل الفاتورة وعرض البنود الفرعية بشكل جذاب وانزلاقي.
- **تجاوب الحقول**: مدعوم بحامي أخطاء يمنع حفظ البنود القيمية الصفرية أو الحقول الفارغة، مع لوحة مفاتيح رقمية للراحة المطلقة.
- **تغليف مبهج وجذاب**: بطاقات ذات حواف دائرية أنيقة بمقدار `14.dp` ونمط لوني ذهبي متوافق للتنبيه على المبالغ والعمليات المحاسبية الشاملة.
- **تصدير ومشاركة فورية**: إمكانية توليد رسائل نصية ذكية واحترافية وإرسالها مباشرة عن طريق خدمة المشاركة للواتساب وحافظة المحمول.

---

## 🛠️ 5. إرشادات التطوير والصيانة المستقبلية

ليظل نظام التمتير متناهي الكفاءة وخالياً من الأخطاء، يُنصح باتباع القواعد التالية:
1. **قيد الحذف**: لا تسمح أبداً بحذف مستند تمتير تم ترحيله مالياً، إلا بعد القيام بإلغاء ترحيل قيد اليومية المرتبط به في البداية لتجنب تفاوت الأرصدة.
2. **سقف المديونية**: في التحديثات القادمة، يوصى بالتحقق من مديونية العميل بفرق إجمالي الفاتورة المعتمدة ومضاهاتها مع سقف المديونية المتاح للمستند قبل إتمام الحفظ أو الترحيل.
3. **الدقة الكسرية**: نوصي بالاحتفاظ دوماً بحجم المساحات كـ `Double` للأمتار والمليمترات، وتحويل المبالغ المالية فوراً وضمنياً عبر دالة `toLong()` مع ضربها في `1000` للحفظ الدقيق في قاعدة البيانات بدون مشاكل الفاصلة العائمة التقليدية.
