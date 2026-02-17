package ai.openclaw.android

enum class ThemeMode(val rawValue: String) {
  System("system"),
  Light("light"),
  Dark("dark"),
  ;

  companion object {
    fun fromRawValue(raw: String?): ThemeMode {
      return entries.firstOrNull { it.rawValue == raw?.trim()?.lowercase() } ?: System
    }
  }
}

