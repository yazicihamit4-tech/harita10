const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

// Node ortamında Service Account bilgisini saklamak için en iyi yol Environment Variable'dır.
// Ancak kolaylık olması açısından siz bu projeyi canlıya alırken serviceAccountKey.json dosyanızı
// backend klasörüne atıp veya aşağıda process.env.GOOGLE_APPLICATION_CREDENTIALS kullanabilirsiniz.
// Eğer dosyadan okumak isterseniz `var serviceAccount = require("./serviceAccountKey.json");` şeklinde yapabilirsiniz.

// Şimdilik varsayılan uygulama kimlik bilgilerini kullanarak başlatalım
// (Render/Heroku/Google Cloud gibi yerlerde Env var. tanımlarsanız otomatik algılar)
try {
  admin.initializeApp({
      credential: admin.credential.applicationDefault()
      // Eğer JSON dosyası kullanacaksanız üst satırı yoruma alıp alt satırı açın:
      // credential: admin.credential.cert(require('./izmirharita-firebase-adminsdk.json'))
  });
} catch (e) {
  console.log("Firebase Admin başlatılamadı:", e.message);
}

// 1. Endpoint: Kullanıcıya Bildirim Gönder (Admin cevap verdiğinde tetiklenir)
app.post('/notify-user', async (req, res) => {
    const { token, durum, cevap } = req.body;

    if (!token) return res.status(400).send({ error: "FCM token eksik." });

    const message = {
        notification: {
            title: `Sinyalinizin Durumu Güncellendi: ${durum}`,
            body: `Yetkili Yanıtı: ${cevap}`
        },
        token: token
    };

    try {
        const response = await admin.messaging().send(message);
        res.status(200).send({ success: true, response });
    } catch (error) {
        console.error("Kullanıcıya bildirim hatası:", error);
        res.status(500).send({ error: error.message });
    }
});

// 2. Endpoint: Adminlere Bildirim Gönder (Kullanıcı sinyal çaktığında tetiklenir)
app.post('/notify-admin', async (req, res) => {
    const { isim, mesaj } = req.body;

    try {
        // Firestore'dan admin tokenlarını çekiyoruz
        const snapshot = await admin.firestore().collection('admin_tokens').get();
        const tokens = snapshot.docs.map(doc => doc.data().token).filter(t => t);

        if (tokens.length === 0) {
            return res.status(200).send({ success: true, message: "Kayıtlı admin bulunamadı." });
        }

        const payload = {
            notification: {
                title: "🚨 Yeni Sinyal Çakıldı",
                body: `${isim}: ${mesaj}`
            },
            tokens: tokens
        };

        const response = await admin.messaging().sendMulticast(payload);
        res.status(200).send({ success: true, response });
    } catch (error) {
        console.error("Admine bildirim hatası:", error);
        res.status(500).send({ error: error.message });
    }
});

app.get('/', (req, res) => res.send('Izmir Harita Notification Backend API Çalışıyor'));

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`Sunucu 0.0.0.0:${PORT} portunda çalışıyor. Ağdaki cihazlar erişebilir.`));
