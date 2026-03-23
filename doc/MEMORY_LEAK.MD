# Memory Leak Report — cpu-info

> Phân tích 76 file Kotlin. Tổng cộng **7 memory leak** đã được tìm thấy và fix.

---

## 🔴 Critical (đã fix)

### 1. `VMSensorsInfo` — SensorListener không unregister trong `onCleared()`

**File:** `feat/infor/sensor/VMSensorsInfo.kt`

| | Trước | Sau |
|--|-----|-----|
| Thread | Raw `Thread { ... }.start()` | `viewModelScope.launch(dispatchersProvider.io)` |
| Cleanup | Phụ thuộc vào Fragment gọi `stopProvidingData()` | `onCleared()` luôn gọi `sensorManager.unregisterListener(this)` |

```kotlin
// THÊM VÀO
override fun onCleared() {
    super.onCleared()
    sensorManager.unregisterListener(this)
}
```

---

### 2. `FrmApplications` — BroadcastReceiver đăng ký sai lifecycle

**File:** `feat/app/FrmApplications.kt`

| | Trước | Sau |
|--|-----|-----|
| Register | `onCreate()` | `onStart()` |
| Unregister | `onDestroy()` | `onStop()` |

**Lý do:** Fragment có thể detach/reattach mà không destroy → receiver bị đăng ký nhiều lần.

---

### 3. `FrmNewApplications` — BroadcastReceiver đăng ký sai lifecycle

**File:** `feat/app/FrmNewApplications.kt`

Cùng fix như #2 — `onCreate`/`onDestroy` → `onStart`/`onStop`.

---

## 🟠 High (đã fix)

### 4. `SplashActivity` — `postDelayed` giữ Activity reference

**File:** `feat/SplashActivity.kt`

```kotlin
// TRƯỚC — anonymous lambda, không thể cancel
window.decorView.postDelayed({ finish() }, 300)

// SAU — named Runnable, cancel được trong onDestroy
private val delayedFinishRunnable = Runnable { finish() }

window.decorView.postDelayed(delayedFinishRunnable, 300)

override fun onDestroy() {
    window.decorView.removeCallbacks(delayedFinishRunnable)
    super.onDestroy()
}
```

---

### 5. `BaseActivity` — ReviewManager callback giữ Activity reference

**File:** `BaseActivity.kt`

```kotlin
// TRƯỚC
request.addOnCompleteListener { task -> ... }

// SAU — pass Activity làm LifecycleOwner → tự unregister khi Activity destroyed
request.addOnCompleteListener(this) { task -> ... }
```

---

## 🟡 Medium (đã fix)

### 6. `VMSensorsInfo` — Raw Thread với race condition (xem fix #1)

Raw Thread không lifecycle-aware và có thể race giữa register/unregister.
→ Đã fix cùng với issue #1 bằng cách dùng `viewModelScope.launch`.

---

### 7. `ActHost` — NavController listener không được remove

**File:** `feat/ActHost.kt`

```kotlin
// TRƯỚC — anonymous lambda, không có cách remove
navController.addOnDestinationChangedListener { _, destination, _ -> ... }

// SAU — lưu reference vào property
private val destinationChangedListener =
    NavController.OnDestinationChangedListener { _, destination, _ ->
        setToolbarTitleAndElevation(destination.label.toString())
        rateAppInApp(BuildConfig.DEBUG)
    }

// onDestroy
navController.removeOnDestinationChangedListener(destinationChangedListener)
```

---

## Không fix (ngoài scope)

| File | Vấn đề | Ghi chú |
|------|--------|---------|
| `ServiceStorageUsage` | Deprecated `IntentService` + hidden API không hoạt động trên Android O+ | Cần viết lại bằng WorkManager, nhưng service đã bị guard bằng `runOnApiBelow(O)` |

---

## Tóm tắt

| # | File | Mức | Loại leak | Trạng thái |
|---|------|-----|-----------|------------|
| 1 | `VMSensorsInfo` | 🔴 | SensorListener không unregister | ✅ Fixed |
| 2 | `FrmApplications` | 🔴 | BroadcastReceiver lifecycle sai | ✅ Fixed |
| 3 | `FrmNewApplications` | 🔴 | BroadcastReceiver lifecycle sai | ✅ Fixed |
| 4 | `SplashActivity` | 🟠 | postDelayed giữ Activity ref | ✅ Fixed |
| 5 | `BaseActivity` | 🟠 | ReviewManager callback leak | ✅ Fixed |
| 6 | `VMSensorsInfo` | 🟡 | Raw Thread race condition | ✅ Fixed |
| 7 | `ActHost` | 🟡 | NavController listener không remove | ✅ Fixed |
