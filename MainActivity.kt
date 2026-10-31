package com.safeguard.app

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF102A43)
private val Teal = Color(0xFF087E8B)
private val Pale = Color(0xFFF2F6FA)

class MainActivity : ComponentActivity() {
    private val callRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeGuardApp(
                context = this,
                requestCallRole = { requestCallScreeningRole() }
            )
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                callRoleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        }
    }
}

private fun prefs(context: Context) = context.getSharedPreferences("safeguard_local", Context.MODE_PRIVATE)
private fun getBlocked(context: Context) = prefs(context).getStringSet("blocked_numbers", emptySet())?.toList()?.sorted() ?: emptyList()
private fun addBlocked(context: Context, number: String) {
    val set = prefs(context).getStringSet("blocked_numbers", emptySet())?.toMutableSet() ?: mutableSetOf()
    set.add(number.trim())
    prefs(context).edit().putStringSet("blocked_numbers", set).apply()
}
private fun removeBlocked(context: Context, number: String) {
    val set = prefs(context).getStringSet("blocked_numbers", emptySet())?.toMutableSet() ?: mutableSetOf()
    set.remove(number)
    prefs(context).edit().putStringSet("blocked_numbers", set).apply()
}

@Composable
fun SafeGuardApp(context: Context, requestCallRole: () -> Unit) {
    var page by remember { mutableStateOf("home") }
    var refresh by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, secondary = Navy, background = Pale)) {
        Surface(Modifier.fillMaxSize(), color = Pale) {
            when (page) {
                "sms" -> SmsPage { page = "home" }
                "call" -> CallPage(context, requestCallRole, { refresh++ }) { page = "home" }
                "bank" -> BankPage { page = "home" }
                "link" -> LinkPage { page = "home" }
                "apps" -> AppPermissionPage(context) { page = "home" }
                "device" -> DevicePage(context) { page = "home" }
                "help" -> HelpPage { page = "home" }
                else -> HomePage(
                    blockedCount = getBlocked(context).size,
                    onSms = { page = "sms" }, onCall = { page = "call" },
                    onBank = { page = "bank" }, onLink = { page = "link" },
                    onApps = { page = "apps" }, onDevice = { page = "device" },
                    onHelp = { page = "help" }
                )
            }
        }
    }
}

@Composable
fun Header(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
        Column {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (onBack == null) Text("আপনার ডিজিটাল নিরাপত্তা", color = Color(0xFFD7E8F2), fontSize = 13.sp)
        }
    }
    Divider(color = Color(0xFF31536D))
}

@Composable
fun Shell(title: String, back: () -> Unit, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Navy)) { Header(title, back) }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), content = body)
    }
}

@Composable
fun HomePage(blockedCount: Int, onSms: () -> Unit, onCall: () -> Unit, onBank: () -> Unit, onLink: () -> Unit, onApps: () -> Unit, onDevice: () -> Unit, onHelp: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(Navy)) { Header("🛡️ SafeGuard") }
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Security Dashboard", color = Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("$blockedCountটি নম্বর আপনার ব্যক্তিগত Block List-এ আছে", color = Color.DarkGray, fontSize = 13.sp)
                    Text("ফোনের নিরাপত্তা নিয়মিত পরীক্ষা করুন।", color = Teal, fontSize = 12.sp)
                }
            }
            Feature("📩", "SMS নিরাপত্তা পরীক্ষা", "মেসেজ পেস্ট করে ঝুঁকির লক্ষণ দেখুন", onSms)
            Feature("📞", "কল নিরাপত্তা", "নিজের Block List ও Call Screening সেট করুন", onCall)
            Feature("🏦", "ব্যাংক সুরক্ষা পরীক্ষা", "ব্যাংকিং নিরাপত্তার চেকলিস্ট", onBank)
            Feature("🔗", "সন্দেহজনক লিংক পরীক্ষা", "URL-এর সাধারণ ঝুঁকির লক্ষণ দেখুন", onLink)
            Feature("📱", "App Permission Checker", "Android-এর অ্যাপ সেটিংস পরীক্ষা করুন", onApps)
            Feature("🛡️", "Phone Security Checkup", "নিরাপত্তা সেটিংস খুলুন", onDevice)
            Feature("🚨", "জরুরি সাহায্য", "প্রতারণার পর করণীয়", onHelp)
            Text("SafeGuard কোনো অ্যান্টিভাইরাস নয় এবং শতভাগ সুরক্ষার নিশ্চয়তা দেয় না।", fontSize = 11.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun Feature(icon: String, title: String, subtitle: String, click: () -> Unit) {
    Card(onClick = click, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 27.sp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Navy, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.DarkGray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Teal)
        }
    }
}

