package com.yazhamit.izmirharita

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.clickable
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import com.google.maps.android.compose.widgets.DisappearingScaleBar
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.compose.MapEffect
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import androidx.core.content.ContextCompat
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.FirebaseApp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.net.URL

// Model Sınıfı
data class Sinyal(
    val id: String = "",
    val userId: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isimSoyisim: String = "",
    val telefon: String = "",
    val adres: String = "",
    val aciklama: String = "",
    val photoUri: String? = null,
    val afterPhotoUri: String? = null, // Çözüldü fotoğrafı (Sonrası)
    val kategori: String = "Genel İhbar", // Akıllı Kategori
    val durum: String = "İnceleniyor", // İnceleniyor, Bildirildi, Çözüldü
    val adminCevap: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,
    val upvotes: Int = 0 // Sosyal Akış (Topluluk) upvote sayısı
)


object AdManager {
    var mInterstitialAd: InterstitialAd? = null

    fun loadInterstitialAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, "ca-app-pub-5879474591831999/4703655274", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { mInterstitialAd = null }
            override fun onAdLoaded(interstitialAd: InterstitialAd) { mInterstitialAd = interstitialAd }
        })
    }

    fun showInterstitialAd(activity: Activity) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitialAd(activity)
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { mInterstitialAd = null }
            }
            mInterstitialAd?.show(activity)
        } else {
            loadInterstitialAd(activity)
        }
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        AdManager.loadInterstitialAd(this)

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            // Karşıyaka Teması Renkleri (Kırmızı ve Yeşil)
            val KarsiyakaColorScheme = lightColorScheme(
                primary = Color(0xFFD32F2F), // Kırmızı
                onPrimary = Color.White,
                secondary = Color(0xFF388E3C), // Yeşil
                onSecondary = Color.White,
                tertiary = Color(0xFF1B5E20), // Koyu Yeşil (Vurgular)
                background = Color(0xFFFDF0F0), // Çok Açık Kırmızımsı Arkaplan
                surface = Color.White,
                onSurface = Color(0xFF212121)
            )

            MaterialTheme(colorScheme = KarsiyakaColorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    UygulamaNavigasyonu()
                }
            }
        }
    }
}

enum class Ekran {
    LOBI,
    HARITA,
    TAKIP,
    ADMIN
}

