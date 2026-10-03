package app.dnsgate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab { Home, Servers, Activity, Settings, Add }

private var autoDone = false
private val LtrStyle = TextStyle(textDirection = TextDirection.Ltr, color = Ink, fontSize = 15.sp)

private fun startVpn(c: Context) { c.startService(Intent(c, DnsVpnService::class.java)) }
private fun stopVpn(c: Context) {
    c.startService(Intent(c, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP))
}

@Composable
fun AppRoot(link: String) {
    val ctx = LocalContext.current
    var tab by remember {
        mutableStateOf(if (link.isNotEmpty() || Store.profiles(ctx).isEmpty()) Tab.Add else Tab.Home)
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF14110A), Bg, Color(0xFF110C00)))
        )
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.Home -> HomeScreen(onAdd = { tab = Tab.Add })
                    Tab.Servers -> ServersScreen(onAdd = { tab = Tab.Add })
                    Tab.Activity -> ActivityScreen()
                    Tab.Settings -> SettingsScreen()
                    Tab.Add -> AddScreen(link) { tab = Tab.Home }
                }
            }
            BottomBar(tab) { tab = it }
        }
    }
}

@Composable
fun BottomBar(tab: Tab, onTab: (Tab) -> Unit) {
    val items = listOf<Triple<Tab, ImageVector, String>>(
        Triple(Tab.Home, Icons.Default.Home, "خانه"),
        Triple(Tab.Servers, Icons.Default.Share, "سرورها"),
        Triple(Tab.Activity, Icons.Default.Menu, "فعالیت"),
        Triple(Tab.Settings, Icons.Default.Settings, "تنظیمات"),
        Triple(Tab.Add, Icons.Default.Add, "افزودن")
    )
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(CardBg).border(1.dp, Line, shape)
            .navigationBarsPadding().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEach { (t, icon, name) ->
            val active = t == tab
            Column(
                Modifier.clip(RoundedCornerShape(14.dp)).clickable { onTab(t) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, name, tint = if (active) Neon else Mut, modifier = Modifier.size(26.dp))
                Text(name, fontSize = 11.sp, color = if (active) Neon else Mut)
            }
        }
    }
}

@Composable
fun NeonButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(NeonBrush).clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
fun Title(t: String) {
    Text(t, style = TextStyle(brush = NeonBrush, fontSize = 26.sp, fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(14.dp))
}

@Composable
fun NeonRing(status: Status, label: String, sub: String, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "ring")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "rot")
    val pulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse")
    val on = status == Status.ON
    val busy = status == Status.CONNECTING
    Box(Modifier.size(270.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2 - 30.dp.toPx()
            val glow = if (on) 0.22f * pulse else if (busy) 0.2f else 0.10f
            for (i in 1..7) {
                drawCircle(Neon.copy(alpha = glow / (i * 0.8f)), radius = r, center = c, style = Stroke(width = (8 + i * 7).dp.toPx()))
            }
            drawCircle(
                Brush.radialGradient(listOf(Neon.copy(alpha = if (on) 0.16f else 0.05f), Color.Transparent), center = c, radius = r),
                radius = r, center = c
            )
            val ring = if (on || busy) Brush.sweepGradient(listOf(Neon, Amber, Orange, Amber, Neon), center = c)
            else Brush.sweepGradient(listOf(Color(0xFFEDEDED), Color(0xFFBFBAA0), Color(0xFFEDEDED)), center = c)
            rotate(if (busy) rot else 0f, pivot = c) {
                if (busy) drawArc(
                    ring, 0f, 270f, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r),
                    style = Stroke(10.dp.toPx(), cap = StrokeCap.Round)
                ) else drawCircle(ring, radius = r, center = c, style = Stroke(10.dp.toPx()))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = if (on) Neon else Ink)
            if (sub.isNotEmpty()) Text(sub, fontSize = 15.sp, color = Mut)
        }
    }
}

@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Neon)
        Text(label, fontSize = 12.sp, color = Mut)
    }
}