@Composable
fun SmsPage(back: () -> Unit) {
    var message by remember { mutableStateOf("") }; var result by remember { mutableStateOf("") }
    Shell("SMS Scam Alert", back) {
        Text("SMS-এর লেখা এখানে পেস্ট করুন। SafeGuard আপনার ইনবক্স নিজে থেকে পড়ে না।", color = Navy)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(message, { message = it }, Modifier.fillMaxWidth(), minLines = 5, label = { Text("SMS text") })
        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            val s = message.lowercase()
            val words = listOf("otp", "urgent", "blocked", "verify", "click", "http", "account", "kyc", "পিন", "ওটিপি", "লিংক", "বন্ধ", "পুরস্কার")
            val hits = words.filter { s.contains(it) }
            result = when {
                s.isBlank() -> "আগে SMS পেস্ট করুন।"
                hits.isNotEmpty() -> "⚠️ ঝুঁকির শব্দ পাওয়া গেছে: ${hits.joinToString()}। লিংকে ক্লিক করবেন না; ব্যাংকের অফিসিয়াল নম্বরে যাচাই করুন।"
                else -> "পরিচিত ঝুঁকির শব্দ পাওয়া যায়নি। তবুও প্রেরক যাচাই না করে বিশ্বাস করবেন না।"
            }
        }, Modifier.fillMaxWidth()) { Text("SMS পরীক্ষা করুন") }
        if (result.isNotBlank()) Text(result, Modifier.padding(top = 14.dp), color = Navy, fontWeight = FontWeight.SemiBold)
        Text("এটি সাধারণ keyword check; নিশ্চিত scam detection নয়।", Modifier.padding(top = 12.dp), fontSize = 12.sp)
    }
}

@Composable
fun LinkPage(back: () -> Unit) {
    var url by remember { mutableStateOf("") }; var result by remember { mutableStateOf("") }
    Shell("Link Safety Scanner", back) {
        Text("লিংক পেস্ট করুন। SafeGuard লিংকটি খুলবে না।", color = Navy)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("https://...") })
        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            val s = url.trim().lowercase()
            val flags = listOf("bit.ly", "tinyurl", "ngrok", "login-", "verify-", ".xyz", ".top", ".click", "xn--", "@")
            result = when {
                s.isBlank() -> "একটি URL লিখুন।"
                !(s.startsWith("https://") || s.startsWith("http://")) -> "URL-এর ফরম্যাট অস্বাভাবিক।"
                flags.any { s.contains(it) } -> "⚠️ সন্দেহজনক বৈশিষ্ট্য পাওয়া গেছে। লিংকটি খুলবেন না; অফিসিয়াল সাইট নিজে লিখুন।"
                else -> "এই সাধারণ পরীক্ষায় পরিচিত ঝুঁকির লক্ষণ মেলেনি। নিরাপদ হওয়ার নিশ্চয়তা নয়।"
            }
        }, Modifier.fillMaxWidth()) { Text("লিংক পরীক্ষা করুন") }
        if (result.isNotBlank()) Text(result, Modifier.padding(top = 14.dp), color = Navy, fontWeight = FontWeight.SemiBold)
        Text("এটি offline heuristic scanner; live blacklist বা malware scan নয়।", Modifier.padding(top = 12.dp), fontSize = 12.sp)
    }
}

@Composable
fun CallPage(context: Context, requestRole: () -> Unit, refresh: () -> Unit, back: () -> Unit) {
    var number by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(getBlocked(context)) }
    Shell("Call Protection", back) {
        Text("আপনার Block List-এ থাকা নম্বরের incoming call Android Call Screening role সক্রিয় থাকলে reject হবে।", color = Navy)
        Spacer(Modifier.height(10.dp))
        Button(onClick = requestRole, Modifier.fillMaxWidth()) { Text("Call Screening চালু / অনুমতি দিন") }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(number, { number = it }, Modifier.fillMaxWidth(), label = { Text("ফোন নম্বর") })
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            if (number.trim().length >= 5) { addBlocked(context, number); number = ""; list = getBlocked(context); refresh() }
        }, Modifier.fillMaxWidth()) { Text("Block List-এ যোগ করুন") }
        Spacer(Modifier.height(16.dp))
        Text("আপনার Block List", fontWeight = FontWeight.Bold, color = Navy)
        if (list.isEmpty()) Text("এখনও কোনো নম্বর যোগ করা হয়নি।")
        list.forEach { n ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(n, Modifier.weight(1f), color = Navy)
                    TextButton(onClick = { removeBlocked(context, n); list = getBlocked(context); refresh() }) { Text("মুছুন") }
                }
            }
        }
        Text("এটি সব unknown/spoofed call শনাক্ত করে না। Block List-এর নম্বরই ব্লক করার চেষ্টা করে।", Modifier.padding(top = 12.dp), fontSize = 12.sp)
    }
}

