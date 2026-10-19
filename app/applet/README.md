# 💎 Finance David — Личные Финансы & ИИ-Ассистент (Android)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room-4285F4.svg?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Gemini AI](https://img.shields.io/badge/AI-Google%20Gemini-8E44AD.svg?style=flat&logo=google)](https://ai.google.dev/)
[![Build APK](https://github.com/actions/workflows/build-apk.yml/badge.svg)](../../actions)

**Finance David** — современное, стильное и функциональное мобильное приложение для управления личными финансами, бюджетом и накоплениями со встроенным умным ИИ-ассистентом **Дэвидом** на базе **Google Gemini API** и системой **живого голосового ввода** финансовых операций.

---

## ✨ Основные Особенности

### 🎙️ Живой Голосовой Ввод Операций
- **Морфинг кнопки `+` (FAB):** 
  - Нажатие (`onClick`) — открытие экрана ручного ввода транзакции.
  - Долгое зажатие (`onLongClick`) — плавный морфинг плавающей кнопки в компактную нижнюю капсулу с тактильной вибрацией (`HapticFeedback`).
- **Движущийся Неоновый Визуализатор (`MovingNeonGlowVisualizer`):**
  - Бесконечный анимационный градиент (`NeonEmerald`, `NeonIndigo`, `NeonCyan`, `NeonRose`) c мягким размытием (`BlurMaskFilter`).
  - Ореол микрофона и световая полоса в реальном времени динамически меняют радиус и яркость в зависимости от уровня громкости голоса (`rmsDb`).
- **Интеллектуальный Парсинг Речи (`VoiceParserHelper`):**
  - Распознавание сложных разговорных фраз («такси 450 рублей и кофе 220», «получил аванс 15 тысяч», «зарплата полторы тысячи»).
  - Автоматическое определение суммы, направления (Доход/Расход) и соответствующей категории.
- **Динамическое Пружинное Расширение:**
  - При диктовке нескольких операций окно плавно растет вверх (`animateContentSize` с `spring`), отображая аккуратный список карточек `ParsedOperationItem` для сохранения в 1 клик.

### 🤖 ИИ-Ассистент Дэвид (Gemini AI)
- Персональный финансовый консультант на базе **Google Gemini 2.0 / 1.5**.
- Глубокий анализ истории доходов, расходов, баланса и категорий.
- Персонализированные советы по экономии, оптимизации бюджета и достижению накоплений.

### 📊 Учет Финансов & Аналитика
- Мгновенный добавление, редактирование и удаление транзакций.
- Наглядная аналитика расходов и доходов по категориям и периодам.
- Управление категориями (Еда, Транспорт, Жилье, Зарплата, Инвестиции и др.).

### 🎯 Финансовые Цели & Накопления
- Создание целей накопления (на отпуск, технику, подушку безопасности).
- Интерактивный прогресс-бар, отслеживание оставшейся суммы и даты достижения.

### 🔒 Локальное Хранилище & Приватность
- База данных **Room (SQLite)** гарантирует высокую скорость работы и доступ ко всем данным оффлайн.
- Безопасное хранение ключей API в `BuildConfig` без утечки в публичные репозитории.

---

## 🛠️ Технологический Стек

- **Язык:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Архитектура:** Clean Architecture / MVVM + Unidirectional Data Flow (UDF)
- **Асинхронность & Реактивность:** Kotlin Coroutines & `StateFlow`
- **Локальная БД:** Jetpack Room Database
- **Распознавание речи:** Native `android.speech.SpeechRecognizer`
- **ИИ Интеграция:** Google Gemini API (Ktor / REST Client)
- **Навигация:** Jetpack Navigation Compose
- **Сборка:** Gradle (Kotlin DSL `.gradle.kts`)

---

## 🚀 Автоматическая Сборка APK (GitHub Actions)

В проекте настроен автоматический CI/CD workflow (`.github/workflows/build-apk.yml`).

### Как скачать готовый APK:
1. Перейдите во вкладку **Actions** в верхней панели репозитория GitHub.
2. Выберите последний запуск workflow **Build Android APK**.
3. Внизу страницы в разделе **Artifacts** скачайте архив `finance-david-debug-apk`, распакуйте его и установите `.apk` на ваше устройство.

### Публикация релиза (Release):
При создании и пуше Git-тега версии (например, `v1.0.0`) GitHub Actions автоматически создаст официальный релиз во вкладке **Releases** с прикреплённым исполняемым APK-файлом:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 💻 Сборка и Запуск Проекта

### Требования
- Android Studio Ladybug (2024.2.1+) или новее
- JDK 17
- Устройство или эмулятор с Android 8.0+ (API level 26+)

### Шаги установки

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/your-username/finance-david.git
   cd finance-david
   ```

2. **Настройте API ключ Gemini (Опционально):**
   Скопируйте файл `.env.example` в `.env` или укажите ключ `GEMINI_API_KEY` в переменных окружения:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Соберите и запустите проект:**
   ```bash
   # Запуск сборки Debug APK через Gradle
   gradle assembleDebug
   ```

---

## 📱 Скриншоты и Интерфейс

| Главный экран & Дэвид | Голосовой ввод & Неон | Цели & Накопления |
|:---:|:---:|:---:|
| *Баланс, категории и аналитика* | *Живая звуковая волна и распознавание* | *Отслеживание прогресса накоплений* |

---

## 📄 Лицензия

Проект распространяется под лицензией **MIT**. Подробности в файле [LICENSE](LICENSE).