@Composable
fun HomeScreen(onAdd: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by VpnState.status.collectAsState()
    val err by VpnState.error.collectAsState()
    val q by VpnState.queries.collectAsState()
    val failed by VpnState.failed.collectAsState()
    var profiles by remember { mutableStateOf(Store.profiles(ctx)) }
    var sel by remember { mutableStateOf(Store.selected(ctx)) }
    var menu by remember { mutableStateOf(false) }
    var ping by remember { mutableStateOf("—") }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(status) {
        while (status == Status.ON) {
            elapsed = (System.currentTimeMillis() - VpnState.startedAt) / 1000
            delay(1000)
        }
        elapsed = 0
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) startVpn(ctx)
    }
    fun connect() {
        val i = VpnService.prepare(ctx)
        if (i != null) launcher.launch(i) else startVpn(ctx)
    }
    LaunchedEffect(Unit) {
        if (!autoDone) {
            autoDone = true
            if (Store.settings(ctx).autoConnect && sel != null && status == Status.OFF && VpnService.prepare(ctx) == null) startVpn(ctx)
        }
    }

    val label = when (status) {
        Status.OFF, Status.ERROR -> "اتصال"
        Status.CONNECTING -> "در حال اتصال"
        Status.ON -> "متصل"
    }
    val sub = if (status == Status.ON) "%02d:%02d:%02d".format(elapsed / 3600, elapsed / 60 % 60, elapsed % 60) else ""
    val text = when (status) {
        Status.OFF -> "قطع است"
        Status.CONNECTING -> "ثبت و بررسی کلید…"
        Status.ON -> "DNS امن فعال است"
        Status.ERROR -> err
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("kankiDNS", Modifier.padding(top = 16.dp), style = TextStyle(brush = NeonBrush, fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            NeonRing(status, label, sub) {
                if (status == Status.ON || status == Status.CONNECTING) stopVpn(ctx)
                else if (sel == null) onAdd() else connect()
            }
            Spacer(Modifier.height(8.dp))
            Text(text, color = if (status == Status.ERROR) Bad else Mut, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        }
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp).clip(shape).background(CardBg).border(1.dp, Line, shape).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(width = 44.dp, height = 4.dp).clip(CircleShape).background(Line))
            Spacer(Modifier.height(12.dp))
            Box {
                Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable { menu = true }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sel?.name ?: "سروری نیست", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Mut)
                }
                DropdownMenu(menu, { menu = false }) {
                    profiles.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = {
                            Store.select(ctx, p.id); sel = p; menu = false; ping = "—"
                        })
                    }
                    DropdownMenuItem(text = { Text("افزودن سرور…", color = Neon) }, onClick = { menu = false; onAdd() })
                }
            }
            Text(sel?.let { "DNS | " + it.host.ifEmpty { it.ip } } ?: "با «افزودن» کد را وارد کنید", color = Mut, fontSize = 14.sp, style = LtrStyle.copy(color = Mut, fontSize = 14.sp))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(ping, "تاخیر", Modifier.weight(1f))
                Stat(q.toString(), "کوئری", Modifier.weight(1f))
                Stat(failed.toString(), "ردشده", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val s = sel ?: return@OutlinedButton
                    ping = "…"
                    scope.launch {
                        val ms = withContext(Dispatchers.IO) { Dns.ping(s.ip.ifEmpty { s.host }) }
                        ping = if (ms < 0) "ناموفق" else "$ms ms"
                    }
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) { Text("تست تاخیر سرور", color = Neon) }
        }
    }
}

@Composable
fun ServersScreen(onAdd: () -> Unit) {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf(Store.profiles(ctx)) }
    var selId by remember { mutableStateOf(Store.selected(ctx)?.id) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Title("سرورها")
        if (list.isEmpty()) Text("هنوز سروری اضافه نشده.", color = Mut)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.id }) { p ->
                val active = p.id == selId
                val shape = RoundedCornerShape(18.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(CardBg)
                        .border(if (active) 2.dp else 1.dp, if (active) Neon else Line, shape)
                        .clickable { Store.select(ctx, p.id); selId = p.id }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (active) NeonBrush else Brush.linearGradient(listOf(Line, Line))),
                        contentAlignment = Alignment.Center
                    ) { if (active) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(p.host.ifEmpty { p.ip }, style = LtrStyle.copy(color = Mut, fontSize = 13.sp))
                    }
                    IconButton(onClick = {
                        Store.remove(ctx, p.id); list = Store.profiles(ctx); selId = Store.selected(ctx)?.id
                    }) { Icon(Icons.Default.Delete, "حذف", tint = Bad) }
                }
            }
        }
        NeonButton("افزودن سرور جدید", Modifier.fillMaxWidth().padding(top = 12.dp), onAdd)
    }
}

