# Вирішення DNS проблеми в Android Emulator

## Проблема
Емулятор Android не може розрезолвити домени `*.mcp.inevitable.fyi` та `*.fly.dev`:
```
java.net.UnknownHostException: Unable to resolve host "echo.mcp.inevitable.fyi": No address associated with hostname
```

## Рішення 1: Налаштування DNS через ADB (Рекомендовано)

### Крок 1: Відкрийте термінал/командний рядок

### Крок 2: Виконайте команди для налаштування Google DNS

```bash
# Підключитися до емулятора через adb
adb root

# Встановити Google DNS
adb shell settings put global private_dns_mode hostname
adb shell settings put global private_dns_default_hostname dns.google

# Або використати публічні DNS сервери безпосередньо
adb shell "setprop net.dns1 8.8.8.8"
adb shell "setprop net.dns2 8.8.4.4"

# Перезапустити мережу
adb shell "svc wifi disable"
adb shell "svc wifi enable"
```

### Крок 3: Перезапустіть додаток

## Рішення 2: Перезапуск емулятора з холодним завантаженням

1. Закрийте емулятор повністю
2. В Android Studio: `Tools -> Device Manager`
3. Клікніть на меню емулятора (три крапки)
4. Виберіть `Cold Boot Now`
5. Зачекайте поки емулятор запуститься
6. Запустіть додаток знову

## Рішення 3: Використання локального MCP сервера для тестування

Якщо проблема з DNS не вирішується, можна налаштувати локальний MCP сервер:

### Крок 1: Встановити MCP сервер локально

```bash
# Клонувати репозиторій з прикладом MCP сервера
git clone https://github.com/kitemcp/public-mcp-servers.git
cd public-mcp-servers

# Запустити один із серверів (наприклад, echo server)
npm install
npm start
```

### Крок 2: Використовувати IP емулятора

В додатку використовуйте адресу `http://10.0.2.2:3000` (для емулятора Android) замість публічного URL.

## Рішення 4: Налаштування DNS в Emulator Settings

1. Відкрийте налаштування емулятора (Settings during runtime)
2. Перейдіть в Settings -> Network & Internet -> Advanced -> Private DNS
3. Виберіть "Private DNS provider hostname"
4. Введіть: `dns.google`
5. Збережіть та перезапустіть емулятор

## Рішення 5: Зміна налаштувань емулятора в AVD Manager

1. Відкрийте AVD Manager в Android Studio
2. Клікніть Edit (іконка олівця) на вашому емуляторі
3. Клікніть "Show Advanced Settings"
4. В розділі Network:
   - Latency: None
   - Speed: Full
5. Збережіть та перезапустіть емулятор

## Перевірка DNS

Після застосування будь-якого рішення, перевірте DNS:

```bash
# Перевірити чи працює DNS
adb shell "ping -c 4 echo.mcp.inevitable.fyi"

# Або через nslookup
adb shell "nslookup echo.mcp.inevitable.fyi 8.8.8.8"
```

## Якщо нічого не допомагає

Використайте реальний пристрій замість емулятора. На реальних пристроях проблем з DNS зазвичай не виникає.

## Тестування MCP інтеграції

Після вирішення DNS проблеми:

1. Відкрийте MCP Tools екран
2. Підключіться до Echo Server (`https://echo.mcp.inevitable.fyi/mcp`)
3. Перевірте що з'явився список інструментів
4. Поверніться в чат
5. Спробуйте запитати Claude щось що потребує використання MCP tools

Приклад запиту для тестування:
```
Привіт! Які у тебе є інструменти?
```

Claude автоматично виявить доступні MCP tools і зможе їх використовувати.