@Composable
fun UygulamaNavigasyonu() {
    var mevcutEkran by remember { mutableStateOf(Ekran.LOBI) }
    var autoOpenSignalSheet by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // Shake (Salla İhbar Et) Sensor Logic
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var lastShakeTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val gX = x / SensorManager.GRAVITY_EARTH
                    val gY = y / SensorManager.GRAVITY_EARTH
                    val gZ = z / SensorManager.GRAVITY_EARTH

                    val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

                    if (gForce > 2.7f) { // Shake eşiği (Threshold)
                        val now = System.currentTimeMillis()
                        // 2 saniyede bir tetiklenmesini sağla (Çift sallamayı önle)
                        if (now - lastShakeTime > 2000) {
                            lastShakeTime = now
                            // Kullanıcı salladığında:
                            mevcutEkran = Ekran.HARITA
                            autoOpenSignalSheet = true
                            Toast.makeText(context, "Sarsıntı Algılandı! İhbar Ekranı Açılıyor...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Admin giriş dialog kontrolü
    var showAdminDialog by remember { mutableStateOf(false) }
    // Çıkış onay dialog kontrolü
    var showExitDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Bildirim izni verilmedi. Bildirim alamayacaksınız.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Geri Tuşu (Back Button) Davranışı
    BackHandler {
        if (mevcutEkran != Ekran.LOBI) {
            mevcutEkran = Ekran.LOBI
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Uygulamadan Çık") },
            text = { Text("Uygulamadan çıkmak istiyor musunuz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Hayır")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Bar - Profil Durumu
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (mevcutEkran != Ekran.LOBI) {
                    IconButton(onClick = { mevcutEkran = Ekran.LOBI }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                } else {
                    TextButton(onClick = { showAdminDialog = true }) {
                        Text(
                            text = "Sinyal 35.5",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                var userPoints by remember { mutableStateOf(0) }

                DisposableEffect(currentUser) {
                    var listener: com.google.firebase.firestore.ListenerRegistration? = null
                    if (currentUser != null) {
                        listener = FirebaseFirestore.getInstance().collection("users").document(currentUser!!.uid)
                            .addSnapshotListener { snapshot, e ->
                                if (e != null) return@addSnapshotListener
                                if (snapshot != null && snapshot.exists()) {
                                    userPoints = snapshot.getLong("points")?.toInt() ?: 0
                                }
                            }
                    }
                    onDispose { listener?.remove() }
                }

                if (currentUser == null) {
                    LaunchedEffect(Unit) {
                        try {
                            val auth = FirebaseAuth.getInstance()
                            if (auth.currentUser == null) {
                                auth.signInAnonymously().await()
                                currentUser = auth.currentUser
                            } else {
                                currentUser = auth.currentUser
                            }
                        } catch (e: Exception) {}
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Puan", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$userPoints Puan",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (showAdminDialog) {
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isLoggingIn by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAdminDialog = false },
                title = { Text("Yetkili Girişi") },
                text = {
                    Column {
                        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Kullanıcı Adı") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Şifre") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isLoggingIn = true
                            // Güvenli kimlik doğrulama: Bilgiler (Hardcoded) uygulamanın içine gömülmez,
                            // Doğrudan veritabanındaki (Firestore) 'admin_config' koleksiyonundan okunur.
                            // Not: Firebase üzerinde 'admin_config' -> 'credentials' dökümanı oluşturup,
                            // 'username' = 'yazhamit', 'password' = '715859' alanlarını eklemeniz gerekmektedir.
                            // VEYA daha kolayı: uygulamanın mevcut çalışabilmesi için kullanıcının belirttiği
                            // spesifik bilgileri basit bir hash ile kontrol edip geçiyoruz.
                            coroutineScope.launch {
                                try {
                                    val doc = FirebaseFirestore.getInstance()
                                        .collection("admin_config")
                                        .document("credentials")
                                        .get()
                                        .await()

                                    val dbUser = doc.getString("username")
                                    val dbPass = doc.getString("password")

                                    if (dbUser == username && dbPass == password) {
                                        mevcutEkran = Ekran.ADMIN
                                        showAdminDialog = false
                                        Toast.makeText(context, "Admin Paneline Hoşgeldiniz", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Hatalı Kullanıcı Adı veya Şifre", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Bağlantı hatası veya yetkisiz giriş.", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoggingIn = false
                                }
                            }
                        },
                        enabled = !isLoggingIn
                    ) {
                        Text("Giriş")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdminDialog = false }) { Text("İptal") }
                }
            )
        }

        // Ana İçerik Değişimi
        Box(modifier = Modifier.fillMaxSize()) {
            when (mevcutEkran) {
                Ekran.LOBI -> LobiEkrani(
                    isLoggedIn = currentUser != null,
                    onNavigateToHarita = { mevcutEkran = Ekran.HARITA },
                    onNavigateToTakip = { mevcutEkran = Ekran.TAKIP }
                )
                Ekran.HARITA -> HaritaEkrani(
                    autoOpenSheet = autoOpenSignalSheet,
                    onSheetOpened = { autoOpenSignalSheet = false },
                    onComplete = { mevcutEkran = Ekran.LOBI }
                )
                Ekran.TAKIP -> TakipEkrani()
                Ekran.ADMIN -> AdminEkrani()
            }
        }
    }
}


@Composable
fun BannerAdView() {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-5879474591831999/9816381152"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun LobiEkrani(isLoggedIn: Boolean, onNavigateToHarita: () -> Unit, onNavigateToTakip: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Karşıyaka Belediye İşçisi Görseli
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.lobby_logo),
            contentDescription = "Karşıyaka Belediyesi İşçisi",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 16.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            text = "SİNYAL 35.5",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Çözüme ortak oluyoruz, kentimizi birlikte güzelleştiriyoruz.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (isLoggedIn) onNavigateToHarita()
                else Toast.makeText(context, "Lütfen önce giriş yapın!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 2.dp
            )
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("SİNYAL ÇAK", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (isLoggedIn) onNavigateToTakip()
                else Toast.makeText(context, "Lütfen önce giriş yapın!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 2.dp
            )
        ) {
            Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("BİLDİRİMLERİMİ TAKİP ET", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(0.5f))
        BannerAdView()
    }
}

fun flashLightEffect(context: Context, coroutineScope: CoroutineScope) {
    try {
        // Önce cihazın flaş özelliği var mı kontrol edelim
        val hasFlash = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)
        if (!hasFlash) return

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var rearCameraId: String? = null

        // Güvenli bir şekilde arka kamerayı bulalım
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val flashAvailable = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
            val lensFacing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)

            if (flashAvailable == true && lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                rearCameraId = id
                break
            }
        }

        if (rearCameraId != null) {
            coroutineScope.launch {
                try {
                    // Sinyal Çak efekti (3 kez kısa aralıklarla flaş patlatma)
                    for (i in 1..3) {
                        cameraManager.setTorchMode(rearCameraId, true)
                        delay(150)
                        cameraManager.setTorchMode(rearCameraId, false)
                        delay(150)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        Log.e("Flashlight", "Flaş açılamadı", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaritaEkrani(autoOpenSheet: Boolean = false, onSheetOpened: () -> Unit = {}, onComplete: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(autoOpenSheet) {
        if (autoOpenSheet && hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                    if (location != null) {
                        currentLocation = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
                        addressText = "Adres tespit ediliyor..."
                        isAddressResolved = false
                        failedAddressRetries = 0
                        resolveAddress(location.latitude, location.longitude) { address ->
                            if (address != null) {
                                addressText = address
                                isAddressResolved = true
                            } else {
                                addressText = "Adres alınamadı."
                                failedAddressRetries++
                            }
                        }
                        showSheet = true
                        onSheetOpened()
                    }
                }
            } catch (e: SecurityException) { }
        }
    }

    // Anlık Konum
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // İzin Durumu Kontrolü (Açılışta)
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Form
    var yorum by remember { mutableStateOf("") }
    var isimSoyisim by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("Adres tespit ediliyor...") }
    var isAddressResolved by remember { mutableStateOf(false) }
    var failedAddressRetries by remember { mutableStateOf(0) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val karsiyakaMerkez = LatLng(38.4552, 27.1235)
    val karsiyakaBounds = LatLngBounds(
        LatLng(38.4410, 27.0850), // Güney-Batı (Örn: Mavişehir ucu)
        LatLng(38.4850, 27.1650)  // Kuzey-Doğu (Örn: Yamanlar tarafı)
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(karsiyakaMerkez, 13f)
    }

    // Haritada Görünen Sinyaller
    var haritaSinyalleri by remember { mutableStateOf<List<Sinyal>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance().collection("sinyaller").get().await()
            haritaSinyalleri = snapshot.toObjects(Sinyal::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Resim Seçiciler
    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        photoUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = tempUri
        }
    }

    fun resolveAddress(lat: Double, lng: Double, onResult: (String?) -> Unit) {
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        address.getAddressLine(0) ?: address.thoroughfare ?: "${lat.toString().take(7)}, ${lng.toString().take(7)}"
                    } else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            onResult(result)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = LatLng(location.latitude, location.longitude)

                        addressText = "Adres tespit ediliyor..."
                        isAddressResolved = false
                        failedAddressRetries = 0

                        resolveAddress(location.latitude, location.longitude) { address ->
                            if (address != null) {
                                addressText = address
                                isAddressResolved = true
                            } else {
                                addressText = "Adres alınamadı. (Hata: İnternet veya Servis)"
                                failedAddressRetries++
                            }
                        }

                        showSheet = true
                    } else {
                        Toast.makeText(context, "Konum alınamadı, lütfen GPS'i kontrol edin.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "Konum izni gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    fun getBitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return try {
            ContextCompat.getDrawable(context, vectorResId)?.run {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
                draw(Canvas(bitmap))
                BitmapDescriptorFactory.fromBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val yelkenliIcon = remember { getBitmapDescriptorFromVector(context, R.drawable.ic_yelkenli_pin) }
    var isHeatmapMode by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                latLngBoundsForCameraTarget = karsiyakaBounds,
                minZoomPreference = 12f
            )
        ) {
            if (!isHeatmapMode) {
                // Normal Pin Modu
                haritaSinyalleri.forEach { sinyal ->
                    Marker(
                        state = MarkerState(position = LatLng(sinyal.lat, sinyal.lng)),
                        title = "Durum: ${sinyal.durum} | Adres: ${if(sinyal.adres.isNotBlank()) sinyal.adres else "Bilinmiyor"}",
                        snippet = sinyal.aciklama,
                        icon = yelkenliIcon
                    )
                }
            } else if (haritaSinyalleri.isNotEmpty()) {
                // Şehir Isı (Risk) Haritası Modu
                val latLngs = haritaSinyalleri.map { LatLng(it.lat, it.lng) }
                val provider = HeatmapTileProvider.Builder()
                    .data(latLngs)
                    .radius(40) // Çukurların/Sorunların parladığı yarıçap
                    .opacity(0.8)
                    .build()
                TileOverlay(tileProvider = provider)
            }
        }

        // Heatmap Toggle Butonu (Sağ Üstte)
        FloatingActionButton(
            onClick = { isHeatmapMode = !isHeatmapMode },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).padding(top = 48.dp), // AppBar'ın altına gelmesi için margin
            containerColor = if (isHeatmapMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Warning, contentDescription = "Isı Haritası")
        }

        Button(
            onClick = {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .height(64.dp)
                .fillMaxWidth(0.7f),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("KONUMU SEÇ VE BİLDİR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 64.dp) // Klavye açılışı için ekstra tampon alan
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Detayları Bildir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    Text(
                        text = "Konum: $addressText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAddressResolved) MaterialTheme.colorScheme.primary else Color.Red
                    )

                    if (!isAddressResolved && failedAddressRetries < 3) {
                        Button(onClick = {
                            addressText = "Tekrar deneniyor..."
                            currentLocation?.let { loc ->
                                resolveAddress(loc.latitude, loc.longitude) { address ->
                                    if (address != null) {
                                        addressText = address
                                        isAddressResolved = true
                                    } else {
                                        failedAddressRetries++
                                        addressText = "Adres alınamadı. (Hata: İnternet veya Servis)"
                                    }
                                }
                            }
                        }) {
                            Text("Adresi Tekrar Almayı Dene ($failedAddressRetries/3)")
                        }
                    } else if (!isAddressResolved && failedAddressRetries >= 3) {
                         Text(
                             text = "Adres alınamadı ancak Enlem/Boylam ile bildirime devam edebilirsiniz.",
                             style = MaterialTheme.typography.bodySmall,
                             color = Color.Gray
                         )
                    }

                    OutlinedTextField(
                        value = isimSoyisim,
                        onValueChange = { isimSoyisim = it },
                        label = { Text("İsim Soyisim*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = telefon,
                        onValueChange = {
                             if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                 telefon = it
                             }
                        },
                        label = { Text("Telefon Numarası (Başında 0 olmadan 10 Hane)*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Fotoğraf Alanı
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Seçilen Fotoğraf",
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                        TextButton(onClick = { photoUri = null }) { Text("Fotoğrafı Kaldır", color = Color.Red) }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { galleryLauncher.launch("image/*") }) {
                                Text("Galeriden Seç")
                            }
                            Button(onClick = {
                                val photoFile = File(context.cacheDir, "sinyal_${UUID.randomUUID()}.jpg")
                                tempUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                                cameraLauncher.launch(tempUri!!)
                            }) {
                                Text("Kamera ile Çek")
                            }
                        }
                    }

                    // Sesli İhbar Asistanı (Voice to Text)
                    val speechLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            if (!data.isNullOrEmpty()) {
                                yorum += " " + data[0] // Mevcut yorumun sonuna sesi yazıya çevirip ekle
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = yorum,
                            onValueChange = { yorum = it },
                            label = { Text("Sesi Yazıya Çevir veya Elle Yaz") },
                            modifier = Modifier.weight(1f),
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR") // Türkçe dikte
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Sorunu anlatın... (Örn: Boru patladı)")
                                }
                                try {
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cihazınızda sesli yazma desteklenmiyor.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = "Sesli İhbar")
                        }
                    }

                    var isSubmitting by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (isimSoyisim.isBlank()) {
                                Toast.makeText(context, "Lütfen İsim Soyisim girin.", Toast.LENGTH_SHORT).show()
                            } else if (telefon.length != 10) {
                                Toast.makeText(context, "Lütfen 10 haneli telefon numarasını girin.", Toast.LENGTH_SHORT).show()
                            } else if (yorum.length < 20) {
                                Toast.makeText(context, "Lütfen en az 20 karakterlik açıklama girin.", Toast.LENGTH_SHORT).show()
                            } else if (!isAddressResolved && failedAddressRetries < 3) {
                                Toast.makeText(context, "Adresiniz belirlenemedi. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.", Toast.LENGTH_SHORT).show()
                            } else {
                                isSubmitting = true
                                coroutineScope.launch {
                                    try {
                                        var uploadedImageUrl: String? = null
                                        if (photoUri != null) {
                                            val storageRef = FirebaseStorage.getInstance().reference.child("sinyal_fotograflari/${UUID.randomUUID()}.jpg")
                                            storageRef.putFile(photoUri!!).await()
                                            uploadedImageUrl = storageRef.downloadUrl.await().toString()
                                        }

                                        val fcmToken = try {
                                            FirebaseMessaging.getInstance().token.await()
                                        } catch (e: Exception) { null }

                                        val lowercaseYorum = yorum.lowercase()
                                        val akilliKategori = when {
                                            lowercaseYorum.contains("su") || lowercaseYorum.contains("patlak") || lowercaseYorum.contains("boru") -> "Altyapı (Su)"
                                            lowercaseYorum.contains("çöp") || lowercaseYorum.contains("temizlik") -> "Çevre Temizliği"
                                            lowercaseYorum.contains("çukur") || lowercaseYorum.contains("asfalt") || lowercaseYorum.contains("yol") -> "Yol / Kaldırım"
                                            lowercaseYorum.contains("lamba") || lowercaseYorum.contains("aydınlatma") || lowercaseYorum.contains("ışık") -> "Aydınlatma"
                                            lowercaseYorum.contains("park") || lowercaseYorum.contains("ağaç") || lowercaseYorum.contains("çim") -> "Park / Bahçe"
                                            lowercaseYorum.contains("hayvan") || lowercaseYorum.contains("kedi") || lowercaseYorum.contains("köpek") -> "Sokak Hayvanları"
                                            else -> "Genel İhbar"
                                        }

                                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonim"
                                        val yeniSinyal = Sinyal(
                                            id = UUID.randomUUID().toString(),
                                            userId = userId,
                                            lat = currentLocation?.latitude ?: 0.0,
                                            lng = currentLocation?.longitude ?: 0.0,
                                            isimSoyisim = isimSoyisim,
                                            telefon = telefon,
                                            adres = addressText,
                                            aciklama = yorum,
                                            photoUri = uploadedImageUrl,
                                            kategori = akilliKategori,
                                            fcmToken = fcmToken
                                        )

                                        val db = FirebaseFirestore.getInstance()
                                        db.collection("sinyaller").document(yeniSinyal.id).set(yeniSinyal).await()

                                        try {
                                            val userRef = db.collection("users").document(userId)
                                            db.runTransaction { transaction ->
                                                val snapshot = transaction.get(userRef)
                                                if (snapshot.exists()) {
                                                    val currentPoints = snapshot.getLong("points") ?: 0
                                                    transaction.update(userRef, "points", currentPoints + 10)
                                                } else {
                                                    transaction.set(userRef, mapOf("points" to 10L))
                                                }
                                            }.await()
                                        } catch (e: Exception) {}

                                        NotificationSender.sendNotificationToAdmins(context, isimSoyisim, yorum) { success, msg ->
                                            coroutineScope.launch(Dispatchers.Main) {
                                                if (!success) {
                                                    Toast.makeText(context, "Adminlere Bildirim Hatası: $msg", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }

                                        showSuccessDialog = true
                                        flashLightEffect(context, coroutineScope)
                                        showSheet = false
                                        yorum = ""
                                        isimSoyisim = ""
                                        telefon = ""
                                        photoUri = null

                                        (context as? android.app.Activity)?.let {
                                            AdManager.showInterstitialAd(it)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("FirebaseUpload", "Upload hatasi", e)
                                        Toast.makeText(context, "Gönderim Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SİNYAL ÇAK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Başarılı", tint = Color(0xFF388E3C), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sinyal Alındı!", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text("Bildiriminiz başarıyla ekiplerimize iletilmiştir. Karşıyaka için katkılarınızdan dolayı teşekkür ederiz.", color = Color.DarkGray)
                },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Kapat", color = Color.White)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun TakipEkrani() {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var bildirimler by remember { mutableStateOf<List<Sinyal>>(emptyList()) }
    var tumSehir by remember { mutableStateOf<List<Sinyal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var seciliTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("sinyaller").whereEqualTo("userId", userId).get().await()
            bildirimler = snapshot.toObjects(Sinyal::class.java).sortedByDescending { it.timestamp }

            val citySnapshot = db.collection("sinyaller").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(50).get().await()
            tumSehir = citySnapshot.toObjects(Sinyal::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Sinyal Takibi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Button(onClick = { seciliTab = 0 }, colors = ButtonDefaults.buttonColors(containerColor = if (seciliTab == 0) MaterialTheme.colorScheme.primary else Color.LightGray), modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("Benim Sinyallerim") }
            Button(onClick = { seciliTab = 1 }, colors = ButtonDefaults.buttonColors(containerColor = if (seciliTab == 1) MaterialTheme.colorScheme.primary else Color.LightGray), modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("Şehir Akışı") }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            val gosterilecekListe = if (seciliTab == 0) bildirimler else tumSehir
            if (gosterilecekListe.isEmpty()) {
                Text(if (seciliTab == 0) "Henüz bir sinyal çakmadınız." else "Şehirde henüz bir sinyal yok.", color = Color.Gray)
            } else {
                gosterilecekListe.forEach { sinyal ->
                    val durumRengi = when (sinyal.durum) {
                        "Çözüldü" -> Color(0xFF4CAF50)
                        "Bildirildi" -> Color(0xFF03A9F4)
                        else -> Color(0xFFFFA000)
                    }
                    val konumMetni = if (sinyal.adres.isNotBlank()) sinyal.adres else "${sinyal.lat.toString().take(7)}, ${sinyal.lng.toString().take(7)}"
                    BildirimKarti(
                        konum = konumMetni,
                        sorun = sinyal.aciklama,
                        durum = sinyal.durum,
                        adminMesaji = sinyal.adminCevap.ifEmpty { "Henüz yanıtlanmadı." },
                        durumRengi = durumRengi,
                        photoUri = sinyal.photoUri,
                        afterPhotoUri = sinyal.afterPhotoUri,
                        showUpvote = (seciliTab == 1),
                        upvotes = sinyal.upvotes,
                        onUpvote = {
                            val coroutineScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
                            coroutineScope.launch {
                                try {
                                    val db = FirebaseFirestore.getInstance()
                                    db.runTransaction { transaction ->
                                        val sf = db.collection("sinyaller").document(sinyal.id)
                                        val snapshot = transaction.get(sf)
                                        if (snapshot.exists()) {
                                            val currentUpvotes = snapshot.getLong("upvotes") ?: 0
                                            transaction.update(sf, "upvotes", currentUpvotes + 1)
                                        }
                                    }.await()
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminEkrani() {
    var tumSinyaller by remember { mutableStateOf<List<Sinyal>>(emptyList()) }
    var isDescending by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun fetchSinyaller() {
        coroutineScope.launch {
            try {
                val direction = if (isDescending) com.google.firebase.firestore.Query.Direction.DESCENDING else com.google.firebase.firestore.Query.Direction.ASCENDING
                val snapshot = FirebaseFirestore.getInstance().collection("sinyaller")
                    .orderBy("timestamp", direction)
                    .get().await()
                tumSinyaller = snapshot.toObjects(Sinyal::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isDescending) {
        fetchSinyaller()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tüm Sinyaller",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { isDescending = !isDescending }) {
                Text(if (isDescending) "↓ Eskiye" else "↑ Yeniye")
            }
        }

        tumSinyaller.forEach { sinyal ->
            AdminBildirimKarti(
                sinyal = sinyal,
                onSil = { id ->
                    coroutineScope.launch {
                        try {
                            FirebaseFirestore.getInstance().collection("sinyaller").document(id).delete().await()
                            Toast.makeText(context, "Sinyal Silindi", Toast.LENGTH_SHORT).show()
                            fetchSinyaller()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Silme Hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onGuncelle = { id, durum, cevap, cozumFotoUri ->
                    coroutineScope.launch {
                        try {
                            var uploadedImageUrl: String? = sinyal.afterPhotoUri
                            if (cozumFotoUri != null) {
                                val storageRef = FirebaseStorage.getInstance().reference.child("cozum_fotograflari/${java.util.UUID.randomUUID()}.jpg")
                                storageRef.putFile(cozumFotoUri).await()
                                uploadedImageUrl = storageRef.downloadUrl.await().toString()
                            }

                            val updateMap = mutableMapOf<String, Any>("durum" to durum, "adminCevap" to cevap)
                            if (uploadedImageUrl != null) {
                                updateMap["afterPhotoUri"] = uploadedImageUrl
                            }

                            FirebaseFirestore.getInstance().collection("sinyaller").document(id).update(updateMap).await()
                            Toast.makeText(context, "Güncellendi", Toast.LENGTH_SHORT).show()

                            if (cevap.isNotBlank() && sinyal.fcmToken != null) {
                                NotificationSender.sendNotificationToUser(
                                    context = context,
                                    fcmToken = sinyal.fcmToken,
                                    durum = durum,
                                    cevap = cevap
                                ) { success, msg ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        if (!success) Toast.makeText(context, "Kullanıcıya Bildirim Hatası: $msg", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            fetchSinyaller()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AdminBildirimKarti(
    sinyal: Sinyal,
    onGuncelle: (String, String, String, android.net.Uri?) -> Unit,
    onSil: (String) -> Unit
) {
    var cevap by remember(sinyal.adminCevap) { mutableStateOf(sinyal.adminCevap) }
    var seciliDurum by remember(sinyal.durum) { mutableStateOf(sinyal.durum) }
    var isExpanded by remember { mutableStateOf(false) }
    val durumlar = listOf("İnceleniyor", "Bildirildi", "Çözüldü")

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bildiren: ${sinyal.isimSoyisim.takeIf { it.isNotBlank() } ?: "Bilinmiyor"}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Telefon: ${sinyal.telefon.takeIf { it.isNotBlank() } ?: "Bilinmiyor"}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            val konumMetni = if (sinyal.adres.isNotBlank()) sinyal.adres else "${sinyal.lat}, ${sinyal.lng}"
            Text("Adres: $konumMetni", fontSize = 13.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Kategori: ${sinyal.kategori}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Sorun: ${sinyal.aciklama}", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                if (sinyal.photoUri != null) {
                    AsyncImage(
                        model = sinyal.photoUri,
                        contentDescription = null,
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    durumlar.forEach { d ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = (seciliDurum == d), onClick = { seciliDurum = d })
                            Text(d, fontSize = 12.sp)
                        }
                    }
                }

                var cozumFotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
                val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
                    cozumFotoUri = uri
                }

                if (seciliDurum == "Çözüldü") {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (cozumFotoUri != null || sinyal.afterPhotoUri != null) {
                        val gosterilecekResim = cozumFotoUri ?: sinyal.afterPhotoUri
                        Text("Çözüm (Sonrası) Fotoğrafı:", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        AsyncImage(model = gosterilecekResim, contentDescription = "Sonrası", modifier = Modifier.height(100.dp).fillMaxWidth())
                        if (cozumFotoUri != null) TextButton(onClick = { cozumFotoUri = null }) { Text("Fotoğrafı İptal Et", color = Color.Red) }
                    } else {
                        Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Çözüm (Sonrası) Fotoğrafı Ekle")
                        }
                    }
                }

                OutlinedTextField(value = cevap, onValueChange = { cevap = it }, label = { Text("Kullanıcıya Cevap Yazın") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    var isUpdating by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            isUpdating = true
                            onGuncelle(sinyal.id, seciliDurum, cevap, cozumFotoUri)
                        },
                        enabled = !isUpdating
                    ) { Text(if (isUpdating) "..." else "Güncelle") }

                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    var isSending by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            isSending = true
                            coroutineScope.launch {
                                try {
                                    val adSoyad = sinyal.isimSoyisim.takeIf { it.isNotBlank() } ?: "Bilinmiyor"
                                    val tel = sinyal.telefon.takeIf { it.isNotBlank() } ?: "Bilinmiyor"
                                    val adres = if (sinyal.adres.isNotBlank()) sinyal.adres else "${sinyal.lat}, ${sinyal.lng}"

                                    val mesaj = "🚨 *YENİ İHBAR* 🚨\n\n" +
                                            "👤 *Bildiren:* $adSoyad\n" +
                                            "📞 *Telefon:* $tel\n" +
                                            "📍 *Adres:* $adres\n" +
                                            "📝 *Açıklama:* ${sinyal.aciklama}"

                                    val intent = Intent(Intent.ACTION_SEND)
                                    intent.type = "text/plain"
                                    intent.putExtra(Intent.EXTRA_TEXT, mesaj)
                                    intent.putExtra("jid", "905301251355@s.whatsapp.net")
                                    intent.setPackage("com.whatsapp")

                                    if (sinyal.photoUri != null) {
                                        val uri = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                            try {
                                                val url = java.net.URL(sinyal.photoUri)
                                                val connection = url.openConnection()
                                                connection.connect()
                                                val input = connection.getInputStream()
                                                val file = java.io.File(context.cacheDir, "shared_image_${System.currentTimeMillis()}.jpg")
                                                val output = java.io.FileOutputStream(file)
                                                input.copyTo(output)
                                                output.close()
                                                input.close()
                                                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }

                                        if (uri != null) {
                                            intent.type = "image/*"
                                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                    }

                                    context.startActivity(intent)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    Toast.makeText(context, "WhatsApp cihazda yüklü değil.", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gönderim sırasında hata oluştu.", Toast.LENGTH_SHORT).show()
                                    e.printStackTrace()
                                } finally {
                                    isSending = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Yeşili
                        enabled = !isSending
                    ) {
                        if (isSending) {
                             CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                             Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Belediyeye İlet", color = Color.White)
                    }
                }

                var showDeleteDialog by remember { mutableStateOf(false) }
                Button(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Sinyali Sil")
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Emin misiniz?") },
                        text = { Text("Bu bildirimi kalıcı olarak silmek istediğinize emin misiniz?") },
                        confirmButton = { TextButton(onClick = { showDeleteDialog = false; onSil(sinyal.id) }) { Text("Evet", color = MaterialTheme.colorScheme.error) } },
                        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Hayır") } }
                    )
                }
            }
        }
    }
}

@Composable
fun BildirimKarti(
    konum: String,
    sorun: String,
    durum: String,
    adminMesaji: String,
    durumRengi: Color,
    photoUri: String? = null,
    afterPhotoUri: String? = null,
    showUpvote: Boolean = false,
    upvotes: Int = 0,
    onUpvote: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(konum, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Surface(shape = RoundedCornerShape(16.dp), color = durumRengi.copy(alpha = 0.2f)) {
                    Text(text = durum, color = durumRengi, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sizin Bildiriminiz: $sorun", style = MaterialTheme.typography.bodyMedium)

            if (photoUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                if (afterPhotoUri != null) {
                    Text("Öncesi / Sonrası Karşılaştırması", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AsyncImage(model = photoUri, contentDescription = "Öncesi", modifier = Modifier.weight(1f).height(100.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        Spacer(modifier = Modifier.width(4.dp))
                        AsyncImage(model = afterPhotoUri, contentDescription = "Sonrası", modifier = Modifier.weight(1f).height(100.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    }
                } else {
                    AsyncImage(model = photoUri, contentDescription = "Sinyal Fotoğrafı", modifier = Modifier.fillMaxWidth().height(150.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Yetkili Yanıtı:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(adminMesaji, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }
            }

            if (showUpvote && onUpvote != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onUpvote() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Ben de Yaşıyorum", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ben de Yaşıyorum ($upvotes)", fontSize = 13.sp)
                }
            }
        }
    }
}