@Composable
fun ActivityScreen() {
    val logs by VpnState.logs.collectAsState()
    val q by VpnState.queries.collectAsState()
    val failed by VpnState.failed.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Title("فعالیت زنده")
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Stat(q.toString(), "کل کوئری", Modifier.weight(1f))
            Stat((q - failed).toString(), "موفق", Modifier.weight(1f))
            Stat(failed.toString(), "ناموفق", Modifier.weight(1f))
        }
        if (logs.isEmpty()) Text("با اتصال، دامنه‌هایی که برنامه‌ها می‌پرسند اینجا نمایش داده می‌شود.", color = Mut)
        LazyColumn(Modifier.weight(1f)) {
            items(logs) { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(if (e.ok) Neon else Bad))
                    Text(e.name, Modifier.weight(1f).padding(horizontal = 10.dp), style = LtrStyle, maxLines = 1)
                    Text(e.type, color = Amber, fontSize = 12.sp)
                    Text("  ${e.ms}ms", color = Mut, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(sub, fontSize = 12.sp, color = Mut)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black, checkedTrackColor = Neon, checkedBorderColor = Neon,
                uncheckedThumbColor = Mut, uncheckedTrackColor = Line, uncheckedBorderColor = Line
            )
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Neon, unfocusedBorderColor = Line, cursorColor = Neon,
    focusedTextColor = Ink, unfocusedTextColor = Ink
)

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var st by remember { mutableStateOf(Store.settings(ctx)) }
    fun upd(n: AppSettings) { st = n; Store.saveSettings(ctx, n) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Title("تنظیمات")
        SwitchRow("اتصال خودکار", "هنگام باز شدن برنامه وصل شود", st.autoConnect) { upd(st.copy(autoConnect = it)) }
        SwitchRow("DNS جایگزین", "اگر سرور شما جواب نداد از آدرس زیر استفاده شود", st.fallback) { upd(st.copy(fallback = it)) }
        if (st.fallback) OutlinedTextField(
            st.fallbackIp, { upd(st.copy(fallbackIp = it.trim())) }, Modifier.fillMaxWidth(),
            singleLine = true, textStyle = LtrStyle, colors = fieldColors(), label = { Text("آدرس DNS جایگزین") }
        )
        SwitchRow("ثبت فعالیت", "نمایش دامنه‌ها در تب فعالیت (فقط روی گوشی)", st.keepLog) { upd(st.copy(keepLog = it)) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { VpnState.reset() }, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
            Text("پاک کردن لاگ", color = Neon)
        }
        Spacer(Modifier.weight(1f))
        Text("kankiDNS 1.0.0", color = Mut, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun AddScreen(initial: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var code by remember { mutableStateOf(initial) }
    var err by remember { mutableStateOf("") }
    fun submit() {
        try {
            Store.add(ctx, KeyParser.parse(code))
            onDone()
        } catch (e: IllegalArgumentException) {
            err = e.message ?: "کد معتبر نیست."
        } catch (e: Exception) {
            err = "کد معتبر نیست یا ناقص است."
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Title("ورود با کد")
        Text("کدی را که پنل به شما داده اینجا وارد یا paste کنید.", color = Mut)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            code, { code = it; err = "" }, Modifier.fillMaxWidth().height(150.dp),
            textStyle = LtrStyle, colors = fieldColors(), placeholder = { Text("DG1.… یا dnsgate://…", color = Mut) }
        )
        Text(err, color = Bad, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { clip.getText()?.text?.let { code = it; err = "" } },
            modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) { Text("چسباندن از کلیپ‌بورد", color = Neon) }
        NeonButton("ورود و افزودن", Modifier.fillMaxWidth().padding(top = 10.dp)) { submit() }
    }
}
