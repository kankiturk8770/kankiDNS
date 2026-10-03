# kankiDNS (Android / Kotlin)

اپ اندروید برای اتصال به پنل kankiDNS با «کد ورود».

## ساخت APK با GitHub Actions
1. این پوشه را در یک ریپو بگذارید و روی `main` پوش کنید.
2. در تب **Actions** ورک‌فلو `Build APK` اجرا می‌شود.
3. فایل `kankiDNS.apk` هم در **Releases** و هم در Artifacts همان اجرا قرار می‌گیرد.

(APK از نوع debug است و مستقیم روی گوشی نصب می‌شود.)

## فرمت کد ورود
فایل `KeyParser.kt` دو فرمت را می‌فهمد:

- `dnsgate://HOST/TOKEN?ip=1.2.3.4&name=Ali`
- `DG1.` + base64url از JSON: `{"name":"Ali","host":"dns.example.com","ip":"203.0.113.10","token":"abc"}`

هر وقت پنل کد را تولید کرد، فقط `KeyParser.kt` را با فرمت واقعی هماهنگ کنید.
لینک‌های `dnsgate://` در مرورگر/تلگرام هم مستقیم اپ را باز می‌کنند.

## طرز کار
- `DnsVpnService`: یک VPN فقط برای DNS (آدرس مجازی 10.77.0.1). کوئری‌ها به UDP/53 سرور شما می‌روند؛ بقیه ترافیک از VPN رد نمی‌شود.
- پنل شما whitelist بر اساس IP است؛ ثبت IP کاربر باید طبق روش خود پنل انجام شود (`Portal.touch` در `Dns.kt` جای اتصال به آن است).
- فعلاً فقط IPv4 و UDP پشتیبانی می‌شود.