@Composable
fun BankPage(back: () -> Unit) {
    val items = listOf("শক্তিশালী Screen Lock চালু", "Android ও ব্যাংকিং অ্যাপ আপডেট", "অজানা উৎস থেকে APK ইনস্টল বন্ধ", "অপ্রয়োজনীয় Accessibility access পর্যালোচনা", "ব্যাংকিং অ্যাপে PIN/বায়োমেট্রিক চালু", "OTP, UPI PIN, CVV কাউকে না বলা", "ব্যাংকের অফিসিয়াল অ্যাপ/ওয়েবসাইট ব্যবহার")
    val checked = remember { mutableStateListOf<Boolean>().apply { repeat(items.size) { add(false) } } }
    Shell("Banking Safety Check", back) {
        Text("প্রতিটি বিষয় যাচাই করে টিক দিন।", color = Navy)
        items.forEachIndexed { i, item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked[i], { checked[i] = it })
                    Text(item, Modifier.weight(1f), color = Navy)
                }
            }
        }
        Text("${checked.count { it }} / ${items.size} সম্পন্ন", color = Teal, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppPermissionPage(context: Context, back: () -> Unit) {
    Shell("App Permission Checker", back) {
        Text("Android-এর Permission Manager-এ গিয়ে Camera, Microphone, Location, SMS ও Contacts permission পর্যালোচনা করুন। অপ্রয়োজনীয় access বন্ধ করুন।", color = Navy)
        Spacer(Modifier.height(14.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Installed Apps সেটিংস খুলুন") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Privacy সেটিংস খুলুন") }
        Text("Android সংস্করণ ও ফোনের নির্মাতাভেদে সেটিংসের নাম আলাদা হতে পারে।", Modifier.padding(top = 12.dp), fontSize = 12.sp)
    }
}

@Composable
fun DevicePage(context: Context, back: () -> Unit) {
    Shell("Phone Security Checkup", back) {
        Text("নিচের Android সেটিংসগুলো নিজে পরীক্ষা করুন। SafeGuard সিস্টেমের গোপন নিরাপত্তা অবস্থা পড়তে পারে না।", color = Navy)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Security Settings খুলুন") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("System Settings খুলুন") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))) }, Modifier.fillMaxWidth()) { Text("এই অ্যাপের install permission দেখুন") }
        Text("Play Protect চালু রাখুন, Android আপডেট দিন এবং অজানা APK ইনস্টল করবেন না।", Modifier.padding(top = 14.dp), color = Navy)
    }
}

@Composable
fun HelpPage(back: () -> Unit) {
    Shell("Emergency Help", back) {
        Text("প্রতারণার শিকার হলে দ্রুত করণীয়", color = Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        listOf(
            "ব্যাংকের অফিসিয়াল নম্বরে ফোন করে কার্ড/অ্যাকাউন্ট/UPI সাময়িকভাবে ব্লক করুন।",
            "অন্য নিরাপদ ডিভাইস থেকে ব্যাংকিং পাসওয়ার্ড বদলান।",
            "OTP, UPI PIN, ATM PIN, CVV বা screen-sharing access কাউকে দেবেন না।",
            "সন্দেহজনক remote-access অ্যাপ সরান এবং Accessibility permission পরীক্ষা করুন।",
            "লেনদেনের প্রমাণ, নম্বর, SMS ও স্ক্রিনশট সংরক্ষণ করুন।",
            "ভারতে আর্থিক সাইবার প্রতারণা হলে দ্রুত 1930 নম্বরে যোগাযোগ করুন এবং cybercrime.gov.in-এ রিপোর্ট করুন।"
        ).forEachIndexed { i, item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text("${i + 1}. $item", Modifier.padding(13.dp), color = Navy)
            }
        }
        Text("জরুরি নম্বর/রিপোর্টিং নিয়ম পরিবর্তিত হতে পারে; সরকারি উৎসে যাচাই করুন।", Modifier.padding(top = 10.dp), fontSize = 12.sp)
    }
}
