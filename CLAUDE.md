# Budget — приложение для учёта личного бюджета (Android)

## Стек
- Kotlin, Jetpack Compose, Material 3
- Архитектура: MVVM (ViewModel + StateFlow), один Activity, Navigation Compose
- Хранение: Room (SQLite), всё офлайн, без бэкенда
- DI: Hilt
- Графики: Vico (compose) — либо рисовать на Canvas, если проще
- minSdk 26, targetSdk — актуальный, язык интерфейса — русский (строки только в strings.xml)

## Структура пакетов
- `data/` — Room: entities, DAO, database, repository
- `domain/` — модели и use-case'ы (если нужны)
- `ui/expenses`, `ui/income`, `ui/wallets`, `ui/analytics` — экраны и ViewModel
- `ui/navigation`, `ui/theme`, `ui/components`

## Модель данных
- `Wallet`: id, name, type (CARD | CASH), initialBalance
- `Category`: id, name, icon/color (предзаполненный список: Продукты, Транспорт, Жильё, Кафе, Здоровье, Развлечения, Покупки, Прочее)
- `Transaction`: id, type (EXPENSE | INCOME), amount, description, categoryId (null для дохода), walletId, date
- Баланс кошелька = initialBalance + доходы − расходы по нему. Не хранить отдельно, считать запросом.

## Правила
- Деньги хранить как `Long` в копейках, никаких Double/Float.
- Форматирование сумм — через единую функцию (`1 234,50 ₽`).
- Расход нельзя сохранить без суммы > 0 и выбранного кошелька.
- Не добавлять лишние зависимости без необходимости.
- Один экран = один ViewModel с одним UiState.
- Поддержка тёмной темы.

## Команды
- Сборка: `./gradlew assembleDebug`
- Линт: `./gradlew lint`
- Тесты: `./gradlew test`
- Установка на устройство: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Процесс работы
- Небольшими шагами: после каждого шага запускай `./gradlew assembleDebug` и исправляй ошибки.
- Не пиши следующую функцию, пока сборка красная.
- Коммить после каждого рабочего шага.
